package model.helper.watermarks;

import model.logger.ErrorLogger;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

public class WatermarkVideoHelper {

    /**
     * GPU encoder candidates tried in order. Falls back to software h264 if none work.
     * <p>h264_nvenc — NVIDIA CUDA</p>
     * <p>h264_amf — AMD VCE<p/>
     * <p>h264_qsv — Intel Quick Sync</p>
     */
    private static final String[] GPU_ENCODERS = {"h264_nvenc", "h264_amf", "h264_qsv"};

    public static Optional<BufferedImage> getRandomImageFromVideo(File videoFile) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile);
             Java2DFrameConverter converter = new Java2DFrameConverter()) {
            grabber.start();

           long duration = grabber.getLengthInTime();

            long randomTime = (long) (Math.random() * duration);

            grabber.setTimestamp(randomTime);

            Frame frame = grabber.grabImage();

            if (frame != null) {
                return Optional.of(converter.convert(frame));
            }

            grabber.stop();
        } catch (Exception e) {
            ErrorLogger.log(2002, ErrorLogger.Level.ERROR, "Failed to grab image from video", e);
        }
        return Optional.empty();
    }

    public static boolean applyWatermark(File srcFile, File outputFile, WatermarkSettings settings,
                                          String format, LongConsumer progressCallback,
                                          AtomicBoolean cancelFlag) {
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
            grabber = new FFmpegFrameGrabber(srcFile.getAbsolutePath());
            grabber.start();

            long totalFrames = grabber.getLengthInFrames();

            recorder = buildRecorder(grabber, outputFile, format);
            recorder.start();

            WatermarkOverlayCache overlayCache = WatermarkOverlayCache.build(
                    settings, grabber.getImageWidth(), grabber.getImageHeight());

            boolean completed = processFrames(grabber, recorder, converter, overlayCache,
                    totalFrames, progressCallback, cancelFlag);

            if (completed) {
                ErrorLogger.info("Watermark applied successfully!");
            }
            return completed;
        } catch (Exception e) {
            ErrorLogger.log(2002, ErrorLogger.Level.ERROR, "Failed to apply watermark to video", e);
            return false;
        } finally {
            try { if (recorder != null) recorder.stop(); } catch (Exception ignored) {}
            try { if (grabber  != null) grabber.stop();  } catch (Exception ignored) {}
        }
    }

    /**
     * Creates and fully configures an {@link FFmpegFrameRecorder} based on the source grabber's
     * stream parameters (resolution, frame rate, audio/video bitrates, sample rate).
     * Attempts GPU-accelerated H.264 encoding first; falls back to software H.264 if unavailable.
     */
    private static FFmpegFrameRecorder buildRecorder(FFmpegFrameGrabber grabber,
                                                     File outputFile,
                                                     String format) {
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                outputFile,
                grabber.getImageWidth(),
                grabber.getImageHeight(),
                grabber.getAudioChannels());

        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setFormat(format);
        recorder.setSampleRate(grabber.getSampleRate());
        long videoBitrate = grabber.getVideoBitrate();
        recorder.setVideoBitrate(videoBitrate > 0 ? (int) videoBitrate : 8_000_000);
        long audioBitrate = grabber.getAudioBitrate();
        if (audioBitrate > 0) recorder.setAudioBitrate((int) audioBitrate);

        String chosenEncoder = tryConfigureGpuEncoder(recorder);
        if (chosenEncoder == null) {
            ErrorLogger.info("No GPU encoder available — using software h264 (preset=fast).");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setVideoOption("crf", "18");
            recorder.setVideoOption("preset", "fast");
        } else {
            ErrorLogger.info("GPU encoder selected: " + chosenEncoder);
        }

        return recorder;
    }

    /**
     * Iterates over all frames from {@code grabber}, stamps the watermark overlay onto video
     * frames, and writes every frame to {@code recorder}.
     *
     * @return {@code true} if all frames were processed, {@code false} if canceled.
     */
    private static boolean processFrames(FFmpegFrameGrabber grabber,
                                         FFmpegFrameRecorder recorder,
                                         Java2DFrameConverter converter,
                                         WatermarkOverlayCache overlayCache,
                                         long totalFrames,
                                         LongConsumer progressCallback,
                                         AtomicBoolean cancelFlag) throws Exception {
        Frame frame;
        long frameIndex = 0;
        while ((frame = grabber.grab()) != null) {
            if (cancelFlag != null && cancelFlag.get()) {
                ErrorLogger.info("Watermark processing cancelled by user.");
                return false;
            }
            if (frame.image != null) {
                BufferedImage bgrFrame = converter.getBufferedImage(frame);
                applyOverlayToBGR(bgrFrame, overlayCache);
                recorder.record(converter.getFrame(bgrFrame));
                frameIndex++;
                if (progressCallback != null && totalFrames > 0) {
                    progressCallback.accept(frameIndex * 100 / totalFrames);
                }
            } else {
                recorder.record(frame);
            }
        }
        return true;
    }

    /**
     * Attempt to configure the recorder with an available GPU encoder.
     *
     * @return the encoder name that succeeded or null if none are available.
     */
    private static String tryConfigureGpuEncoder(FFmpegFrameRecorder recorder) {
        for (String encoderName : GPU_ENCODERS) {
            try {
                int codecId = org.bytedeco.ffmpeg.global.avcodec.avcodec_find_encoder_by_name(encoderName) != null
                        ? org.bytedeco.ffmpeg.global.avcodec.avcodec_find_encoder_by_name(encoderName).id()
                        : -1;
                if (codecId < 0) continue;

                recorder.setVideoCodecName(encoderName);
                switch (encoderName) {
                    case "h264_nvenc" -> {
                        recorder.setVideoOption("rc", "vbr");
                        recorder.setVideoOption("cq", "20");
                        recorder.setVideoOption("preset", "p4");
                    }
                    case "h264_amf" -> recorder.setVideoOption("quality", "balanced");
                    case "h264_qsv" -> {
                        recorder.setVideoOption("global_quality", "20");
                        recorder.setVideoOption("preset", "medium");
                    }
                }
                return encoderName;
            } catch (Exception ignored) {
                // encoder is not available on this system — try next
            }
        }
        return null;
    }

    /**
     * Apply pre-rendered ARGB overlay directly onto a BGR BufferedImage
     * without converting its pixel format (avoids the red-channel swap bug).
     */
    private static void applyOverlayToBGR(BufferedImage bgrFrame, WatermarkOverlayCache cache) {
        if (cache == null || cache.overlay() == null) return;
        Graphics2D g2d = bgrFrame.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(cache.overlay(), 0, 0, null);
        } finally {
            g2d.dispose();
        }
    }
}



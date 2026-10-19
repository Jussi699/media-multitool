package model.utility.media;

import model.logger.ErrorLogger;
import model.utility.io.FileUtils;
import ws.schild.jave.Encoder;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.progress.EncoderProgressListener;

import java.io.File;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.io.PathWorker.IO_EXECUTOR;

public class EncoderUtility {
    private EncoderUtility() {
        /* This utility class should not be instantiated */
    }

    public static void abortEncoding(Encoder encoder, File file){
        encoder.abortEncoding();

        if (isNull(file)) return;

        IO_EXECUTOR.submit(() -> {
            ErrorLogger.info("Attempting to delete partial file: " + file.getName());
            int attempts = 0;
            boolean deleted = false;

            while (attempts < 10 && !deleted) {
                try {
                    Thread.sleep(500 + (attempts * 200L));
                    if (!file.exists()) {
                        deleted = true;
                        break;
                    }
                    if (FileUtils.cleanupFile(file, false)) {
                        ErrorLogger.info("Successfully deleted partial file after cancellation.");
                        deleted = true;
                    } else {
                        attempts++;
                        ErrorLogger.warn("Delete attempt " + attempts + " failed. File might be locked.");
                    }
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!deleted && file.exists()) {
                ErrorLogger.error("Could not delete partial file after 10 attempts: " + file.getAbsolutePath());
            }
        });
    }

    public static EncoderProgressListener getEncoderProgressListener(DoubleConsumer progressConsumer) {
        return new EncoderProgressListener() {
            @Override
            public void sourceInfo(MultimediaInfo info) {
                ErrorLogger.info("Source info: " + info);
            }


            @Override
            public void progress(int permille) {
                if (nonNull(progressConsumer)) {
                    progressConsumer.accept(permille / 1000.0);
                }
            }

            @Override
            public void message(String message) {
                ErrorLogger.info("FFmpeg: " + message);
            }
        };
    }
}

package model.utility;

import javafx.scene.control.ComboBox;
import ws.schild.jave.info.MultimediaInfo;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class Parsers {
    private Parsers() {
        /* This utility class should not be instantiated */
    }

    public static int parseComboBoxStringToInt(ComboBox<String> cb) {
        return Integer.parseInt(cb.getValue().replaceAll("[^0-9]", ""));
    }

    public static int parseChannels(MultimediaInfo info) {
        return (nonNull(info) && nonNull(info.getAudio())) ? info.getAudio().getChannels() : -1;
    }

    public static int parseSamplingRate(MultimediaInfo info) {
        return (nonNull(info) && nonNull(info.getAudio())) ? info.getAudio().getSamplingRate() : -1;
    }

    public static int parseBitrate(MultimediaInfo info) {
        if (isNull(info)) return -1;
        if (nonNull(info.getVideo()) && info.getVideo().getBitRate() > 0) return info.getVideo().getBitRate() / 1000;
        if (nonNull(info.getAudio()) && info.getAudio().getBitRate() > 0) return info.getAudio().getBitRate() / 1000;
        return -1;
    }

    public static int parseVideoBitrate(MultimediaInfo info) {
        if (nonNull(info) && nonNull(info.getVideo()) && info.getVideo().getBitRate() > 0) {
            return info.getVideo().getBitRate() / 1000;
        }
        return -1;
    }

    public static int parseAudioBitrate(MultimediaInfo info) {
        if (nonNull(info) && nonNull(info.getAudio()) && info.getAudio().getBitRate() > 0) {
            return info.getAudio().getBitRate() / 1000;
        }
        return -1;
    }

    public static Optional<String> parseResolution(MultimediaInfo info) {
        if (nonNull(info) && nonNull(info.getVideo()) && nonNull(info.getVideo().getSize())) {
            return Optional.of(info.getVideo().getSize().getWidth() + "x" + info.getVideo().getSize().getHeight());
        }
        return Optional.empty();
    }

    public static int parseFps(MultimediaInfo info) {
        return (nonNull(info) && nonNull(info.getVideo())) ? (int) info.getVideo().getFrameRate() : -1;
    }
}

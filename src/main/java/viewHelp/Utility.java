package viewHelp;

import model.logger.ErrorLogger;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.Optional;

public class Utility {
    public static Optional<MultimediaInfo> getMetadata(File file) {
        if (file == null || !file.exists()) return Optional.empty();
        try {
            return Optional.of(new MultimediaObject(file).getInfo());
        } catch (Exception e) {
            ErrorLogger.log(111, ErrorLogger.Level.ERROR, "Failed to get metadata", e);
            return Optional.empty();
        }
    }

    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}

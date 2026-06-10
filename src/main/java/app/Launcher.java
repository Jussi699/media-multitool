package app;

import javafx.application.Application;
import model.utility.Util;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Launcher {
    static void main(String[] args) {
        Logger jaudiotaggerLogger = Logger.getLogger("org.jaudiotagger");
        jaudiotaggerLogger.setLevel(Level.OFF);
        jaudiotaggerLogger.setUseParentHandlers(false);

        String logDir = Util.getAppConfigDir() + File.separator + "logs";
        System.setProperty("LOG_DIR", logDir);

        String ffmpegPath = System.getenv("FFMPEG_PATH");
        if (ffmpegPath != null && !ffmpegPath.isEmpty()) {
            System.setProperty("jave.ffmpeg.executable", ffmpegPath);
        }

        Application.launch(ConverterApp.class, args);
    }
}

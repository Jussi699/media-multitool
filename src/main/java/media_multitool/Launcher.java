package media_multitool;

import javafx.application.Application;
import model.utility.Util;
import java.io.File;

public class Launcher {
    public static void main(String[] args) {
        String logDir = Util.getAppConfigDir() + File.separator + "logs";
        System.setProperty("LOG_DIR", logDir);

        String ffmpegPath = System.getenv("FFMPEG_PATH");
        if (ffmpegPath != null && !ffmpegPath.isEmpty()) {
            System.setProperty("jave.ffmpeg.executable", ffmpegPath);
        }
        
        Application.launch(ConverterApp.class, args);
    }
}

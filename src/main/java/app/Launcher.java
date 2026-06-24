package app;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import javafx.application.Application;
import model.logger.ErrorLogger;
import model.utility.OS;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Launcher {
    static void main(String[] args) {
        Logger jaudiotaggerLogger = Logger.getLogger("org.jaudiotagger");
        jaudiotaggerLogger.setLevel(Level.OFF);
        jaudiotaggerLogger.setUseParentHandlers(false);

        String logDir = OS.getAppConfigDir() + File.separator + "logs";
        File logDirFile = new File(logDir);
        if (!logDirFile.exists()) {
            logDirFile.mkdirs();
        }
        System.setProperty("LOG_DIR", logDir);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        try {
            configurator.doConfigure(Objects.requireNonNull(Launcher.class.getClassLoader().getResource("logback.xml")));
        } catch (JoranException e) {
            ErrorLogger.error(e.getMessage());
        }

        String ffmpegPath = System.getenv("FFMPEG_PATH");
        if (ffmpegPath != null && !ffmpegPath.isEmpty()) {
            System.setProperty("jave.ffmpeg.executable", ffmpegPath);
        }

        Application.launch(MediaMultitoolApp.class, args);
    }
}
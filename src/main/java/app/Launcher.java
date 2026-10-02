package app;

import javafx.application.Application;
import lombok.Getter;
import model.utility.OS;
import org.bytedeco.ffmpeg.global.avutil;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.nonNull;

public class Launcher {
    @Getter private static boolean canWriteInLog = true;

    static void main(String[] args) {
        Logger jaudiotaggerLogger = Logger.getLogger("org.jaudiotagger");
        jaudiotaggerLogger.setLevel(Level.OFF);
        jaudiotaggerLogger.setUseParentHandlers(false);

        ImageIO.setUseCache(false);

        // Suppress FFmpeg native log output (Input #0, Output #0, avformat_open_input info, etc.)
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);

        String logDir = OS.getAppConfigDir() + File.separator + "logs";
        File logDirFile = new File(logDir);

        if (!logDirFile.exists()) {
            logDirFile.mkdirs();
        }

        System.setProperty("LOG_DIR", logDir);

        Exception writeError = checkLogWritable(logDirFile);

        if (nonNull(writeError)) {
            canWriteInLog = false;
            File fallbackLogFile = new File(logDirFile, "logback.log");
            try (PrintWriter writer = new PrintWriter(new FileWriter(fallbackLogFile, true))) {
                writer.println(LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        + " - Logback cannot write logs to app.log:");
                writeError.printStackTrace(writer);
                writer.flush();
            } catch (IOException _) {
                // Ignored and continue
            }
        }

        String ffmpegPath = System.getenv("FFMPEG_PATH");
        if (nonNull(ffmpegPath) && !ffmpegPath.isEmpty()) {
            System.setProperty("jave.ffmpeg.executable", ffmpegPath);
        }

        Application.launch(MediaMultitoolApp.class, args);
    }

    private static Exception checkLogWritable(File logDirFile) {
        String todayLogName = "app." + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log";
        File checkFile = new File(logDirFile, todayLogName);

        if (checkFile.exists()) {
            if (!checkFile.canWrite()) {
                return new IOException("File 'app.log' is not writable (Read-only attribute is set).");
            }
        } else {
            if (!logDirFile.canWrite()) {
                return new IOException("Log directory '" + logDirFile.getAbsolutePath() + "' is not writable.");
            }
            try {
                if (!checkFile.createNewFile() && !checkFile.canWrite()) {
                    return new IOException("Failed to create or write to 'app.log'.");
                }
            } catch (IOException | SecurityException e) {
                return e;
            }
        }
        return null;
    }
}

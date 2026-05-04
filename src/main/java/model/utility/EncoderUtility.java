package model.utility;

import model.logger.ErrorLogger;
import ws.schild.jave.Encoder;

import java.io.File;

import static model.utility.Util.IO_EXECUTOR;

public class EncoderUtility {
    public static void abortEncoding(Encoder encoder, File file){
        encoder.abortEncoding();

        if (file == null) return;

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
                    if (file.delete()) {
                        ErrorLogger.info("Successfully deleted partial file after cancellation.");
                        deleted = true;
                    } else {
                        attempts++;
                        ErrorLogger.warn("Delete attempt " + attempts + " failed. File might be locked.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!deleted && file.exists()) {
                ErrorLogger.error("Could not delete partial file after 10 attempts: " + file.getAbsolutePath());
            }
        });
    }
}

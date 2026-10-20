package model.utility.io;

import model.logger.ErrorLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class EnvironmentSetup {
    private EnvironmentSetup() {
        /* This utility class should not be instantiated */
    }

    public static void setupPythonIfNeeded() {
        Path targetDir = AppPathManager.getBaseDir().resolve("python_script");
        File pythonExe = targetDir.resolve("python.exe").toFile();

        if (pythonExe.exists()) {
            return;
        }

        try (InputStream is = EnvironmentSetup.class.getResourceAsStream("/python_embedded.zip")) {
            if (isNull(is)) {
                throw new FileNotFoundException("Embedded Python archive '/python_embedded.zip' not found in resources.");
            }
            unzip(is, targetDir);
            ErrorLogger.info(EnvironmentSetup.class, "Python environment successfully unpacked to: " + targetDir);
        } catch (IOException e) {
            ErrorLogger.error(EnvironmentSetup.class, "Failed to unpack the Python environment archive", e);
        }
    }

    private static void unzip(InputStream is, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while (nonNull((entry = zis.getNextEntry()))) {
                Path resolvedPath = targetDir.resolve(entry.getName()).normalize();

                if (!resolvedPath.startsWith(targetDir)) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Path parent = resolvedPath.getParent();
                    if (nonNull(parent) && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zis, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}

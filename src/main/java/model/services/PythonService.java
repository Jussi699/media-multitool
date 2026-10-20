package model.services;

import model.enums.PythonScript;
import model.logger.ErrorLogger;
import model.utility.system.OS;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Objects.nonNull;

public class PythonService {
    private PythonService() {
        /* This utility class should not be instantiated */
    }

    private static final Path APP_DIR = Paths.get(OS.getAppConfigDir(), "python_script");

    public static void preparePython() throws IOException {
        Path pythonExePath = APP_DIR.resolve("python.exe");
        Path zipPath = APP_DIR.resolve("python_embedded.zip");

        if (Files.exists(pythonExePath)) {
            // Python is already unpacked. If the zip archive is still present, delete it.
            if (Files.exists(zipPath)) {
               tryToDeleteZip(zipPath);
            }
            return;
        }

        if (!Files.exists(APP_DIR)) {
            Files.createDirectories(APP_DIR);
        }

        if (!Files.exists(zipPath)) {
            throw new FileNotFoundException("Python executable not found at " + pythonExePath.toAbsolutePath()
                    + " and embedded Python archive not found at " + zipPath.toAbsolutePath());
        }

        try (InputStream is = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = APP_DIR.resolve(entry.getName()).normalize();
                if (!targetPath.startsWith(APP_DIR)) {
                    throw new IOException("Zip entry attempted directory traversal: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Path parent = targetPath.getParent();
                    if (nonNull(parent) && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    public static void tryToDeleteZip(@NonNull Path zipPath) {
        try {
            Files.deleteIfExists(zipPath);
            ErrorLogger.info(PythonService.class, "Embedded Python archive removed successfully: " + zipPath.getFileName());
        } catch (IOException e) {
            ErrorLogger.warn(PythonService.class, "Failed to remove embedded Python archive " + zipPath + ": " + e.getMessage());
        }
    }

    public static String runScript(PythonScript script, File inputFile, File outputFile, DoubleConsumer progressConsumer) throws IOException, InterruptedException {
        Objects.requireNonNull(inputFile, "Input file cannot be null");
        Objects.requireNonNull(outputFile, "Output file cannot be null");
        return runScript(script, inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), progressConsumer);
    }

    private static void validatePythonEnvironment(Path pythonExePath, Path scriptPath) throws FileNotFoundException {
        if (!Files.isRegularFile(pythonExePath)) {
            throw new FileNotFoundException("Python executable not found or not a file: " + pythonExePath.toAbsolutePath());
        }
        if (!Files.isRegularFile(scriptPath)) {
            throw new FileNotFoundException("Python script not found or not a file: " + scriptPath.toAbsolutePath());
        }
    }

    private static void validateInputFile(Path inputPath) throws FileNotFoundException {
        if (!Files.isRegularFile(inputPath)) {
            throw new FileNotFoundException("Input file not found: " + inputPath.toAbsolutePath());
        }
    }

    public static String runScript(
            @NonNull PythonScript script,
            @NonNull String inputFilePath,
            @NonNull String outputFilePath,
            @Nullable DoubleConsumer progressConsumer) throws IOException, InterruptedException {
        Objects.requireNonNull(script, "Python script cannot be null");
        Objects.requireNonNull(inputFilePath, "Input file path cannot be null");
        Objects.requireNonNull(outputFilePath, "Output file path cannot be null");

        preparePython();

        Path pythonExePath = APP_DIR.resolve("python.exe");
        Path scriptPath    = APP_DIR.resolve(script.getNameScript());
        Path inputPath     = Paths.get(inputFilePath);
        Path outputPath    = Paths.get(outputFilePath);

        File inputFile = new File(inputFilePath);

        validatePythonEnvironment(pythonExePath, scriptPath);
        validateInputFile(inputPath);

        Path parentDir = outputPath.getParent();
        if (nonNull(parentDir) && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        ErrorLogger.info(PythonService.class, """
                Python EXE: %s
                Script Path: %s
                Input: %s
                Output: %s
        """.formatted(pythonExePath, scriptPath, inputPath.toAbsolutePath(), outputPath.toAbsolutePath()));

        ProcessBuilder pb = new ProcessBuilder(
                pythonExePath.toString(),
                scriptPath.toString(),
                inputFile.getAbsolutePath(),
                outputPath.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (nonNull((line = reader.readLine()))) {
                    output.append(line).append(System.lineSeparator());
                    ErrorLogger.info(PythonService.class, "PYTHON LOG: " + line);

                    if (nonNull(progressConsumer) && line.startsWith("PROGRESS:")) {
                        try {
                            String val = line.substring("PROGRESS:".length()).trim();
                            double pct = Double.parseDouble(val);
                            progressConsumer.accept(Math.min(pct / 100.0, 1.0));
                        } catch (NumberFormatException _) {
                            // Ignored
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            String outputStr = output.toString();

            if (exitCode != 0) {
                String errorMsg = parseErrorMessage(outputStr, exitCode);
                ErrorLogger.error(PythonService.class,"Python script " + script.getNameScript() + " failed with exit code " + exitCode + ": " + errorMsg);
                throw new IOException(errorMsg);
            }

            ErrorLogger.info(PythonService.class,"Python script completed successfully: " + script.getNameScript());
            return outputStr;
        } catch (InterruptedException e) {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            throw e;
        }
    }

    private static String parseErrorMessage(String rawOutput, int exitCode) {
        if (nonNull(rawOutput) && !rawOutput.isBlank()) {
            Pattern pattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(rawOutput);
            if (matcher.find()) {
                return matcher.group(1);
            }
            String trimmed = rawOutput.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return switch (exitCode) {
            case 1 -> "Input file not found.";
            case 2 -> "Conversion error occurred during script execution.";
            case 3 -> "Required arguments (file paths) were not provided.";
            default -> "Python execution failed with exit code: " + exitCode;
        };
    }
}

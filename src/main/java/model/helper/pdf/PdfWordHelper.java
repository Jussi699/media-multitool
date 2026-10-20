package model.helper.pdf;

import javafx.concurrent.Task;
import model.enums.PythonScript;
import model.services.PythonService;
import model.utility.io.EnvironmentSetup;
import model.utility.io.FileUtils;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.io.PathWorker.createOutputFile;

public class PdfWordHelper {
    private PdfWordHelper() {
        /* Utility class should not be instantiated */
    }

    public static String getTargetExtension(PythonScript script) {
        if (isNull(script)) {
            return "docx";
        }
        return switch (script) {
            case PDF_TO_WORD -> "docx";
            case WORD_TO_PDF -> "pdf";
        };
    }

    public static File resolveOutputFile(@NonNull File inputFile, @NonNull File outputDestination, @NonNull String targetExtension) {
        Objects.requireNonNull(inputFile, "Input file cannot be null");
        Objects.requireNonNull(outputDestination, "Output destination cannot be null");

        String ext = !targetExtension.isBlank()
                ? targetExtension.replaceFirst("^\\.", "")
                : "docx";

        if (outputDestination.isDirectory()) {
            return createOutputFile(inputFile, outputDestination, ext);
        }

        if (!outputDestination.exists() && !outputDestination.getName().contains(".")) {
            outputDestination.mkdirs();
            return createOutputFile(inputFile, outputDestination, ext);
        }

        return outputDestination;
    }

    public static Task<File> createConversionTask(PythonScript script, File inputFile, File outputDestination) {
        return createConversionTask(script, inputFile, outputDestination, getTargetExtension(script));
    }

    public static Task<File> createConversionTask(PythonScript script, File inputFile, File outputDestination, String targetExtension) {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                File resolvedOutputFile = prepareOutputFile(inputFile, outputDestination, targetExtension);

                updateProgress(5.0, 100.0);
                checkCancelled(this::isCancelled, null);

                runConversionWithProgress(
                        script,
                        inputFile,
                        resolvedOutputFile,
                        progress -> updateProgress(progress, 100.0),
                        this::isCancelled
                );

                checkCancelled(this::isCancelled, resolvedOutputFile);
                validateOutputFile(resolvedOutputFile);

                updateProgress(100.0, 100.0);
                return resolvedOutputFile;
            }
        };
    }

    private static File prepareOutputFile(File inputFile, File outputDestination, String targetExtension) throws IOException {
        validateInputs(inputFile, outputDestination);
        File resolvedOutputFile = resolveOutputFile(inputFile, outputDestination, targetExtension);
        ensureOutputDirectoryWritable(resolvedOutputFile);
        return resolvedOutputFile;
    }

    private static void validateInputs(File inputFile, File outputDestination) throws FileNotFoundException {
        if (isNull(inputFile) || !inputFile.exists()) {
            throw new FileNotFoundException("Input file does not exist: " + (nonNull(inputFile) ? inputFile.getAbsolutePath() : "null"));
        }
        if (isNull(outputDestination)) {
            throw new IllegalArgumentException("The output destination cannot be null.");
        }
    }

    private static void ensureOutputDirectoryWritable(File outputFile) throws IOException {
        File outputDir = outputFile.getParentFile();
        if (nonNull(outputDir)) {
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            if (!outputDir.isDirectory() || !outputDir.canWrite()) {
                throw new IOException("The selected output directory is not writable: " + outputDir.getAbsolutePath());
            }
        }
    }

    private static void checkCancelled(BooleanSupplier isCancelledSupplier, File fileToCleanup) throws InterruptedException {
        if (isCancelledSupplier.getAsBoolean()) {
            if (nonNull(fileToCleanup)) {
                FileUtils.cleanupFile(fileToCleanup);
            }
            throw new InterruptedException("Conversion cancelled");
        }
    }

    private static void runConversionWithProgress(
            PythonScript script,
            File inputFile,
            File resolvedOutputFile,
            DoubleConsumer progressUpdater,
            BooleanSupplier isCancelledSupplier) throws IOException, InterruptedException {

        AtomicReference<Double> currentProgress = new AtomicReference<>(0.05);
        AtomicReference<Double> targetProgress  = new AtomicReference<>(0.10);

        try (ScheduledExecutorService progressTicker = createProgressTicker()) {
            progressTicker.scheduleAtFixedRate(() -> {
                if (isCancelledSupplier.getAsBoolean()) {
                    return;
                }
                double current = currentProgress.get();
                double target = targetProgress.get();
                double next = calculateNextProgress(current, target);

                currentProgress.set(next);
                progressUpdater.accept(Math.min(next * 100.0, 99.0));
            }, 100, 100, TimeUnit.MILLISECONDS);

            DoubleConsumer progressConsumer = pct -> {
                if (pct > 0) {
                    targetProgress.set(Math.min(pct, 0.95));
                }
            };

            try {
                PythonService.runScript(script, inputFile, resolvedOutputFile, progressConsumer);
            } catch (Exception ex) {
                progressTicker.shutdownNow();
                FileUtils.cleanupFile(resolvedOutputFile);
                throw ex;
            } finally {
                progressTicker.shutdownNow();
            }
        }
    }

    private static ScheduledExecutorService createProgressTicker() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Progress-Ticker");
            t.setDaemon(true);
            return t;
        });
    }

    private static double calculateNextProgress(double current, double target) {
        if (current < target) {
            return current + Math.max(0.005, (target - current) * 0.25);
        } else if (current < 0.92) {
            return current + (0.92 - current) * 0.015;
        } else {
            return current;
        }
    }

    private static void validateOutputFile(File resolvedOutputFile) throws IOException {
        if (!resolvedOutputFile.exists() || resolvedOutputFile.length() == 0) {
            FileUtils.cleanupFile(resolvedOutputFile);
            throw new IOException("Conversion failed: output file was not generated at " + resolvedOutputFile.getAbsolutePath());
        }
    }
}

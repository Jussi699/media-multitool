package model.converterImage;

import javafx.concurrent.Task;
import model.converterImage.strategy.ImageConversionStrategy;
import model.converterImage.strategy.ImageStrategyFactory;
import model.converterImage.strategy.IcoImageStrategy;
import java.io.File;
import java.util.List;

public class ConvertImageTask extends Task<Boolean> {
    private final List<File> files;
    private final File outputDir;
    private final String targetFormat;
    private final int icoSize;

    public ConvertImageTask(List<File> files, File outputDir, String targetFormat, int icoSize) {
        this.files = files;
        this.outputDir = outputDir;
        this.targetFormat = targetFormat;
        this.icoSize = icoSize;
    }

    @Override
    protected Boolean call() throws Exception {
        int total = files.size();
        int successCount = 0;

        for (int i = 0; i < total; i++) {
            if (isCancelled()) break;
            
            File file = files.get(i);
            updateMessage("Processing: " + file.getName());
            
            ImageConversionStrategy strategy = ImageStrategyFactory.getStrategy(file, targetFormat);
            if (strategy instanceof IcoImageStrategy && icoSize > 0) {
                ((IcoImageStrategy) strategy).setSize(icoSize);
            }
            
            try {
                strategy.convert(file, outputDir, targetFormat);
                successCount++;
            } catch (Exception e) {
                if (total == 1) throw e;
            }
            
            updateProgress(i + 1, total);
        }
        return successCount > 0;
    }
}

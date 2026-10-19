package controllers.pdf;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import controllers.abstractControllers.AbstractPdfToolController;
import model.utility.checks.Checking;
import model.helper.pdf.PdfHelper;
import model.logger.ErrorLogger;
import model.utility.context.ResetContext;
import model.utility.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import visual.dialogs.InfoAlert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.io.PathWorker.createOutputFile;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.*;

public class ConverterPdfToImageController extends AbstractPdfToolController {
    private final ToggleGroup toggleGroup = new ToggleGroup();

    @FXML private Button btnAllImageToPng, btnAllImageToJpeg;
    @FXML private ToggleButton btnToPNG, btnToJPEG, btnToWEBP;
    @FXML private ToggleButton btnToTIFF, btnToBMP, btnToPPM;
    @FXML private ToggleButton btnToPGM, btnToPAM;

    private boolean isExtract = false;

    private abstract class PdfConversionTask extends Task<File> {
        protected final void checkCancellation() throws InterruptedException {
            if (isCancelled() || cancelFlag.get()) {
                throw new InterruptedException("Conversion cancelled");
            }
        }

        protected final void reportProgress(long progress) {
            updateProgress(progress, 100);
        }
    }

    @FXML
    public void initialize() {
        initLists();

        properties.setOutput(getSavedPath());

        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);

        setupClearMessageTimer(labelSuccess, properties.getHideSuccessMessageTimer(), true);
        setupDragAndDrop(dropZone, "pdf", this::loadFile);
        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "PDF to Image",
                "1. Select a PDF file using \"Select PDF\" or drag and drop it into the dash-bordered zone;",
                """
                        3. Choose the desired image format ("to PNG", "to JPEG", etc.);

                        4. Click "Convert and Download" to convert the first page;
                        
                        5. (Optional) Click "Extract Jpeg And Download" / "Extract Png And Download" to extract all pages into a ZIP archive.
                       
                        Note: "Convert and Download" converts only the first page of the PDF.
                       
                        You can cancel the conversion at any time using the "Cancel Conversion" button.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        cancelProcessing();

        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true, "PDF"
        );
        reset(properties, ctx, "Selected PDF file: none");

        disableControls();

        setFormatTogglesDisabled(true);

        closeCurrentDoc();

        imageViewPdf.setImage(null);
        labelPreviewPlaceholder.setVisible(true);
        clearToggleGroup(toggleGroup);
        properties.setTypeImage(null);
        isExtract = false;
    }

    @FXML
    private void onChoiceFormat() {
        ToggleButton selectedBtn = (ToggleButton) toggleGroup.getSelectedToggle();
        if (nonNull(selectedBtn)) {
            String format = selectedBtn.getText().replace("to ", "").toLowerCase();
            properties.setTypeImage(format);
        }
    }

    @FXML
    private void onClickToExtractImages(ActionEvent event) {
        if (Checking.checkImageAndOutputOnNull(properties)) {
            return;
        }

        Button source = (Button) event.getSource();

        switch (source.getId()) {
            case "btnAllImageToPng"  -> properties.setTypeImage("png");
            case "btnAllImageToJpeg" -> properties.setTypeImage("jpeg");
            default -> throw new IllegalArgumentException("Unexpected id: " + source.getId());
        }

        cancelFlag.set(false);
        isExtract = true;
        executeMediaTask(taskConvertAllToZip());
        labelSuccess.setManaged(true);
    }

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(properties)) {
            return;
        }

        if (isNull(properties.getTypeImage()) || properties.getTypeImage().isEmpty()) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please select output format", properties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return;
        }

        cancelFlag.set(false);
        executeMediaTask(taskConvert());
        labelSuccess.setManaged(true);
    }

    @Override
    protected void loadFile(File file) {
        if (!validateSelectedFile(file)) {
            return;
        }

        closeCurrentDoc();
        properties.setImage(file);
        labelSelectFileName.setText("Selected PDF: " + file.getName());

        markDropZoneLoaded(dropZone, textDragZone, file.getName());
        bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);

        try {
            currentDoc = PdfHelper.loadUnencryptedPdf(file);
            updatePreview();

            enableControls();

            setFormatTogglesDisabled(false);
        } catch (IOException e) {
            boolean encrypted = PdfHelper.isEncryptedException(e);
            if (encrypted) {
                ErrorLogger.error("Encrypted PDF rejected: " + e.getMessage());
                handleEncryptedPdf(e);
            } else {
                handlePdfLoadError(e);
            }
        }
    }

    @Override
    protected void toggleAdditionalControls(boolean disabled) {
        setControlsDisabled(disabled, btnAllImageToJpeg, btnAllImageToPng);
        setFormatTogglesDisabled(disabled);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (isNull(result) || Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;

        String message = isExtract ? "Images saved to ZIP!" : "Image saved!";
        ErrorLogger.info("PDF to image conversion successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, message, properties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            progressBar.setProgress(0);
        });

        isExtract = false;
    }

    @Override
    protected void handleTaskCancelled() {
        super.handleTaskCancelled();
        isExtract = false;
    }

    private File convertAllPagesToZip(PdfConversionTask task) throws IOException, InterruptedException {
        task.checkCancellation();
        PDDocument document = requireCurrentDocument();
        int totalPages = document.getNumberOfPages();
        if (totalPages == 0) {
            throw new IOException("PDF document has no pages");
        }

        final String baseName = properties.getImage().getName().replaceFirst("[.][^.]+$", "");

        File zipFile = createOutputFile(
                properties.getImage(),
                properties.getOutput(),
                "_images",
                "zip"
        );

        String targetFormat = properties.getTypeImage();
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile))) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < totalPages; page++) {
                task.checkCancellation();

                BufferedImage image = renderer.renderImageWithDPI(page, 300);
                String entryName = baseName + "_page_" + (page + 1) + "." + targetFormat;
                zip.putNextEntry(new ZipEntry(entryName));
                writeImage(image, getImageIoFormat(targetFormat), zip);
                zip.closeEntry();

                task.reportProgress((page + 1L) * 95 / totalPages);
            }

            task.checkCancellation();
            task.reportProgress(100);
            return zipFile;
        } catch (Exception e) {
            FileUtils.cleanupFile(zipFile);
            throw e;
        }
    }

    private File convertFirstPage(PdfConversionTask task) throws IOException, InterruptedException {
        task.reportProgress(10);
        task.checkCancellation();

        PDFRenderer renderer = new PDFRenderer(requireCurrentDocument());
        task.reportProgress(30);
        BufferedImage image = renderer.renderImageWithDPI(0, 300);

        task.reportProgress(60);
        task.checkCancellation();

        File outputFile = createOutputFile(
                properties.getImage(),
                properties.getOutput(),
                "converted",
                properties.getTypeImage()
        );

        try {
            writeImage(image, getImageIoFormat(properties.getTypeImage()), outputFile);
            task.checkCancellation();
            task.reportProgress(100);
            return outputFile;
        } catch (Exception e) {
            FileUtils.cleanupFile(outputFile);
            throw e;
        }
    }

    private Task<File> taskConvert() {
        return new PdfConversionTask() {
            @Override
            protected File call() throws Exception {
                return convertFirstPage(this);
            }
        };
    }

    private Task<File> taskConvertAllToZip() {
        return new PdfConversionTask() {
            @Override
            protected File call() throws Exception {
                return convertAllPagesToZip(this);
            }
        };
    }

    private PDDocument requireCurrentDocument() throws IOException {
        if (isNull(currentDoc)) {
            throw new IOException("PDF document not loaded");
        }
        return currentDoc;
    }

    private String getImageIoFormat(String targetFormat) {
        return targetFormat.equalsIgnoreCase("JPEG") || targetFormat.equalsIgnoreCase("JPG")
                ? "jpg" : targetFormat;
    }

    private void writeImage(BufferedImage image, String format, File outputFile) throws IOException {
        if (!ImageIO.write(image, format, outputFile)) {
            throw new IOException("No image writer available for format: " + format);
        }
    }

    private void writeImage(BufferedImage image, String format, ZipOutputStream output) throws IOException {
        OutputStream nonClosingOutput = new FilterOutputStream(output) {
            @Override
            public void close() throws IOException {
                flush();
            }
        };
        if (!ImageIO.write(image, format, nonClosingOutput)) {
            throw new IOException("No image writer available for format: " + format);
        }
    }

    private void initLists() {
        configureToggleGroup(toggleGroup, btnToPNG, btnToJPEG, btnToWEBP, btnToTIFF,
                btnToBMP, btnToPPM, btnToPGM, btnToPAM);
    }

    private void setFormatTogglesDisabled(boolean disabled) {
        toggleGroup.getToggles().forEach(toggle -> {
            if (toggle instanceof Control control) {
                control.setDisable(disabled);
            }
        });
    }
}

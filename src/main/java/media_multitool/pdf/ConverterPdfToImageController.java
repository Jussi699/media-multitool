package media_multitool.pdf;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.checks.Checking;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;
import viewHelp.Utility;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class ConverterPdfToImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private ImageView imageViewPdf;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit, btnAllImageToPng, btnAllImageToJpeg, btnCancel;
    @FXML private Label labelSelectFileName, textDragZone, labelPreviewPlaceholder;
    @FXML private ToggleButton btnToPNG, btnToJPEG, btnToWEBP, btnToTIFF, btnToBMP, btnToPPM, btnToPGM, btnToPAM;

    private PDDocument currentDoc;
    private ToggleGroup toggleGroup;
    private List<Control> listControls;
    private List<ToggleButton> listToggleBtn;
    private boolean isExtract = false;

    @FXML
    public void initialize() {
        toggleGroup = new ToggleGroup();

        initLists();

        imageProperties.setOutput(getSavedPath());

        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0);
        }

        setupClearMessageTimer(labelSuccess, imageProperties.getHideSuccessMessageTimer(), true);

        setupDragAndDrop(dropZone, List.of("pdf"), this::loadFile);

        isPressedReset();
    }

    private void initLists() {
        listToggleBtn = List.of(btnToPNG, btnToJPEG, btnToWEBP, btnToTIFF, btnToBMP, btnToPPM, btnToPGM, btnToPAM);

        listToggleBtn.forEach(tb -> tb.setToggleGroup(toggleGroup));

        List<Control> temp = new ArrayList<>(listToggleBtn);
        temp.addAll(List.of(btnAllImageToJpeg, btnAllImageToPng, btnSubmit, btnReset));
        listControls = List.copyOf(temp);
    }

    @FXML
    private void onActionClickToggleBtnFormat() {
        ToggleButton selectedBtn = (ToggleButton) toggleGroup.getSelectedToggle();
        if (selectedBtn != null) {
            String format = selectedBtn.getText().replace("to ", "").toLowerCase();
            imageProperties.setTypeImage(format);
        }
    }

    @FXML
    private void onActionClickBtnToExtractImages(ActionEvent event) {
        if (Checking.checkImageAndOutputOnNull(imageProperties)) {
            return;
        }

        Button selectedBtn = (Button) event.getSource();

        switch (selectedBtn.getId()) {
            case "btnAllImageToPng"  -> imageProperties.setTypeImage("png");
            case "btnAllImageToJpeg" -> imageProperties.setTypeImage("jpeg");
        }

        cancelFlag.set(false);
        isExtract = true;
        executeMediaTask(taskConvertAllToZip());
        labelSuccess.setManaged(true);
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChoiceDirForSaveFile.setDisable(true);
        btnReset.setDisable(true);
        btnAllImageToJpeg.setDisable(true);
        btnAllImageToPng.setDisable(true);
        btnSubmit.setDisable(true);
        if (btnCancel != null) btnCancel.setDisable(false);
        listToggleBtn.forEach(tb -> tb.setDisable(true));
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceDirForSaveFile.setDisable(false);
        btnReset.setDisable(false);
        btnAllImageToJpeg.setDisable(false);
        btnAllImageToPng.setDisable(false);
        btnSubmit.setDisable(false);
        if (btnCancel != null) btnCancel.setDisable(true);
        listToggleBtn.forEach(tb -> tb.setDisable(false));
        cancelFlag.set(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
        if (btnCancel != null) btnCancel.setDisable(true);
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
        if (btnCancel != null) btnCancel.setDisable(true);
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectPdfFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectPdfFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")).ifPresent(this::loadFile);
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save images");
    }

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties)) {
            return;
        }

        if (imageProperties.getTypeImage() == null || imageProperties.getTypeImage().isEmpty()) {
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Please select output format", imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
            return;
        }

        cancelFlag.set(false);
        executeMediaTask(taskConvert());
        labelSuccess.setManaged(true);
    }

    private Task<File> taskConvert() {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                if (currentDoc == null) {
                    throw new IOException("PDF document not loaded");
                }
                if (isCancelled() || cancelFlag.get()) {
                    throw new InterruptedException("Conversion cancelled");
                }

                PDFRenderer renderer = new PDFRenderer(currentDoc);

                updateProgress(30, 100);
                if (isCancelled() || cancelFlag.get()) {
                    throw new InterruptedException("Conversion cancelled");
                }

                BufferedImage image = renderer.renderImageWithDPI(0, 300);

                updateProgress(60, 100);
                if (isCancelled() || cancelFlag.get()) {
                    throw new InterruptedException("Conversion cancelled");
                }

                File outputFile = createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        imageProperties.getTypeImage()
                );

                String format = imageProperties.getTypeImage().toUpperCase();
                if (format.equals("JPEG") || format.equals("JPG")) {
                    format = "jpg";
                }

                ImageIO.write(image, format, outputFile);

                if (isCancelled() || cancelFlag.get()) {
                    Utility.cleanupFile(outputFile);
                    throw new InterruptedException("Conversion cancelled");
                }

                updateProgress(100, 100);

                return outputFile;
            }
        };
    }

    private Task<File> taskConvertAllToZip() {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                if (currentDoc == null) {
                    throw new IOException("PDF document not loaded");
                }

                PDFRenderer renderer = new PDFRenderer(currentDoc);
                int totalPages = currentDoc.getNumberOfPages();
                List<File> tempFiles = new ArrayList<>();
                File zipFile = null;

                String baseName = imageProperties.getImage().getName().replaceFirst("[.][^.]+$", "");
                String targetFormat = imageProperties.getTypeImage();
                String format = (targetFormat.equalsIgnoreCase("JPEG") || targetFormat.equalsIgnoreCase("JPG")) ? "jpg" : targetFormat;

                try {
                    for (int i = 0; i < totalPages; i++) {
                        if (isCancelled() || cancelFlag.get()) {
                            throw new InterruptedException("Conversion cancelled");
                        }

                        updateProgress(i * 50L / totalPages, 100);

                        BufferedImage image = renderer.renderImageWithDPI(i, 300);

                        File tempFile = new File(imageProperties.getOutput(), baseName + "_page_" + (i + 1) + "." + targetFormat);

                        ImageIO.write(image, format, tempFile);
                        tempFiles.add(tempFile);
                    }

                    if (isCancelled() || cancelFlag.get()) {
                        throw new InterruptedException("Conversion cancelled");
                    }

                    updateProgress(60, 100);

                    zipFile = new File(imageProperties.getOutput(), baseName + "_" + UUID.randomUUID().toString().substring(0, 3) + "_images.zip");

                    try (FileOutputStream fos = new FileOutputStream(zipFile);
                         ZipOutputStream zos = new ZipOutputStream(fos)) {

                        for (int i = 0; i < tempFiles.size(); i++) {
                            if (isCancelled() || cancelFlag.get()) {
                                throw new InterruptedException("Conversion cancelled");
                            }

                            File file = tempFiles.get(i);
                            updateProgress(60 + (i * 35L / tempFiles.size()), 100);

                            try (FileInputStream fis = new FileInputStream(file)) {
                                ZipEntry zipEntry = new ZipEntry(file.getName());
                                zos.putNextEntry(zipEntry);

                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    zos.write(buffer, 0, length);
                                }

                                zos.closeEntry();
                            }

                            Utility.cleanupFile(file);
                        }
                    }

                    if (isCancelled() || cancelFlag.get()) {
                        throw new InterruptedException("Conversion cancelled");
                    }

                    updateProgress(100, 100);

                    return zipFile;

                } catch (Exception e) {
                    for (File tempFile : tempFiles) {
                        Utility.cleanupFile(tempFile);
                    }
                    Utility.cleanupFile(zipFile);
                    throw e;
                }
            }
        };
    }

    private void updatePreview() {
        if (currentDoc != null) {
            try {
                PDFRenderer renderer = new PDFRenderer(currentDoc);
                BufferedImage bim = renderer.renderImageWithDPI(0, 72);
                imageViewPdf.setImage(SwingFXUtils.toFXImage(bim, null));
                labelPreviewPlaceholder.setVisible(false);
            } catch (IOException e) {
                ErrorLogger.error("Error rendering PDF preview: " + e.getMessage());
            }
        }
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (result == null || Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        
        String message = isExtract ? "Images saved to ZIP!" : "Image saved!";
        ErrorLogger.info("Conversion successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, message, imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            progressBar.setProgress(0);
        });
        
        isExtract = false;
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        if (exception instanceof InterruptedException || cancelFlag.get()) {
            handleTaskCancelled();
            return;
        }
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, progressBar,"Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            progressBar.setProgress(0);
        });
    }

    @Override
    protected void handleTaskCancelled() {
        super.handleTaskCancelled();
        isExtract = false;
    }

    @FXML
    public void isPressedReset() {
        cancelProcessing();

        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true, "PDF"
        );
        reset(imageProperties, ctx, "Selected PDF file: none");

        disableControls();

        listToggleBtn.forEach(tb -> tb.setDisable(true));

        closeCurrentDoc();
        if (imageViewPdf != null) {
            imageViewPdf.setImage(null);
        }

        labelPreviewPlaceholder.setVisible(true);

        toggleGroup.selectToggle(null);
        imageProperties.setTypeImage(null);

        isExtract = false;
    }

    private void closeCurrentDoc() {
        if (currentDoc != null) {
            try {
                currentDoc.close();
            } catch (IOException e) {
                ErrorLogger.error("Error closing PDF document: " + e.getMessage());
            }
            currentDoc = null;
        }
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "PDF to Image",
                """
                        How to use:
                        1. Select a PDF file using 'Select PDF file' or drag and drop.
                        2. Choose output format (PNG, JPEG, WEBP, TIFF, BMP, PPM, PGM, PAM).
                        3. Click 'Convert and Download' to save the image, or 'Extract Jpeg/Png And Download' to extract all pages into a ZIP archive.
                        4. You can cancel the conversion at any time using the 'Cancel Conversion' button.
                        
                        Note: 'Convert and Download' converts only the first page of the PDF.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void loadFile(File selectedFile) {
        closeCurrentDoc();
        imageProperties.setImage(selectedFile);
        labelSelectFileName.setText("Selected PDF: " + selectedFile.getName());
        textDragZone.setText("Selected: " + selectedFile.getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);

        try {
            currentDoc = org.apache.pdfbox.Loader.loadPDF(selectedFile);
            updatePreview();
            
            enableControls();

            listToggleBtn.forEach(tb -> tb.setDisable(false));
        } catch (IOException e) {
            ErrorLogger.error("Error loading PDF: " + e.getMessage());
            Platform.runLater(() -> {
                showErrorMessage(labelSuccess, progressBar,"Error loading PDF: " + e.getMessage(), imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
            });
        }
    }

    @FXML
    public void cancelProcessing() {
        cancelFlag.set(true);
        cancelCurrentTask();
    }
}

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
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static model.utility.Util.getSavedPath;
import static viewHelp.Message.*;

public class ConverterPdfToImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private ImageView imageViewPdf;
    @FXML private ProgressBar progressBar;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit, btnAllImageToPng, btnAllImageToJpeg;
    @FXML private Label labelSelectFileName, textDragZone, labelPreviewPlaceholder;
    
    @FXML private ToggleButton btnToPNG, btnToJPEG, btnToWEBP, btnToTIFF, btnToBMP;
    @FXML private ToggleButton btnToPPM, btnToPGM, btnToPAM;

    private PDDocument currentDoc;
    private List<Control> listControls;
    List<ToggleButton> listToggleBtn;
    private ToggleGroup toggleGroup;
    private boolean isExtract = false;

    @FXML
    public void initialize() {
        toggleGroup = new ToggleGroup();

        listToggleBtn = List.of(btnToPNG, btnToJPEG, btnToWEBP, btnToTIFF, btnToBMP, btnToPPM, btnToPGM, btnToPAM);

        listToggleBtn.forEach(tb -> tb.setToggleGroup(toggleGroup));

        List<Control> temp = new ArrayList<>(listToggleBtn);
        temp.addAll(List.of(btnAllImageToJpeg, btnAllImageToPng, btnSubmit));
        listControls = List.copyOf(temp);
        imageProperties.setOutput(getSavedPath());

        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0);
        }

        setupClearMessageTimer(labelSuccess, imageProperties.getHideSuccessMessageTimer(), true);

        setupDragAndDrop(dropZone, List.of(".pdf"), this::loadFile);

        isPressedReset();
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
            case "btnAllImageToPng" -> imageProperties.setTypeImage("png");
            case "btnAllImageToJpeg" -> imageProperties.setTypeImage("jpeg");
        }

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
        listToggleBtn.forEach(tb -> tb.setDisable(false));
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectPdfFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectPdfFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                "Choice PDF file"
        ).ifPresent(this::loadFile);
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

                PDFRenderer renderer = new PDFRenderer(currentDoc);

                updateProgress(30, 100);

                BufferedImage image = renderer.renderImageWithDPI(0, 300);

                updateProgress(60, 100);

                File outputFile = Util.createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        imageProperties.getTypeImage()
                );

                String format = imageProperties.getTypeImage().toUpperCase();
                if (format.equals("JPEG") || format.equals("JPG")) {
                    format = "jpg";
                }

                ImageIO.write(image, format, outputFile);

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

                try {
                    for (int i = 0; i < totalPages; i++) {
                        updateProgress(i * 50L / totalPages, 100);

                        BufferedImage image = renderer.renderImageWithDPI(i, 300);

                        String baseName = imageProperties.getImage().getName().replaceFirst("[.][^.]+$", "");
                        File tempFile = new File(imageProperties.getOutput(), baseName + "_page_" + (i + 1) + "." + imageProperties.getTypeImage());

                        String format = imageProperties.getTypeImage().toUpperCase();
                        if (format.equals("JPEG") || format.equals("JPG")) {
                            format = "jpg";
                        }

                        ImageIO.write(image, format, tempFile);
                        tempFiles.add(tempFile);
                    }

                    updateProgress(60, 100);

                    String baseName = imageProperties.getImage().getName().replaceFirst("[.][^.]+$", "");
                    File zipFile = new File(imageProperties.getOutput(), baseName + "_images.zip");

                    try (FileOutputStream fos = new FileOutputStream(zipFile);
                         ZipOutputStream zos = new ZipOutputStream(fos)) {

                        for (int i = 0; i < tempFiles.size(); i++) {
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

                            if (file.delete()) {
                                ErrorLogger.info("Temp files has been deleted.");
                            }
                        }
                    }

                    updateProgress(100, 100);

                    return zipFile;

                } catch (Exception e) {
                    for (File tempFile : tempFiles) {
                        if (tempFile.exists()) {
                            if(tempFile.delete()) {
                                ErrorLogger.info(tempFile.getAbsolutePath() + " has been deleted.");
                            }
                        }
                    }
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
        if (Boolean.FALSE.equals(result)) {
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
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, progressBar,"Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            progressBar.setProgress(0);
        });
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPdf, progressBar, true
        );
        Util.reset(imageProperties, ctx, "Selected PDF file: none");

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
                        3. Click 'Convert and Download' to save the image.
                        
                        Note: Only the first page of the PDF will be converted.
                        
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

        Util.bindingImageViewToPreviewContainer(imageViewPdf, previewContainer);

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
}

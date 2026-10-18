package media_multitool;

import app.Launcher;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.helper.pdf.PdfHelper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewHelp.Alerts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;
import static model.select.SelectFile.showOpenMultipleDialog;
import static model.utility.PathWorker.createOutputFile;
import static viewHelp.Message.showErrorMessage;

public abstract class AbstractPdfToolController extends AbstractMediaController {
    protected final ImageProperties properties = new ImageProperties();
    protected final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    @FXML protected Button btnSelectFile, btnChoiceDirForSave, btnSubmit, btnCancel;
    @FXML protected ImageView imageViewPdf;
    @FXML protected Label labelSelectFileName, textDragZone, labelPreviewPlaceholder;
    @FXML protected StackPane dropZone, previewContainer;

    protected PDDocument currentDoc;

    @Override
    protected MediaProperties getProperties() {
        return properties;
    }

    protected abstract void toggleUI(boolean disabled);
    protected abstract void toggleControls(boolean disabled);
    protected abstract void loadFile(File file);

    protected void startDaemonTask(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    protected void disableControls() {
        toggleControls(true);
    }

    @Override
    protected void enableControls() {
        toggleControls(false);
    }

    @FXML
    protected void selectPdfFile() {
        selectInputFile(btnSelectFile,
                new FileChooser.ExtensionFilter("PDF", "*.pdf"), this::loadFile);
    }

    @FXML
    protected void selectImageFile() {
        selectInputFile(btnSelectFile,
                new FileChooser.ExtensionFilter("Images", "*.png", "*.tiff", "*.jpg", "*.jpeg", "*.svg", "*.bmp"), this::loadFile);
    }

    protected void selectMultipleImageFile(Consumer<? super File> action) {
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();

        showOpenMultipleDialog(stage,
                new FileChooser.ExtensionFilter("Images", "*.png", "*.tiff", "*.jpg", "*.jpeg", "*.svg", "*.bmp"))
                .ifPresent(f -> f.forEach(action));
    }

    protected void selectMultiplePdfFile(Consumer<? super File> action) {
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();

        showOpenMultipleDialog(stage,
                new FileChooser.ExtensionFilter("PDF", "*.pdf"))
                .ifPresent(f -> f.forEach(action));
    }

    @FXML
    protected void chooseSaveDirectory() {
        selectOutputDirectory(btnChoiceDirForSave, properties.getOutput(), properties::setOutput, "Select directory for save");
    }

    @FXML
    protected void cancelProcessing() {
        cancelFlag.set(true);
        cancelCurrentTask();
    }

    protected void updatePreview() {
        if (nonNull(currentDoc) && nonNull(imageViewPdf)) {
            try {
                PDFRenderer renderer = new PDFRenderer(currentDoc);
                BufferedImage bim = renderer.renderImageWithDPI(0, 72);
                imageViewPdf.setImage(SwingFXUtils.toFXImage(bim, null));
                if (nonNull(labelPreviewPlaceholder)) {
                    labelPreviewPlaceholder.setVisible(false);
                }
            } catch (IOException e) {
                ErrorLogger.error("Error rendering PDF preview: " + e.getMessage());
            }
        }
    }

    protected void closeCurrentDoc() {
        if (nonNull(currentDoc)) {
            try {
                currentDoc = closeDocument(currentDoc);
            } catch (IOException e) {
                ErrorLogger.error("Error closing PDF document: " + e.getMessage());
            }
        }
    }

    @Override
    protected boolean isTaskCancellationRequested() {
        return cancelFlag.get();
    }

    @Override
    protected void showTaskFailure(Throwable exception) {
        showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), properties.getHideSuccessMessageTimer());
    }

    protected void handleEncryptedPdf(IOException exception) {
        showErrorMessage(labelSuccess, PdfHelper.getEncryptedPdfMessage(), properties.getHideSuccessMessageTimer());
        if (nonNull(labelSuccess)) {
            labelSuccess.setManaged(true);
        }
        Platform.runLater(() -> Alerts.alertDialog(
                Alert.AlertType.ERROR,
                "Error",
                "Failed to load PDF",
                exception.getMessage(),
                Modality.APPLICATION_MODAL
        ));
        disableControls();
        if (nonNull(btnReset)) {
            btnReset.setDisable(false);
        }
        setProtectImage();
    }

    protected void setProtectImage() {
        try {
            Image image = new Image(Objects.requireNonNull(Launcher.class.getResourceAsStream("/img/encrypted.webp")));
            if (nonNull(imageViewPdf)) {
                imageViewPdf.setImage(image);
            }
            if (nonNull(labelPreviewPlaceholder)) {
                labelPreviewPlaceholder.setVisible(false);
            }
        } catch (NullPointerException e) {
            ErrorLogger.error("Image=/img/encrypted.webp |  " + e.getMessage());
        }
    }

    protected PDDocument closeDocument(PDDocument document) throws IOException {
        if (nonNull(document)) {
            document.close();
            document = null;
        }
        return document;
    }

    protected File getOutputFilePdf(String endText) {
        return createOutputFile(
                properties.getImage(),
                properties.getOutput(),
                "_" + endText,
                "pdf"
        );
    }
}

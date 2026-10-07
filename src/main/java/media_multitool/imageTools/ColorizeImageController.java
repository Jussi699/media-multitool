package media_multitool.imageTools;

import com.bric.colorpicker.ColorPicker;
import com.imagetools.ImageTools;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;
import media_multitool.AbstractImageToolController;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import viewHelp.InfoAlert;
import viewHelp.WorkColors;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class ColorizeImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnColorPicker, btnSubmit;
    @FXML private ImageView preview;
    @FXML private StackPane previewContainer;

    private javafx.scene.paint.Color selectedColorFX;
    private java.awt.Color newColor;
    private JDialog swingDialog;
    private List<Control> listControls;

    @Override
    protected ImageProperties getImageProperties() {
        return imageProperties;
    }

    @Override
    protected ImageView getImageView() {
        return preview;
    }

    @FXML
    public void initialize() {
        listControls = List.of(btnColorPicker, btnSubmit, btnSubmitAndCopy, btnReset);
        btnChooseSaveDirectory.setTooltip(new Tooltip("Default directory: Desktop"));

        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(
                () -> {
                    if (isNull(originalImage)) return null;
                    return nonNull(selectedColorFX)
                            ? com.imagetools.ImageTools.applyColorizeEffect(originalImage, selectedColorFX)
                            : originalImage;
                },
                "Colorized"
        );
        bindingImageViewToPreviewContainer(preview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Colorize Image",
                """
                        3. Use the color picker to choose a tint;
                        
                        4. Click "Colorize and Download";
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, preview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        processedImage = null;
        originalImage = null;
        selectedColorFX = null;
        newColor = null;
        previewContainer.setEffect(null);
        WorkColors.updateColorView(new java.awt.Color(64, 64, 64), btnColorPicker);
        disableControls();
    }

    @FXML
    public void handleColorChange() {
        if (isNull(swingDialog)) {
            initSwingColorPicker();
            bindSwingDialogToStage();
        }
        if (!swingDialog.isVisible()) {
            swingDialog.setVisible(true);
        } else {
            swingDialog.toFront();
        }
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }
        processedImage = originalImage;
        setImagePreview(processedImage, preview);

        if (nonNull(newColor)) {
            this.selectedColorFX = WorkColors.toFxColor(newColor);
            previewContainer.setEffect(ImageTools.colorizeImage(processedImage, selectedColorFX));
        } else {
            this.selectedColorFX = null;
            previewContainer.setEffect(null);
        }
    }

    @Override
    protected BufferedImage getFinalImageForDownload(DoubleConsumer progressUpdater) {
        if (isNull(originalImage)) {
            throw new IllegalStateException("Original image is null");
        }

        if (nonNull(selectedColorFX)) {
            return com.imagetools.ImageTools.applyColorizeEffect(originalImage, selectedColorFX);
        }
        return originalImage;
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChooseSaveDirectory.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChooseSaveDirectory.setDisable(false);
        btnReset.setDisable(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Image colorization successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Colorized image saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(@NonNull Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    private void initSwingColorPicker() {
        swingDialog = new JDialog();
        swingDialog.setTitle("Select Color");
        swingDialog.setModal(false);
        swingDialog.setAlwaysOnTop(true);
        swingDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        ColorPicker swingColorPicker = new ColorPicker(true, true);
        swingColorPicker.setColor(nonNull(selectedColorFX)
                ? WorkColors.toAwtColor(selectedColorFX)
                : java.awt.Color.WHITE);

        swingColorPicker.addColorListener(colorModel -> {
            newColor = colorModel.getColor();
            Platform.runLater(() -> {
                generatePreview();
                WorkColors.updateColorView(newColor, btnColorPicker);
            });
        });

        swingDialog.add(swingColorPicker);
        swingDialog.pack();
    }

    private void bindSwingDialogToStage() {
        Platform.runLater(() -> {
            final boolean hasScene = nonNull(btnColorPicker.getScene());
            final boolean hasWindow = nonNull(btnColorPicker.getScene().getWindow());

            if (hasScene && hasWindow) {
                btnColorPicker.getScene().getWindow().addEventHandler(
                        WindowEvent.WINDOW_HIDING,
                        _ -> {
                            if (nonNull(swingDialog)) swingDialog.dispose();
                        }
                );
            }
        });
    }
}

package controllers.imageTools;

import com.bric.colorpicker.ColorPicker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.stage.WindowEvent;
import controllers.abstractControllers.AbstractImageToolController;
import model.imageTools.ImageTools;
import model.logger.ErrorLogger;
import model.utility.format.SupportedFormat;
import model.utility.context.ResetContext;
import org.jspecify.annotations.NonNull;
import visual.dialogs.InfoAlert;
import visual.effects.WorkColors;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.*;

public class ColorizeImageController extends AbstractImageToolController {
    @FXML private Button btnColorPicker;

    private javafx.scene.paint.Color selectedColorFX;
    private java.awt.Color newColorAwt;
    private JDialog swingDialog;

    @FXML
    public void initialize() {
        btnChooseSaveDirectory.setTooltip(new Tooltip("Default directory: Desktop"));

        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(
                () -> {
                    if (isNull(originalImage)) return null;
                    return nonNull(selectedColorFX)
                            ? ImageTools.applyColorizeEffect(originalImage, selectedColorFX)
                            : originalImage;
                },
                "Colorized"
        );
        bindingImageViewToPreviewContainer(imagePreview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, SupportedFormat.getAllSupportedImageFormats(), this::loadFile);
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
                dropZone, imagePreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        processedImage = null;
        originalImage = null;
        selectedColorFX = null;
        newColorAwt = null;
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
        setImagePreview(processedImage, imagePreview);

        if (nonNull(newColorAwt)) {
            this.selectedColorFX = WorkColors.toFxColor(newColorAwt);
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
            return ImageTools.applyColorizeEffect(originalImage, selectedColorFX);
        }
        return originalImage;
    }

    @Override
    protected void disableControls() {
        setControlsDisabled(true, btnColorPicker, btnSubmit, btnSubmitAndCopy, btnReset);
    }

    @Override
    protected void enableControls() {
        setControlsDisabled(false, btnColorPicker, btnSubmit, btnSubmitAndCopy, btnReset);
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
            newColorAwt = colorModel.getColor();
            Platform.runLater(() -> {
                generatePreview();
                WorkColors.updateColorView(newColorAwt, btnColorPicker);
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

package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.checks.Checking;
import model.preprocessing.ImagePreprocessing;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import viewHelp.Alerts;
import com.bric.colorpicker.ColorPicker;
import viewHelp.WorkColors;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class ColorizeImageController extends AbstractMediaController {
    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnColorPicker, btnSubmit;
    @FXML private Label textDragZone, labelPreviewPlaceholder, labelSelectImageName;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane previewContainer, dropZone;

    private final ImageProperties imageProperties = new ImageProperties();
    private BufferedImage originalBufferedImage, currentBufferedImage;
    private Color selectedColorFX = Color.WHITE;

    private JDialog swingDialog;
    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        listControls = List.of(btnColorPicker, btnSubmit);
        btnChoiceFolderForSaveFile.setTooltip(new Tooltip("Default directory: Desktop"));

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Colorize Image",
                """
                        How to use:
                        1. Select an image file using 'Select image' or drag and drop.
                        2. (Optional) Choose a directory for saving the output.
                        3. Use the color picker to choose a tint.
                        4. Click 'Colorize and Download' to apply the effect.
                        
                        This tool colorizes your image using a lighting effect.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChoiceFolderForSaveFile.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSaveFile.setDisable(false);
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

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()),
                "Select image"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    public void submitColorizeAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties) || originalBufferedImage == null) {
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                BufferedImage processedImage = ImagePreprocessing.applyColorizeEffect(currentBufferedImage, selectedColorFX);

                updateProgress(30, 100);

                File outputFile = createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        imageProperties.getTypeImage()
                );

                updateProgress(60, 100);

                ImagePreprocessing.downloadImage(processedImage, imageProperties.getTypeImage(), outputFile);
                updateProgress(100, 100);

                return outputFile;
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info("Image colorization successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Colorized image saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true
        );
        reset(imageProperties, ctx, "Selected image file: none");

        currentBufferedImage = null;
        originalBufferedImage = null;
        selectedColorFX = Color.WHITE;
        WorkColors.updateColorView(java.awt.Color.WHITE, btnColorPicker);
        previewContainer.setEffect(null);
        disableControls();
    }

    private void loadFile(File selectedFile) {
        enableControls();
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectImageName.setText("Select image: " + selectedFile.getName());

        if (imageViewPreview != null) {
            try {
                originalBufferedImage = ImageIO.read(selectedFile);
                currentBufferedImage = originalBufferedImage;
                if (currentBufferedImage != null) {
                    setPreview(currentBufferedImage);
                        labelPreviewPlaceholder.setVisible(false);
                }
            } catch (Exception e) {
                ErrorLogger.error("Failed to load preview: " + e.getMessage());
            }
        }

        textDragZone.setText("Selected: " + selectedFile.getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void setPreview(BufferedImage bi) {
        if (bi != null && imageViewPreview != null) {
            Image image = SwingFXUtils.toFXImage(bi, null);
            imageViewPreview.setImage(image);
        }
    }

    @FXML
    public void handleColorChange() {
        if (swingDialog == null) {
            initSwingColorPicker();
            bindSwingDialogToStage();
        }
        if (!swingDialog.isVisible()) {
            swingDialog.setVisible(true);
        } else {
            swingDialog.toFront();
        }
    }

    private void initSwingColorPicker() {
        swingDialog = new JDialog();
        swingDialog.setTitle("Select Color");
        swingDialog.setModal(false);
        swingDialog.setAlwaysOnTop(true);
        swingDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        ColorPicker swingColorPicker = new ColorPicker(true, true);
        swingColorPicker.setColor(WorkColors.toAwtColor(selectedColorFX));

        swingColorPicker.addColorListener(colorModel -> {
            java.awt.Color newColor = colorModel.getColor();
            Platform.runLater(() -> {
                updateColorModel(newColor);
                WorkColors.updateColorView(newColor, btnColorPicker);
            });
        });

        swingDialog.add(swingColorPicker);
        swingDialog.pack();
    }

    private void bindSwingDialogToStage() {
        Platform.runLater(() -> {
            if (btnColorPicker.getScene() != null && btnColorPicker.getScene().getWindow() != null) {
                btnColorPicker.getScene().getWindow().addEventHandler(
                        javafx.stage.WindowEvent.WINDOW_HIDING,
                        _ -> {
                            if (swingDialog != null) swingDialog.dispose();
                        }
                );
            }
        });
    }

    private void updateColorModel(java.awt.Color awtColor) {
        this.selectedColorFX = WorkColors.toFxColor(awtColor);
        if (currentBufferedImage != null) {
            previewContainer.setEffect(ImagePreprocessing.colorizeImage(currentBufferedImage, selectedColorFX));
        }
    }
}

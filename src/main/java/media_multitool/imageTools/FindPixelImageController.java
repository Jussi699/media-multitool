package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.helper.PixelHelper;
import model.logger.ErrorLogger;
import model.preprocessing.ImagePreprocessing;
import model.properties.MediaProperties;
import model.properties.ImageProperties;
import model.select.SelectFile;
import model.utility.*;
import viewHelp.Alerts;
import viewHelp.ImageZoomHelper;
import viewHelp.ZoomControlHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static model.utility.Util.*;
import static viewHelp.Message.*;

public class FindPixelImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Slider imageScaleSlider;
    @FXML private ScrollPane scrollPanePhoto;
    @FXML private BufferedImage originalBufferedImage;

    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnSaveRGB, btnSaveHex;
    @FXML private Label labelSelectImageName, textDragZone, labelPreviewPlaceholder, labelHex;
    @FXML private ImageView imageViewPreview;

    @FXML private TextField textFieldR, textFieldG, textFieldB, textFieldHEX, textFieldRGB;
    @FXML private Rectangle colorPreview;

    private ZoomControlHelper zoomControlHelper;
    private java.util.List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        listControls = java.util.List.of(textFieldR, textFieldG, textFieldB, textFieldHEX, textFieldRGB, imageScaleSlider, btnSaveRGB, btnSaveHex);
        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        zoomControlHelper = new ZoomControlHelper(scrollPanePhoto, imageViewPreview, imageScaleSlider, previewContainer, 1.0, 3.0);

        if(imageViewPreview != null) {
            imageViewPreview.setOnMouseClicked(this::handlePixelSelection);
            ImageZoomHelper.applyZoomEffect(imageViewPreview, previewContainer);
        }
        else {
            Alerts.alertDialog(Alert.AlertType.WARNING, "ImageView", "ImageView is not loaded!",
                    "Something wrong with ImageView! Maybe his (null).\nTry restarting the application.");
            ErrorLogger.error("ImageView not loaded (null)!");
            return;
        }
        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    private void handlePixelSelection(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) return;

        PixelHelper.pixelSelection(e, imageViewPreview).ifPresent(this::updatePixelInfo);
    }

    private void updatePixelInfo(Color color) {
        textFieldR.setText(String.valueOf((int) (color.getRed() * 255)));
        textFieldG.setText(String.valueOf((int) (color.getGreen() * 255)));
        textFieldB.setText(String.valueOf((int) (color.getBlue() * 255)));
        textFieldRGB.setText((int) (color.getRed() * 255) + " " + (int) (color.getGreen() * 255) + " " + (int) (color.getBlue() * 255));
        textFieldHEX.setText(PixelHelper.toHexString(color));
        colorPreview.setFill(color);
        labelHex.setTextFill(color);
    }

    @FXML
    private void showInfo() {

        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Find Pixel Color",
                """
                        How to use:
                        1. Select an image file using 'Select image' or drag and drop.
                        2. Info:\s
                        The left mouse button allows you to find a pixel.
                        The right  mouse button lets you drag and drop the photo.
                       \s
                        3. Click on any pixel in the image to see its coordinates and color.
                        4. Use the slider or mouse wheel to zoom in for better precision.
                       \s
                        This tool helps you find the exact color and position of pixels in your image.
                       \s
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnReset.setDisable(false);
    }

    @Override
    protected void disableControls() {
        if (listControls != null) listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        if (listControls != null) listControls.forEach(c -> c.setDisable(false));
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()),
                "Choice image"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties) || originalBufferedImage == null) {
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                File outputFile = Util.createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        imageProperties.getTypeImage()
                );

                updateProgress(50, 100);

                ImagePreprocessing.downloadImage(originalBufferedImage, imageProperties.getTypeImage(), outputFile);
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
        ErrorLogger.info("Image saved successfully to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Image saved!", imageProperties.getHideSuccessMessageTimer());
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
        Util.reset(imageProperties, ctx, "Selected image file: none");

        originalBufferedImage = null;
        zoomControlHelper.resetZoom();

        textFieldR.setText("");
        textFieldG.setText("");
        textFieldB.setText("");
        textFieldRGB.setText("");
        textFieldHEX.setText("");
        colorPreview.setFill(Color.WHITE);
        labelHex.setTextFill(Color.WHITE);
        disableControls();
    }

    private void loadFile(File selectedFile) {
        enableControls();
        imageProperties.setImage(selectedFile);

        labelSelectImageName.setText("Select image: " + selectedFile.getName());
        textDragZone.setText("Select image: " + selectedFile.getName());

        if (imageViewPreview != null) {
            try {
                originalBufferedImage = ImageIO.read(selectedFile);
                if (originalBufferedImage != null) {
                    if (labelPreviewPlaceholder != null) {
                        labelPreviewPlaceholder.setVisible(false);
                    }
                    zoomControlHelper.resetZoom();
                    setPreview(originalBufferedImage);
                    zoomControlHelper.updateImageSize();
                }
            } catch (Exception e) {
                ErrorLogger.error("Failed to load preview: " + e.getMessage());
            }
        }

        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void setPreview(BufferedImage bi) {

        if (bi != null && imageViewPreview != null) {
            Image image = SwingFXUtils.toFXImage(bi, null);
            imageViewPreview.setImage(image);
        }
    }

    public void onClickSaveHex(MouseEvent event) {
        saveToClipboard(textFieldHEX.getText(), "HEX copied successfully!", 2, event);
    }

    public void onClickSaveRGB(MouseEvent event) {
        saveToClipboard(textFieldRGB.getText(), "RBG copied successfully!", 2, event);
    }

    public void saveToClipboard(String copyText, String textSuccess, int showSecond, MouseEvent event) {
        Clipboards clipboards = new Clipboards();
        clipboards.clip(copyText, textSuccess, showSecond, event);
    }
}

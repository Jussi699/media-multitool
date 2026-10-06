package media_multitool.imageTools;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import media_multitool.AbstractImageToolController;
import model.helper.images.PixelHelper;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.utility.Clipboards;
import model.utility.Global;
import model.utility.ResetContext;
import viewHelp.Alerts;
import viewHelp.ImageZoomHelper;
import viewHelp.InfoAlert;
import viewHelp.ZoomControlHelper;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.setupClearMessageTimer;

public class FindPixelImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnSaveRGB, btnSaveHex;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane previewContainer;
    @FXML private ScrollPane scrollPaneImage;
    @FXML private Slider imageScaleSlider;
    @FXML private TextField textFieldR, textFieldG, textFieldB;
    @FXML private TextField textFieldHEX, textFieldRGB;
    @FXML private Label labelHex;
    @FXML private Rectangle colorPreview;

    private ZoomControlHelper zoomControlHelper;
    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @Override
    protected ImageProperties getImageProperties() {
        return imageProperties;
    }

    @Override
    protected ImageView getImageView() {
        return imageViewPreview;
    }

    @FXML
    public void initialize() {
        listControls = List.of(textFieldR, textFieldG, textFieldB, textFieldHEX,
                textFieldRGB, imageScaleSlider, btnSaveRGB, btnSaveHex, btnReset);
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> originalImage, "Selected");

        zoomControlHelper = new ZoomControlHelper(scrollPaneImage, imageViewPreview, imageScaleSlider, previewContainer, 1.0, 3.0);

        if(nonNull(imageViewPreview)) {
            imageViewPreview.setOnMouseClicked(this::handlePixelSelection);
            ImageZoomHelper.applyZoomEffect(imageViewPreview, previewContainer);
        }
        else {
            Alerts.alertDialog(Alert.AlertType.WARNING, "ImageView", "ImageView is not loaded!",
                    "Something wrong with ImageView!");
            ErrorLogger.error("ImageView not loaded (null)!");
            return;
        }
        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoSimple(
                "Find Color Pixel",
                """
                        1. Click on any pixel with the left mouse button to view its RGB and HEX color values;
                        
                        2. (Optional) Drag with the right mouse button to move the image;
                        
                        3. (Optional) Use the slider or mouse wheel to zoom in for better precision;
                        
                        4. (Optional) Click "Save HEX" or "Save RGB" to copy color values.
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        originalImage = null;
        processedImage = null;
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

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            ErrorLogger.warn("Cannot generate preview: originalImage is null");
            return;
        }

        processedImage = originalImage;
        zoomControlHelper.resetZoom();
        setImagePreview(processedImage, imageViewPreview);
        zoomControlHelper.updateImageSize();
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
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
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

}

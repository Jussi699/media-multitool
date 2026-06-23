package media_multitool.watermarks;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import model.logger.ErrorLogger;
import model.utility.Global;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class WatermarkPhotoController {
    @FXML private StackPane watermarkDropZone;
    @FXML private javafx.scene.control.Label labelWatermarkName;
    @FXML private javafx.scene.control.Label labelSize;
    @FXML private javafx.scene.control.Label labelOpacity;
    @FXML private javafx.scene.control.Label labelRotation;
    @FXML private javafx.scene.control.Label labelSpacingText;
    @FXML private javafx.scene.control.Label labelSpacingValue;
    @FXML private javafx.scene.control.Button btnSelectWatermark;
    @FXML private Slider sliderSize;
    @FXML private Slider sliderOpacity;
    @FXML private Slider sliderRotation;
    @FXML private Slider sliderSpacing;
    @FXML private ToggleButton tileSingle;
    @FXML private ToggleButton tileEvenGrid;
    @FXML private ToggleButton tileDiamondMesh;

    @Getter private WatermarkSettings settings;
    @Setter private WatermarkImageController mainController;
    @Getter private double relativePositionX = 0;
    @Getter private double relativePositionY = 0;


    @FXML
    public void initialize() {
        settings = new WatermarkSettings();
        settings.setType(WatermarkSettings.WatermarkType.IMAGE);
        
        setupSliders();
        setupTileButtons();
        setupDragAndDrop();
        setupDefaults();
    }

    public void loadSettings(WatermarkSettings settings) {
        if (settings == null || settings.getType() != WatermarkSettings.WatermarkType.IMAGE) {
            return;
        }

        this.settings = settings.copy();
        this.settings.setUseCustomPosition(true);
        this.settings.setPositionX(settings.getPositionX());
        this.settings.setPositionY(settings.getPositionY());
        this.relativePositionX = settings.getPositionX();
        this.relativePositionY = settings.getPositionY();
        
        sliderSize.setValue(settings.getSize());
        sliderOpacity.setValue(settings.getOpacity());
        sliderRotation.setValue(settings.getRotation());
        sliderSpacing.setValue(settings.getSpacing());
        
        String pattern = settings.getTilePattern();
        if      ("single".equals(pattern))  { tileSingle.setSelected(true);     }
        else if ("grid".equals(pattern))    { tileEvenGrid.setSelected(true);   }
        else if ("diamond".equals(pattern)) { tileDiamondMesh.setSelected(true);}
        
        if (settings.getWatermarkImage() != null) {
            labelWatermarkName.setText("Watermark loaded");
        }
    }

    private void setupSliders() {
        sliderSize.valueProperty().addListener((_, _, newVal) -> {
            labelSize.setText(String.format("%.0fpx", newVal.doubleValue()));
            boolean wasCustom = settings.isUseCustomPosition();
            settings.setSize(newVal.doubleValue());
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });

        sliderOpacity.valueProperty().addListener((_, _, newVal) -> {
            labelOpacity.setText(String.format("%.0f%%", newVal.doubleValue()));
            boolean wasCustom = settings.isUseCustomPosition();
            settings.setOpacity(newVal.doubleValue());
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });

        sliderRotation.valueProperty().addListener((_, _, newVal) -> {
            labelRotation.setText(String.format("%.0f°", newVal.doubleValue()));
            boolean wasCustom = settings.isUseCustomPosition();
            settings.setRotation(newVal.doubleValue());
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });

        sliderSpacing.valueProperty().addListener((_, _, newVal) -> {
            labelSpacingValue.setText(String.format("%.0fpx", newVal.doubleValue()));
            boolean wasCustom = settings.isUseCustomPosition();
            settings.setSpacing(newVal.doubleValue());
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });
    }

    private void setupTileButtons() {
        ToggleGroup tileGroup = new ToggleGroup();
        tileSingle.setToggleGroup(tileGroup);
        tileEvenGrid.setToggleGroup(tileGroup);
        tileDiamondMesh.setToggleGroup(tileGroup);
        
        tileSingle.setSelected(true);
        
        tileGroup.selectedToggleProperty().addListener((_, _, newVal) -> {
            boolean isTiled = newVal != tileSingle;
            sliderSpacing.setDisable(!isTiled);
            labelSpacingText.setDisable(!isTiled);
            labelSpacingValue.setDisable(!isTiled);
            
            boolean wasCustom = settings.isUseCustomPosition();
            
            if (newVal == tileSingle) {
                settings.setTileMode(false);
                settings.setTilePattern("single");
            } else if (newVal == tileEvenGrid) {
                settings.setTileMode(true);
                settings.setTilePattern("grid");
            } else if (newVal == tileDiamondMesh) {
                settings.setTileMode(true);
                settings.setTilePattern("diamond");
            }
            
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });
    }

    private void setupDragAndDrop() {
        watermarkDropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        watermarkDropZone.setOnDragDropped(event -> {
            var dragboard = event.getDragboard();
            boolean success = false;
            
            if (dragboard.hasFiles()) {
                File file = dragboard.getFiles().getFirst();
                if (isImageFile(file)) {
                    loadWatermarkImage(file);
                    success = true;
                }
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void setupDefaults() {
        sliderSize.setValue(100);
        sliderOpacity.setValue(100);
        sliderRotation.setValue(0);
        sliderSpacing.setValue(50);
        tileSingle.setSelected(true);
        sliderSpacing.setDisable(true);
        labelSpacingText.setDisable(true);
        labelSpacingValue.setDisable(true);
        
        settings.setSize(100);
        settings.setOpacity(100);
        settings.setRotation(0);
        settings.setSpacing(50);
        settings.setTileMode(false);
        settings.setTilePattern("single");
        settings.setPositionX(0);
        settings.setPositionY(0);
    }

    @FXML
    private void handleSelectWatermark() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Watermark Image");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser())
        );

        Stage stage = (Stage) btnSelectWatermark.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            loadWatermarkImage(file);
        }
    }

    private void loadWatermarkImage(File file) {
        try {
            BufferedImage watermarkImage = ImageIO.read(file);
            if (watermarkImage == null) {
                ErrorLogger.error("Failed to load watermark image: file returned null");
                return;
            }

            labelWatermarkName.setText(file.getName());
            settings.setWatermarkImage(watermarkImage);
            updatePreview();
            
        } catch (Exception e) {
            ErrorLogger.error("Failed to load watermark image: " + e.getMessage());
        }
    }

    private void updatePreview() {
        if (mainController != null) {
            mainController.updateWatermarkPreview(settings);
        }
    }

    @FXML
    private void handlePositionTopLeft() {
        setRelativePosition(0, 0);
    }

    @FXML
    private void handlePositionTopCenter() {
        setRelativePosition(0.5, 0);
    }

    @FXML
    private void handlePositionTopRight() {
        setRelativePosition(1.0, 0);
    }

    @FXML
    private void handlePositionCenterLeft() {
        setRelativePosition(0, 0.5);
    }

    @FXML
    private void handlePositionCenter() {
        setRelativePosition(0.5, 0.5);
    }

    @FXML
    private void handlePositionCenterRight() {
        setRelativePosition(1.0, 0.5);
    }

    @FXML
    private void handlePositionBottomLeft() {
        setRelativePosition(0, 1.0);
    }

    @FXML
    private void handlePositionBottomCenter() {
        setRelativePosition(0.5, 1.0);
    }

    @FXML
    private void handlePositionBottomRight() {
        setRelativePosition(1.0, 1.0);
    }

    private void setRelativePosition(double relX, double relY) {
        this.relativePositionX = relX;
        this.relativePositionY = relY;
        
        if (mainController != null) {
            mainController.updateWatermarkPosition(relX, relY, settings);
        }
    }

    @FXML
    private void handleResetSettings() {
        setupDefaults();
        labelWatermarkName.setText("None selected");
        settings.setWatermarkImage(null);
        updatePreview();
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || 
               name.endsWith(".jpeg") || name.endsWith(".bmp") || 
               name.endsWith(".gif");
    }
}

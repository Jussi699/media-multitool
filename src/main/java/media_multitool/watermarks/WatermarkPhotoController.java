package media_multitool.watermarks;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import model.helper.watermarks.WatermarkSettings;
import model.logger.ErrorLogger;
import model.utility.Global;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;

public class WatermarkPhotoController {
    @FXML private StackPane watermarkDropZone;
    @FXML private Label labelWatermarkName, labelSize, labelOpacity, labelRotation;
    @FXML private Label labelSpacingText, labelSpacingValue;
    @FXML private Button btnSelectWatermark;
    @FXML private Slider sliderSize, sliderOpacity, sliderRotation, sliderSpacing;
    @FXML private ToggleButton tileSingle, tileEvenGrid, tileDiamondMesh;

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
        
        switch (settings.getTilePattern()) {
            case "single"  -> tileSingle.setSelected(true);
            case "grid"    -> tileEvenGrid.setSelected(true);
            case "diamond" -> tileDiamondMesh.setSelected(true);
        }
        
        if (settings.getWatermarkImage() != null) {
            labelWatermarkName.setText("Watermark loaded");
        }
    }

    /**
     * Bind a slider to a label and a settings property, preserving custom position.
     * Eliminates the repetitive wasCustom save/restore pattern.
     */
    private void bindSlider(Slider slider, Label label, String format, Consumer<Double> setter) {
        slider.valueProperty().addListener((_, _, newVal) -> {
            label.setText(String.format(format, newVal.doubleValue()));
            settings.updatePreservingPosition(_ -> setter.accept(newVal.doubleValue()));
            updatePreview();
        });
    }

    private void setupSliders() {
        bindSlider(sliderSize, labelSize, "%.0fpx", settings::setSize);
        bindSlider(sliderOpacity, labelOpacity, "%.0f%%", settings::setOpacity);
        bindSlider(sliderRotation, labelRotation, "%.0f°", settings::setRotation);
        bindSlider(sliderSpacing, labelSpacingValue, "%.0fpx", settings::setSpacing);
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
            
            settings.updatePreservingPosition(s -> {
                if (newVal == tileSingle) {
                    s.setTileMode(false);
                    s.setTilePattern("single");
                } else if (newVal == tileEvenGrid) {
                    s.setTileMode(true);
                    s.setTilePattern("grid");
                } else if (newVal == tileDiamondMesh) {
                    s.setTileMode(true);
                    s.setTilePattern("diamond");
                }
            });
            
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

    @FXML private void handlePositionTopLeft()      { setRelativePosition(0, 0);     }
    @FXML private void handlePositionTopCenter()    { setRelativePosition(0.5, 0);   }
    @FXML private void handlePositionTopRight()     { setRelativePosition(1.0, 0);   }
    @FXML private void handlePositionCenterLeft()   { setRelativePosition(0, 0.5);   }
    @FXML private void handlePositionCenter()       { setRelativePosition(0.5, 0.5); }
    @FXML private void handlePositionCenterRight()  { setRelativePosition(1.0, 0.5); }
    @FXML private void handlePositionBottomLeft()   { setRelativePosition(0, 1.0);   }
    @FXML private void handlePositionBottomCenter() { setRelativePosition(0.5, 1.0); }
    @FXML private void handlePositionBottomRight()  { setRelativePosition(1.0, 1.0); }

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

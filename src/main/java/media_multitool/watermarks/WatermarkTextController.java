package media_multitool.watermarks;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

import java.awt.GraphicsEnvironment;

public class WatermarkTextController {
    @FXML private TextField fieldText;
    @FXML private ComboBox<String> comboBoxFont, comboBoxEffect;
    @FXML private ColorPicker colorPickerForText;
    @FXML private Slider sliderSizeText, sliderSpacing, sliderOpacity, sliderRotation;
    @FXML private Label labelSizeX, labelSpacingX, labelOpacityPercent, labelRotationDegree;
    @FXML private ToggleButton tileSingle, tileEvenGrid, tileDiamondMesh;

    @Getter private WatermarkSettings settings;
    @Setter private WatermarkImageController mainController;

    @FXML
    public void initialize() {
        settings = new WatermarkSettings();
        settings.setType(WatermarkSettings.WatermarkType.TEXT);
        
        setupFontComboBox();
        setupEffectComboBox();
        setupSliders();
        setupTileButtons();
        setupDefaults();
        setupLiveUpdate();
    }

    public void loadSettings(WatermarkSettings settings) {
        if (settings == null || settings.getType() != WatermarkSettings.WatermarkType.TEXT) {
            return;
        }

        this.settings = settings.copy();
        this.settings.setUseCustomPosition(true);
        this.settings.setPositionX(settings.getPositionX());
        this.settings.setPositionY(settings.getPositionY());
        
        fieldText.setText(settings.getText());
        comboBoxFont.setValue(settings.getFontName());
        sliderSizeText.setValue(settings.getFontSize());
        sliderSpacing.setValue(settings.getSpacing());
        sliderOpacity.setValue(settings.getOpacity());
        sliderRotation.setValue(settings.getRotation());
        
        updateColorPicker(settings.getTextColor());
        updateEffectCombo(settings.getEffect());
        updateTilePattern(settings.getTilePattern());
    }
    
    private void updateColorPicker(java.awt.Color awtColor) {
        javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.rgb(
            awtColor.getRed(),
            awtColor.getGreen(),
            awtColor.getBlue()
        );
        colorPickerForText.setValue(fxColor);
    }
    
    private void updateEffectCombo(String effect) {
        if (effect != null && !effect.isEmpty()) {
            String capitalizedEffect = effect.substring(0, 1).toUpperCase() + effect.substring(1).toLowerCase();
            comboBoxEffect.setValue(capitalizedEffect);
        }
    }
    
    private void updateTilePattern(String pattern) {
        if      ("single".equals(pattern))  { tileSingle.setSelected(true);      }
        else if ("grid".equals(pattern))    { tileEvenGrid.setSelected(true);    }
        else if ("diamond".equals(pattern)) { tileDiamondMesh.setSelected(true); }
    }

    private void setupFontComboBox() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontFamilies = ge.getAvailableFontFamilyNames();
        comboBoxFont.getItems().addAll(fontFamilies);
        comboBoxFont.setValue("Arial");
    }

    private void setupEffectComboBox() {
        comboBoxEffect.getItems().addAll("None", "Shadow", "Outline", "Glow");
        comboBoxEffect.setValue("None");
    }

    private void setupSliders() {
        sliderSizeText.valueProperty().addListener((_, _, newVal) -> {
            labelSizeX.setText(String.format("%.1fx", newVal.doubleValue()));
            boolean wasCustom = settings.isUseCustomPosition();
            settings.setFontSize(newVal.doubleValue());
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });

        sliderSpacing.valueProperty().addListener((_, _, newVal) -> {
            labelSpacingX.setText(String.format("%.1fx", newVal.doubleValue()));
            boolean wasCustom = settings.isUseCustomPosition();
            settings.setSpacing(newVal.doubleValue());
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });

        sliderOpacity.valueProperty().addListener((_, _, newVal) -> {
            labelOpacityPercent.setText(String.format("%.0f%%", newVal.doubleValue()));
            boolean wasCustom = settings.isUseCustomPosition();
            settings.setOpacity(newVal.doubleValue());
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });

        sliderRotation.valueProperty().addListener((_, _, newVal) -> {
            labelRotationDegree.setText(String.format("%.0f°", newVal.doubleValue()));
            boolean wasCustom = settings.isUseCustomPosition();
            settings.setRotation(newVal.doubleValue());
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
            labelSpacingX.setDisable(!isTiled);
            
            boolean wasCustom = settings.isUseCustomPosition();
            
            if (newVal == tileSingle) {
                settings.setTilePattern("single");
            } else if (newVal == tileEvenGrid) {
                settings.setTilePattern("grid");
            } else if (newVal == tileDiamondMesh) {
                settings.setTilePattern("diamond");
            }
            
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });
    }

    private void setupDefaults() {
        fieldText.setText("Watermark");
        colorPickerForText.setValue(Color.WHITE);
        sliderSizeText.setValue(2.5);
        sliderSpacing.setValue(0);
        sliderOpacity.setValue(100);
        sliderRotation.setValue(0);
        sliderSpacing.setDisable(true);
        
        settings.setText("Watermark");
        settings.setFontName("Arial");
        settings.setFontSize(2.5);
        settings.setTextColor(java.awt.Color.WHITE);
        settings.setOpacity(100);
        settings.setRotation(0);
        settings.setSpacing(0);
        settings.setTilePattern("single");
        settings.setEffect("none");
    }

    private void setupLiveUpdate() {
        fieldText.textProperty().addListener((_, _, newVal) -> {
            boolean wasCustom = settings.isUseCustomPosition();
            settings.setText(newVal);
            settings.setUseCustomPosition(wasCustom);
            updatePreview();
        });

        comboBoxFont.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                boolean wasCustom = settings.isUseCustomPosition();
                settings.setFontName(newVal);
                settings.setUseCustomPosition(wasCustom);
                updatePreview();
            }
        });

        colorPickerForText.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                java.awt.Color awtColor = new java.awt.Color(
                    (float) newVal.getRed(),
                    (float) newVal.getGreen(),
                    (float) newVal.getBlue()
                );
                boolean wasCustom = settings.isUseCustomPosition();
                settings.setTextColor(awtColor);
                settings.setUseCustomPosition(wasCustom);
                updatePreview();
            }
        });

        comboBoxEffect.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                boolean wasCustom = settings.isUseCustomPosition();
                settings.setEffect(newVal.toLowerCase());
                settings.setUseCustomPosition(wasCustom);
                updatePreview();
            }
        });
    }

    private void updatePreview() {
        if (mainController != null) {
            mainController.updateWatermarkPreview(settings);
        }
    }

    @FXML
    private void handleResetSettings() {
        setupDefaults();
        tileSingle.setSelected(true);
        updatePreview();
    }
}

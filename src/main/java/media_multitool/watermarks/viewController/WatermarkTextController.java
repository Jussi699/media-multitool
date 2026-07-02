package media_multitool.watermarks.viewController;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import media_multitool.watermarks.WatermarkImageController;
import media_multitool.watermarks.WatermarkPdfController;
import model.helper.watermarks.WatermarkSettings;

import java.awt.GraphicsEnvironment;
import java.util.function.Consumer;

public class WatermarkTextController {
    @FXML private TextField fieldText;
    @FXML private ComboBox<String> comboBoxFont, comboBoxEffect;
    @FXML private ColorPicker colorPickerForText;
    @FXML private Slider sliderSizeText, sliderSpacing, sliderOpacity, sliderRotation;
    @FXML private Label labelSizeX, labelSpacingX, labelOpacityPercent, labelRotationDegree;
    @FXML private ToggleButton tileSingle, tileEvenGrid, tileDiamondMesh;

    @Getter private WatermarkSettings settings;
    @Setter private WatermarkImageController mainController;
    @Setter private WatermarkPdfController mainPdfController;

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
        colorPickerForText.setValue(Color.rgb(
            awtColor.getRed(),
            awtColor.getGreen(),
            awtColor.getBlue()
        ));
    }
    
    private void updateEffectCombo(String effect) {
        if (effect != null && !effect.isEmpty()) {
            String capitalized = effect.substring(0, 1).toUpperCase() + effect.substring(1).toLowerCase();
            comboBoxEffect.setValue(capitalized);
        }
    }
    
    private void updateTilePattern(String pattern) {
        switch (pattern) {
            case "single"  -> tileSingle.setSelected(true);
            case "grid"    -> tileEvenGrid.setSelected(true);
            case "diamond" -> tileDiamondMesh.setSelected(true);
        }
    }

    private void setupFontComboBox() {
        String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        comboBoxFont.getItems().addAll(fontFamilies);
        comboBoxFont.setValue("Arial");
    }

    private void setupEffectComboBox() {
        comboBoxEffect.getItems().addAll("None", "Shadow", "Outline", "Glow");
        comboBoxEffect.setValue("None");
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
        bindSlider(sliderSizeText, labelSizeX, "%.1fx", settings::setFontSize);
        bindSlider(sliderSpacing, labelSpacingX, "%.1fx", settings::setSpacing);
        bindSlider(sliderOpacity, labelOpacityPercent, "%.0f%%", settings::setOpacity);
        bindSlider(sliderRotation, labelRotationDegree, "%.0f°", settings::setRotation);
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
            
            settings.updatePreservingPosition(s -> {
                if      (newVal == tileSingle)      s.setTilePattern("single");
                else if (newVal == tileEvenGrid)    s.setTilePattern("grid");
                else if (newVal == tileDiamondMesh) s.setTilePattern("diamond");
            });
            
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
            settings.updatePreservingPosition(s -> s.setText(newVal));
            updatePreview();
        });

        comboBoxFont.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                settings.updatePreservingPosition(s -> s.setFontName(newVal));
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
                settings.updatePreservingPosition(s -> s.setTextColor(awtColor));
                updatePreview();
            }
        });

        comboBoxEffect.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                settings.updatePreservingPosition(s -> s.setEffect(newVal.toLowerCase()));
                updatePreview();
            }
        });
    }

    private void updatePreview() {
        if (mainController != null) {
            mainController.updateWatermarkPreview(settings);
        }
        if (mainPdfController != null) {
            mainPdfController.updateWatermarkPreview(settings);
        }
    }

    @FXML
    private void handleResetSettings() {
        setupDefaults();
        tileSingle.setSelected(true);
        updatePreview();
    }
}

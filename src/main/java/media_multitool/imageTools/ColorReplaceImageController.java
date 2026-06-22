package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
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
import model.helper.imageTools.ColorReplaceHelper;
import viewHelp.Alerts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static model.utility.Util.bindingImageViewToPreviewContainer;
import static model.utility.Util.getSavedPath;
import static viewHelp.Message.*;

public class ColorReplaceImageController extends AbstractMediaController {
    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnSubmit;
    @FXML private Label textDragZone, labelPreviewPlaceholder, labelSelectImageName;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane previewContainer, dropZone;
    
    @FXML private ComboBox<String> comboSourceColor, comboTargetColor;
    @FXML private TextField textSourceColorHex, textTargetColorHex;
    @FXML private Spinner<Double> spinnerIntensity;
    @FXML private Spinner<Integer> spinnerSmoothing, spinnerEnhancement;
    @FXML private ToggleButton toggleJPEG, togglePNG;
    @FXML private CheckBox checkBoxReplaceAllColors;

    private final ImageProperties imageProperties = new ImageProperties();
    private BufferedImage originalBufferedImage, currentBufferedImage;

    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        listControls = List.of(
            comboSourceColor, comboTargetColor, textSourceColorHex, textTargetColorHex,
            spinnerIntensity, spinnerSmoothing, spinnerEnhancement, 
            toggleJPEG, togglePNG, checkBoxReplaceAllColors, btnSubmit
        );
        
        btnChoiceFolderForSaveFile.setTooltip(new Tooltip("Default directory: Desktop"));
        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        initializeColorCombos();
        initializeSpinners();
        initializeToggleGroup();
        initializeCheckBox();

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    private void initializeColorCombos() {
        String[] colors = ColorReplaceHelper.getAvailableColorNames();
        
        comboSourceColor.getItems().addAll(colors);
        comboTargetColor.getItems().addAll(colors);
        
        comboSourceColor.setValue("Red");
        comboTargetColor.setValue("Blue");
        
        comboSourceColor.setOnAction(_ -> updateSourceColorFromCombo());
        comboTargetColor.setOnAction(_ -> updateTargetColorFromCombo());
        
        textSourceColorHex.setText("#FF0000");
        textTargetColorHex.setText("#0000FF");
        
        textSourceColorHex.textProperty().addListener((_, _, newValue) -> {
            if (ColorReplaceHelper.isValidHex(newValue)) {
                updatePreview();
            }
        });
        
        textTargetColorHex.textProperty().addListener((_, _, newValue) -> {
            if (ColorReplaceHelper.isValidHex(newValue)) {
                updatePreview();
            }
        });
    }

    private void initializeSpinners() {
        SpinnerValueFactory<Double> intensityFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 100.0, 50.0, 1.0);
        spinnerIntensity.setValueFactory(intensityFactory);
        spinnerIntensity.setEditable(true);
        
        SpinnerValueFactory<Integer> smoothingFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 5, 1);
        spinnerSmoothing.setValueFactory(smoothingFactory);
        spinnerSmoothing.setEditable(true);
        
        SpinnerValueFactory<Integer> enhancementFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 50, 1);
        spinnerEnhancement.setValueFactory(enhancementFactory);
        spinnerEnhancement.setEditable(true);
        
        spinnerIntensity.valueProperty().addListener((_, _, _) -> updatePreview());
        spinnerSmoothing.valueProperty().addListener((_, _, _) -> updatePreview());
        spinnerEnhancement.valueProperty().addListener((_, _, _) -> updatePreview());
    }

    private void initializeToggleGroup() {
        ToggleGroup formatGroup = new ToggleGroup();
        toggleJPEG.setToggleGroup(formatGroup);
        togglePNG.setToggleGroup(formatGroup);
        togglePNG.setSelected(true);
        
        formatGroup.selectedToggleProperty().addListener((_, _, newToggle) -> {
            if (newToggle == toggleJPEG) {
                imageProperties.setTypeImage("jpeg");
            } else if (newToggle == togglePNG) {
                imageProperties.setTypeImage("png");
            }
        });
    }

    private void initializeCheckBox() {
        checkBoxReplaceAllColors.setSelected(false);
        checkBoxReplaceAllColors.selectedProperty().addListener((_, _, newValue) -> {
            comboSourceColor.setDisable(newValue);
            textSourceColorHex.setDisable(newValue);
            spinnerSmoothing.setDisable(newValue);
            updatePreview();
        });
    }

    private void updateSourceColorFromCombo() {
        String color = comboSourceColor.getValue();
        if (color != null) {
            textSourceColorHex.setText(ColorReplaceHelper.getHexFromColorName(color));
            updatePreview();
        }
    }

    private void updateTargetColorFromCombo() {
        String color = comboTargetColor.getValue();
        if (color != null) {
            textTargetColorHex.setText(ColorReplaceHelper.getHexFromColorName(color));
            updatePreview();
        }
    }

    private void updatePreview() {
        if (originalBufferedImage == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String targetHex = textTargetColorHex.getText();
                
                if (!ColorReplaceHelper.isValidHex(targetHex)) {
                    return;
                }
                
                BufferedImage processed;

                double intensity = spinnerIntensity.getValue();
                int enhancement = spinnerEnhancement.getValue();

                if (checkBoxReplaceAllColors.isSelected()) {
                    processed = ColorReplaceHelper.replaceAllColors(originalBufferedImage, targetHex, intensity, enhancement);
                } else {
                    String sourceHex = textSourceColorHex.getText();
                    if (!ColorReplaceHelper.isValidHex(sourceHex)) {
                        return;
                    }
                    int smoothing = spinnerSmoothing.getValue();
                    processed = ColorReplaceHelper.replaceColor(originalBufferedImage, sourceHex, targetHex, intensity, smoothing, enhancement);
                }
                
                currentBufferedImage = processed;
                setPreview(currentBufferedImage);
            } catch (Exception e) {
                ErrorLogger.error("Error updating preview: " + e.getMessage());
            }
        });
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Color Replace",
                """
                        How to use:
                        1. Select an image file using 'Select image' or drag and drop.
                        2. Choose target color (new color) from combo or enter HEX.
                        3. Check 'Replace All Colors' to shift all colors, or leave unchecked to replace specific color.
                        4. If replacing specific color: choose source color from combo or enter HEX.
                        5. Adjust intensity (1-100) for replacement strength.
                        6. Adjust smoothing (0-50) for color transition softness (specific color mode only).
                        7. Adjust enhancement (1-100) for target color strength.
                        8. Select output format (JPEG or PNG).
                        9. Click 'Replace Color and Download'.
                        
                        This tool replaces specific color or shifts all colors in your image.
                        
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
    public void submitReplaceColorAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties) || currentBufferedImage == null) {
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                String targetHex = textTargetColorHex.getText();
                double intensity = spinnerIntensity.getValue();
                int enhancement = spinnerEnhancement.getValue();

                BufferedImage processedImage;
                
                if (checkBoxReplaceAllColors.isSelected()) {
                    processedImage = ColorReplaceHelper.replaceAllColors(originalBufferedImage, targetHex, intensity, enhancement);
                } else {
                    String sourceHex = textSourceColorHex.getText();
                    int smoothing = spinnerSmoothing.getValue();
                    processedImage = ColorReplaceHelper.replaceColor(originalBufferedImage, sourceHex, targetHex, intensity, smoothing, enhancement);
                }

                updateProgress(50, 100);

                String outputFormat = toggleJPEG.isSelected() ? "jpeg" : "png";
                File outputFile = Util.createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        outputFormat
                );

                updateProgress(80, 100);

                ImagePreprocessing.downloadImage(processedImage, outputFormat, outputFile);
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
        ErrorLogger.info("Color replacement successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Color replaced and saved!", imageProperties.getHideSuccessMessageTimer());
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

        currentBufferedImage = null;
        originalBufferedImage = null;
        
        comboSourceColor.setValue("Red");
        comboTargetColor.setValue("Blue");
        textSourceColorHex.setText("#FF0000");
        textTargetColorHex.setText("#0000FF");
        spinnerIntensity.getValueFactory().setValue(50.0);
        spinnerSmoothing.getValueFactory().setValue(5);
        spinnerEnhancement.getValueFactory().setValue(50);
        togglePNG.setSelected(false);
        toggleJPEG.setSelected(false);
        checkBoxReplaceAllColors.setSelected(false);
        
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
                    updatePreview();
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
            Platform.runLater(() -> {
                Image image = SwingFXUtils.toFXImage(bi, null);
                imageViewPreview.setImage(image);
            });
        }
    }
}

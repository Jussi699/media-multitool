package controllers.appControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.logger.ErrorLogger;
import visual.controls.ComboBoxItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import static java.util.Objects.nonNull;

public class ViewController {
    @FXML private StackPane mainContentPane;

    @FXML private Button navHomeButton, navConverterImageButton, navConverterVideoButton, navConverterAudioButton;
    @FXML private Button navCompressorImage, navCompressorVideo, navInfo, navMediaEditorTag, navCompressorPdf, navMetaData;

    @FXML private VBox imageToolsContent, pdfToolsContent, watermarkContent, mediaToolsContent;
    @FXML private ToggleButton btnConverters, btnCompressors, btnImageTools, btnPdfTools, btnWatermark, btnMediaTools;

    private final List<Button> listSubBtn = new ArrayList<>();

    private final Map<Integer, String> fxmlMapImageTools = Map.ofEntries(
            Map.entry(1,  "image-tools-views/negative-image-view.fxml"),
            Map.entry(2,  "image-tools-views/rotate-image-view.fxml"),
            Map.entry(3,  "image-tools-views/lighten-image-view.fxml"),
            Map.entry(4,  "image-tools-views/darken-image-view.fxml"),
            Map.entry(5,  "image-tools-views/colorize-image-view.fxml"),
            Map.entry(6,  "image-tools-views/black-white-image-view.fxml"),
            Map.entry(7,  "image-tools-views/blur-image-view.fxml"),
            Map.entry(8,  "image-tools-views/spot-blur-image-view.fxml"),
            Map.entry(9,  "image-tools-views/find-color-pixel-image-view.fxml"),
            Map.entry(10, "image-tools-views/color-replace-image-view.fxml"),
            Map.entry(11, "image-tools-views/crop-image-view.fxml")
    );

    private final Map<Integer, String> fxmlMapPdfTools = Map.ofEntries(
            Map.entry(21, "pdf-views/converter-image-to-pdf-view.fxml"),
            Map.entry(22, "pdf-views/images-to-pdf-view.fxml"),
            Map.entry(23, "pdf-views/converter-pdf-to-image-view.fxml"),
            Map.entry(24, "pdf-views/remove-pages-pdf-view.fxml"),
            Map.entry(25, "pdf-views/protect-pdf-view.fxml"),
            Map.entry(26, "pdf-views/unlock-pdf-view.fxml"),
            Map.entry(27, "pdf-views/compressor-pdf-view.fxml"),
            Map.entry(28, "pdf-views/merge-pdf-view.fxml"),
            Map.entry(29, "pdf-views/split-pdf-view.fxml")
    );

    private final Map<Integer, String> fxmlMapWatermark = Map.ofEntries(
            Map.entry(41, "watermark-views/watermark-image-view.fxml"),
            Map.entry(42, "watermark-views/watermark-pdf-view.fxml"),
            Map.entry(43, "watermark-views/watermark-video-view.fxml")
    );

    private record ViewEntry(Node root, Object controller) {}
    private final Map<String, ViewEntry> viewCache = new HashMap<>();

    @FXML
    public void initialize() {
        listSubBtn.addAll(List.of(navConverterImageButton, navConverterVideoButton, navConverterAudioButton,
                navCompressorImage, navCompressorVideo, navCompressorPdf, navMediaEditorTag, navMetaData));

        populateVBox(imageToolsContent, List.of(
                new ComboBoxItem(1, "Negative photo"),
                new ComboBoxItem(2, "Rotate photo"),
                new ComboBoxItem(3, "Lighten photo"),
                new ComboBoxItem(4, "Darken photo"),
                new ComboBoxItem(5, "Colorize photo"),
                new ComboBoxItem(6, "Black-White photo"),
                new ComboBoxItem(7, "Blurry photo"),
                new ComboBoxItem(8, "Spot Blur"),
                new ComboBoxItem(9, "Find Pixel"),
                new ComboBoxItem(10, "Color Replace"),
                new ComboBoxItem(11, "Crop Image")
        ), this::onActionChoiceActionImage);

        populateVBox(pdfToolsContent, List.of(
                new ComboBoxItem(21, "Image To PDF"),
                new ComboBoxItem(22, "Image(s) To PDF"),
                new ComboBoxItem(23, "PDF To Image"),
                new ComboBoxItem(24, "Remove Pages PDF"),
                new ComboBoxItem(25, "Protect PDF"),
                new ComboBoxItem(26, "Unlock PDF"),
                new ComboBoxItem(27, "Compress PDF"),
                new ComboBoxItem(28, "Merge PDF"),
                new ComboBoxItem(29, "Split PDF")
        ), this::onActionChoiceActionPdf);

        populateVBox(watermarkContent, List.of(
                new ComboBoxItem(41, "Watermark Image"),
                new ComboBoxItem(42, "Watermark PDF"),
                new ComboBoxItem(43, "Watermark Video")
        ), this::onActionChoiceActionWatermark);

        showHomePage();
    }

    private void populateVBox(VBox box, List<ComboBoxItem> comboBoxItems, IntConsumer action) {
        for (ComboBoxItem comboBoxItem : comboBoxItems) {
            Button btn = new Button(comboBoxItem.title());
            btn.getStyleClass().add("sub-btn");
            btn.setPrefWidth(150);
            btn.setAlignment(Pos.BASELINE_LEFT);

            btn.setOnAction(_ -> {
                action.accept((int) comboBoxItem.id());
                setActiveSubButton(btn);
            });

            btn.setUserData((int) comboBoxItem.id());
            box.getChildren().add(btn);
            listSubBtn.add(btn);
        }
    }

    private ViewEntry loadView(String fxmlPath) {
        return viewCache.computeIfAbsent(fxmlPath, path -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewses/" + path));
                Node root = loader.load();
                Object controller = loader.getController();

                if (controller instanceof HomeViewController home) {
                    home.setMainController(this);
                }

                return new ViewEntry(root, controller);
            } catch (IOException e) {
                ErrorLogger.error(getClass(), "Failed to load view: " + path + ", " + e.getMessage());
                throw new IllegalStateException("Failed to load view: " + path, e);
            }
        });
    }

    private void setActiveStandalone(String fxmlPath, Button button) {
        ViewEntry entry = loadView(fxmlPath);
        mainContentPane.getChildren().setAll(entry.root());

        if (nonNull(btnConverters) && nonNull(btnConverters.getToggleGroup())) {
            btnConverters.getToggleGroup().selectToggle(null);
        }

        navHomeButton.getStyleClass().remove("standalone-btn-active");
        navInfo.getStyleClass().remove("standalone-btn-active");

        if (nonNull(button)) {
            button.getStyleClass().add("standalone-btn-active");
        }
        setActiveSubButton(null);
    }

    private void setActiveSub(String fxmlPath, Button button, ToggleButton toggleButton) {
        ViewEntry entry = loadView(fxmlPath);
        mainContentPane.getChildren().setAll(entry.root());

        navHomeButton.getStyleClass().remove("standalone-btn-active");
        navInfo.getStyleClass().remove("standalone-btn-active");

        setActiveSubButton(button);
        if (nonNull(toggleButton)) {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    public void showHomePage() {
        setActiveStandalone("home-view.fxml", navHomeButton);
    }

    @FXML
    public void showInfoPage() {
        setActiveStandalone("info-view.fxml", navInfo);
        ViewEntry entry = viewCache.get("info-view.fxml");
        if (nonNull(entry) && entry.controller() instanceof InfoController infoController) {
            infoController.callReloadLogArea();
        }
    }

    @FXML
    public void showConverterImagePage() {
        setActiveSub("converter-views/converter-image-view.fxml", navConverterImageButton, btnConverters);
    }

    @FXML
    public void showConverterVideoPage() {
        setActiveSub("converter-views/converter-video-view.fxml", navConverterVideoButton, btnConverters);
    }

    @FXML
    public void showConverterAudioPage() {
        setActiveSub("converter-views/converter-audio-view.fxml", navConverterAudioButton, btnConverters);
    }

    @FXML
    public void showCompressorImagePage() {
        setActiveSub("compressor-views/compressor-image-view.fxml", navCompressorImage, btnCompressors);
    }

    @FXML
    public void showCompressorVideoPage() {
        setActiveSub("compressor-views/compressor-video-view.fxml", navCompressorVideo, btnCompressors);
    }

    @FXML
    public void showCompressorPdfPage() {
        setActiveSub("pdf-views/compressor-pdf-view.fxml", navCompressorPdf, btnCompressors);
    }

    @FXML
    public void showMediaEditorTagPage() {
        setActiveSub("media-tools/media-tag-editor-view.fxml", navMediaEditorTag, btnMediaTools);
    }

    @FXML
    public void showMetaDataPage() {
        setActiveSub("media-tools/meta-data.fxml", navMetaData, btnMediaTools);
    }

    @FXML
    public void onActionChoiceActionPdf(int index) {
        String fxml = fxmlMapPdfTools.get(index);
        if (nonNull(fxml)) {
            setActiveSub(fxml, null, btnPdfTools);
            highlightSubButtonByUserData(pdfToolsContent, index);
        }
    }

    @FXML
    public void onActionChoiceActionWatermark(int index) {
        String fxml = fxmlMapWatermark.get(index);
        if (nonNull(fxml)) {
            setActiveSub(fxml, null, btnWatermark);
            highlightSubButtonByUserData(watermarkContent, index);
        }
    }

    @FXML
    public void onActionChoiceActionImage(int index) {
        String fxml = fxmlMapImageTools.get(index);
        if (nonNull(fxml)) {
            setActiveSub(fxml, null, btnImageTools);
            highlightSubButtonByUserData(imageToolsContent, index);
        }
    }

    private void highlightSubButtonByUserData(VBox box, int index) {
        for (Node node : box.getChildren()) {
            if (node instanceof Button btn) {
                if (nonNull(btn.getUserData()) && (int) btn.getUserData() == index) {
                    setActiveSubButton(btn);
                    break;
                }
            }
        }
    }

    private void setActiveSubButton(Button activeBtn) {
        for (Button btn : listSubBtn) {
            btn.getStyleClass().remove("sub-btn-active");
        }
        if (nonNull(activeBtn)) {
            if (!activeBtn.getStyleClass().contains("sub-btn-active")) {
                activeBtn.getStyleClass().add("sub-btn-active");
            }
            if (activeBtn == navHomeButton || activeBtn == navInfo) {
                activeBtn.getStyleClass().remove("standalone-btn-active");
                activeBtn.getStyleClass().add("standalone-btn-active");
            }
        }
    }

    @FXML
    public void showPage(ActionEvent actionEvent) {
        Button button = (Button) actionEvent.getSource();

        switch (button.getId()) {
            case "navHomeButton"           -> showHomePage();
            case "navConverterImageButton" -> showConverterImagePage();
            case "navConverterVideoButton" -> showConverterVideoPage();
            case "navConverterAudioButton" -> showConverterAudioPage();
            case "navCompressorImage"      -> showCompressorImagePage();
            case "navCompressorVideo"      -> showCompressorVideoPage();
            case "navCompressorPdf"        -> showCompressorPdfPage();
            case "navMediaEditorTag"       -> showMediaEditorTagPage();
            case "navMetaData"             -> showMetaDataPage();
            case "navInfo"                 -> showInfoPage();
            default -> throw new IllegalArgumentException("Unexpected id: " + button.getId());
        }
    }
}

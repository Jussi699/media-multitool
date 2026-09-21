package media_multitool;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.utility.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.IntConsumer;

public class ViewController {

    @FXML private StackPane audioEditorTagPage, blurPage, blackAndWhitePage, colorizePage, darkenPage, lightenPage, infoPage,
            compressorVideoPage, converterMP3Page, homeView, converterImagePage, converterVideoPage, compressorImagePage,
            negativeImagePage, rotateImagePage, currentPageFromComboBoxAction, findPixelPage, cropPage, imageToPdfPage,
            pdfToImagePage, imagesToPdfPage, protectPdfPage, colorReplaceInImagePage, unlockPdfPage, removePagesPdfPage,
            compressPdfPage, mergePdfPage, splitPdfPage, watermarkImagePane, watermarkPdfPane, watermarkVideoPane, spotBlurPage;

    @FXML private HomeViewController homeViewController;

    @FXML private Button navHomeButton, navConverterImageButton, navConverterVideoButton, navConverterAudioButton,
            navCompressorImage, navCompressorVideo, navInfo, navEditorAudioTag;

    @FXML private VBox imageToolsContent, pdfToolsContent, watermarkContent;
    @FXML private ToggleButton btnConverters, btnCompressors, btnImageTools, btnPdfTools, btnWatermark;

    private final Map<Integer, StackPane> stackPaneMapImageTools = new HashMap<>();
    private final Map<Integer, StackPane> stackPaneMapPdfTools = new HashMap<>();
    private final Map<Integer, StackPane> stackPaneMapWatermark = new HashMap<>();

    private final List<Button> listSubBtn = new ArrayList<>();

    @FXML
    public void initialize() {
        listSubBtn.addAll(List.of(navConverterImageButton, navConverterVideoButton, navConverterAudioButton, navCompressorImage, navCompressorVideo));

        if (homeViewController != null) {
            homeViewController.setMainController(this);
        }

        stackPaneMapImageTools.putAll(Map.ofEntries(
                Map.entry(1,  negativeImagePage),
                Map.entry(2,  rotateImagePage),
                Map.entry(3,  lightenPage),
                Map.entry(4,  darkenPage),
                Map.entry(5,  colorizePage),
                Map.entry(6,  blackAndWhitePage),
                Map.entry(7,  blurPage),
                Map.entry(8,  spotBlurPage),
                Map.entry(9,  findPixelPage),
                Map.entry(10, colorReplaceInImagePage),
                Map.entry(11, cropPage)
        ));

        stackPaneMapPdfTools.putAll(Map.of(
                21, imageToPdfPage,
                22, imagesToPdfPage,
                23, pdfToImagePage,
                24, removePagesPdfPage,
                25, protectPdfPage,
                26, unlockPdfPage,
                27, compressPdfPage,
                28, mergePdfPage,
                29, splitPdfPage
        ));

        stackPaneMapWatermark.putAll(Map.of(
                41, watermarkImagePane,
                42, watermarkPdfPane,
                43, watermarkVideoPane
        ));

        populateVBox(imageToolsContent, List.of(
                new Item(1, "Negative photo"),
                new Item(2, "Rotate photo"),
                new Item(3, "Lighten photo"),
                new Item(4, "Darken photo"),
                new Item(5, "Colorize photo"),
                new Item(6, "Black-White photo"),
                new Item(7, "Blurry photo"),
                new Item(8, "Spot Blur"),
                new Item(9, "Find Pixel"),
                new Item(10, "Color Replace"),
                new Item(11, "Crop Image")
        ), this::onActionChoiceActionImage);

        populateVBox(pdfToolsContent, List.of(
                new Item(21, "Image To PDF"),
                new Item(22, "Image(s) To PDF"),
                new Item(23, "PDF To Image"),
                new Item(24, "Remove Pages PDF"),
                new Item(25, "Protect PDF"),
                new Item(26, "Unlock PDF"),
                new Item(27, "Compress PDF"),
                new Item(28, "Merge PDF"),
                new Item(29, "Split PDF")
        ), this::onActionChoiceActionPdf);

        populateVBox(watermarkContent, List.of(
                new Item(41, "Watermark Image"),
                new Item(42, "Watermark PDF"),
                new Item(43, "Watermark Video")
        ), this::onActionChoiceActionWatermark);

        showHomePage();
    }

    private void populateVBox(VBox box, List<Item> items, IntConsumer action) {
        for (Item item : items) {
            Button btn = new Button(item.title());
            btn.getStyleClass().add("sub-btn");
            btn.setPrefWidth(150);
            btn.setAlignment(Pos.BASELINE_LEFT);

            btn.setOnAction(_ -> {
                action.accept((int) item.id());
                setActiveSubButton(btn);
            });

            btn.setUserData((int) item.id());
            box.getChildren().add(btn);
            listSubBtn.add(btn);
        }
    }

    @FXML
    public void showHomePage() {
        setActivePage(homeView, navHomeButton);
        if (btnConverters != null && btnConverters.getToggleGroup() != null) btnConverters.getToggleGroup().selectToggle(null);
    }

    @FXML
    public void showConverterImagePage() {
        setActivePage(converterImagePage, null);
        setActiveSubButton(navConverterImageButton);
        btnConverters.setSelected(true);
    }

    @FXML
    public void showConverterVideoPage(){
        setActivePage(converterVideoPage, null);
        setActiveSubButton(navConverterVideoButton);
        btnConverters.setSelected(true);
    }

    @FXML
    public void showConverterAudioPage() {
        setActivePage(converterMP3Page, null);
        setActiveSubButton(navConverterAudioButton);
        btnConverters.setSelected(true);
    }

    @FXML
    public void showCompressorImagePage() {
        setActivePage(compressorImagePage, null);
        setActiveSubButton(navCompressorImage);
        btnCompressors.setSelected(true);
    }

    @FXML
    public void showCompressorVideoPage() {
        setActivePage(compressorVideoPage, null);
        setActiveSubButton(navCompressorVideo);
        btnCompressors.setSelected(true);
    }

    @FXML
    public void showEditorAudioTagPage() {
        setActivePage(audioEditorTagPage, navEditorAudioTag);
        if (btnConverters != null && btnConverters.getToggleGroup() != null) btnConverters.getToggleGroup().selectToggle(null);
    }

    @FXML
    public void onActionChoiceActionPdf(int index) {
        currentPageFromComboBoxAction = stackPaneMapPdfTools.get(index);
        setActivePage(currentPageFromComboBoxAction, null);
        btnPdfTools.setSelected(true);
        highlightSubButtonByUserData(pdfToolsContent, index);
    }

    @FXML
    public void onActionChoiceActionWatermark(int index) {
        currentPageFromComboBoxAction = stackPaneMapWatermark.get(index);
        setActivePage(currentPageFromComboBoxAction, null);
        btnWatermark.setSelected(true);
        highlightSubButtonByUserData(watermarkContent, index);
    }

    @FXML
    public void onActionChoiceActionImage(int index) {
        currentPageFromComboBoxAction = stackPaneMapImageTools.get(index);
        setActivePage(currentPageFromComboBoxAction, null);
        btnImageTools.setSelected(true);
        highlightSubButtonByUserData(imageToolsContent, index);
    }

    private void highlightSubButtonByUserData(VBox box, int index) {
        for (Node node : box.getChildren()) {
            if (node instanceof Button btn) {
                if (btn.getUserData() != null && (int) btn.getUserData() == index) {
                    setActiveSubButton(btn);
                    break;
                }
            }
        }
    }

    @FXML
    public void showInfoPage() {
        setActivePage(infoPage, navInfo);
        if (btnConverters != null && btnConverters.getToggleGroup() != null) btnConverters.getToggleGroup().selectToggle(null);
    }

    private void setActiveSubButton(Button activeBtn) {
        for (Button btn : listSubBtn) {
            btn.getStyleClass().remove("sub-btn-active");
        }
        if (activeBtn != null) {
            if (!activeBtn.getStyleClass().contains("sub-btn-active")) {
                activeBtn.getStyleClass().add("sub-btn-active");
            }
            if (activeBtn == navHomeButton || activeBtn == navEditorAudioTag || activeBtn == navInfo) {
                activeBtn.getStyleClass().remove("standalone-btn-active");
                activeBtn.getStyleClass().add("standalone-btn-active");
            }
        }
    }

    private void setActivePage(StackPane pageToShow, Button activeButton) {
        StackPane[] allPages = {
                homeView, converterImagePage, converterVideoPage, converterMP3Page,
                compressorImagePage, compressorVideoPage, negativeImagePage, rotateImagePage,
                infoPage, lightenPage, darkenPage, colorizePage, blackAndWhitePage, blurPage,
                audioEditorTagPage, findPixelPage, cropPage, imageToPdfPage, pdfToImagePage,
                imagesToPdfPage, protectPdfPage, colorReplaceInImagePage, unlockPdfPage,
                removePagesPdfPage, compressPdfPage, mergePdfPage, splitPdfPage, watermarkImagePane,
                watermarkPdfPane, watermarkVideoPane, spotBlurPage
        };

        for (StackPane page : allPages) {
            if (page != null) {
                page.setVisible(page == pageToShow);
                page.setManaged(page == pageToShow);
            }
        }

        navHomeButton.getStyleClass().remove("standalone-btn-active");
        navEditorAudioTag.getStyleClass().remove("standalone-btn-active");
        navInfo.getStyleClass().remove("standalone-btn-active");

        if (activeButton != null) {
            activeButton.getStyleClass().add("standalone-btn-active");
            setActiveSubButton(null);
        }
    }
}

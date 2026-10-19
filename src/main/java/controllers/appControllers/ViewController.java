package controllers.appControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import visual.controls.ComboBoxItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.IntConsumer;

import static java.util.Objects.nonNull;

public class ViewController {

    @FXML private StackPane mediaTagEditorPage, blurPage, blackAndWhitePage, colorizePage, darkenPage, lightenPage, infoPage, metaDataPage;
    @FXML private StackPane compressorVideoPage, converterMP3Page, homeView, converterImagePage, converterVideoPage, compressorImagePage;
    @FXML private StackPane negativeImagePage, rotateImagePage, currentPageFromComboBoxAction, findPixelPage, cropPage, imageToPdfPage;
    @FXML private StackPane pdfToImagePage, imagesToPdfPage, protectPdfPage, colorReplaceInImagePage, unlockPdfPage, removePagesPdfPage;
    @FXML private StackPane compressPdfPage, mergePdfPage, splitPdfPage, watermarkImagePane, watermarkPdfPane, watermarkVideoPane, spotBlurPage;

    @FXML private HomeViewController homeViewController;

    @FXML private Button navHomeButton, navConverterImageButton, navConverterVideoButton, navConverterAudioButton;
    @FXML private Button navCompressorImage, navCompressorVideo, navInfo, navMediaEditorTag, navCompressorPdf, navMetaData;

    @FXML private VBox imageToolsContent, pdfToolsContent, watermarkContent, mediaToolsContent;
    @FXML private ToggleButton btnConverters, btnCompressors, btnImageTools, btnPdfTools, btnWatermark, btnMediaTools;

    @FXML private InfoController infoPageController;

    private final Map<Integer, StackPane> stackPaneMapImageTools = new HashMap<>();
    private final Map<Integer, StackPane> stackPaneMapPdfTools = new HashMap<>();
    private final Map<Integer, StackPane> stackPaneMapWatermark = new HashMap<>();

    private final List<Button> listSubBtn = new ArrayList<>();

    @FXML
    public void initialize() {
        listSubBtn.addAll(List.of(navConverterImageButton, navConverterVideoButton, navConverterAudioButton,
                navCompressorImage, navCompressorVideo, navCompressorPdf, navMediaEditorTag, navMetaData));

        if (nonNull(homeViewController)) {
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

    public void setActive(StackPane page, Button button) {
        setActivePage(page, button);
        if (nonNull(btnConverters) && nonNull(btnConverters.getToggleGroup())) {
            btnConverters.getToggleGroup().selectToggle(null);
        }
    }

    public void setActive(StackPane page, Button button, ToggleButton toggleButton) {
        setActivePage(page, null);
        setActiveSubButton(button);
        if (nonNull(toggleButton)) {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    public void showHomePage() {
        setActive(homeView, navHomeButton);
    }

    @FXML
    public void showInfoPage() {
        setActive(infoPage, navInfo);
        if (nonNull(infoPageController)) {
            infoPageController.callReloadLogArea();
        }
    }

    @FXML
    public void showConverterImagePage() {
        setActive(converterImagePage, navConverterImageButton, btnConverters);
    }

    @FXML
    public void showConverterVideoPage() {
        setActive(converterVideoPage, navConverterVideoButton, btnConverters);
    }

    @FXML
    public void showConverterAudioPage() {
        setActive(converterMP3Page, navConverterAudioButton, btnConverters);
    }

    @FXML
    public void showCompressorImagePage() {
        setActive(compressorImagePage, navCompressorImage, btnCompressors);
    }

    @FXML
    public void showCompressorVideoPage() {
        setActive(compressorVideoPage, navCompressorVideo, btnCompressors);
    }

    @FXML
    public void showCompressorPdfPage() {
        setActive(compressPdfPage, navCompressorPdf, btnCompressors);
    }

    @FXML
    public void showMediaEditorTagPage() {
        setActive(mediaTagEditorPage, navMediaEditorTag, btnMediaTools);
    }

    @FXML
    public void showMetaDataPage() {
        setActive(metaDataPage, navMetaData, btnMediaTools);
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

    private void setActivePage(StackPane pageToShow, Button activeButton) {
        StackPane[] allPages = {
                homeView, converterImagePage, converterVideoPage, converterMP3Page,
                compressorImagePage, compressorVideoPage, negativeImagePage, rotateImagePage,
                infoPage, lightenPage, darkenPage, colorizePage, blackAndWhitePage, blurPage,
                mediaTagEditorPage, findPixelPage, cropPage, imageToPdfPage, pdfToImagePage,
                imagesToPdfPage, protectPdfPage, colorReplaceInImagePage, unlockPdfPage,
                removePagesPdfPage, compressPdfPage, mergePdfPage, splitPdfPage, watermarkImagePane,
                watermarkPdfPane, watermarkVideoPane, spotBlurPage, metaDataPage
        };

        for (StackPane page : allPages) {
            if (nonNull(page)) {
                page.setVisible(page == pageToShow);
                page.setManaged(page == pageToShow);
            }
        }

        navHomeButton.getStyleClass().remove("standalone-btn-active");
        navInfo.getStyleClass().remove("standalone-btn-active");

        if (nonNull(activeButton)) {
            activeButton.getStyleClass().add("standalone-btn-active");
            setActiveSubButton(null);
        }
    }

    @FXML
    public void showPage(ActionEvent actionEvent) {
        Button button = (Button) actionEvent.getSource();

        switch (button.getId()) {
            case "navHomeButton"           -> setActive(homeView, navHomeButton);
            case "navConverterImageButton" -> setActive(converterImagePage, navConverterImageButton, btnConverters);
            case "navConverterVideoButton" -> setActive(converterVideoPage, navConverterVideoButton, btnConverters);
            case "navConverterAudioButton" -> setActive(converterMP3Page, navConverterAudioButton, btnConverters);
            case "navCompressorImage"      -> setActive(compressorImagePage, navCompressorImage, btnCompressors);
            case "navCompressorVideo"      -> setActive(compressorVideoPage, navCompressorVideo, btnCompressors);
            case "navCompressorPdf"        -> setActive(compressPdfPage, navCompressorPdf, btnCompressors);
            case "navMediaEditorTag"       -> setActive(mediaTagEditorPage, navMediaEditorTag, btnMediaTools);
            case "navMetaData"             -> setActive(metaDataPage, navMetaData, btnMediaTools);
            case "navInfo"                 ->  {
                setActive(infoPage, navInfo);
                showInfoPage();
            }
        }
    }
}

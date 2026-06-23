package media_multitool;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import model.utility.Item;
import viewHelp.ComboBoxes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewController {

    @FXML private StackPane audioEditorTagPage, blurPage, blackAndWhitePage, colorizePage, darkenPage, lightenPage, infoPage,
            compressorVideoPage, converterMP3Page, homeView, converterImagePage, converterVideoPage, compressorImagePage,
            negativeImagePage, turnImagePage, currentPageFromComboBoxAction, findPixelPage, cropPage, imageToPdfPage,
            pdfToImagePage, imagesToPdfPage, protectPdfPage, colorReplaceInImagePage, unlockPdfPage, removePagesPdfPage,
            compressPdfPage, mergePdfPage, splitPdfPage, watermarkImagePane;

    @FXML private HomeViewController homeViewController;

    @FXML private Button navHomeButton, navConverterImageButton, navConverterVideoButton, navConverterAudioButton,
            navCompressorImage, navCompressorVideo, navInfo, navEditorAudioTag;

    @FXML private ComboBox<Item> comboBoxChoiceActionImage, comboBoxChoiceActionPdf, comboBoxChoiceActionWatermark;

    /* index page image
      1, negativeImagePage,
      2, turnImagePage,
      3, lightenPage,
      4, darkenPage,
      5, colorizePage,
      6, blackAndWhitePage,
      7, blurPage
      8  findPixelPage
      8  cropPage,
      10 colorReplaceInImagePage,
      */

    /* index page pdf
       21, imageToPdfPage,
       22, pdfToImagePage,
       23, imagesToPdfPage,
       24, protectPdfPage,
       25, unlockPdfPage,
       26, removePagesPdfPage,
       27, compressPdfPage,
       28, mergePdfPage,
       29, splitPdfPage
       */

    /* index page watermark
       1, watermarkImagePane
     */

    private final Map<Integer, StackPane> stackPaneMapImageTools = new HashMap<>();
    private final Map<Integer, StackPane> stackPaneMapPdfTools = new HashMap<>();
    private final Map<Integer, StackPane> stackPaneMapWatermark = new HashMap<>();

    private List<Button> listNavBtn;

    @FXML
    public void initialize() {
        listNavBtn = List.of(navHomeButton, navConverterImageButton, navConverterVideoButton, navConverterAudioButton,
                navCompressorImage, navCompressorVideo, navInfo, navEditorAudioTag);

        if (homeViewController != null) {
            homeViewController.setMainController(this);
        }

        showHomePage();

        // Image
        comboBoxChoiceActionImage.setPromptText("Image tools");
        ComboBoxes.setupComboBox(comboBoxChoiceActionImage, Item::title);
        comboBoxChoiceActionImage.getSelectionModel().clearSelection();
        comboBoxChoiceActionImage.buttonCellProperty().bind(Bindings.createObjectBinding(() -> {
            if (comboBoxChoiceActionImage.getSelectionModel().isEmpty()) {
                return new javafx.scene.control.ListCell<Item>() {
                    @Override
                    protected void updateItem(Item item, boolean empty) {
                        super.updateItem(item, empty);
                        setText("Image tools");
                    }
                };
            }
            return null;
        }, comboBoxChoiceActionImage.getSelectionModel().selectedItemProperty()));

        // PDF
        comboBoxChoiceActionPdf.setPromptText("PDF tools");
        ComboBoxes.setupComboBox(comboBoxChoiceActionPdf, Item::title);
        comboBoxChoiceActionPdf.getSelectionModel().clearSelection();
        comboBoxChoiceActionPdf.buttonCellProperty().bind(Bindings.createObjectBinding(() -> {
            if (comboBoxChoiceActionPdf.getSelectionModel().isEmpty()) {
                return new javafx.scene.control.ListCell<Item>() {
                    @Override
                    protected void updateItem(Item item, boolean empty) {
                        super.updateItem(item, empty);
                        setText("PDF tools");
                    }
                };
            }
            return null;
        }, comboBoxChoiceActionPdf.getSelectionModel().selectedItemProperty()));

        // Watermark
        comboBoxChoiceActionWatermark.setPromptText("Watermark");
        ComboBoxes.setupComboBox(comboBoxChoiceActionWatermark, Item::title);
        comboBoxChoiceActionWatermark.getSelectionModel().clearSelection();
        comboBoxChoiceActionWatermark.buttonCellProperty().bind(Bindings.createObjectBinding(() -> {
            if (comboBoxChoiceActionWatermark.getSelectionModel().isEmpty()) {
                return new javafx.scene.control.ListCell<Item>() {
                    @Override
                    protected void updateItem(Item item, boolean empty) {
                        super.updateItem(item, empty);
                        setText("Watermark");
                    }
                };
            }
            return null;
        }, comboBoxChoiceActionWatermark.getSelectionModel().selectedItemProperty()));

        stackPaneMapImageTools.putAll(Map.of(
                1, negativeImagePage,
                2, turnImagePage,
                3, lightenPage,
                4, darkenPage,
                5, colorizePage,
                6, blackAndWhitePage,
                7, blurPage,
                8, findPixelPage,
                9, cropPage,
                10, colorReplaceInImagePage
        ));

        stackPaneMapPdfTools.putAll(Map.of(
                21, imageToPdfPage,
                22, pdfToImagePage,
                23, imagesToPdfPage,
                24, removePagesPdfPage,
                25, protectPdfPage,
                26, unlockPdfPage,
                27, compressPdfPage,
                28, mergePdfPage,
                29, splitPdfPage
        ));

        stackPaneMapWatermark.put(
                41, watermarkImagePane
        );

        comboBoxChoiceActionWatermark.getItems().addAll(
                new Item(41, "Watermark Image")
        );

        comboBoxChoiceActionImage.getItems().addAll(
                new Item(1, "Negative photo"),
                new Item(2, "Turn photo"),
                new Item(3, "Lighten photo"),
                new Item(4, "Darken photo"),
                new Item(5, "Colorize photo"),
                new Item(6, "Black-White photo"),
                new Item(7, "Blurry photo"),
                new Item(8, "Find Pixel"),
                new Item(9, "Crop Image"),
                new Item(10, "Color Replace")
        );

        comboBoxChoiceActionPdf.getItems().addAll(
                new Item(21, "Image To PDF"),
                new Item(22, "PDF To Image"),
                new Item(23, "Image(s) To PDF"),
                new Item(24, "Remove Pages PDF"),
                new Item(25, "Protect PDF"),
                new Item(26, "Unlock PDF"),
                new Item(27, "Compress PDF"),
                new Item(28, "Merge PDF"),
                new Item(29, "Split PDF")
        );
    }

    @FXML
    public void showHomePage() {
        setActivePage(homeView, navHomeButton);
    }

    @FXML
    public void showConverterImagePage() {
        setActivePage(converterImagePage, navConverterImageButton);
    }

    @FXML
    public void showConverterVideoPage(){
        setActivePage(converterVideoPage, navConverterVideoButton);
    }

    @FXML
    public void showConverterAudioPage() {
        setActivePage(converterMP3Page, navConverterAudioButton);
    }

    @FXML
    public void showCompressorImagePage() {
        setActivePage(compressorImagePage, navCompressorImage);
    }

    @FXML
    public void showCompressorVideoPage() {
        setActivePage(compressorVideoPage, navCompressorVideo);
    }

    @FXML
    public void showEditorAudioTagPage() {
        setActivePage(audioEditorTagPage, navEditorAudioTag);
    }

    @FXML
    public void onActionChoiceActionPdf() {
        Item selectedItem = comboBoxChoiceActionPdf.getValue();
        if (selectedItem != null) {
            currentPageFromComboBoxAction = stackPaneMapPdfTools.get((int) selectedItem.id());
            setActivePage(currentPageFromComboBoxAction, null);
            comboBoxChoiceActionPdf.getSelectionModel().clearSelection();
            comboBoxChoiceActionPdf.setStyle(getComboBoxStyle(true));
        }
    }

    @FXML
    public void onActionChoiceActionPdf(int index) {
        currentPageFromComboBoxAction = stackPaneMapPdfTools.get(index);
        setActivePage(currentPageFromComboBoxAction, null);
        comboBoxChoiceActionImage.getSelectionModel().clearSelection();
        
        comboBoxChoiceActionPdf.getItems().stream()
                .filter(item -> item.id() == index)
                .findFirst()
                .ifPresent(item -> comboBoxChoiceActionPdf.getSelectionModel().select(item));
        
        comboBoxChoiceActionPdf.setStyle(getComboBoxStyle(true));
    }

    @FXML
    public void onActionChoiceActionWatermark() {
        Item selectedItem = comboBoxChoiceActionWatermark.getValue();
        if (selectedItem != null) {
            currentPageFromComboBoxAction = stackPaneMapWatermark.get((int) selectedItem.id());
            setActivePage(currentPageFromComboBoxAction, null);
            comboBoxChoiceActionWatermark.getSelectionModel().clearSelection();
            comboBoxChoiceActionWatermark.setStyle(getComboBoxStyle(true));
        }
    }

    @FXML
    public void onActionChoiceActionWatermark(int index) {
        currentPageFromComboBoxAction = stackPaneMapWatermark.get(index);
        setActivePage(currentPageFromComboBoxAction, null);
        comboBoxChoiceActionWatermark.getSelectionModel().clearSelection();

        comboBoxChoiceActionWatermark.getItems().stream()
                .filter(item -> item.id() == index)
                .findFirst()
                .ifPresent(item -> comboBoxChoiceActionWatermark.getSelectionModel().select(item));

        comboBoxChoiceActionWatermark.setStyle(getComboBoxStyle(true));
    }

    @FXML
    public void onActionChoiceActionImage() {
        Item selectedItem = comboBoxChoiceActionImage.getValue();
        if (selectedItem != null) {
            currentPageFromComboBoxAction = stackPaneMapImageTools.get((int) selectedItem.id());
            setActivePage(currentPageFromComboBoxAction, null);
            comboBoxChoiceActionPdf.getSelectionModel().clearSelection();
            comboBoxChoiceActionImage.setStyle(getComboBoxStyle(true));
        }
    }

    @FXML
    public void onActionChoiceActionImage(int index) {
            currentPageFromComboBoxAction = stackPaneMapImageTools.get(index);
            setActivePage(currentPageFromComboBoxAction, null);
            comboBoxChoiceActionPdf.getSelectionModel().clearSelection();
            
            comboBoxChoiceActionImage.getItems().stream()
                    .filter(item -> item.id() == index)
                    .findFirst()
                    .ifPresent(item -> comboBoxChoiceActionImage.getSelectionModel().select(item));
            
            comboBoxChoiceActionImage.setStyle(getComboBoxStyle(true));
    }

    @FXML
    public void showInfoPage() {
        setActivePage(infoPage, navInfo);
    }

    private void setActivePage(StackPane pageToShow, Button activeButton) {
        StackPane[] allPages = {
                homeView, converterImagePage, converterVideoPage, converterMP3Page,
                compressorImagePage, compressorVideoPage, negativeImagePage, turnImagePage,
                infoPage, lightenPage, darkenPage, colorizePage, blackAndWhitePage, blurPage,
                audioEditorTagPage, findPixelPage, cropPage, imageToPdfPage, pdfToImagePage,
                imagesToPdfPage, protectPdfPage, colorReplaceInImagePage, unlockPdfPage,
                removePagesPdfPage, compressPdfPage, mergePdfPage, splitPdfPage, watermarkImagePane
        };

        for (StackPane page : allPages) {
            if (page != null) {
                page.setVisible(page == pageToShow);
                page.setManaged(page == pageToShow);
            }
        }

        comboBoxChoiceActionImage.setStyle(getComboBoxStyle(false));
        comboBoxChoiceActionPdf.setStyle(getComboBoxStyle(false));
        comboBoxChoiceActionWatermark.setStyle(getComboBoxStyle(false));

        if (activeButton != null) {
            comboBoxChoiceActionImage.getSelectionModel().clearSelection();
            comboBoxChoiceActionPdf.getSelectionModel().clearSelection();
            comboBoxChoiceActionWatermark.getSelectionModel().clearSelection();
        }

        listNavBtn.forEach(btn -> btn.setStyle(getNavButtonStyle(activeButton == btn)));
    }

    private String getNavButtonStyle(boolean active) {
        if (active) {
            return "-fx-background-color: #32CD32; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 8;";
        }
        return "-fx-background-color: #323232; -fx-text-fill: white; -fx-background-radius: 8;";
    }

    private String getComboBoxStyle(boolean active) {
        if (active) {
            return "-fx-background-color: #32CD32; -fx-background-radius: 8; -fx-font-weight: bold;";
        }
        return "-fx-background-color: #323232; -fx-background-radius: 8;";
    }
}

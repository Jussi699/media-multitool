package media_multitool;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import model.utility.Item;
import viewHelp.ComboBoxes;

import java.util.HashMap;
import java.util.Map;

public class ControllerView {

    @FXML private StackPane blurPage;
    @FXML private StackPane blackAndWhitePage;
    @FXML private StackPane colorizePage;
    @FXML private StackPane darkenPage;
    @FXML private StackPane lightenPage;
    @FXML private StackPane infoPage;
    @FXML private StackPane compressorVideoPage;
    @FXML private StackPane converterMP3Page;
    @FXML private StackPane homeView;
    @FXML private StackPane converterImagePage;
    @FXML private StackPane converterVideoPage;
    @FXML private StackPane compressorImagePage;

    @FXML private StackPane negativeImagePage;
    @FXML private StackPane turnImagePage;
    @FXML private StackPane currentPageFromComboBoxAction;

    @FXML private HomeViewController homeViewController;
    @FXML private Button navHomeButton;
    @FXML private Button navConverterImageButton;
    @FXML private Button navConverterVideoButton;
    @FXML private Button navConverterAudioButton;
    @FXML private Button navCompressorImage;
    @FXML private Button navCompressorVideo;
    @FXML private Button navInfo;

    @FXML private ComboBox<Item> comboBoxChoiceActionImage;

    /** index page
     * 1, negativeImagePage,
     * 2, turnImagePage,
     * 3, lightenPage,
     * 4, darkenPage,
     * 5, colorizePage,
     * 6, blackAndWhitePage,
     * 7, blurPage
     * **/

    private final Map<Integer, StackPane> stackPaneMap = new HashMap<>();

    @FXML
    public void initialize() {
        if (homeViewController != null) {
            homeViewController.setMainController(this);
        }

        showHomePage();

        comboBoxChoiceActionImage.setPromptText("Image tools");
        ComboBoxes.setupComboBox(comboBoxChoiceActionImage, Item::title);
        comboBoxChoiceActionImage.getSelectionModel().clearSelection();
        comboBoxChoiceActionImage.buttonCellProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(() -> {
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

        stackPaneMap.putAll(Map.of(
                1, negativeImagePage,
                2, turnImagePage,
                3, lightenPage,
                4, darkenPage,
                5, colorizePage,
                6, blackAndWhitePage,
                7, blurPage
        ));

        comboBoxChoiceActionImage.getItems().addAll(
                new Item(1, "Negative photo"),
                new Item(2, "Turn photo"),
                new Item(3, "Lighten photo"),
                new Item(4, "Darken photo"),
                new Item(5, "Colorize photo"),
                new Item(6, "Black-White photo"),
                new Item(7, "Blurry photo")
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
    public void onActionChoiceActionImage() {
        Item selectedItem = comboBoxChoiceActionImage.getValue();
        if (selectedItem != null) {
            currentPageFromComboBoxAction = stackPaneMap.get((int) selectedItem.id());
            setActivePage(currentPageFromComboBoxAction, null);
            comboBoxChoiceActionImage.setStyle(getComboBoxStyle(true));
        }
    }

    @FXML
    public void onActionChoiceActionImage(int index) {
            currentPageFromComboBoxAction = stackPaneMap.get(index);
            setActivePage(currentPageFromComboBoxAction, null);
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
                infoPage, lightenPage, darkenPage, colorizePage, blackAndWhitePage, blurPage
        };

        for (StackPane page : allPages) {
            if (page != null) {
                page.setVisible(page == pageToShow);
                page.setManaged(page == pageToShow);
            }
        }

        if (activeButton != null) {
            comboBoxChoiceActionImage.getSelectionModel().clearSelection();
            comboBoxChoiceActionImage.setStyle(getComboBoxStyle(false));
        }

        navHomeButton.setStyle(getNavButtonStyle(activeButton == navHomeButton));
        navConverterImageButton.setStyle(getNavButtonStyle(activeButton == navConverterImageButton));
        navConverterVideoButton.setStyle(getNavButtonStyle(activeButton == navConverterVideoButton));
        navConverterAudioButton.setStyle(getNavButtonStyle(activeButton == navConverterAudioButton));
        navCompressorImage.setStyle(getNavButtonStyle(activeButton == navCompressorImage));
        navCompressorVideo.setStyle(getNavButtonStyle(activeButton == navCompressorVideo));
        navInfo.setStyle(getNavButtonStyle(activeButton == navInfo));
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

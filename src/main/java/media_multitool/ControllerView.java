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

    @FXML private ComboBox<Item> comboBoxChoiceActionImage;
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
                2, turnImagePage
        ));

        comboBoxChoiceActionImage.getItems().addAll(
                new Item(1, "Negative photo"),
                new Item(2, "Turn photo")
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
        }
    }

    @FXML
    public void showInfoPage() {
        setActivePage(infoPage, navInfo);
    }

    private void setActivePage(StackPane pageToShow, Button activeButton) {
        StackPane[] allPages = {
                homeView, converterImagePage, converterVideoPage, converterMP3Page,
                compressorImagePage, compressorVideoPage, negativeImagePage, turnImagePage, infoPage
        };

        for (StackPane page : allPages) {
            if (page != null) {
                page.setVisible(page == pageToShow);
                page.setManaged(page == pageToShow);
            }
        }

        if (activeButton != null) {
            comboBoxChoiceActionImage.getSelectionModel().clearSelection();
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
}

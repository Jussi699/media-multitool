package media_multitool;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class ControllerView {

    @FXML private StackPane infoPage;
    @FXML private StackPane compressorVideoPage;
    @FXML private StackPane converterMP3Page;
    @FXML private StackPane homeView;
    @FXML private StackPane converterImagePage;
    @FXML private StackPane converterVideoPage;
    @FXML private StackPane compressorImagePage;
    @FXML private HomeViewController homeViewController;
    @FXML private Button navHomeButton;
    @FXML private Button navConverterImageButton;
    @FXML private Button navConverterVideoButton;
    @FXML private Button navConverterAudioButton;
    @FXML private Button navCompressorImage;
    @FXML private Button navCompressorVideo;
    @FXML private Button navInfo;

    @FXML
    public void initialize() {
        if (homeViewController != null) {
            homeViewController.setMainController(this);
        }

        showHomePage();
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
    public void showInfoPage() {
        setActivePage(infoPage, navInfo);
    }

    private void setActivePage(StackPane pageToShow, Button activeButton) {
        homeView.setVisible(pageToShow == homeView);
        homeView.setManaged(pageToShow == homeView);

        converterImagePage.setVisible(pageToShow == converterImagePage);
        converterImagePage.setManaged(pageToShow == converterImagePage);

        converterVideoPage.setVisible(pageToShow == converterVideoPage);
        converterVideoPage.setManaged(pageToShow == converterVideoPage);

        converterMP3Page.setVisible(pageToShow == converterMP3Page);
        converterMP3Page.setManaged(pageToShow == converterMP3Page);

        compressorImagePage.setVisible(pageToShow == compressorImagePage);
        compressorImagePage.setManaged(pageToShow == compressorImagePage);

        compressorVideoPage.setVisible(pageToShow == compressorVideoPage);
        compressorVideoPage.setManaged(pageToShow == compressorVideoPage);

        infoPage.setVisible(pageToShow == infoPage);
        infoPage.setManaged(pageToShow == infoPage);

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

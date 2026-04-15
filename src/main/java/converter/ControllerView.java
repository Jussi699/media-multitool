package converter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ControllerView {

    @FXML private VBox converterMP3Page;
    @FXML private StackPane rightPane;
    @FXML private VBox leftPane;
    @FXML private VBox homeView;
    @FXML private VBox converterImagePage;
    @FXML private VBox converterVideoPage;
    @FXML private HomeViewController homeViewController;
    @FXML private ConverterImageViewController converterImageViewController;
    @FXML private Button navHomeButton;
    @FXML private Button navConverterImageButton;
    @FXML private Button navConverterVideoButton;
    @FXML private Button navConverterMP3Button;

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
    public void showConverterMP3Page() {
        setActivePage(converterMP3Page, navConverterMP3Button);
    }

    private void setActivePage(VBox pageToShow, Button activeButton) {
        homeView.setVisible(pageToShow == homeView);
        homeView.setManaged(pageToShow == homeView);
        converterImagePage.setVisible(pageToShow == converterImagePage);
        converterImagePage.setManaged(pageToShow == converterImagePage);
        converterVideoPage.setVisible(pageToShow == converterVideoPage);
        converterVideoPage.setManaged(pageToShow == converterVideoPage);
        converterMP3Page.setVisible(pageToShow == converterMP3Page);
        converterMP3Page.setManaged(pageToShow == converterMP3Page);

        navHomeButton.setStyle(getNavButtonStyle(activeButton == navHomeButton));
        navConverterImageButton.setStyle(getNavButtonStyle(activeButton == navConverterImageButton));
        navConverterVideoButton.setStyle(getNavButtonStyle(activeButton == navConverterVideoButton));
        navConverterMP3Button.setStyle(getNavButtonStyle(activeButton == navConverterMP3Button));
    }

    private String getNavButtonStyle(boolean active) {
        if (active) {
            return "-fx-background-color: #32CD32; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 8;";
        }
        return "-fx-background-color: #323232; -fx-text-fill: white; -fx-background-radius: 8;";
    }
}

package converter;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class HomeViewController {
    @FXML private VBox homePage;
    @FXML private Button btnOpenImageConverter;
    @FXML private Button btnOpenVideoConverter;
    private ControllerView mainController;

    public void setMainController(ControllerView mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void onOpenImageConverterPressed() {
        if (mainController != null) {
            mainController.showConverterImagePage();
        }
    }

    @FXML
    private void onOpenVideoConverterPressed() {
        if (mainController != null) {
            mainController.showConverterVideoPage();
        }
    }
}

package converter;

import Model.Converter.Converter;
import Model.WorkWithFiles.ClassSelect;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Paths;

public class Controller {
    private static File image;
    private static File outputPath ;
    private static String typeImage;

    @FXML
    private AnchorPane AnchorPane;

    @FXML
    private Pane mainPane;

    @FXML
    private Button btnSelectPhotoFile;

    @FXML
    private Label LabelConvertPhoto;

    @FXML
    private ToggleButton btnToPNG;

    @FXML
    private ToggleButton btnToJPEG;

    @FXML
    private ToggleButton btnToICO;

    @FXML
    private Button btnSubmitConvert;

    @FXML
    private ProgressBar ProgressBarCompleteConvert;

    @FXML
    private Button btnChoiceDirForSaveImage;

    @FXML
    public void initialize() {
        assert AnchorPane != null : "fx:id=\"AnchorPane\" was not injected!";
        assert mainPane != null : "fx:id=\"mainPane\" was not injected!";
        assert btnSelectPhotoFile != null : "fx:id=\"btnSelectPhotoFile\" was not injected!";
        assert LabelConvertPhoto != null : "fx:id=\"LabelConvertPhoto\" was not injected!";
        assert btnToPNG != null : "fx:id=\"btnToPNG\" was not injected!";
        assert btnToJPEG != null : "fx:id=\"btnToJPEG\" was not injected!";
        assert btnToICO != null : "fx:id=\"btnToICO\" was not injected!";
        assert btnSubmitConvert != null : "fx:id=\"btnSubmitConvert\" was not injected!";
        assert ProgressBarCompleteConvert != null : "fx:id=\"ProgressBarCompleteConvert\" was not injected!";
        assert btnChoiceDirForSaveImage != null : "fx:id=\"btnChoiceDirForSaveImage\" was not injected!";

        outputPath = Paths.get(System.getProperty("user.home"), "Desktop").toFile();
    }

    @FXML
    private void ActionBtnToPNG(MouseEvent event) {
        btnToICO.setSelected(false);
        btnToJPEG.setSelected(false);
        typeImage = "png";
    }

    @FXML
    private void ActionBtnToICO(MouseEvent event) {
        btnToJPEG.setSelected(false);
        btnToPNG.setSelected(false);
        typeImage = "ico";
    }

    @FXML
    private void ActionBtnToJPEG(MouseEvent event) {
        btnToICO.setSelected(false);
        btnToPNG.setSelected(false);
        typeImage = "jpeg";
    }

    @FXML
    public void ActionBtnSelectFile(MouseEvent mouseEvent) {
        ClassSelect selectImageFile = new ClassSelect();
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        image = selectImageFile.choiceFile(mouseEvent, stage);
    }

    public void btnChoiceDirForSaveImage(MouseEvent mouseEvent) {
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        outputPath  = ClassSelect.setPathForSave(mouseEvent, stage);
    }

    public void SubmitConvertAndDownload(MouseEvent event) {
        if(btnToICO.isSelected()){
            Converter.convertToIco(image, outputPath, typeImage);
        }
        else{
            Converter.convert(image, outputPath, typeImage);
        }
    }
}

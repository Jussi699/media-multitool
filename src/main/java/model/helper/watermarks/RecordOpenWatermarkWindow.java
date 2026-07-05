package model.helper.watermarks;

import javafx.scene.control.Button;
import javafx.stage.Stage;

public record RecordOpenWatermarkWindow(Stage[] stageHolder,
                                        String fxmlPath,
                                        String title,
                                        WatermarkSettings currentWatermarkSettings,
                                        Button ownerButton,
                                        WatermarkSettings.WatermarkType expectedType) {
}

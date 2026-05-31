package model.utility;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public record ResetContext(
    Label labelSelectFileName,
    Label labelSuccess,
    Label textDragZone,
    Label labelPreviewPlaceholder,
    StackPane dropZone,
    ImageView imageViewPreview,
    ProgressBar progressBar,
    boolean managed
) {}

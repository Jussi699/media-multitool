package viewHelp;

import javafx.animation.ScaleTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import lombok.Getter;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class PdfPagePreviewCard {
    @Getter private final StackPane container;
    @Getter private final int pageIndex;
    @Getter private final String cardId;

    private static final String STYLE_NORMAL = 
        "-fx-background-color: #2a2a2a; -fx-background-radius: 8; -fx-padding: 5; " +
        "-fx-border-color: #555; -fx-border-radius: 8; -fx-border-width: 1;";

    private static final String STYLE_DRAGGING = 
        "-fx-background-color: #3a3a3a; -fx-background-radius: 8; -fx-padding: 5; " +
        "-fx-border-color: #32CD32; -fx-border-radius: 8; -fx-border-width: 2;";

    private static final String STYLE_DRAG_OVER = 
        "-fx-background-color: #3a3a3a; -fx-background-radius: 8; -fx-padding: 5; " +
        "-fx-border-color: #FFD700; -fx-border-radius: 8; -fx-border-width: 2;";

    public PdfPagePreviewCard(BufferedImage pageImage, String fileName, int pageIndex, String cardId, Runnable onDelete,
                              java.util.function.Consumer<String> onDragDropped,
                              int containerWidth, int containerHeight, int imageContainerWidth, int imageContainerHeight) {
        this.pageIndex = pageIndex;
        this.cardId = cardId;
        
        this.container = new StackPane();
        container.setPrefSize(containerWidth, containerHeight);
        container.setStyle(STYLE_NORMAL);
        
        VBox contentBox = new VBox(5);
        contentBox.setAlignment(Pos.CENTER);
        
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(imageContainerWidth, imageContainerHeight);
        imageContainer.setStyle("-fx-background-color: transparent;");
        imageContainer.setAlignment(Pos.CENTER);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(imageContainerWidth);
        imageView.setFitHeight(imageContainerHeight);
        imageView.setPreserveRatio(true);
        
        if (pageImage != null) {
            Image img = SwingFXUtils.toFXImage(pageImage, null);
            imageView.setImage(img);
        }

        imageContainer.getChildren().add(imageView);

        String displayName = fileName;
        if (displayName.length() > 20) {
            displayName = displayName.substring(0, 17) + "...";
        }
        Label nameLabel = new Label(displayName + " (p." + (pageIndex + 1) + ")");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(containerWidth - 10);

        contentBox.getChildren().addAll(imageContainer, nameLabel);

        StackPane deleteButton = createDeleteButton(onDelete);

        container.getChildren().addAll(contentBox, deleteButton);

        setupDragAndDrop(onDragDropped);
    }

    private void setupDragAndDrop(Consumer<String> onDragDropped) {
        container.setOnDragDetected(event -> {
            Dragboard db = container.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(cardId);
            db.setContent(content);

            ScaleTransition st = new ScaleTransition(Duration.millis(100), container);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();

            container.setStyle(STYLE_DRAGGING);
            event.consume();
        });

        container.setOnDragOver(event -> {
            if (event.getGestureSource() != container && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
                container.setStyle(STYLE_DRAG_OVER);
            }
            event.consume();
        });

        container.setOnDragExited(event -> {
            container.setStyle(STYLE_NORMAL);
            event.consume();
        });

        container.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString() && onDragDropped != null) {
                onDragDropped.accept(db.getString());
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

        container.setOnDragDone(event -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), container);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();

            container.setStyle(STYLE_NORMAL);
            event.consume();
        });
    }

    private StackPane createDeleteButton(Runnable onDelete) {
        StackPane button = new StackPane();
        button.setPrefSize(24, 24);
        button.setMaxSize(24, 24);
        button.setMinSize(24, 24);
        
        Circle circle = new Circle(12);
        circle.setStyle("-fx-fill: #ff4444; -fx-stroke: white; -fx-stroke-width: 1;");
        
        Label xLabel = new Label("X");
        xLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        
        button.getChildren().addAll(circle, xLabel);
        StackPane.setAlignment(button, Pos.TOP_RIGHT);
        
        button.setOnMouseEntered(_ -> circle.setStyle("-fx-fill: #ff6666; -fx-stroke: white; -fx-stroke-width: 2;"));
        button.setOnMouseExited(_  -> circle.setStyle("-fx-fill: #ff4444; -fx-stroke: white; -fx-stroke-width: 1;"));
        button.setOnMouseClicked(_ -> onDelete.run());
        
        return button;
    }

    public void animateRemoval(Runnable onComplete) {
        ScaleTransition st = new ScaleTransition(Duration.millis(300), container);
        st.setToX(0);
        st.setToY(0);
        st.setOnFinished(_ -> onComplete.run());
        st.play();
    }
}

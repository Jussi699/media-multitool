package viewHelp;

import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import lombok.Getter;
import model.logger.ErrorLogger;

import java.io.File;
import java.util.function.Consumer;

/**
 * UI component for displaying an image preview card with drag-and-drop and delete functionality
 */
public class ImagePreviewCard {
    @Getter private final StackPane container;
    @Getter private final String imageId;

    // Style constants
    private static final String STYLE_NORMAL = 
        "-fx-background-color: #2a2a2a; -fx-background-radius: 8; -fx-padding: 5; " +
        "-fx-border-color: #555; -fx-border-radius: 8; -fx-border-width: 1;";
    private static final String STYLE_DRAGGING = 
        "-fx-background-color: #3a3a3a; -fx-background-radius: 8; -fx-padding: 5; " +
        "-fx-border-color: #32CD32; -fx-border-radius: 8; -fx-border-width: 2;";
    private static final String STYLE_DRAG_OVER = 
        "-fx-background-color: #3a3a3a; -fx-background-radius: 8; -fx-padding: 5; " +
        "-fx-border-color: #FFD700; -fx-border-radius: 8; -fx-border-width: 2;";

    /**
     * Creates a new image preview card
     * 
     * @param imageFile the image file to display
     * @param imageId unique identifier for this image
     * @param onDelete callback when delete button is clicked
     * @param onDragDropped callback when an item is dropped on this card (receives dragged image ID)
     */
    public ImagePreviewCard(
            File imageFile, 
            String imageId, 
            Runnable onDelete,
            Consumer<String> onDragDropped
    ) {
        this.imageId = imageId;
        
        // Main container
        this.container = new StackPane();
        container.setPrefSize(120, 140);
        container.setStyle(STYLE_NORMAL);
        
        // Content box with image and label
        VBox contentBox = new VBox(5);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMouseTransparent(false);
        
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(110, 110);
        imageContainer.setStyle("-fx-background-color: transparent;");
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setMouseTransparent(false);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(110);
        imageView.setFitHeight(110);
        imageView.setPreserveRatio(true);
        imageView.setMouseTransparent(true);
        StackPane.setAlignment(imageView, Pos.CENTER);
        
        try {
            Image img = new Image(imageFile.toURI().toString(), 110, 110, true, true);
            imageView.setImage(img);
        } catch (Exception e) {
            ErrorLogger.error("Error loading image preview: " + e.getMessage());
        }

        imageContainer.getChildren().add(imageView);

        Label nameLabel = new Label(imageFile.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        nameLabel.setMaxWidth(110);
        nameLabel.setWrapText(false);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMouseTransparent(true);

        contentBox.getChildren().addAll(imageContainer, nameLabel);

        StackPane deleteButton = createDeleteButton(onDelete);

        container.getChildren().addAll(contentBox, deleteButton);

        setupDragAndDrop(onDragDropped);
    }

    /**
     * Creates the delete button with hover effects
     */
    private StackPane createDeleteButton(Runnable onDelete) {
        StackPane button = new StackPane();
        button.setPrefSize(24, 24);
        button.setMaxSize(24, 24);
        button.setMinSize(24, 24);
        
        Circle circle = new Circle(12);
        circle.setStyle("-fx-fill: #ff4444; -fx-stroke: white; -fx-stroke-width: 1;");
        circle.setMouseTransparent(true);
        
        Label crossLabel = new Label("×");
        crossLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        crossLabel.setMouseTransparent(true);
        
        button.getChildren().addAll(circle, crossLabel);
        
        button.setOnMouseClicked(e -> {
            e.consume();
            if (onDelete != null) {
                onDelete.run();
            }
        });
        
        button.setStyle("-fx-cursor: hand;");
        button.setMouseTransparent(false);
        StackPane.setAlignment(button, Pos.TOP_RIGHT);
        StackPane.setMargin(button, new javafx.geometry.Insets(3, 3, 0, 0));
        
        // Hover effects
        button.setOnMouseEntered(_ -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
            st.setToX(1.2);
            st.setToY(1.2);
            st.play();
        });
        
        button.setOnMouseExited(_ -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        
        return button;
    }

    /**
     * Sets up drag and drop functionality
     */
    private void setupDragAndDrop(Consumer<String> onDragDropped) {
        container.setOnDragDetected(event -> {
            Dragboard db = container.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(imageId);
            db.setContent(content);
            
            ScaleTransition st = new ScaleTransition(Duration.millis(100), container);
            st.setToX(1.1);
            st.setToY(1.1);
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
                String draggedId = db.getString();
                onDragDropped.accept(draggedId);
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

    /**
     * Animates the removal of this card
     * 
     * @param onComplete callback when animation completes
     */
    public void animateRemoval(Runnable onComplete) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), container);
        st.setToX(0);
        st.setToY(0);
        st.setOnFinished(_ -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        st.play();
    }

}

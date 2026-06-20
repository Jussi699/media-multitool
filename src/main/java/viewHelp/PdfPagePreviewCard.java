package viewHelp;

import javafx.animation.ScaleTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import lombok.Getter;

import java.awt.image.BufferedImage;

public class PdfPagePreviewCard {
    @Getter private final StackPane container;
    @Getter private final int pageIndex;
    @Getter private final String cardId;

    private static final String STYLE_NORMAL = 
        "-fx-background-color: #2a2a2a; -fx-background-radius: 8; -fx-padding: 5; " +
        "-fx-border-color: #555; -fx-border-radius: 8; -fx-border-width: 1;";

    public PdfPagePreviewCard(BufferedImage pageImage, int pageIndex, String cardId, Runnable onDelete) {
        this.pageIndex = pageIndex;
        this.cardId = cardId;
        
        this.container = new StackPane();
        container.setPrefSize(120, 140);
        container.setStyle(STYLE_NORMAL);
        
        VBox contentBox = new VBox(5);
        contentBox.setAlignment(Pos.CENTER);
        
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(110, 110);
        imageContainer.setStyle("-fx-background-color: transparent;");
        imageContainer.setAlignment(Pos.CENTER);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(110);
        imageView.setFitHeight(110);
        imageView.setPreserveRatio(true);
        
        if (pageImage != null) {
            Image img = SwingFXUtils.toFXImage(pageImage, null);
            imageView.setImage(img);
        }

        imageContainer.getChildren().add(imageView);

        Label nameLabel = new Label("Page " + (pageIndex + 1));
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        nameLabel.setAlignment(Pos.CENTER);

        contentBox.getChildren().addAll(imageContainer, nameLabel);

        StackPane deleteButton = createDeleteButton(onDelete);

        container.getChildren().addAll(contentBox, deleteButton);
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
        
        button.setOnMouseEntered(e -> circle.setStyle("-fx-fill: #ff6666; -fx-stroke: white; -fx-stroke-width: 2;"));
        button.setOnMouseExited(e -> circle.setStyle("-fx-fill: #ff4444; -fx-stroke: white; -fx-stroke-width: 1;"));
        button.setOnMouseClicked(e -> onDelete.run());
        
        return button;
    }

    public void animateRemoval(Runnable onComplete) {
        ScaleTransition st = new ScaleTransition(Duration.millis(300), container);
        st.setToX(0);
        st.setToY(0);
        st.setOnFinished(e -> onComplete.run());
        st.play();
    }
}

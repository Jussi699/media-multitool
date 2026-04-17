module converter {
    exports model.converterImage;

    requires javafx.controls;
    requires javafx.fxml;

    requires java.desktop;
    requires image4j;
    requires org.slf4j;
    requires javafx.swing;
    requires com.luciad.imageio.webp;
    requires jave.core;

    opens converter to javafx.fxml;
    exports converter;
    exports model.converterMP3;
}
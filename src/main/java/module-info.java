module converter {
    exports model.converterImage;

    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires image4j;
    requires org.slf4j;
    requires javafx.swing;
    requires ch.qos.logback.classic;
    requires com.luciad.imageio.webp;
    requires jave.core;
    requires java.compiler;

    opens converter to javafx.fxml;
    exports converter;
    exports model.converterMP3;
}
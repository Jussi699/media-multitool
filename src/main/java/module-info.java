module media_multitool {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.desktop;
    requires image4j;
    requires org.slf4j;
    requires javafx.swing;
    requires jave.core;
    requires net.coobird.thumbnailator;
    requires org.apache.commons.io;
    requires java.prefs;

    opens media_multitool to javafx.fxml;
    exports model.converterImage;
    exports model.converterVideo;
    exports model.compressorImage;
    exports media_multitool;
    exports model.utility;
    exports model.properties;
    exports viewHelp;
}
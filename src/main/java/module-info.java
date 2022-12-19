module com.example.simplebrowser_7 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires javafx.web;
    requires com.google.gson;

    opens com.example.simplebrowser_7 to javafx.fxml;
    exports com.example.simplebrowser_7;
}
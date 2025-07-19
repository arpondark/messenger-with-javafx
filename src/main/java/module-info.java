module com.arpon7fx.ar.messenger {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.arpon7fx.ar.messenger to javafx.fxml;
    exports com.arpon7fx.ar.messenger;
}
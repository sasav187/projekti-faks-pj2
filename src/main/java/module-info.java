module org.unibl.etf.gui {
    requires javafx.controls;
    requires javafx.fxml;
    requires gs.core;
    requires com.fasterxml.jackson.databind;


    opens org.unibl.etf.gui to javafx.fxml;
    exports org.unibl.etf.gui;
}
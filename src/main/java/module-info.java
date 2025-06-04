module org.unibl.etf.pathfinder {
    requires javafx.controls;
    requires javafx.fxml;
    requires gs.core;
    requires com.fasterxml.jackson.databind;


    opens org.unibl.etf.pathfinder to javafx.fxml;
    exports org.unibl.etf.pathfinder;
}
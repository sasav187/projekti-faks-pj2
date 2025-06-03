module org.unibl.etf.pathfinder {
    requires javafx.controls;
    requires javafx.fxml;
    requires gs.core;


    opens org.unibl.etf.pathfinder to javafx.fxml;
    exports org.unibl.etf.pathfinder;
}
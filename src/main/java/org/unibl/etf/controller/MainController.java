package org.unibl.etf.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.unibl.etf.model.Departure;

public class MainController {

    @FXML
    private ComboBox<String> startCityCombo;

    @FXML
    private ComboBox<String> endCityCombo;

    @FXML
    private ComboBox<String> criteriaCombo;

    @FXML
    private TableView<Departure> routeTable;

    @FXML
    private TableColumn<Departure, String> fromCol;

    @FXML
    private TableColumn<Departure, String> toCol;

    @FXML
    private TableColumn<Departure, String> typeCol;

    @FXML
    private TableColumn<Departure, String> depTimeCol;

    @FXML
    private TableColumn<Departure, Integer> durationCol;

    @FXML
    private TableColumn<Departure, Integer> priceCol;

    @FXML
    private Label soldLabel;

    @FXML
    private Label revenueLabel;

    @FXML
    private Button moreRoutesBtn;

    @FXML
    private Button buyTicketBtn;

    // Ovo ćeš kasnije popuniti na osnovu JSON fajla
    private ObservableList<String> allCities = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // povezivanje kolona sa poljima iz klase Departure
        fromCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().from));
        toCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().to));
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type));
        depTimeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().departureTime));
        durationCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().duration).asObject());
        priceCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().price).asObject());

        // učitaj gradove iz JSON-a (kasnije implementirati)
        startCityCombo.setItems(allCities);
        endCityCombo.setItems(allCities);
    }

    @FXML
    public void onFindRoute() {
        // TODO: pronađi rutu i prikaži u tabeli
        System.out.println("Pretraga rute...");
    }

    @FXML
    public void onShowMoreRoutes() {
        // TODO: otvori prozor sa dodatnim rutama
        System.out.println("Prikaz dodatnih ruta...");
    }

    @FXML
    public void onBuyTicket() {
        // TODO: kupovina karte
        System.out.println("Kupovina karte...");
    }
}
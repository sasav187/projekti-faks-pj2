package org.unibl.etf.gui;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.unibl.etf.data.JsonLoader;
import org.unibl.etf.generator.TransportDataGenerator;
import org.unibl.etf.model.City;

import java.util.Map;

public class InputWindow {

    private final Stage primaryStage;
    private Map<String, City> cityMap;

    public InputWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void show() {
        BorderPane root = new BorderPane();

        GridPane inputGrid = new GridPane();
        inputGrid.setPadding(new Insets(10));
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);

        Label rowsLabel = new Label("Broj redova (n):");
        TextField rowsField = new TextField();

        Label colsLabel = new Label("Broj kolona (m):");
        TextField colsField = new TextField();

        Button generateBtn = new Button("Generiši mapu i prikaži graf");
        Label statusLabel = new Label();

        inputGrid.add(rowsLabel, 0, 0);
        inputGrid.add(rowsField, 1, 0);
        inputGrid.add(colsLabel, 0, 1);
        inputGrid.add(colsField, 1, 1);
        inputGrid.add(generateBtn, 0, 2, 2, 1);
        inputGrid.add(statusLabel, 0, 3, 2, 1);

        root.setCenter(inputGrid);

        Scene scene = new Scene(root, 350, 180);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Pathfinder - Unos dimenzija");
        primaryStage.show();

        generateBtn.setOnAction(e -> {
            try {
                int rows = Integer.parseInt(rowsField.getText());
                int cols = Integer.parseInt(colsField.getText());

                statusLabel.setText("Generišem...");

                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        TransportDataGenerator generator = new TransportDataGenerator(rows, cols);
                        TransportDataGenerator.TransportData data = generator.generateData();
                        generator.saveToJson(data, "transport_data.json");
                        cityMap = JsonLoader.loadCityMap("transport_data.json");
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        GraphWindow graphWindow = new GraphWindow(rows, cols, cityMap);
                        graphWindow.showGraph();
                        statusLabel.setText("Mapa generisana i prikazana.");
                        primaryStage.close();
                    }

                    @Override
                    protected void failed() {
                        Throwable ex = getException();
                        statusLabel.setText("Greška: " + (ex != null ? ex.getMessage() : "Nepoznata greška."));
                    }
                };

                new Thread(task).start();

            } catch (NumberFormatException ex) {
                statusLabel.setText("Unesite ispravne brojeve za redove i kolone.");
            }
        });
    }
}
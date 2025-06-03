package org.unibl.etf.pathfinder;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.unibl.etf.generator.TransportDataGenerator;

public class MainApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Path Finder - Inicijalizacija Podataka");

        Label rowsLabel = new Label("Broj redova (n):");
        TextField rowsField = new TextField();

        Label colsLabel = new Label("Broj kolona (m):");
        TextField colsField = new TextField();

        Button generateButton = new Button("Generiši mapu i pokreni aplikaciju");
        Label statusLabel = new Label();

        generateButton.setOnAction(e -> {
            try {
                int rows = Integer.parseInt(rowsField.getText());
                int cols = Integer.parseInt(colsField.getText());
                TransportDataGenerator generator = new TransportDataGenerator(rows, cols);
                TransportDataGenerator.TransportData data = generator.generateData();
                generator.saveToJson(data, "transport_data.json");
                statusLabel.setText("Podaci generisani. Spremni ste za nastavak rada.");
            } catch (NumberFormatException ex) {
                statusLabel.setText("Unesite ispravne brojeve za redove i kolone.");
            } catch (Exception ex) {
                statusLabel.setText("Greška: " + ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(10);
        grid.setHgap(10);

        grid.add(rowsLabel, 0, 0);
        grid.add(rowsField, 1, 0);
        grid.add(colsLabel, 0, 1);
        grid.add(colsField, 1, 1);
        grid.add(generateButton, 0, 2, 2, 1);
        grid.add(statusLabel, 0, 3, 2, 1);

        Scene scene = new Scene(grid, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
package org.unibl.etf.pathfinder;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.unibl.etf.generator.TransportDataGenerator;
import org.unibl.etf.model.City;
import org.unibl.etf.model.Departure;
import org.unibl.etf.model.Station;
import org.unibl.etf.data.JsonLoader;

import java.io.IOException;
import java.util.*;

public class MainApplication extends Application {

    private static final double NODE_RADIUS = 15;

    private Map<String, City> cityMap; // učitani gradovi
    private final Map<CircleNode, City> cityNodes = new HashMap<>();

    // UI elementi za detalje
    private ListView<String> cityListView = new ListView<>();
    private ListView<String> stationListView = new ListView<>();
    private ListView<String> departureListView = new ListView<>();

    @Override
    public void start(Stage primaryStage) {
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

                TransportDataGenerator generator = new TransportDataGenerator(rows, cols);
                TransportDataGenerator.TransportData data = generator.generateData();
                generator.saveToJson(data, "transport_data.json");

                cityMap = JsonLoader.loadCityMap("transport_data.json");

                showGraphWindow(rows, cols);

                statusLabel.setText("Mapa generisana i prikazana.");

            } catch (NumberFormatException ex) {
                statusLabel.setText("Unesite ispravne brojeve za redove i kolone.");
            } catch (IOException ex) {
                statusLabel.setText("Greška pri učitavanju JSON-a: " + ex.getMessage());
            } catch (Exception ex) {
                statusLabel.setText("Greška: " + ex.getMessage());
            }
        });
    }

    private void showGraphWindow(int rows, int cols) {
        BorderPane graphRoot = new BorderPane();

        int cellSize = 80;
        int padding = 50;

        int width = cols * cellSize + padding * 2;
        int height = rows * cellSize + padding * 2;

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Panel detalja sa tri liste: gradovi, stanice, polasci
        VBox detailsBox = new VBox(10);
        detailsBox.setPadding(new Insets(10));
        detailsBox.setPrefWidth(350);

        Label cityLabel = new Label("Gradovi");
        cityListView.setMinHeight(150);
        cityListView.setPrefHeight(200);

        Label stationLabel = new Label("Stanice");
        stationListView.setMinHeight(100);
        stationListView.setPrefHeight(150);

        Label departureLabel = new Label("Polasci");
        departureListView.setMinHeight(150);
        departureListView.setPrefHeight(200);

        detailsBox.getChildren().addAll(cityLabel, cityListView, stationLabel, stationListView, departureLabel, departureListView);

        graphRoot.setCenter(new ScrollPane(canvas));
        graphRoot.setRight(detailsBox);

        drawGraph(gc, rows, cols, cellSize, padding);

        // Podesi selekciju gradova iz liste i prikaz stanica i polazaka
        cityListView.getItems().clear();
        cityMap.values().forEach(city -> cityListView.getItems().add(city.getName()));

        cityListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            stationListView.getItems().clear();
            departureListView.getItems().clear();

            if (newVal != null) {
                City city = cityMap.values().stream().filter(c -> c.getName().equals(newVal)).findFirst().orElse(null);
                if (city != null) {
                    Station bus = city.getBusStation();
                    Station train = city.getTrainStation();
                    stationListView.getItems().add("Bus stanica: " + bus.getId());
                    stationListView.getItems().add("Voz stanica: " + train.getId());
                }
            }
        });

        stationListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            departureListView.getItems().clear();
            if (newVal != null) {
                String cityName = cityListView.getSelectionModel().getSelectedItem();
                if (cityName != null) {
                    City city = cityMap.values().stream().filter(c -> c.getName().equals(cityName)).findFirst().orElse(null);
                    if (city != null) {
                        Station station = newVal.toLowerCase().contains("bus") ? city.getBusStation() : city.getTrainStation();
                        for (Departure d : station.getDepartures()) {
                            departureListView.getItems().add(d.toString());
                        }
                    }
                }
            }
        });

        // Detekcija klika na Canvas (grad)
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            double xClick = event.getX();
            double yClick = event.getY();

            Optional<City> clickedCityOpt = cityNodes.entrySet().stream()
                    .filter(entry -> entry.getKey().containsPoint(xClick, yClick))
                    .map(Map.Entry::getValue)
                    .findFirst();

            if (clickedCityOpt.isPresent()) {
                City clickedCity = clickedCityOpt.get();
                cityListView.getSelectionModel().select(clickedCity.getName());
                cityListView.scrollTo(clickedCity.getName());
            }
        });

        Scene scene = new Scene(graphRoot, width + 400, Math.max(height, 600));

        Stage stage = new Stage();
        stage.setTitle("Pathfinder - Mapa gradova (" + rows + " x " + cols + ")");

        // Ako je prevelik ekran, fullscreen
        double screenW = Screen.getPrimary().getBounds().getWidth();
        double screenH = Screen.getPrimary().getBounds().getHeight();

        if (width + 400 > screenW || height > screenH) {
            stage.setMaximized(true);
        } else {
            stage.setWidth(width + 400);
            stage.setHeight(Math.max(height, 600));
        }

        stage.setScene(scene);
        stage.show();
    }

    private void drawGraph(GraphicsContext gc, int rows, int cols, int cellSize, int padding) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(2);
        gc.setFill(Color.LIGHTBLUE);
        cityNodes.clear();

        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < cols; y++) {
                double cx = padding + y * cellSize;
                double cy = padding + x * cellSize;

                // Crtaj linije do suseda
                if (x + 1 < rows)
                    gc.strokeLine(cx, cy, cx, padding + (x + 1) * cellSize);
                if (y + 1 < cols)
                    gc.strokeLine(cx, cy, padding + (y + 1) * cellSize, cy);

                // Crtaj čvor (grad)
                gc.setFill(Color.LIGHTBLUE);
                gc.fillOval(cx - NODE_RADIUS, cy - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
                gc.setStroke(Color.GRAY);
                gc.strokeOval(cx - NODE_RADIUS, cy - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

                // Ime grada
                String cityName = "G_" + x + "_" + y;
                gc.setFill(Color.BLACK);
                gc.fillText(cityName, cx - NODE_RADIUS, cy - NODE_RADIUS - 5);

                // Poveži krug sa gradom
                City city = cityMap.get(cityName);
                if (city != null) {
                    cityNodes.put(new CircleNode(cx, cy, NODE_RADIUS), city);
                }
            }
        }
    }

    private static class CircleNode {
        final double x, y, radius;

        CircleNode(double x, double y, double radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        boolean containsPoint(double px, double py) {
            double dx = px - x;
            double dy = py - y;
            return dx * dx + dy * dy <= radius * radius;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

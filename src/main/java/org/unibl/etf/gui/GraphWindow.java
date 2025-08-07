package org.unibl.etf.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.unibl.etf.algo.RouteFinder;
import org.unibl.etf.model.City;
import org.unibl.etf.model.Departure;
import org.unibl.etf.model.Station;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GraphWindow {

    private final int rows;
    private final int cols;
    private final Map<String, City> cityMap;

    private boolean selectingStart = true;
    private City selectedStartNode = null;
    private City selectedEndNode = null;

    private ListView<String> cityListView = new ListView<>();
    private ListView<String> stationListView = new ListView<>();
    private ListView<String> departureListView = new ListView<>();
    private ComboBox<String> startCityBox = new ComboBox<>();
    private ComboBox<String> endCityBox = new ComboBox<>();
    private ComboBox<RouteFinder.Criteria> criteriaBox = new ComboBox<>();
    private TableView<Departure> routeTableView = new TableView<>();
    private Label totalLabel = new Label("Ukupno: ");
    private List<Departure> bestRoute = new ArrayList<>();

    private final TransportGraphPainter graphPainter;

    public GraphWindow(int rows, int cols, Map<String, City> cityMap) {
        this.rows = rows;
        this.cols = cols;
        this.cityMap = cityMap;
        this.graphPainter = new TransportGraphPainter(cityMap);
    }

    public void showGraph() {
        BorderPane graphRoot = new BorderPane();

        int cellSize = 80;
        int padding = 50;

        int width = cols * cellSize + padding * 2;
        int height = rows * cellSize + padding * 2;

        Canvas canvas = new Canvas(width, height);
        graphPainter.setCanvas(canvas);

        VBox detailsBox = createDetailsBox();
        ScrollPane detailsScrollPane = new ScrollPane(detailsBox);
        detailsScrollPane.setFitToWidth(true);
        detailsScrollPane.setPrefWidth(400);
        detailsScrollPane.setPadding(new Insets(10));

        graphRoot.setCenter(new ScrollPane(canvas));
        graphRoot.setRight(detailsScrollPane);

        setupEventHandlers(canvas);
        initializeUIComponents();

        graphPainter.drawGraph(rows, cols, selectedStartNode, selectedEndNode);

        Scene scene = new Scene(graphRoot, width + 450, Math.max(height, 700));
        Stage stage = new Stage();
        stage.setTitle("🚂 Pathfinder - Mapa gradova (" + rows + " x " + cols + ")");

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

    private VBox createDetailsBox() {
        VBox detailsBox = new VBox(15);
        detailsBox.setPadding(new Insets(15));
        detailsBox.setPrefWidth(400);
        detailsBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1;");

        Label headerLabel = new Label("INFORMACIJE O GRADOVIMA");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2E86AB; -fx-padding: 0 0 10 0;");

        Label cityLabel = new Label("Gradovi");
        cityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #495057;");
        cityListView.setMinHeight(120);
        cityListView.setPrefHeight(150);
        cityListView.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 5;");

        Label stationLabel = new Label("Stanice");
        stationLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #495057;");
        stationListView.setMaxHeight(60);
        stationListView.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 5;");

        Label departureLabel = new Label("Polasci");
        departureLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #495057;");
        departureListView.setMaxHeight(150);
        departureListView.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 5;");

        Label searchSectionLabel = new Label("PRETRAGA RUTA");
        searchSectionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2E86AB; -fx-padding: 10 0 10 0;");

        Label startCityLabel = new Label("Početni grad:");
        startCityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #495057;");
        startCityBox.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 5;");

        Label endCityLabel = new Label("Odredišni grad:");
        endCityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #495057;");
        endCityBox.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 5;");

        Label criteriaLabel = new Label("Kriterijum:");
        criteriaLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #495057;");
        criteriaBox.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 5;");

        Button searchButton = new Button("Pronađi rutu");
        searchButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 5; -fx-padding: 8 16;");
        searchButton.setOnAction(e -> handleSearchAction());

        Button top5RoutesButton = new Button("Prikaz dodatnih ruta");
        top5RoutesButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 5; -fx-padding: 8 16;");
        top5RoutesButton.setOnAction(e -> handleTop5RoutesAction());

        Label routeLabel = new Label("REZULTAT PRETRAGE");
        routeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2E86AB; -fx-padding: 10 0 10 0;");

        setupRouteTableView();

        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #495057; -fx-padding: 5 0;");

        detailsBox.getChildren().addAll(
                headerLabel,
                cityLabel, cityListView,
                stationLabel, stationListView,
                departureLabel, departureListView,
                searchSectionLabel,
                startCityLabel, startCityBox,
                endCityLabel, endCityBox,
                criteriaLabel, criteriaBox,
                searchButton,
                routeLabel,
                routeTableView,
                totalLabel,
                top5RoutesButton
        );
        return detailsBox;
    }

    private void setupRouteTableView() {
        TableColumn<Departure, String> fromCol = new TableColumn<>("Polazak");
        fromCol.setCellValueFactory(data -> {
            String from = data.getValue().from;
            String time = data.getValue().departureTime;
            return new SimpleStringProperty(from + " (" + time + ")");
        });
        fromCol.setPrefWidth(150);

        TableColumn<Departure, String> toCol = new TableColumn<>("Dolazak");
        toCol.setCellValueFactory(data -> {
            String to = data.getValue().to;
            int minutes = data.getValue().duration;
            String depTime = data.getValue().departureTime;

            String arrivalTime = computeArrivalTime(depTime, minutes);
            return new SimpleStringProperty(to + " (" + arrivalTime + ")");
        });
        toCol.setPrefWidth(150);

        TableColumn<Departure, String> typeCol = new TableColumn<>("Tip");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type));
        typeCol.setPrefWidth(80);

        TableColumn<Departure, Integer> priceCol = new TableColumn<>("Cijena");
        priceCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().price).asObject());
        priceCol.setPrefWidth(80);

        routeTableView.getColumns().addAll(fromCol, toCol, typeCol, priceCol);
        routeTableView.setPrefHeight(200);
        routeTableView.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 5;");
        routeTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void initializeUIComponents() {
        startCityBox.getItems().addAll(cityMap.keySet());
        endCityBox.getItems().addAll(cityMap.keySet());
        criteriaBox.getItems().addAll(RouteFinder.Criteria.values());
        criteriaBox.getSelectionModel().selectFirst();
        criteriaBox.setValue(RouteFinder.Criteria.TIME);

        cityListView.getItems().addAll(cityMap.keySet());
    }

    private void setupEventHandlers(Canvas canvas) {
        cityListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            stationListView.getItems().clear();
            departureListView.getItems().clear();
            if (newVal != null) {
                City city = cityMap.get(newVal);
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
            String cityName = cityListView.getSelectionModel().getSelectedItem();
            if (cityName != null && newVal != null) {
                City city = cityMap.get(cityName);
                if (city != null) {
                    Station station = newVal.toLowerCase().contains("bus") ? city.getBusStation() : city.getTrainStation();
                    for (Departure d : station.getDepartures()) {
                        departureListView.getItems().add(d.toString());
                    }
                }
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Optional<Map.Entry<CircleNode, City>> clickedCityEntry = graphPainter.getCityNodeAt(event.getX(), event.getY());

            clickedCityEntry.ifPresent(entry -> {
                City city = entry.getValue();
                String name = city.getName();
                cityListView.getSelectionModel().select(name);
                cityListView.scrollTo(name);

                if (selectingStart) {
                    selectedStartNode = city;
                    startCityBox.setValue(name);
                } else {
                    selectedEndNode = city;
                    endCityBox.setValue(name);
                }

                selectingStart = !selectingStart;
                bestRoute.clear();
                graphPainter.drawGraph(rows, cols, selectedStartNode, selectedEndNode);
            });
        });
    }

    private void handleSearchAction() {
        String from = startCityBox.getValue();
        String to = endCityBox.getValue();
        RouteFinder.Criteria crit = criteriaBox.getValue();

        if (from == null || to == null || crit == null) {
            totalLabel.setText("Molimo odaberite sve parametre.");
            return;
        }

        if (from.equals(to)) {
            totalLabel.setText("Početni i odredišni grad ne mogu biti isti.");
            return;
        }

        totalLabel.setText("Tražim rutu...");

        Task<List<Departure>> task = new Task<>() {
            @Override
            protected List<Departure> call() {
                RouteFinder rf = new RouteFinder(cityMap);
                return rf.findRoute(from, to, crit);
            }

            @Override
            protected void succeeded() {
                List<Departure> route = getValue();
                routeTableView.getItems().clear();

                if (route.isEmpty() || from.equals(to)) {
                    totalLabel.setText("Nema dostupne rute.");
                    bestRoute.clear();
                } else {
                    routeTableView.getItems().addAll(route);
                    calculateAndDisplayTotal(route);

                    bestRoute = new ArrayList<>(route);
                    highlightBestRoute();
                }
            }

            @Override
            protected void failed() {
                totalLabel.setText("Greška prilikom traženja rute.");
                getException().printStackTrace();

                bestRoute.clear();
                graphPainter.drawGraph(rows, cols, selectedStartNode, selectedEndNode);
            }
        };
        new Thread(task).start();
    }

    private void handleTop5RoutesAction() {
        String from = startCityBox.getValue();
        String to = endCityBox.getValue();
        RouteFinder.Criteria crit = criteriaBox.getValue();

        if (from == null || to == null || crit == null) {
            totalLabel.setText("Molimo odaberite sve parametre.");
            return;
        }

        if (from.equals(to)) {
            totalLabel.setText("Početni i odredišni grad ne mogu biti isti.");
            return;
        }

        Task<List<Departure>> task = new Task<>() {
            @Override
            protected List<Departure> call() {
                RouteFinder rf = new RouteFinder(cityMap);
                return rf.findBestRoute(from, to, crit);
            }

            @Override
            protected void succeeded() {
                List<Departure> route = getValue();
                if (!route.isEmpty()) {

                    bestRoute = new ArrayList<>(route);
                    highlightBestRoute();
                } else {
                    bestRoute.clear();
                    graphPainter.drawGraph(rows, cols, selectedStartNode, selectedEndNode);
                    totalLabel.setText("Nema dostupne rute.");
                }
            }

            @Override
            protected void failed() {
                totalLabel.setText("Greška prilikom traženja najbolje rute.");
                getException().printStackTrace();
                bestRoute.clear();
                graphPainter.drawGraph(rows, cols, selectedStartNode, selectedEndNode);
            }
        };
        new Thread(task).start();

        new TopRoutesWindow(cityMap, from, to, crit).show();
    }

    private void calculateAndDisplayTotal(List<Departure> route) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalDate currentDate = LocalDate.now();

            LocalDateTime firstDeparture = LocalDateTime.of(currentDate, LocalTime.parse(route.get(0).departureTime, formatter));
            LocalDateTime lastArrival = firstDeparture;

            for (Departure d : route) {
                LocalTime depTime = LocalTime.parse(d.departureTime, formatter);
                LocalDateTime depDateTime = LocalDateTime.of(currentDate, depTime);

                if (depDateTime.isBefore(lastArrival)) {
                    currentDate = currentDate.plusDays(1);
                    depDateTime = LocalDateTime.of(currentDate, depTime);
                }

                LocalDateTime arrDateTime = depDateTime.plusMinutes(d.duration);
                lastArrival = arrDateTime;
            }

            Duration totalDuration = Duration.between(firstDeparture, lastArrival);
            long totalMinutes = totalDuration.toMinutes();

            int totalTransferTime = 0;
            for (int i = 0; i < route.size() - 1; i++) {
                totalTransferTime += route.get(i).minTransferTime;
            }
            totalMinutes += totalTransferTime;
            
            int hours = (int) (totalMinutes / 60);
            int minutes = (int) (totalMinutes % 60);
            int totalPrice = route.stream().mapToInt(d -> d.price).sum();
            totalLabel.setText("Ukupno: " + hours + "h " + minutes + "min, " + totalPrice + " novčanih jedinica.");
        } catch (Exception ex) {
            totalLabel.setText("Greška u računanju vremena.");
        }
    }

    private String computeArrivalTime(String depTime, int duration) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime time = LocalTime.parse(depTime, formatter);
            time = time.plusMinutes(duration);
            return time.format(formatter);
        } catch (Exception e) {
            return "??:??";
        }
    }

    private void highlightBestRoute() {
        if (bestRoute.isEmpty()) {
            graphPainter.drawGraph(rows, cols, selectedStartNode, selectedEndNode);
        } else {
            graphPainter.drawGraphWithRoute(rows, cols, selectedStartNode, selectedEndNode, bestRoute);
        }
    }
}
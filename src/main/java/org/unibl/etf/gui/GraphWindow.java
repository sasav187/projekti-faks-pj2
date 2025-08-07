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
    private List<Departure> bestRoute = new ArrayList<>(); // Store the best route for highlighting

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

        Scene scene = new Scene(graphRoot, width + 400, Math.max(height, 600));
        Stage stage = new Stage();
        stage.setTitle("Pathfinder - Mapa gradova (" + rows + " x " + cols + ")");

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
        VBox detailsBox = new VBox(10);
        detailsBox.setPadding(new Insets(10));
        detailsBox.setPrefWidth(350);

        Label cityLabel = new Label("Gradovi");
        cityListView.setMinHeight(150);
        cityListView.setPrefHeight(200);

        Label stationLabel = new Label("Stanice");
        stationListView.setMaxHeight(50);

        Label departureLabel = new Label("Polasci");
        departureListView.setMaxHeight(150);

        Label routeLabel = new Label("Ruta");

        setupRouteTableView();

        Button searchButton = new Button("Pronađi rutu");
        searchButton.setOnAction(e -> handleSearchAction());

        Button top5RoutesButton = new Button("Pronađi top 5 ruta");
        top5RoutesButton.setOnAction(e -> handleTop5RoutesAction());

        Button clearRouteButton = new Button("Ukloni prikaz rute");
        clearRouteButton.setOnAction(e -> clearRouteHighlight());

        detailsBox.getChildren().addAll(
                cityLabel, cityListView,
                stationLabel, stationListView,
                departureLabel, departureListView,
                new Label("Početni grad:"), startCityBox,
                new Label("Odredišni grad:"), endCityBox,
                new Label("Kriterijum:"), criteriaBox,
                searchButton,
                top5RoutesButton,
                clearRouteButton,
                routeLabel, routeTableView,
                totalLabel
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

        TableColumn<Departure, String> toCol = new TableColumn<>("Dolazak");
        toCol.setCellValueFactory(data -> {
            String to = data.getValue().to;
            int minutes = data.getValue().duration;
            String depTime = data.getValue().departureTime;

            String arrivalTime = computeArrivalTime(depTime, minutes);
            return new SimpleStringProperty(to + " (" + arrivalTime + ")");
        });

        TableColumn<Departure, String> typeCol = new TableColumn<>("Tip");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type));

        TableColumn<Departure, Integer> priceCol = new TableColumn<>("Cijena");
        priceCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().price).asObject());

        routeTableView.getColumns().addAll(fromCol, toCol, typeCol, priceCol);
        routeTableView.setPrefHeight(200);
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
                // Clear the best route when user selects new cities
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
                    // Store the best route and highlight it on the graph
                    bestRoute = new ArrayList<>(route);
                    highlightBestRoute();
                }
            }

            @Override
            protected void failed() {
                totalLabel.setText("Greška prilikom traženja rute.");
                getException().printStackTrace();
                // Clear highlighting on error
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

        // Find and highlight the best route for the selected criteria
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
                    // Store and highlight the best route
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

        // Open the TopRoutesWindow to show all routes
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
            // Clear highlighting if no route
            graphPainter.drawGraph(rows, cols, selectedStartNode, selectedEndNode);
        } else {
            // Highlight the best route on the graph
            graphPainter.drawGraphWithRoute(rows, cols, selectedStartNode, selectedEndNode, bestRoute);
        }
    }

    private void clearRouteHighlight() {
        bestRoute.clear();
        graphPainter.drawGraph(rows, cols, selectedStartNode, selectedEndNode);
        totalLabel.setText("Prikaz rute uklonjen.");
    }
}
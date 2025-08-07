package org.unibl.etf.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.unibl.etf.algo.RouteFinder;
import org.unibl.etf.model.City;
import org.unibl.etf.model.Departure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TopRoutesWindow {

    private final Map<String, City> cityMap;
    private final String startCity;
    private final String endCity;
    private final RouteFinder.Criteria criteria;
    private final Label statusLabel = new Label();
    private final VBox tableContainer = new VBox(10);
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Stage stage;

    public TopRoutesWindow(Map<String, City> cityMap, String startCity, String endCity, RouteFinder.Criteria criteria) {
        this.cityMap = cityMap;
        this.startCity = startCity;
        this.endCity = endCity;
        this.criteria = criteria;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("Top 5 ruta po kriterijumu: " + criteria.name());
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label headerLabel = new Label("Top 5 ruta od " + startCity + " do " + endCity + ":");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        VBox progressBox = new VBox(5);
        progressBox.setAlignment(javafx.geometry.Pos.CENTER);
        progressBox.getChildren().addAll(progressIndicator, statusLabel);

        ScrollPane scrollPane = new ScrollPane(tableContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        root.getChildren().addAll(headerLabel, progressBox, scrollPane);

        Scene scene = new Scene(root, 900, 700);
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            executorService.shutdown();
        });

        startSearch();
        stage.show();
    }

    private void startSearch() {
        statusLabel.setText("Tražim top 5 ruta...");
        progressIndicator.setVisible(true);
        tableContainer.getChildren().clear();

        Task<List<List<Departure>>> task = new Task<>() {
            @Override
            protected List<List<Departure>> call() {
                RouteFinder rf = new RouteFinder(cityMap);
                return rf.findTopRoutes(startCity, endCity, criteria, 5);
            }

            @Override
            protected void succeeded() {
                List<List<Departure>> topRoutes = getValue();
                progressIndicator.setVisible(false);
                displayTopRoutes(topRoutes);
                
                if (topRoutes.isEmpty()) {
                    statusLabel.setText("Nema dostupnih ruta.");
                } else {
                    statusLabel.setText("Prikazano " + topRoutes.size() + " ruta.");
                }
            }

            @Override
            protected void failed() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Greška prilikom traženja ruta: " + getException().getMessage());
                getException().printStackTrace();
            }
        };

        executorService.submit(task);
    }

    private void displayTopRoutes(List<List<Departure>> topRoutes) {
        tableContainer.getChildren().clear();
        
        for (int i = 0; i < topRoutes.size(); i++) {
            List<Departure> route = topRoutes.get(i);

            TitledPane routeSection = createRouteSection(i + 1, route);
            tableContainer.getChildren().add(routeSection);
        }
    }

    private TitledPane createRouteSection(int routeNumber, List<Departure> route) {
        String summary = calculateRouteSummary(route);
        String title = "Ruta " + routeNumber + ": " + summary;
        
        TableView<Departure> routeTable = new TableView<>();
        setupRouteTableColumns(routeTable);
        routeTable.setItems(javafx.collections.FXCollections.observableArrayList(route));
        routeTable.setPrefHeight(Math.min(200, 50 + route.size() * 30));

        Label detailsLabel = new Label("Detalji rute:");
        detailsLabel.setStyle("-fx-font-weight: bold;");

        Button buyTicketButton = new Button("Kupi kartu");
        buyTicketButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        buyTicketButton.setOnAction(e -> handleBuyTicket(routeNumber, route));
        
        VBox content = new VBox(5);
        content.getChildren().addAll(detailsLabel, routeTable, buyTicketButton);
        
        TitledPane section = new TitledPane(title, content);
        section.setExpanded(routeNumber <= 2);
        section.setCollapsible(true);
        
        return section;
    }

    private void setupRouteTableColumns(TableView<Departure> table) {
        table.getColumns().clear();
        
        TableColumn<Departure, String> fromCol = new TableColumn<>("Polazak");
        fromCol.setCellValueFactory(data -> {
            String from = data.getValue().from;
            String time = data.getValue().departureTime;
            return new SimpleStringProperty(from + " (" + time + ")");
        });
        fromCol.setPrefWidth(200);

        TableColumn<Departure, String> toCol = new TableColumn<>("Dolazak");
        toCol.setCellValueFactory(data -> {
            String to = data.getValue().to;
            String depTime = data.getValue().departureTime;
            int duration = data.getValue().duration;
            String arrivalTime = computeArrivalTime(depTime, duration);
            return new SimpleStringProperty(to + " (" + arrivalTime + ")");
        });
        toCol.setPrefWidth(200);

        TableColumn<Departure, String> typeCol = new TableColumn<>("Tip");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type));
        typeCol.setPrefWidth(100);

        TableColumn<Departure, Integer> priceCol = new TableColumn<>("Cijena");
        priceCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().price).asObject());
        priceCol.setPrefWidth(80);

        TableColumn<Departure, Integer> durationCol = new TableColumn<>("Trajanje (min)");
        durationCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().duration).asObject());
        durationCol.setPrefWidth(120);

        table.getColumns().addAll(fromCol, toCol, typeCol, priceCol, durationCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private String calculateRouteSummary(List<Departure> route) {
        if (route.isEmpty()) {
            return "N/A";
        }

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
            int transfers = route.size() - 1;

            return String.format("%dh %dmin, %d KM, %d presjedanja", hours, minutes, totalPrice, transfers);
        } catch (Exception ex) {
            return "Greška u računanju.";
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

    private void handleBuyTicket(int routeNumber, List<Departure> route) {
        try {
            Path receiptsDir = Paths.get("racuni");
            if (!Files.exists(receiptsDir)) {
                Files.createDirectories(receiptsDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "racun_" + startCity + "_do_" + endCity + "_ruta" + routeNumber + "_" + timestamp + ".txt";
            Path receiptPath = receiptsDir.resolve(filename);

            String relation = startCity + " → " + endCity;
            String time = calculateRouteTime(route);
            int price = calculateRoutePrice(route);
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String purchaseTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));

            StringBuilder receipt = new StringBuilder();
            receipt.append("==========================================\n");
            receipt.append("              KARTA ZA PUTOVANJE\n");
            receipt.append("==========================================\n\n");
            receipt.append("Datum kupovine: ").append(purchaseTime).append("\n");
            receipt.append("Relacija: ").append(relation).append("\n");
            receipt.append("Kriterijum: ").append(criteria.name()).append("\n");
            receipt.append("Ruta broj: ").append(routeNumber).append("\n\n");
            receipt.append("DETALJI PUTOVANJA:\n");
            receipt.append("------------------------------------------\n");

            for (int i = 0; i < route.size(); i++) {
                Departure dep = route.get(i);
                String arrivalTime = computeArrivalTime(dep.departureTime, dep.duration);
                receipt.append(String.format("%d. %s (%s) → %s (%s) [%s] - %d KM\n", 
                    i + 1, dep.from, dep.departureTime, dep.to, arrivalTime, dep.type, dep.price));
            }

            receipt.append("\n------------------------------------------\n");
            receipt.append("Ukupno vrijeme: ").append(time).append("\n");
            receipt.append("Ukupna cijena: ").append(price).append(" KM\n");
            receipt.append("Broj presjedanja: ").append(route.size() - 1).append("\n\n");
            receipt.append("==========================================\n");
            receipt.append("Hvala na kupovini!\n");
            receipt.append("==========================================\n");

            try (FileWriter writer = new FileWriter(receiptPath.toFile())) {
                writer.write(receipt.toString());
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Karta kupljena");
            alert.setHeaderText("Uspješno ste kupili kartu!");
            alert.setContentText("Račun je sačuvan u: " + receiptPath.toFile());
            alert.showAndWait();

            StatisticsWindow.showStatistics();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Greška");
            alert.setHeaderText("Greška prilikom kupovine karte");
            alert.setContentText("Nije moguće sačuvati račun: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    private String calculateRouteTime(List<Departure> route) {
        if (route.isEmpty()) return "N/A";

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
            
            return String.format("%dh %dmin", hours, minutes);
        } catch (Exception ex) {
            return "N/A";
        }
    }

    private int calculateRoutePrice(List<Departure> route) {
        return route.stream().mapToInt(dep -> dep.price).sum();
    }
} 
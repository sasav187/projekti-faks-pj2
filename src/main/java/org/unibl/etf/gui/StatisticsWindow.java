package org.unibl.etf.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.unibl.etf.stats.ReceiptStatistics;

public class StatisticsWindow {
    
    public static void showStatistics() {
        Stage stage = new Stage();
        stage.setTitle("Statistika prodaje karata");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setAlwaysOnTop(true); // Make sure it appears on top
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        
        // Calculate statistics
        ReceiptStatistics.StatisticsData stats = ReceiptStatistics.calculateStatistics();
        
        // Header
        Label headerLabel = new Label("STATISTIKA PRODAJE KARATA");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2E86AB;");
        
        // Total tickets
        Label ticketsLabel = new Label("Ukupno kupljenih karata:");
        ticketsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label ticketsValueLabel = new Label(String.valueOf(stats.getTotalTickets()));
        ticketsValueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        
        // Total money earned
        Label moneyLabel = new Label("Ukupno zaradjeno:");
        moneyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label moneyValueLabel = new Label(stats.getTotalMoneyEarned() + " KM");
        moneyValueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FF6B35;");
        
        // Add all elements to the layout
        root.getChildren().addAll(
            headerLabel,
            ticketsLabel,
            ticketsValueLabel,
            moneyLabel,
            moneyValueLabel
        );
        
        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        stage.setResizable(false);
        
        // Center the window on screen
        stage.centerOnScreen();
        
        // Auto-close after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(stage::close);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        stage.show();
        stage.toFront(); // Bring to front
    }
} 
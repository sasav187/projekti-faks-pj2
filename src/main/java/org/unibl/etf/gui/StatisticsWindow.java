package org.unibl.etf.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.unibl.etf.stats.ReceiptStatistics;

/**
 * StatisticsWindow je pomoćna klasa koja prikazuje
 * prozor sa osnovnom statistikom prodaje karata.
 *
 * <p>Glavne funkcionalnosti uključuju:</p>
 * <ul>
 *     <li>Prikaz ukupnog broja prodatih karata</li>
 *     <li>Prikaz ukupnog prihoda od prodaje</li>
 *     <li>Automatsko zatvaranje prozora nakon nekoliko sekundi</li>
 * </ul>
 */
public class StatisticsWindow {
    
    public static void showStatistics() {
        Stage stage = new Stage();
        stage.setTitle("Statistika prodaje karata");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setAlwaysOnTop(true);
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        ReceiptStatistics.StatisticsData stats = ReceiptStatistics.calculateStatistics();

        Label headerLabel = new Label("STATISTIKA PRODAJE KARATA");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2E86AB;");

        Label ticketsLabel = new Label("Ukupan broj prodatih karata:");
        ticketsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label ticketsValueLabel = new Label(String.valueOf(stats.getTotalTickets()));
        ticketsValueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        Label moneyLabel = new Label("Ukupan prihod od prodaje:");
        moneyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label moneyValueLabel = new Label(stats.getTotalMoneyEarned() + " KM");
        moneyValueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FF6B35;");

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

        stage.centerOnScreen();

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(stage::close);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        stage.show();
        stage.toFront();
    }
} 
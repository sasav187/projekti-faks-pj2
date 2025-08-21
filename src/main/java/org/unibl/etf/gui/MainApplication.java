package org.unibl.etf.gui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Glavna klasa JavaFX aplikacije "Pathfinder" koja pokreće GUI.
 *
 * <p>Glavne funkcionalnosti:</p>
 * <ul>
 *     <li>Pokretanje {@link InputWindow} za unos dimenzija transportne mreže</li>
 *     <li>Prikaz statistike prodaje karata preko {@link StatisticsWindow} nakon kratkog odlaganja</li>
 *     <li>Pokretanje JavaFX aplikacije</li>
 * </ul>
 */
public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        InputWindow inputWindow = new InputWindow(primaryStage);
        inputWindow.show();

        new Thread(() -> {
            try {
                Thread.sleep(500);
                javafx.application.Platform.runLater(StatisticsWindow::showStatistics);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
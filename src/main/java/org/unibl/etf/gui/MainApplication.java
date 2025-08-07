package org.unibl.etf.gui;

import javafx.application.Application;
import javafx.stage.Stage;

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
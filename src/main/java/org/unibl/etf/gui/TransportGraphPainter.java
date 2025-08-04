package org.unibl.etf.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.unibl.etf.model.City;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TransportGraphPainter {

    private static final double NODE_RADIUS = 15;
    private final Map<String, City> cityMap;
    private final Map<CircleNode, City> cityNodes = new HashMap<>();
    private Canvas canvas;

    public TransportGraphPainter(Map<String, City> cityMap) {
        this.cityMap = cityMap;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public void drawGraph(int rows, int cols, City selectedStartNode, City selectedEndNode) {
        if (canvas == null) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(2);
        gc.setFill(Color.LIGHTBLUE);
        cityNodes.clear();

        int cellSize = 80;
        int padding = 50;

        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < cols; y++) {
                double cx = padding + y * cellSize;
                double cy = padding + x * cellSize;

                if (x + 1 < rows)
                    gc.strokeLine(cx, cy, cx, padding + (x + 1) * cellSize);
                if (y + 1 < cols)
                    gc.strokeLine(cx, cy, padding + (y + 1) * cellSize, cy);

                String cityName = "G_" + x + "_" + y;
                City city = cityMap.get(cityName);

                if (city != null) {
                    Color fillColor = Color.LIGHTBLUE;

                    if (city.equals(selectedStartNode)) {
                        fillColor = Color.LIGHTGREEN;
                    } else if (city.equals(selectedEndNode)) {
                        fillColor = Color.ORANGERED;
                    }

                    gc.setFill(fillColor);
                    gc.fillOval(cx - NODE_RADIUS, cy - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
                    gc.setStroke(Color.GREY);
                    gc.strokeOval(cx - NODE_RADIUS, cy - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
                    gc.setFill(Color.BLACK);
                    gc.fillText(cityName, cx - NODE_RADIUS, cy - NODE_RADIUS - 5);

                    cityNodes.put(new CircleNode(cx, cy, NODE_RADIUS), city);
                }
            }
        }
    }

    public Optional<Map.Entry<org.unibl.etf.gui.CircleNode, City>> getCityNodeAt(double x, double y) {
        return cityNodes.entrySet().stream()
                .filter(entry -> entry.getKey().containsPoint(x, y))
                .findFirst();
    }
}
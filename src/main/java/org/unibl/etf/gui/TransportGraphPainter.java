package org.unibl.etf.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.unibl.etf.model.City;
import org.unibl.etf.model.Departure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TransportGraphPainter je klasa zadužena za crtanje grafičkog prikaza mreže gradova
 * i transportnih veza između njih na JavaFX {@link Canvas}-u.
 *
 * <p>Glavne funkcionalnosti uključuju:</p>
 * <ul>
 *     <li>Crtanje čvorova (gradova) i veza između njih</li>
 *     <li>Isticanje početnog i krajnjeg grada</li>
 *     <li>Prikaz rute između gradova sa označenim gradovima</li>
 *     <li>Prepoznavanje gradova klikom miša na Canvas</li>
 * </ul>
 *
 * @author Saša Vujančević
 */
public class TransportGraphPainter {

    private static final double NODE_RADIUS = 15;
    private final Map<String, City> cityMap;
    private final Map<CircleNode, City> cityNodes = new HashMap<>();
    private Canvas canvas;

    public TransportGraphPainter(Map<String, City> cityMap) {
        this.cityMap = cityMap;
    }

    /**
     * Postavlja Canvas na kojem će se crtati graf.
     *
     * @param canvas JavaFX Canvas objekat
     */
    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Crta mrežu gradova i veza između njih, ističući početni i krajnji grad.
     *
     * @param rows broj redova u mreži
     * @param cols broj kolona u mreži
     * @param selectedStartNode početni grad
     * @param selectedEndNode krajnji grad
     */
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

    /**
     * Vraća opcionalni par čvora i grada na koji je korisnik kliknuo.
     *
     * @param x X koordinata klika
     * @param y Y koordinata klika
     * @return Optional sa parom {@link CircleNode} i {@link City}, ili prazno ako nije kliknuto na grad
     */
    public Optional<Map.Entry<org.unibl.etf.gui.CircleNode, City>> getCityNodeAt(double x, double y) {
        return cityNodes.entrySet().stream()
                .filter(entry -> entry.getKey().containsPoint(x, y))
                .findFirst();
    }

    /**
     * Crta graf sa istaknutom rutom između gradova označenih žutom bojom.
     *
     * @param rows broj redova u mreži
     * @param cols broj kolona u mreži
     * @param selectedStartNode početni grad
     * @param selectedEndNode krajnji grad
     * @param route lista {@link Departure} objekata koji predstavljaju rutu
     */
    public void drawGraphWithRoute(int rows, int cols, City selectedStartNode, City selectedEndNode, List<Departure> route) {
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

        if (!route.isEmpty()) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(4);

            for (int i = 0; i < route.size(); i++) {
                Departure dep = route.get(i);
                String fromCity = dep.from;
                String toCity = dep.to;

                double fromX = 0, fromY = 0, toX = 0, toY = 0;
                boolean foundFrom = false, foundTo = false;
                
                for (int x = 0; x < rows; x++) {
                    for (int y = 0; y < cols; y++) {
                        String cityName = "G_" + x + "_" + y;
                        if (cityName.equals(fromCity)) {
                            fromX = padding + y * cellSize;
                            fromY = padding + x * cellSize;
                            foundFrom = true;
                        }
                        if (cityName.equals(toCity)) {
                            toX = padding + y * cellSize;
                            toY = padding + x * cellSize;
                            foundTo = true;
                        }
                    }
                }
                
                if (foundFrom && foundTo) {
                    gc.strokeLine(fromX, fromY, toX, toY);

                    double angle = Math.atan2(toY - fromY, toX - fromX);
                    double arrowLength = 10;
                    double arrowAngle = Math.PI / 6;
                    
                    double arrowX1 = toX - arrowLength * Math.cos(angle - arrowAngle);
                    double arrowY1 = toY - arrowLength * Math.sin(angle - arrowAngle);
                    double arrowX2 = toX - arrowLength * Math.cos(angle + arrowAngle);
                    double arrowY2 = toY - arrowLength * Math.sin(angle + arrowAngle);
                    
                    gc.strokeLine(toX, toY, arrowX1, arrowY1);
                    gc.strokeLine(toX, toY, arrowX2, arrowY2);
                }
            }

            gc.setFill(Color.YELLOW);
            for (Departure dep : route) {
                String cityName = dep.to;
                for (int x = 0; x < rows; x++) {
                    for (int y = 0; y < cols; y++) {
                        String gridCityName = "G_" + x + "_" + y;
                        if (gridCityName.equals(cityName)) {
                            double cx = padding + y * cellSize;
                            double cy = padding + x * cellSize;

                            gc.setFill(Color.YELLOW);
                            gc.fillOval(cx - NODE_RADIUS - 2, cy - NODE_RADIUS - 2, (NODE_RADIUS + 4) * 2, (NODE_RADIUS + 4) * 2);

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
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}
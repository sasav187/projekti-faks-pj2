package org.unibl.etf.gui;

public class CircleNode {
    final double x, y, radius;

    public CircleNode(double x, double y, double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public boolean containsPoint(double px, double py) {
        double dx = px - x;
        double dy = py - y;
        return dx * dx + dy * dy <= radius * radius;
    }
}
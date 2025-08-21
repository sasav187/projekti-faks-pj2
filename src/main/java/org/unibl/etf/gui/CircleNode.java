package org.unibl.etf.gui;

/**
 * Klasa {@code CircleNode} predstavlja kružni čvor na grafičkom prikazu,
 * definisan koordinatama centra i poluprečnikom.
 *
 * Ova klasa omogućava proveru da li se određena tačka nalazi unutar
 * kružnog čvora, što je korisno za interakcije u GUI aplikaciji, kao što
 * su selekcija ili klik na čvor.
 *
 * @author Saša Vujančević
 */
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
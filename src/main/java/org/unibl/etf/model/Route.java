package org.unibl.etf.model;

import java.util.*;

public class Route {
    private final List<Departure> steps;

    public Route(String start) {
        this.steps = new ArrayList<>();
        this.steps.add(new Departure("START", start, start, "00:00", 0, 0, 0));
    }

    public Route(Route other) {
        this.steps = new ArrayList<>(other.steps);
    }

    public void addDeparture(Departure dep) {
        this.steps.add(dep);
    }

    public String getLastStop() {
        return steps.get(steps.size() - 1).to;
    }

    public boolean containsStop(String stop) {
        return steps.stream().anyMatch(d -> d.to.equals(stop));
    }

    public int getTotalDuration() {
        return steps.stream().mapToInt(d -> d.duration).sum();
    }

    public int getTotalPrice() {
        return steps.stream().mapToInt(d -> d.price).sum();
    }

    public int getTransfers() {
        return (int) steps.stream().map(d -> d.type).distinct().count() - 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Departure d : steps) {
            if (!d.type.equals("START"))
                sb.append(String.format("%s: %s -> %s (%d min, %d BAM)\n", d.type, d.from, d.to, d.duration, d.price));
        }
        sb.append(String.format("Ukupno: %d min, %d BAM, %d presjedanja\n",
                getTotalDuration(), getTotalPrice(), getTransfers()));
        return sb.toString();
    }
}

package org.unibl.etf.model;

import java.util.ArrayList;
import java.util.List;

public class Station {
    private final String id;
    private final List<Departure> departures = new ArrayList<>();

    public Station(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<Departure> getDepartures() {
        return departures;
    }

    public void addDeparture(Departure departure) {
        departures.add(departure);
    }

    @Override
    public String toString() {
        return String.format("Stanica %s, broj polazaka: %d", id, departures.size());
    }

}

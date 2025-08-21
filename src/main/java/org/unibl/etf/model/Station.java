package org.unibl.etf.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa {@code Station} predstavlja stanicu u sistemu modelovanja mreže gradova.
 *
 * Svaka stanica je identifikovana jedinstvenim ID-em i sadrži listu polazaka
 * ({@link Departure}) koji kreću sa te stanice. Na ovaj način stanica služi kao čvor
 * u grafu transportne mreže i omogućava dodavanje, čuvanje i pristup svim polascima
 * koji su povezani sa njom.
 *
 */
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

}

package org.unibl.etf.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Klasa {@code Departure} predstavlja pojedinačni polazak u okviru
 * transportne mreže.
 *
 * Polazak sadrži informacije o vrsti prevoza (npr. autobus, voz),
 * polaznoj i odredišnoj stanici, vremenu polaska, trajanju putovanja,
 * ceni karte i minimalnom vremenu potrebnom za presedanje.
 *
 * Ova klasa omogućava jednostavno modelovanje i praćenje svih
 * relevantnih podataka vezanih za jedno putovanje između dvije stanice.
 *
 * @author Saša Vujančević
 */
public class Departure {
    public String type;
    public String from;
    public String to;
    public String departureTime;
    public int duration;
    public int price;
    public int minTransferTime;

    public Departure(String type, String from, String to, String departureTime,
                     int duration, int price, int minTransferTime) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.departureTime = departureTime;
        this.duration = duration;
        this.price = price;
        this.minTransferTime = minTransferTime;
    }

    public LocalTime getDepartureTime() {
        return LocalTime.parse(departureTime);
    }

    public LocalTime getArrivalTime() {
        return LocalTime.parse(departureTime).plusMinutes(duration);
    }

    @Override
    public String toString() {
        return String.format(
                "Tip: %s, Polazi sa: %s, Destinacija: %s, Vreme polaska: %s, Trajanje: %d min, Cena: %d, Min. vreme presedanja: %d min",
                type, from, to, departureTime, duration, price, minTransferTime
        );
    }

}

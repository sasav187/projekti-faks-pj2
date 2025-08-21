package org.unibl.etf.model;

/**
 * Klasa {@code City} predstavlja grad u transportnoj mreži.
 *
 * Svaki grad ima naziv i dvije stanice:
 * <ul>
 *   <li>Autobusku stanicu</li>
 *   <li>Željezničku stanicu</li>
 * </ul>
 *
 * Ova klasa omogućava modelovanje grada kao čvora u mreži transporta,
 * pri čemu se sa njim povezuju autobuski i željeznički polasci.
 *
 * @author Saša Vujančević
 */
public class City {

    private final String name;
    private final Station busStation;
    private final Station trainStation;

    public City(String name, Station busStation, Station trainStation) {
        this.name = name;
        this.busStation = busStation;
        this.trainStation = trainStation;
    }

    public String getName() {
        return name;
    }

    public Station getBusStation() {
        return busStation;
    }

    public Station getTrainStation() {
        return trainStation;
    }
}

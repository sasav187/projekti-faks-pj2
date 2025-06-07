package org.unibl.etf.model;

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

package org.unibl.etf.vehicles;

public abstract class Vehicle {

    protected String id;
    protected String manufacturer;
    protected String model;

    protected double priceOfSupply;

    protected int batteryLevel;

    protected Breakdown breakdown;

    protected void batteryCharge() {

    }

    protected void batteryUsage() {

    }

    public String getId() {
        return id;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public double getPriceOfSupply() {
        return priceOfSupply;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }
}

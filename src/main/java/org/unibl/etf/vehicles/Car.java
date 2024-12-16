package org.unibl.etf.vehicles;

import java.util.Date;

public class Car extends Vehicle {

    protected String description;

    protected Date dateOfSupply;

    protected int passengers;

    public String getDescription() {
        return description;
    }

    public Date getDateOfSupply() {
        return dateOfSupply;
    }

    public int getPassengers() {
        return passengers;
    }
}
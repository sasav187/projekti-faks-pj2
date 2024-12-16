package org.unibl.etf.vehicles;

import java.time.LocalDateTime;

public class Breakdown {

    protected String description;

    protected LocalDateTime dateTime;

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
}

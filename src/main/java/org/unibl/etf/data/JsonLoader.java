package org.unibl.etf.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.unibl.etf.model.City;
import org.unibl.etf.model.Departure;
import org.unibl.etf.model.Station;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonLoader {

    public static Map<String, City> loadCityMap(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(filePath));

        Map<String, Station> stationMap = new HashMap<>();
        Map<String, City> cityMap = new HashMap<>();

        for (JsonNode s : root.get("stations")) {
            String cityName = s.get("city").asText();
            String busId = s.get("busStation").asText();
            String trainId = s.get("trainStation").asText();
            Station bus = new Station(busId);
            Station train = new Station(trainId);
            stationMap.put(busId, bus);
            stationMap.put(trainId, train);
            cityMap.put(cityName, new City(cityName, bus, train));
        }

        for (JsonNode d : root.get("departures")) {
            Departure dep = new Departure(
                    d.get("type").asText(),
                    d.get("from").asText(),
                    d.get("to").asText(),
                    d.get("departureTime").asText(),
                    d.get("duration").asInt(),
                    d.get("price").asInt(),
                    d.get("minTransferTime").asInt()
            );
            Station fromStation = stationMap.get(dep.from);
            if (fromStation != null)
                fromStation.addDeparture(dep);
        }

        return cityMap;
    }
}

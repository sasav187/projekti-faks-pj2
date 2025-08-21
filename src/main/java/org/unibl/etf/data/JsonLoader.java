package org.unibl.etf.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.unibl.etf.model.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Klasa {@code JsonLoader} služi za učitavanje podataka iz JSON fajla
 * i njihovu konverziju u objekte modela aplikacije ({@link City}, {@link Station}, {@link Departure}).
 * <p>
 * JSON fajl treba da sadrži sljedeće sekcije:
 * <ul>
 *     <li><b>stations</b> – lista stanica sa pripadajućim gradom, autobuskom i željezničkom stanicom</li>
 *     <li><b>departures</b> – lista polazaka (autobus ili voz) sa svim potrebnim informacijama</li>
 * </ul>
 * <p>
 * Učitani podaci se čuvaju u mapama, gde se grad identifikuje po imenu,
 * a stanice po svom ID-u.
 *
 * @author Saša Vujančević
 */
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

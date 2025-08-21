package org.unibl.etf.algo;

import org.unibl.etf.model.City;
import org.unibl.etf.model.Departure;
import org.unibl.etf.model.Station;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Klasa {@code RouteFinder} implementira algoritme za pronalaženje najboljih ruta
 * između gradova na osnovu različitih kriterijuma: vremena, cijene i broja presjedanja.
 * <p>
 * Gradovi su predstavljeni pomoću objekata {@link City}, a veze između njih preko
 * polazaka {@link Departure} sa stanica {@link Station}.
 * </p>
 *
 * <h2>Korišćeni algoritmi</h2>
 * <ul>
 *     <li><b>Pronalaženje rute po vremenu:</b> koristi se algoritam sličan
 *     <i>Dijkstra algoritmu</i> uz pomoć {@link PriorityQueue}, gdje se prioritet daje
 *     rutama sa najmanjim ukupnim vremenom putovanja.</li>
 *
 *     <li><b>Pronalaženje rute po cijeni:</b> takođe se koristi varijacija
 *     <i>Dijkstra algoritma</i>, ali se heuristika zasniva na ukupnoj cijeni umjesto na vremenu.</li>
 *
 *     <li><b>Pronalaženje rute po broju presjedanja:</b> koristi se
 *     <i>BFS (Breadth-First Search)</i> algoritam preko {@link LinkedList} reda,
 *     jer je cilj da se pronađe ruta sa najmanjim brojem koraka (presjedanja).</li>
 *
 *     <li><b>Pronalaženje više najboljih ruta:</b> nakon što se pronađe optimalna ruta,
 *     algoritam nastavlja pretragu i sortira rezultate kako bi se dobilo prvih N ruta
 *     (npr. top 5 najboljih po vremenu, cijeni ili broju presjedanja).</li>
 * </ul>
 *
 * <p>
 * Uvedena su i ograničenja poput maksimalnog broja presjedanja ({@link #MAX_TRANSFERS})
 * i maksimalnog broja iteracija, kako bi se spriječilo beskonačno pretraživanje
 * u složenim mrežama.
 * </p>
 *
 * @author Saša Vujančević
 */
public class RouteFinder {

    public enum Criteria {
        TIME, PRICE, TRANSFERS
    }

    private static final int MAX_TRANSFERS = 50;
    private final Map<String, City> cityMap;

    public RouteFinder(Map<String, City> cityMap) {
        this.cityMap = cityMap;
    }

    /**
     * Pronalazi najbolju rutu između dva grada na osnovu zadatog kriterijuma.
     *
     * @param startCity početni grad
     * @param endCity   krajnji grad
     * @param criteria  kriterijum (vrijeme, cijena ili broj presjedanja)
     * @return lista polazaka koja predstavlja najbolju rutu
     */
    public List<Departure> findRoute(String startCity, String endCity, Criteria criteria) {
        
        List<Departure> result = switch (criteria) {
            case TIME -> findBestTimeRoute(startCity, endCity);
            case PRICE -> findBestPriceRoute(startCity, endCity);
            case TRANSFERS -> findBestTransfersRoute(startCity, endCity);
        };

        return result;
    }

    /**
     * Pronalazi više najboljih ruta između dva grada po zadatom kriterijumu.
     *
     * @param startCity početni grad
     * @param endCity   krajnji grad
     * @param criteria  kriterijum pretrage
     * @param limit     maksimalan broj ruta koje treba pronaći
     * @return lista ruta, gdje svaka ruta predstavlja listu polazaka
     */
    public List<List<Departure>> findTopRoutes(String startCity, String endCity, Criteria criteria, int limit) {
        
        List<List<Departure>> result = switch (criteria) {
            case TIME -> findTopFastestRoutes(startCity, endCity, limit);
            case PRICE -> findTopByPrice(startCity, endCity, limit);
            case TRANSFERS -> findTopByTransfers(startCity, endCity, limit);
        };

        return result;
    }

    /**
     * Vraća najbolju rutu između dva grada po zadatom kriterijumu.
     *
     * @param startCity početni grad
     * @param endCity   krajnji grad
     * @param criteria  kriterijum pretrage
     * @return najbolja ruta kao lista polazaka
     */
    public List<Departure> findBestRoute(String startCity, String endCity, Criteria criteria) {
        List<List<Departure>> topRoutes = findTopRoutes(startCity, endCity, criteria, 1);
        return topRoutes.isEmpty() ? Collections.emptyList() : topRoutes.get(0);
    }

    /**
     * Pronalazi najbržu rutu između dva grada koristeći algoritam
     * sličan Dijkstra algoritmu.
     * <p>
     * Algoritam koristi {@link PriorityQueue} u kojoj se čuvaju parcijalne rute,
     * a prioritet imaju one sa najmanjim ukupnim vremenom putovanja.
     * Svaka ruta se proširuje dodavanjem novih polazaka iz trenutnog grada,
     * dok se ne stigne do odredišta.
     * </p>
     *
     * @param start početni grad
     * @param end   krajnji grad
     * @return lista polazaka koja predstavlja najbržu rutu ili prazna lista ako ruta ne postoji
     */
    private List<Departure> findBestTimeRoute(String start, String end) {
        PriorityQueue<RouteNode> queue = new PriorityQueue<>((a, b) -> Long.compare(a.totalTime, b.totalTime));
        Map<String, Long> bestTimes = new HashMap<>();

        City startCity = cityMap.get(start);
        if (startCity == null) {
            return Collections.emptyList();
        }
        
        int initialCount = 0;
        for (Station station : List.of(startCity.getBusStation(), startCity.getTrainStation())) {
            for (Departure dep : station.getDepartures()) {
                List<Departure> path = new ArrayList<>();
                path.add(dep);
                long totalTime = calculateTotalTime(path);
                queue.add(new RouteNode(dep.to, path, totalTime));
                bestTimes.put(dep.to, totalTime);
                initialCount++;
            }
        }
        
        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                return current.path;
            }

            Long bestTime = bestTimes.get(current.city);
            if (bestTime != null && current.totalTime > bestTime) continue;

            City currentCity = cityMap.get(current.city);
            if (currentCity == null) continue;
            
            for (Station station : List.of(currentCity.getBusStation(), currentCity.getTrainStation())) {
                for (Departure dep : station.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(current.path);
                    newPath.add(dep);
                    long newTotalTime = calculateTotalTime(newPath);

                    Long existingTime = bestTimes.get(dep.to);
                    if (existingTime == null || newTotalTime <= existingTime) {
                        queue.add(new RouteNode(dep.to, newPath, newTotalTime));
                        bestTimes.put(dep.to, newTotalTime);
                    }
                }
            }
        }
        
        return Collections.emptyList();
    }

    /**
     * Pronalazi najjeftiniju rutu između dva grada koristeći algoritam
     * zasnovan na Dijkstra algoritmu.
     * <p>
     * Umjesto vremena, prioritet u {@link PriorityQueue} određuje ukupna cijena rute.
     * Svaka ruta se proširuje tako što se dodaje novi polazak iz trenutnog grada
     * i ponovo računa cijena. Najmanja ukupna cijena se čuva u mapi
     * kako bi se spriječilo ponovno razmatranje skupljih varijanti.
     * </p>
     *
     * @param start početni grad
     * @param end   krajnji grad
     * @return lista polazaka koja predstavlja najjeftiniju rutu ili prazna lista ako ruta ne postoji
     */
    private List<Departure> findBestPriceRoute(String start, String end) {

        PriorityQueue<RouteNode> queue = new PriorityQueue<>((a, b) -> Long.compare(a.totalTime, b.totalTime));
        Map<String, Integer> bestPrices = new HashMap<>();

        City startCity = cityMap.get(start);
        if (startCity == null) {
            return Collections.emptyList();
        }
        
        int initialCount = 0;
        for (Station station : List.of(startCity.getBusStation(), startCity.getTrainStation())) {
            for (Departure dep : station.getDepartures()) {
                List<Departure> path = new ArrayList<>();
                path.add(dep);
                int totalPrice = calculateTotalPrice(path);
                queue.add(new RouteNode(dep.to, path, totalPrice));
                bestPrices.put(dep.to, totalPrice);
                initialCount++;
            }
        }

        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                return current.path;
            }

            Integer bestPrice = bestPrices.get(current.city);
            if (bestPrice != null && current.totalTime > bestPrice) continue;

            City currentCity = cityMap.get(current.city);
            if (currentCity == null) continue;
            
            for (Station station : List.of(currentCity.getBusStation(), currentCity.getTrainStation())) {
                for (Departure dep : station.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(current.path);
                    newPath.add(dep);
                    int newTotalPrice = calculateTotalPrice(newPath);

                    Integer existingPrice = bestPrices.get(dep.to);
                    if (existingPrice == null || newTotalPrice <= existingPrice) {
                        queue.add(new RouteNode(dep.to, newPath, newTotalPrice));
                        bestPrices.put(dep.to, newTotalPrice);
                    }
                }
            }
        }
        
        return Collections.emptyList();
    }

    /**
     * Pronalazi rutu sa najmanjim brojem presjedanja koristeći
     * algoritam BFS (Breadth-First Search).
     * <p>
     * U ovom slučaju koristi se {@link LinkedList} red za pretragu po širini.
     * BFS je pogodan jer uvijek prvo pronalazi rutu sa najmanjim brojem koraka
     * (tj. najmanjim brojem presjedanja). Svaka ruta se proširuje dodavanjem
     * svih mogućih narednih polazaka dok se ne stigne do cilja.
     * </p>
     *
     * @param start početni grad
     * @param end   krajnji grad
     * @return lista polazaka koja predstavlja rutu sa najmanje presjedanja ili prazna lista ako ruta ne postoji
     */
    private List<Departure> findBestTransfersRoute(String start, String end) {

        Queue<RouteNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        City startCity = cityMap.get(start);
        if (startCity == null) {
            return Collections.emptyList();
        }
        
        int initialCount = 0;
        for (Station station : List.of(startCity.getBusStation(), startCity.getTrainStation())) {
            for (Departure dep : station.getDepartures()) {
                List<Departure> path = new ArrayList<>();
                path.add(dep);
                int transfers = path.size() - 1;
                queue.add(new RouteNode(dep.to, path, transfers));
                initialCount++;
            }
        }

        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                return current.path;
            }

            String visitedKey = current.city + "_" + current.path.size();
            if (!visited.add(visitedKey)) continue;

            City currentCity = cityMap.get(current.city);
            if (currentCity == null) continue;
            
            for (Station station : List.of(currentCity.getBusStation(), currentCity.getTrainStation())) {
                for (Departure dep : station.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(current.path);
                    newPath.add(dep);
                    int newTransfers = newPath.size() - 1;
                    queue.add(new RouteNode(dep.to, newPath, newTransfers));
                }
            }
        }
        
        return Collections.emptyList();
    }

    /**
     * Pronalazi više najbržih ruta između dva grada.
     * <p>
     * Ova metoda koristi kombinaciju algoritma sličnog Dijkstra algoritmu
     * i BFS proširenja kako bi pronašla više različitih ruta.
     * Nakon što se generišu kandidati, rezultati se sortiraju po ukupnom vremenu putovanja,
     * a vraća se najviše {@code limit} najboljih ruta.
     * </p>
     *
     * @param start početni grad
     * @param end   krajnji grad
     * @param limit maksimalan broj ruta koje treba vratiti
     * @return lista najboljih ruta sortiranih po vremenu
     */
    private List<List<Departure>> findTopFastestRoutes(String start, String end, int limit) {

        List<Departure> bestRoute = findBestTimeRoute(start, end);

        Queue<RouteNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        List<List<Departure>> results = new ArrayList<>();

        if (!bestRoute.isEmpty()) {
            results.add(bestRoute);

        }

        City startCity = cityMap.get(start);
        if (startCity == null) {
            return results;
        }
        
        int initialCount = 0;
        for (Station station : List.of(startCity.getBusStation(), startCity.getTrainStation())) {
            for (Departure dep : station.getDepartures()) {
                List<Departure> path = new ArrayList<>();
                path.add(dep);
                long totalTime = calculateTotalTime(path);
                queue.add(new RouteNode(dep.to, path, totalTime));
                initialCount++;
            }
        }
        
        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && results.size() < limit && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                if (!results.contains(current.path)) {
                    results.add(current.path);
                }
                continue;
            }

            String visitedKey = current.city + "_" + current.path.size();
            if (!visited.add(visitedKey)) continue;

            City currentCity = cityMap.get(current.city);
            if (currentCity == null) continue;
            
            for (Station station : List.of(currentCity.getBusStation(), currentCity.getTrainStation())) {
                for (Departure dep : station.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(current.path);
                    newPath.add(dep);
                    long newTotalTime = calculateTotalTime(newPath);
                    queue.add(new RouteNode(dep.to, newPath, newTotalTime));
                }
            }
        }

        results.sort(Comparator.comparingLong((List<Departure> path) -> calculateTotalTime(path)));
        return results;
    }

    /**
     * Pronalazi više najjeftinijih ruta između dva grada.
     * <p>
     * Algoritam je zasnovan na Dijkstra algoritmu, ali se umjesto vremena
     * koristi ukupna cijena kao metrika prioriteta. Nakon što se pronađe
     * više kandidata, rezultati se sortiraju po cijeni i vraća se
     * najviše {@code limit} ruta.
     * </p>
     *
     * @param start početni grad
     * @param end   krajnji grad
     * @param limit maksimalan broj ruta koje treba vratiti
     * @return lista najboljih ruta sortiranih po cijeni
     */
    private List<List<Departure>> findTopByPrice(String start, String end, int limit) {

        List<Departure> bestRoute = findBestPriceRoute(start, end);

        Queue<RouteNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        List<List<Departure>> results = new ArrayList<>();

        if (!bestRoute.isEmpty()) {
            results.add(bestRoute);
        }

        City startCity = cityMap.get(start);
        if (startCity == null) {
            return results;
        }
        
        int initialCount = 0;
        for (Station station : List.of(startCity.getBusStation(), startCity.getTrainStation())) {
            for (Departure dep : station.getDepartures()) {
                List<Departure> path = new ArrayList<>();
                path.add(dep);
                int totalPrice = calculateTotalPrice(path);
                queue.add(new RouteNode(dep.to, path, totalPrice));
                initialCount++;
            }
        }

        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && results.size() < limit && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                if (!results.contains(current.path)) {
                    results.add(current.path);
                }
                continue;
            }

            String visitedKey = current.city + "_" + current.path.size();
            if (!visited.add(visitedKey)) continue;

            City currentCity = cityMap.get(current.city);
            if (currentCity == null) continue;
            
            for (Station station : List.of(currentCity.getBusStation(), currentCity.getTrainStation())) {
                for (Departure dep : station.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(current.path);
                    newPath.add(dep);
                    int newTotalPrice = calculateTotalPrice(newPath);
                    queue.add(new RouteNode(dep.to, newPath, newTotalPrice));
                }
            }
        }

        results.sort(Comparator.comparingInt((List<Departure> path) -> calculateTotalPrice(path)));
        return results;
    }

    /**
     * Pronalazi više ruta sa najmanjim brojem presjedanja između dva grada.
     * <p>
     * Algoritam koristi BFS pretragu za generisanje više ruta i vodi računa
     * da se ne posjećuju iste kombinacije grada i broja presjedanja.
     * Nakon što se generiše skup kandidata, rezultati se sortiraju
     * po broju presjedanja i vraća se najviše {@code limit} ruta.
     * </p>
     *
     * @param start početni grad
     * @param end   krajnji grad
     * @param limit maksimalan broj ruta koje treba vratiti
     * @return lista najboljih ruta sortiranih po broju presjedanja
     */
    private List<List<Departure>> findTopByTransfers(String start, String end, int limit) {

        List<Departure> bestRoute = findBestTransfersRoute(start, end);

        Queue<RouteNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        List<List<Departure>> results = new ArrayList<>();

        if (!bestRoute.isEmpty()) {
            results.add(bestRoute);
        }

        City startCity = cityMap.get(start);
        if (startCity == null) {
            return results;
        }
        
        int initialCount = 0;
        for (Station station : List.of(startCity.getBusStation(), startCity.getTrainStation())) {
            for (Departure dep : station.getDepartures()) {
                List<Departure> path = new ArrayList<>();
                path.add(dep);
                int transfers = path.size() - 1;
                queue.add(new RouteNode(dep.to, path, transfers));
                initialCount++;
            }
        }
        
        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && results.size() < limit && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                if (!results.contains(current.path)) {
                    results.add(current.path);
                }
                continue;
            }

            String visitedKey = current.city + "_" + current.path.size();
            if (!visited.add(visitedKey)) continue;

            City currentCity = cityMap.get(current.city);
            if (currentCity == null) continue;
            
            for (Station station : List.of(currentCity.getBusStation(), currentCity.getTrainStation())) {
                for (Departure dep : station.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(current.path);
                    newPath.add(dep);
                    int newTransfers = newPath.size() - 1;
                    queue.add(new RouteNode(dep.to, newPath, newTransfers));
                }
            }
        }

        results.sort(Comparator.comparingInt(path -> path.size() - 1));
        return results;
    }

    private static class RouteNode {
        String city;
        List<Departure> path;
        long totalTime;

        RouteNode(String city, List<Departure> path, long totalTime) {
            this.city = city;
            this.path = path;
            this.totalTime = totalTime;
        }
    }

    /**
     * Računa ukupno trajanje putovanja za datu rutu.
     * <p>
     * Vrijeme se računa na osnovu prvog polaska i poslednjeg dolaska.
     * Algoritam vodi računa o prelasku dana – ako naredni polazak
     * kreće prije prethodnog dolaska, automatski se dodaje jedan dan.
     * </p>
     *
     * @param path lista polazaka koja čini rutu
     * @return ukupno trajanje u minutima
     */
    private long calculateTotalTime(List<Departure> path) {
        if (path.isEmpty()) return 0;
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalDate today = LocalDate.now();
            
            LocalDateTime firstDeparture = LocalDateTime.of(today, LocalTime.parse(path.get(0).departureTime, formatter));
            LocalDateTime lastArrival = firstDeparture;
            
            for (Departure dep : path) {
                LocalTime depTime = LocalTime.parse(dep.departureTime, formatter);
                LocalDateTime depDateTime = LocalDateTime.of(today, depTime);
                
                if (depDateTime.isBefore(lastArrival)) {
                    today = today.plusDays(1);
                    depDateTime = LocalDateTime.of(today, depTime);
                }
                
                LocalDateTime arrDateTime = depDateTime.plusMinutes(dep.duration);
                lastArrival = arrDateTime;
            }
            
            return ChronoUnit.MINUTES.between(firstDeparture, lastArrival);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Računa ukupnu cijenu rute.
     *
     * @param path lista polazaka koja čini rutu
     * @return zbir svih cijena na ruti
     */
    private int calculateTotalPrice(List<Departure> path) {
        return path.stream().mapToInt(dep -> dep.price).sum();
    }
}

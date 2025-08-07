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

public class RouteFinder {

    public enum Criteria {
        TIME, PRICE, TRANSFERS
    }

    private static final int MAX_TRANSFERS = 50;
    private final Map<String, City> cityMap;

    public RouteFinder(Map<String, City> cityMap) {
        this.cityMap = cityMap;
    }

    public List<Departure> findRoute(String startCity, String endCity, Criteria criteria) {
        
        List<Departure> result = switch (criteria) {
            case TIME -> findBestTimeRoute(startCity, endCity);
            case PRICE -> findBestPriceRoute(startCity, endCity);
            case TRANSFERS -> findBestTransfersRoute(startCity, endCity);
        };

        return result;
    }

    public List<List<Departure>> findTopRoutes(String startCity, String endCity, Criteria criteria, int limit) {
        
        List<List<Departure>> result = switch (criteria) {
            case TIME -> findTopFastestRoutes(startCity, endCity, limit);
            case PRICE -> findTopByPrice(startCity, endCity, limit);
            case TRANSFERS -> findTopByTransfers(startCity, endCity, limit);
        };

        return result;
    }

    public List<Departure> findBestRoute(String startCity, String endCity, Criteria criteria) {
        List<List<Departure>> topRoutes = findTopRoutes(startCity, endCity, criteria, 1);
        return topRoutes.isEmpty() ? Collections.emptyList() : topRoutes.get(0);
    }

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

    private int calculateTotalPrice(List<Departure> path) {
        return path.stream().mapToInt(dep -> dep.price).sum();
    }
}

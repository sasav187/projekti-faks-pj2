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

    private static final int MAX_TRANSFERS = 50; // Higher limit for large matrices
    private final Map<String, City> cityMap;

    public RouteFinder(Map<String, City> cityMap) {
        this.cityMap = cityMap;
    }

    public List<Departure> findRoute(String startCity, String endCity, Criteria criteria) {
        System.out.println("Finding single route from " + startCity + " to " + endCity + " with criteria: " + criteria);
        
        List<Departure> result = switch (criteria) {
            case TIME -> findBestTimeRoute(startCity, endCity);
            case PRICE -> findBestPriceRoute(startCity, endCity);
            case TRANSFERS -> findBestTransfersRoute(startCity, endCity);
        };
        
        System.out.println("Single route found: " + (result.isEmpty() ? "none" : result.size() + " departures"));
        return result;
    }

    public List<List<Departure>> findTopRoutes(String startCity, String endCity, Criteria criteria, int limit) {
        System.out.println("Searching for routes from " + startCity + " to " + endCity + " with criteria: " + criteria);
        System.out.println("City map size: " + cityMap.size());
        System.out.println("Start city exists: " + cityMap.containsKey(startCity));
        System.out.println("End city exists: " + cityMap.containsKey(endCity));
        
        List<List<Departure>> result = switch (criteria) {
            case TIME -> findTopFastestRoutes(startCity, endCity, limit);
            case PRICE -> findTopByPrice(startCity, endCity, limit);
            case TRANSFERS -> findTopByTransfers(startCity, endCity, limit);
        };
        System.out.println("Found " + result.size() + " routes");
        return result;
    }

    public List<Departure> findBestRoute(String startCity, String endCity, Criteria criteria) {
        List<List<Departure>> topRoutes = findTopRoutes(startCity, endCity, criteria, 1);
        return topRoutes.isEmpty() ? Collections.emptyList() : topRoutes.get(0);
    }

    private List<Departure> findBestTimeRoute(String start, String end) {
        System.out.println("Finding best time route...");
        
        // Use PriorityQueue for Dijkstra-like approach to find fastest route
        PriorityQueue<RouteNode> queue = new PriorityQueue<>((a, b) -> Long.compare(a.totalTime, b.totalTime));
        Map<String, Long> bestTimes = new HashMap<>();
        
        // Add all initial departures from start city
        City startCity = cityMap.get(start);
        if (startCity == null) {
            System.out.println("Start city not found!");
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
        System.out.println("Added " + initialCount + " initial departures");
        
        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                // Found the fastest route to destination
                System.out.println("Found fastest route with " + current.path.size() + " departures, time: " + current.totalTime);
                return current.path;
            }
            
            // Only explore if this is the best time to this city
            Long bestTime = bestTimes.get(current.city);
            if (bestTime != null && current.totalTime > bestTime) continue;
            
            // Explore next departures
            City currentCity = cityMap.get(current.city);
            if (currentCity == null) continue;
            
            for (Station station : List.of(currentCity.getBusStation(), currentCity.getTrainStation())) {
                for (Departure dep : station.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(current.path);
                    newPath.add(dep);
                    long newTotalTime = calculateTotalTime(newPath);
                    
                    // Only add if this is a better or equal time
                    Long existingTime = bestTimes.get(dep.to);
                    if (existingTime == null || newTotalTime <= existingTime) {
                        queue.add(new RouteNode(dep.to, newPath, newTotalTime));
                        bestTimes.put(dep.to, newTotalTime);
                    }
                }
            }
            
            if (iterations % 20000 == 0) {
                System.out.println("TIME search: " + iterations + " iterations, queue: " + queue.size());
            }
        }
        
        return Collections.emptyList();
    }

    private List<Departure> findBestPriceRoute(String start, String end) {
        System.out.println("Finding best price route...");
        
        // Use PriorityQueue for Dijkstra-like approach to find cheapest route
        PriorityQueue<RouteNode> queue = new PriorityQueue<>((a, b) -> Long.compare(a.totalTime, b.totalTime));
        Map<String, Integer> bestPrices = new HashMap<>();
        
        // Add all initial departures from start city
        City startCity = cityMap.get(start);
        if (startCity == null) {
            System.out.println("Start city not found!");
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
        System.out.println("Added " + initialCount + " initial departures");
        
        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                // Found the cheapest route to destination
                System.out.println("Found cheapest route with " + current.path.size() + " departures, price: " + current.totalTime);
                return current.path;
            }
            
            // Only explore if this is the best price to this city
            Integer bestPrice = bestPrices.get(current.city);
            if (bestPrice != null && current.totalTime > bestPrice) continue;
            
            // Explore next departures
            City currentCity = cityMap.get(current.city);
            if (currentCity == null) continue;
            
            for (Station station : List.of(currentCity.getBusStation(), currentCity.getTrainStation())) {
                for (Departure dep : station.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(current.path);
                    newPath.add(dep);
                    int newTotalPrice = calculateTotalPrice(newPath);
                    
                    // Only add if this is a better or equal price
                    Integer existingPrice = bestPrices.get(dep.to);
                    if (existingPrice == null || newTotalPrice <= existingPrice) {
                        queue.add(new RouteNode(dep.to, newPath, newTotalPrice));
                        bestPrices.put(dep.to, newTotalPrice);
                    }
                }
            }
            
            if (iterations % 20000 == 0) {
                System.out.println("PRICE search: " + iterations + " iterations, queue: " + queue.size());
            }
        }
        
        return Collections.emptyList();
    }

    private List<Departure> findBestTransfersRoute(String start, String end) {
        System.out.println("Finding best transfers route...");
        
        // Use BFS to find route with minimal transfers
        Queue<RouteNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        // Add all initial departures from start city
        City startCity = cityMap.get(start);
        if (startCity == null) {
            System.out.println("Start city not found!");
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
        System.out.println("Added " + initialCount + " initial departures");
        
        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                // Found the route with minimal transfers to destination
                System.out.println("Found route with minimal transfers: " + current.path.size() + " departures, transfers: " + (int)current.totalTime);
                return current.path;
            }
            
            // Mark as visited to avoid cycles
            String visitedKey = current.city + "_" + current.path.size();
            if (!visited.add(visitedKey)) continue;
            
            // Explore next departures
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
            
            if (iterations % 20000 == 0) {
                System.out.println("TRANSFERS search: " + iterations + " iterations, queue: " + queue.size());
            }
        }
        
        return Collections.emptyList();
    }

    private List<List<Departure>> findTopFastestRoutes(String start, String end, int limit) {
        System.out.println("Finding fastest routes...");
        
        // First, find the best route using the specific algorithm
        List<Departure> bestRoute = findBestTimeRoute(start, end);
        
        // Use BFS with time tracking to find additional routes
        Queue<RouteNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        List<List<Departure>> results = new ArrayList<>();
        
        // Add the best route first if found
        if (!bestRoute.isEmpty()) {
            results.add(bestRoute);
            System.out.println("Added best route with " + bestRoute.size() + " departures");
        }
        
        // Add all initial departures from start city
        City startCity = cityMap.get(start);
        if (startCity == null) {
            System.out.println("Start city not found!");
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
        System.out.println("Added " + initialCount + " initial departures");
        
        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && results.size() < limit && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                // Found a route to destination
                // Skip if it's the same as the best route
                if (!results.contains(current.path)) {
                    results.add(current.path);
                    System.out.println("Found fastest route " + results.size() + " with " + current.path.size() + " departures, time: " + current.totalTime);
                }
                continue;
            }
            
            // Mark as visited to avoid cycles
            String visitedKey = current.city + "_" + current.path.size();
            if (!visited.add(visitedKey)) continue;
            
            // Explore next departures
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
            
            if (iterations % 20000 == 0) {
                System.out.println("TIME search: " + iterations + " iterations, queue: " + queue.size() + ", found: " + results.size());
            }
        }
        
        // Sort by total time to ensure fastest first
        results.sort(Comparator.comparingLong((List<Departure> path) -> calculateTotalTime(path)));
        return results;
    }

    private List<List<Departure>> findTopByPrice(String start, String end, int limit) {
        System.out.println("Finding cheapest routes...");
        
        // First, find the best route using the specific algorithm
        List<Departure> bestRoute = findBestPriceRoute(start, end);
        
        // Use BFS with price tracking to find additional routes
        Queue<RouteNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        List<List<Departure>> results = new ArrayList<>();
        
        // Add the best route first if found
        if (!bestRoute.isEmpty()) {
            results.add(bestRoute);
            System.out.println("Added best route with " + bestRoute.size() + " departures");
        }
        
        // Add all initial departures from start city
        City startCity = cityMap.get(start);
        if (startCity == null) {
            System.out.println("Start city not found!");
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
        System.out.println("Added " + initialCount + " initial departures");
        
        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && results.size() < limit && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                // Found a route to destination
                // Skip if it's the same as the best route
                if (!results.contains(current.path)) {
                    results.add(current.path);
                    System.out.println("Found cheapest route " + results.size() + " with " + current.path.size() + " departures, price: " + current.totalTime);
                }
                continue;
            }
            
            // Mark as visited to avoid cycles
            String visitedKey = current.city + "_" + current.path.size();
            if (!visited.add(visitedKey)) continue;
            
            // Explore next departures
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
            
            if (iterations % 20000 == 0) {
                System.out.println("PRICE search: " + iterations + " iterations, queue: " + queue.size() + ", found: " + results.size());
            }
        }
        
        // Sort by total price to ensure cheapest first
        results.sort(Comparator.comparingInt((List<Departure> path) -> calculateTotalPrice(path)));
        return results;
    }

    private List<List<Departure>> findTopByTransfers(String start, String end, int limit) {
        System.out.println("Finding routes with minimal transfers...");
        
        // First, find the best route using the specific algorithm
        List<Departure> bestRoute = findBestTransfersRoute(start, end);
        
        // Use BFS to find additional routes with minimal transfers
        Queue<RouteNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        List<List<Departure>> results = new ArrayList<>();
        
        // Add the best route first if found
        if (!bestRoute.isEmpty()) {
            results.add(bestRoute);
            System.out.println("Added best route with " + bestRoute.size() + " departures");
        }
        
        // Add all initial departures from start city
        City startCity = cityMap.get(start);
        if (startCity == null) {
            System.out.println("Start city not found!");
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
        System.out.println("Added " + initialCount + " initial departures");
        
        int iterations = 0;
        int maxIterations = 200000;
        
        while (!queue.isEmpty() && results.size() < limit && iterations < maxIterations) {
            iterations++;
            RouteNode current = queue.poll();
            
            if (current.path.size() > MAX_TRANSFERS) continue;
            
            if (current.city.equals(end)) {
                // Found a route to destination
                // Skip if it's the same as the best route
                if (!results.contains(current.path)) {
                    results.add(current.path);
                    System.out.println("Found route with minimal transfers " + results.size() + " with " + current.path.size() + " departures, transfers: " + (int)current.totalTime);
                }
                continue;
            }
            
            // Mark as visited to avoid cycles
            String visitedKey = current.city + "_" + current.path.size();
            if (!visited.add(visitedKey)) continue;
            
            // Explore next departures
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
            
            if (iterations % 20000 == 0) {
                System.out.println("TRANSFERS search: " + iterations + " iterations, queue: " + queue.size() + ", found: " + results.size());
            }
        }
        
        // Sort by number of transfers to ensure minimal transfers first
        results.sort(Comparator.comparingInt(path -> path.size() - 1));
        return results;
    }

    private static class RouteNode {
        String city;
        List<Departure> path;
        long totalTime; // Used for time, price, or transfers

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

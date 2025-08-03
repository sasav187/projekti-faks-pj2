package org.unibl.etf.algo;

import org.unibl.etf.model.City;
import org.unibl.etf.model.Departure;
import org.unibl.etf.model.Station;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RouteFinder {

    public enum Criteria {
        TIME, PRICE, TRANSFERS
    }

    private static final int MAX_TRANSFERS = 20;
    private final Map<String, City> cityMap;

    public RouteFinder(Map<String, City> cityMap) {
        this.cityMap = cityMap;
    }

    public List<Departure> findRoute(String startCity, String endCity, Criteria criteria) {
        return switch (criteria) {
            case TIME -> findFastestRoute(startCity, endCity);
            case PRICE -> dijkstra(startCity, endCity, Criteria.PRICE);
            case TRANSFERS -> bfs(startCity, endCity);
        };
    }

    public List<List<Departure>> findTopRoutes(String startCity, String endCity, Criteria criteria, int limit) {
        return switch (criteria) {
            case TIME -> findTopFastestRoutes(startCity, endCity, limit);
            case PRICE -> findTopByDijkstra(startCity, endCity, limit, Criteria.PRICE);
            case TRANSFERS -> findTopByDijkstra(startCity, endCity, limit, Criteria.TRANSFERS);
        };
    }

    private List<Departure> findFastestRoute(String start, String end) {
        List<List<Departure>> top = findTopFastestRoutes(start, end, 1);
        return top.isEmpty() ? Collections.emptyList() : top.get(0);
    }

    private List<List<Departure>> findTopFastestRoutes(String start, String end, int limit) {
        class Node {
            String city;
            List<Departure> path;
            LocalDateTime arrivalDateTime;
            long totalMinutes;

            Node(String city, List<Departure> path, LocalDateTime arrivalDateTime, long totalMinutes) {
                this.city = city;
                this.path = path;
                this.arrivalDateTime = arrivalDateTime;
                this.totalMinutes = totalMinutes;
            }
        }

        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingLong(n -> n.totalMinutes));
        Map<String, Integer> visited = new HashMap<>();
        List<List<Departure>> results = new ArrayList<>();
        LocalDate today = LocalDate.now();

        City startCity = cityMap.get(start);
        if (startCity == null) return results;

        for (Station s : List.of(startCity.getBusStation(), startCity.getTrainStation())) {
            for (Departure d : s.getDepartures()) {
                List<Departure> path = new ArrayList<>();
                path.add(d);
                LocalDateTime depTime = LocalDateTime.of(today, d.getDepartureTime());
                LocalDateTime arrTime = depTime.plusMinutes(d.duration);
                long totalMinutes = ChronoUnit.MINUTES.between(depTime, arrTime);
                queue.add(new Node(d.to, path, arrTime, totalMinutes));
            }
        }

        while (!queue.isEmpty() && results.size() < limit) {
            Node node = queue.poll();

            if (node.path.size() > MAX_TRANSFERS) continue;

            int previousTransfers = visited.getOrDefault(node.city, Integer.MAX_VALUE);
            if (node.path.size() >= previousTransfers) continue;
            visited.put(node.city, node.path.size());

            if (node.city.equals(end)) {
                results.add(node.path);
                continue;
            }

            City current = cityMap.get(node.city);
            if (current == null) continue;

            for (Station s : List.of(current.getBusStation(), current.getTrainStation())) {
                for (Departure d : s.getDepartures()) {
                    LocalDateTime earliest = node.arrivalDateTime.plusMinutes(d.minTransferTime);
                    LocalDateTime depDateTime = LocalDateTime.of(earliest.toLocalDate(), d.getDepartureTime());
                    if (depDateTime.isBefore(earliest)) depDateTime = depDateTime.plusDays(1);
                    LocalDateTime arrDateTime = depDateTime.plusMinutes(d.duration);

                    long totalMinutes = ChronoUnit.MINUTES.between(
                            node.path.get(0).getDepartureTime().atDate(today),
                            arrDateTime
                    );

                    List<Departure> newPath = new ArrayList<>(node.path);
                    newPath.add(d);
                    queue.add(new Node(d.to, newPath, arrDateTime, totalMinutes));
                }
            }
        }

        return results;
    }

    private List<Departure> dijkstra(String start, String end, Criteria criteria) {
        List<List<Departure>> top = findTopByDijkstra(start, end, 1, criteria);
        return top.isEmpty() ? Collections.emptyList() : top.get(0);
    }

    private List<List<Departure>> findTopByDijkstra(String start, String end, int limit, Criteria criteria) {
        class Node {
            String city;
            List<Departure> path;
            int cost;

            Node(String city, List<Departure> path, int cost) {
                this.city = city;
                this.path = path;
                this.cost = cost;
            }
        }

        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Map<String, Integer> visited = new HashMap<>();
        List<List<Departure>> results = new ArrayList<>();

        queue.add(new Node(start, new ArrayList<>(), 0));

        while (!queue.isEmpty() && results.size() < limit) {
            Node node = queue.poll();

            if (node.path.size() > MAX_TRANSFERS) continue;

            int previousCost = visited.getOrDefault(node.city, Integer.MAX_VALUE);
            if (node.cost >= previousCost) continue;
            visited.put(node.city, node.cost);

            if (node.city.equals(end)) {
                results.add(node.path);
                continue;
            }

            City city = cityMap.get(node.city);
            if (city == null) continue;

            for (Station s : List.of(city.getBusStation(), city.getTrainStation())) {
                for (Departure d : s.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(node.path);
                    newPath.add(d);
                    int cost = node.cost + getCost(d, criteria);
                    queue.add(new Node(d.to, newPath, cost));
                }
            }
        }

        return results;
    }

    private int getCost(Departure d, Criteria criteria) {
        return switch (criteria) {
            case PRICE -> d.price;
            case TIME -> d.duration;
            case TRANSFERS -> 1;
        };
    }

    private List<Departure> bfs(String start, String end) {
        Queue<List<Departure>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        City startCity = cityMap.get(start);
        if (startCity == null) return Collections.emptyList();

        for (Station s : List.of(startCity.getBusStation(), startCity.getTrainStation())) {
            for (Departure d : s.getDepartures()) {
                List<Departure> path = new ArrayList<>();
                path.add(d);
                queue.add(path);
            }
        }

        while (!queue.isEmpty()) {
            List<Departure> path = queue.poll();
            if (path.size() > MAX_TRANSFERS) continue;

            Departure last = path.get(path.size() - 1);
            if (last.to.equals(end)) return path;
            if (!visited.add(last.to)) continue;

            City next = cityMap.get(last.to);
            if (next == null) continue;

            for (Station s : List.of(next.getBusStation(), next.getTrainStation())) {
                for (Departure d : s.getDepartures()) {
                    List<Departure> newPath = new ArrayList<>(path);
                    newPath.add(d);
                    queue.add(newPath);
                }
            }
        }

        return Collections.emptyList();
    }
}

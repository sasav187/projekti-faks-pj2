package org.unibl.etf.algo;

import org.unibl.etf.model.City;
import org.unibl.etf.model.Departure;
import org.unibl.etf.model.Station;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RouteFinder {

    public enum Criteria {
        TIME, PRICE, TRANSFERS
    }

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

    private List<Departure> findFastestRoute(String start, String end) {
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
        Set<String> visited = new HashSet<>();

        City startCity = cityMap.get(start);
        if (startCity == null) return Collections.emptyList();

        LocalDate today = LocalDate.now(); // Polazni dan

        for (Station s : List.of(startCity.getBusStation(), startCity.getTrainStation())) {
            for (Departure d : s.getDepartures()) {
                List<Departure> initialPath = new ArrayList<>();
                initialPath.add(d);
                LocalDateTime departureDateTime = LocalDateTime.of(today, d.getDepartureTime());
                LocalDateTime arrivalDateTime = LocalDateTime.of(today, d.getArrivalTime());
                long totalMinutes = ChronoUnit.MINUTES.between(departureDateTime, arrivalDateTime);
                queue.add(new Node(d.to, initialPath, arrivalDateTime, totalMinutes));
            }
        }

        while (!queue.isEmpty()) {
            Node node = queue.poll();

            if (node.city.equals(end)) return node.path;
            if (!visited.add(node.city)) continue;

            City city = cityMap.get(node.city);
            if (city == null) continue;

            for (Station s : List.of(city.getBusStation(), city.getTrainStation())) {
                for (Departure d : s.getDepartures()) {
                    if (!visited.contains(d.to)) {

                        // Minimalno vrijeme presjedanja
                        LocalDateTime earliestPossibleDeparture = node.arrivalDateTime.plusMinutes(d.minTransferTime);
                        LocalTime candidateDepartureTime = d.getDepartureTime();
                        LocalDateTime departureDateTime = LocalDateTime.of(earliestPossibleDeparture.toLocalDate(), candidateDepartureTime);

                        // Ako bi vožnja krenula ranije od minimalnog transfera (npr. prelaz preko ponoći)
                        if (departureDateTime.isBefore(earliestPossibleDeparture)) {
                            departureDateTime = departureDateTime.plusDays(1);
                        }

                        // Sada možemo računati
                        LocalDateTime arrivalDateTime = departureDateTime.plusMinutes(d.duration);
                        long totalMinutes = ChronoUnit.MINUTES.between(
                                node.path.get(0).getDepartureTime().atDate(today),
                                arrivalDateTime
                        );

                        List<Departure> newPath = new ArrayList<>(node.path);
                        newPath.add(d);
                        queue.add(new Node(d.to, newPath, arrivalDateTime, totalMinutes));
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    private List<Departure> dijkstra(String start, String end, Criteria criteria) {
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
        Set<String> visited = new HashSet<>();
        queue.add(new Node(start, new ArrayList<>(), 0));

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (!visited.add(node.city)) continue;
            if (node.city.equals(end)) return node.path;

            City city = cityMap.get(node.city);
            if (city == null) continue;

            for (Station s : List.of(city.getBusStation(), city.getTrainStation())) {
                for (Departure d : s.getDepartures()) {
                    if (!visited.contains(d.to)) {
                        List<Departure> newPath = new ArrayList<>(node.path);
                        newPath.add(d);
                        int cost = node.cost + getCost(d, criteria);
                        queue.add(new Node(d.to, newPath, cost));
                    }
                }
            }
        }
        return Collections.emptyList();
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
        visited.add(start);

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

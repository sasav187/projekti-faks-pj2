package org.unibl.etf.stats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptStatistics {
    
    private static final Pattern PRICE_PATTERN = Pattern.compile("Ukupna cijena: (\\d+) KM");
    private static final String RECEIPTS_DIR = "receipts";
    
    public static StatisticsData calculateStatistics() {
        StatisticsData stats = new StatisticsData();
        
        try {
            Path receiptsPath = Paths.get(RECEIPTS_DIR);
            if (!Files.exists(receiptsPath)) {
                return stats; // Return empty stats if directory doesn't exist
            }
            
            // Count all .txt files in receipts directory
            List<Path> receiptFiles = Files.walk(receiptsPath)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .toList();
            
            stats.setTotalTickets(receiptFiles.size());
            
            // Calculate total money earned
            int totalMoney = 0;
            for (Path receiptFile : receiptFiles) {
                try {
                    String content = Files.readString(receiptFile);
                    Matcher matcher = PRICE_PATTERN.matcher(content);
                    if (matcher.find()) {
                        totalMoney += Integer.parseInt(matcher.group(1));
                    }
                } catch (IOException e) {
                    System.err.println("Error reading receipt file: " + receiptFile);
                }
            }
            
            stats.setTotalMoneyEarned(totalMoney);
            
        } catch (IOException e) {
            System.err.println("Error calculating statistics: " + e.getMessage());
        }
        
        return stats;
    }
    
    public static class StatisticsData {
        private int totalTickets;
        private int totalMoneyEarned;
        
        public StatisticsData() {
            this.totalTickets = 0;
            this.totalMoneyEarned = 0;
        }
        
        public int getTotalTickets() {
            return totalTickets;
        }
        
        public void setTotalTickets(int totalTickets) {
            this.totalTickets = totalTickets;
        }
        
        public int getTotalMoneyEarned() {
            return totalMoneyEarned;
        }
        
        public void setTotalMoneyEarned(int totalMoneyEarned) {
            this.totalMoneyEarned = totalMoneyEarned;
        }
    }
} 
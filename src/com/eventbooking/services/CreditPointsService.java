package com.eventbooking.services;

import com.eventbooking.database.DatabaseManager;
import org.bson.Document;
import java.time.LocalDateTime;
import java.util.List;

public class CreditPointsService {
    private static CreditPointsService instance;
    private DatabaseManager dbManager;
    private EmailService emailService;

    private static final int[] RANK_POINTS = {
        1000, 750, 500, 400, 350, 300, 250, 200, 150, 100, 75, 50, 25
    };

    private CreditPointsService() {
        this.dbManager = new DatabaseManager();
        this.emailService = EmailService.getInstance();
    }

    public static synchronized CreditPointsService getInstance() {
        if (instance == null) {
            instance = new CreditPointsService();
        }
        return instance;
    }

    /**
     * NEW: Calculate points based on daily rank.
     */
    public int calculatePointsForRank(int rank) {
        if (rank <= 10) return RANK_POINTS[rank - 1];
        if (rank <= 20) return RANK_POINTS[10];
        if (rank <= 50) return RANK_POINTS[11];
        return RANK_POINTS[12];
    }

    /**
     * NEW: Award daily credit points to top attendees of the day.
     */
    public void awardDailyCredits() {
        System.out.println("\nDistributing daily credit points...");
        List<Document> topAttendees = dbManager.getDailyTopAttendees();
        if (topAttendees.isEmpty()) {
            System.out.println("No bookings today. No points awarded.");
            return;
        }
        
        int rank = 1;
        for (Document attendee : topAttendees) {
            String username = attendee.getString("username");
            int tickets = attendee.getInteger("dailyTickets", 0);
            int points = calculatePointsForRank(rank);
            
            if (dbManager.addCreditPoints(username, points)) {
                String notificationMessage = String.format(
                    "Daily Reward! You ranked #%d today and earned %d credit points!", rank, points
                );
                dbManager.addUserNotification(username, notificationMessage);
                System.out.printf(
                    "Rank #%d: Awarded %d points to '%s' for purchasing %d tickets.\n", rank, points, username, tickets
                );
            }
            rank++;
        }
        System.out.println("\nDaily credit point distribution complete.");
    }

    public void awardMonthlyCredits() {
        System.out.println("\n--- MONTHLY CREDIT POINTS DISTRIBUTION ---");
        List<Document> topAttendees = dbManager.getMonthlyTopAttendees(100);
        
        int rank = 1;
        int usersAwarded = 0;
        
        for (Document attendee : topAttendees) {
            String username = attendee.getString("username");
            int monthlyTickets = attendee.getInteger("monthlyTickets", 0);
            
            if (monthlyTickets == 0) continue;
            
            int creditPoints = calculatePointsForRank(rank);
            
            if (dbManager.addCreditPoints(username, creditPoints)) {
                usersAwarded++;
                
                Document userDoc = dbManager.getUserDetails(username);
                String email = userDoc != null ? userDoc.getString("email") : null;
                
                if (email != null && !email.isEmpty()) {
                    emailService.sendCreditPointsNotification(email, username, rank, creditPoints, monthlyTickets);
                }
                
                dbManager.addUserNotification(
                    username,
                    String.format("Monthly Rewards! You ranked #%d and earned %d credit points!", rank, creditPoints)
                );
                
                System.out.printf("Rank #%d: %s - %d points awarded (Tickets: %d)\n", rank, username, creditPoints, monthlyTickets);
            }
            
            rank++;
        }
        
        System.out.println("\nTotal Users Awarded: " + usersAwarded);
        dbManager.resetMonthlyStats();
    }

    public boolean awardPoints(String username, int points, String reason) {
        boolean success = dbManager.addCreditPoints(username, points);
        
        if (success) {
            dbManager.addUserNotification(
                username,
                String.format("You received %d credit points! Reason: %s", points, reason)
            );
            System.out.printf("Awarded %d points to %s - Reason: %s\n", points, username, reason);
        }
        
        return success;
    }

    public boolean redeemPoints(String username, int points) {
        int currentPoints = dbManager.getUserCreditPoints(username);
        
        if (currentPoints < points) {
            System.err.println("Insufficient points for " + username);
            return false;
        }
        
        boolean success = dbManager.deductCreditPoints(username, points);
        
        if (success) {
            dbManager.addUserNotification(
                username,
                String.format("You redeemed %d credit points for a discount!", points)
            );
            System.out.printf("%s redeemed %d points\n", username, points);
        }
        
        return success;
    }

    public double calculateDiscount(int points) {
        return points * 0.10;
    }

    public int getPointsForDiscount(double discountAmount) {
        return (int) Math.ceil(discountAmount / 0.10);
    }

    public int getUserPoints(String username) {
        return dbManager.getUserCreditPoints(username);
    }

    public String getCreditPointsTierInfo() {
        return "--- CREDIT POINTS REWARD TIERS ---\n" +
               "Rank #1    : 1000 points\n" +
               "Rank #2    :  750 points\n" +
               "Rank #3    :  500 points\n" +
               "Rank #4-10 :  100-400 points\n" +
               "Rank #11-20:   75 points\n" +
               "Rank #21-50:   50 points\n" +
               "Rank #51+  :   25 points\n" +
               "-------------------------------------\n" +
               "Points are awarded at month end\n" +
               "1 point = $0.10 discount\n" +
               "-------------------------------------";
    }
}
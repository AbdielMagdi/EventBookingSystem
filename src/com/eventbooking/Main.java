package com.eventbooking;

import com.eventbooking.ui.LoginFrame;
import com.eventbooking.database.DatabaseManager;
import com.eventbooking.services.DailyTaskScheduler; // Import the scheduler
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Initializing database connection...");
            DatabaseManager dbManager = new DatabaseManager();
            System.out.println("Success: Database connection established.");
            
            seedInitialData(dbManager);
            
            // Start the automatic daily task scheduler
            DailyTaskScheduler scheduler = new DailyTaskScheduler();
            scheduler.start();
            
            javax.swing.SwingUtilities.invokeLater(() -> {
                new LoginFrame();
                System.out.println("Success: Event Booking System launched.");
            });
            
        } catch (Exception e) {
            System.err.println("FATAL: Failed to connect to database: " + e.getMessage());
            System.err.println("Please ensure MongoDB is running on localhost:27017");
            e.printStackTrace();
        }
    }

    private static void seedInitialData(DatabaseManager dbManager) {
        try {
            MongoDatabase database = MongoDBConnection.getDatabase();
            MongoCollection<Document> usersCollection = database.getCollection("users");
            
            Document adminQuery = new Document("username", "admin").append("role", "Admin");
            
            if (usersCollection.find(adminQuery).first() == null) {
                Document adminDoc = new Document("username", "admin")
                        .append("password", dbManager.hashPassword("admin"))
                        .append("email", "admin@eventbooking.com")
                        .append("phone", "0000000000")
                        .append("role", "Admin");
                
                usersCollection.insertOne(adminDoc);
                System.out.println("Success: Default admin user created (username: admin, password: admin)");
            } else {
                System.out.println("Info: Admin user already exists.");
            }
            
            System.out.println("Info: Initial data check complete.");
        } catch (Exception e) {
            System.err.println("Warning: Could not seed initial data - " + e.getMessage());
        }
    }
}
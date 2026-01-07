package com.eventbooking;

import com.mongodb.client.*;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;

public class MongoDBConnection {
    private static MongoClient client;
    private static MongoDatabase database;

    static {
        ConnectionString connString = new ConnectionString("mongodb://localhost:27017/");
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .build();
        client = MongoClients.create(settings);
        database = client.getDatabase("event_booking_db");
    }

    public static MongoDatabase getDatabase() {
        return database;
    }
}

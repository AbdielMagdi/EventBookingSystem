package com.eventbooking.models;

import java.time.LocalDateTime;

public class Booking {
    private int id;
    private String username;
    private int eventId;
    private String eventName;
    private int seatsBooked;
    private double totalPrice;
    private LocalDateTime timestamp;
    private boolean cancelled = false;
    private String status;
    private String paymentMethod;
    private String transactionId;

    public Booking(int id, String username, int eventId, String eventName,
                   int seatsBooked, double totalPrice, LocalDateTime timestamp) {
        this.id = id;
        this.username = username;
        this.eventId = eventId;
        this.eventName = eventName;
        this.seatsBooked = seatsBooked;
        this.totalPrice = totalPrice;
        this.timestamp = timestamp;
        this.paymentMethod = "Not specified";
        this.transactionId = "";
        updateStatus();
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public int getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public int getSeatsBooked() { return seatsBooked; }
    public double getTotalPrice() { return totalPrice; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isCancelled() { return cancelled; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransactionId() { return transactionId; }

    // Setters for payment details
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void cancel() { 
        this.cancelled = true; 
        updateStatus();
    }

    private void updateStatus() {
        this.status = cancelled ? "Cancelled" : "Active";
    }
}
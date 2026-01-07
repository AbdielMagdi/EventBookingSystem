package com.eventbooking.models;

import java.time.LocalDate;

public class Event {
    private int id;
    private String name;
    private String type;
    private LocalDate date;
    private String venue;
    private int totalSeats;
    private int seatsAvailable;
    private double price;
    private String status; // "Upcoming", "Event Day", "Completed"
    private String cancellationReason;
    private LocalDate originalDate;

    public Event(int id, String name, String type, LocalDate date, String venue, int totalSeats, double price) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.date = date;
        this.venue = venue;
        this.totalSeats = totalSeats;
        this.seatsAvailable = totalSeats;
        this.price = price;
        updateStatus(); // Calculate status based on date
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public LocalDate getDate() { return date; }
    public String getVenue() { return venue; }
    public int getTotalSeats() { return totalSeats; }
    public int getSeatsAvailable() { return seatsAvailable; }
    public double getPrice() { return price; }
    public String getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDate getOriginalDate() { return originalDate; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setDate(LocalDate date) { 
        this.date = date;
        updateStatus(); // Recalculate status when date changes
    }
    public void setVenue(String venue) { this.venue = venue; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public void setSeatsAvailable(int seatsAvailable) { this.seatsAvailable = seatsAvailable; }
    public void setPrice(double price) { this.price = price; }
    public void setStatus(String status) { this.status = status; }
    public void setCancellationReason(String reason) { this.cancellationReason = reason; }
    public void setOriginalDate(LocalDate date) { this.originalDate = date; }

    /**
     * Update event status based on current date
     * - Before event date: "Upcoming"
     * - On event date: "Event Day"
     * - After event date: "Completed"
     */
    public void updateStatus() {
        LocalDate today = LocalDate.now();
        
        if (date.isBefore(today)) {
            status = "Completed";
        } else if (date.isEqual(today)) {
            status = "Event Day";
        } else {
            status = "Upcoming";
        }
    }

    /**
     * Check if event is bookable
     * Event is bookable only if it's upcoming and has available seats
     */
    public boolean isBookable() {
        updateStatus(); // Ensure status is current
        return (status.equals("Upcoming") || status.equals("Event Day")) && seatsAvailable > 0;
    }

    /**
     * Check if event is completed (past date)
     */
    public boolean isCompleted() {
        updateStatus();
        return status.equals("Completed");
    }

    /**
     * Check if today is the event day
     */
    public boolean isEventDay() {
        updateStatus();
        return status.equals("Event Day");
    }

    /**
     * Allocate seats (reduce available seats)
     */
    public boolean allocateSeats(int seats) {
        if (seats > seatsAvailable) {
            return false;
        }
        seatsAvailable -= seats;
        return true;
    }

    /**
     * Book seats for this event
     */
    public boolean bookSeats(int seats) {
        if (!isBookable()) {
            return false;
        }
        if (seats > seatsAvailable) {
            return false;
        }
        seatsAvailable -= seats;
        return true;
    }

    /**
     * Release seats (return seats to pool)
     */
    public void releaseSeats(int seats) {
        seatsAvailable = Math.min(seatsAvailable + seats, totalSeats);
    }
    
    /**
     * Cancel booking and return seats
     */
    public void returnSeats(int seats) {
        seatsAvailable = Math.min(seatsAvailable + seats, totalSeats);
    }

    /**
     * Calculate tickets sold
     */
    public int getTicketsSold() {
        return totalSeats - seatsAvailable;
    }

    /**
     * Calculate occupancy rate
     */
    public double getOccupancyRate() {
        if (totalSeats == 0) return 0.0;
        return ((double) getTicketsSold() / totalSeats) * 100;
    }

    /**
     * Calculate total revenue
     */
    public double getTotalRevenue() {
        return getTicketsSold() * price;
    }

    /**
     * Get formatted date string
     */
    public String getFormattedDate() {
        return date.toString();
    }

    /**
     * Get status with emoji indicator
     */
    public String getStatusWithIcon() {
        updateStatus();
        switch (status) {
            case "Upcoming":
                return "📅 " + status;
            case "Event Day":
                return "🎉 " + status;
            case "Completed":
                return "✅ " + status;
            default:
                return status;
        }
    }

    @Override
    public String toString() {
        updateStatus();
        return String.format("%s - %s (%s) - %s - $%.2f [%s]", 
            name, type, date, venue, price, status);
    }
}
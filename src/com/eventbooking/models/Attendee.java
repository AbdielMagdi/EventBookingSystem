package com.eventbooking.models;

import com.eventbooking.ui.AttendeeDashboard;

public class Attendee extends User {
    private int ticketsBought = 0;
    private int eventsAttended = 0;

    public Attendee(String username, String password) {
        super(username, password);
    }

    public Attendee(String username, String password, String email, String phone) {
        super(username, password, email, phone);
    }

    @Override
    public String getRole() {
        return "Attendee";
    }

    public int getTicketsBought() { return ticketsBought; }
    public void incrementTickets(int n) { ticketsBought += n; }
    public void setTicketsBought(int n) { ticketsBought = n; }
    
    public int getEventsAttended() { return eventsAttended; }
    public void setEventsAttended(int n) { eventsAttended = n; }

    @Override
    public void openDashboard() {
        new AttendeeDashboard(username);
    }
}
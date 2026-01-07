package com.eventbooking.models;

public class Seat {
    private String seatId;
    private String row;
    private int number;
    private boolean isBooked;
    private String bookedBy;

    public Seat(String row, int number) {
        this.row = row;
        this.number = number;
        this.seatId = row + number;
        this.isBooked = false;
    }

    public String getSeatId() { return seatId; }
    public int getNumber() { return number; }
    public boolean isBooked() { return isBooked; }

    public void book(String username) {
        this.isBooked = true;
        this.bookedBy = username;
    }
}
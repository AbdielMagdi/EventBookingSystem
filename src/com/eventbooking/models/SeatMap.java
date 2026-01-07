package com.eventbooking.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeatMap {
    private int eventId;
    private List<String> rows;
    private int seatsPerRow;
    private Map<String, Seat> seats;

    public SeatMap(int eventId, List<String> rows, int seatsPerRow) {
        this.eventId = eventId;
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        this.seats = new HashMap<>();
        for (String row : rows) {
            for (int i = 1; i <= seatsPerRow; i++) {
                seats.put(row + i, new Seat(row, i));
            }
        }
    }

    public List<String> getRows() { return rows; }
    public int getSeatsPerRow() { return seatsPerRow; }
    public Seat getSeat(String seatId) { return seats.get(seatId); }
}
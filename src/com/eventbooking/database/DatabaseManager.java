package com.eventbooking.database;

import com.eventbooking.models.*;
import com.eventbooking.services.EmailService;
import com.eventbooking.services.RefundService;
import com.eventbooking.MongoDBConnection;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class DatabaseManager {
    private MongoDatabase database;
    private MongoCollection<Document> usersCollection;
    private MongoCollection<Document> eventsCollection;
    private MongoCollection<Document> bookingsCollection;
    private MongoCollection<Document> seatMapsCollection;
    private MongoCollection<Document> notificationsCollection;

    public DatabaseManager() {
        this.database = MongoDBConnection.getDatabase();
        this.usersCollection = database.getCollection("users");
        this.eventsCollection = database.getCollection("events");
        this.bookingsCollection = database.getCollection("bookings");
        this.seatMapsCollection = database.getCollection("seatmaps");
        this.notificationsCollection = database.getCollection("notifications");
    }

    // ==========================================================
    // == USER & PROFILE OPERATIONS ==
    // ==========================================================
    public boolean registerUser(String u, String p, String e, String ph) {
        if (usersCollection.find(new Document("username", u)).first() != null) return false;
        String hp = hashPassword(p);
        Document d = new Document("username", u)
            .append("password", hp)
            .append("email", e)
            .append("phone", ph)
            .append("role", "Attendee")
            .append("ticketsBought", 0)
            .append("eventsAttended", 0)
            .append("creditPoints", 0)
            .append("monthlyTickets", 0)
            .append("monthlyEvents", 0)
            .append("monthlySpent", 0.0)
            .append("profileImagePath", null); // ADDED: Field for profile image path
        usersCollection.insertOne(d);
        return true;
    }

    public User authenticateUser(String u, String p, String r) {
        Document d = usersCollection.find(new Document("username", u).append("role", r)).first();
        if (d != null && d.getString("password").equals(hashPassword(p))) {
            if (r.equals("Admin")) return new Admin(u, p, d.getString("email"), d.getString("phone"));
            else {
                Attendee a = new Attendee(u, p, d.getString("email"), d.getString("phone"));
                a.setTicketsBought(d.getInteger("ticketsBought", 0));
                a.setEventsAttended(d.getInteger("eventsAttended", 0));
                return a;
            }
        }
        return null;
    }
    
    public Document getUserDetails(String u) { 
        return usersCollection.find(new Document("username", u)).first(); 
    }
    
    /**
     * UPDATED: Now accepts and updates the user's profile image path.
     */
    public boolean updateUserProfile(String u, String e, String ph, String imagePath) { 
        try { 
            usersCollection.updateOne(Filters.eq("username", u), 
                Updates.combine(
                    Updates.set("email", e), 
                    Updates.set("phone", ph),
                    Updates.set("profileImagePath", imagePath) // ADDED: Update profile image path
                )); 
            return true; 
        } catch (Exception ex) { 
            return false; 
        }
    }
    
    public boolean changePassword(String u, String op, String np) {
        Document d = usersCollection.find(Filters.eq("username", u)).first();
        if (d == null || !d.getString("password").equals(hashPassword(op))) return false;
        try { 
            usersCollection.updateOne(Filters.eq("username", u), 
                Updates.set("password", hashPassword(np))); 
            return true; 
        } catch (Exception ex) { 
            return false; 
        }
    }

    public boolean isEmailInUse(String e, String u) {
        Document d = usersCollection.find(Filters.eq("email", e)).first();
        return d != null && !d.getString("username").equals(u);
    }
    
    public String hashPassword(String p) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(p.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);
            StringBuilder hexString = new StringBuilder(number.toString(16));
            while (hexString.length() < 64) { hexString.insert(0, '0'); }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) { 
            throw new RuntimeException(e); 
        }
    }

    // ==========================================================
    // == EVENT CRUD & SEARCH OPERATIONS ==
    // ==========================================================

    public boolean addEvent(Event event) {
        try {
            Document doc = eventToDocument(event);
            eventsCollection.insertOne(doc);
            createDefaultSeatMap(event.getId());
            return true;
        } catch (Exception e) { 
            return false; 
        }
    }

    public List<Booking> getBookingsForEvent(int eventId) {
        List<Booking> bookings = new ArrayList<>();
        try {
            for (Document doc : bookingsCollection.find(new Document("eventId", eventId))) {
                try {
                    Booking booking = new Booking(
                        doc.getInteger("id"),
                        doc.getString("username"),
                        doc.getInteger("eventId"),
                        doc.getString("eventName"),
                        doc.getInteger("seatsBooked"),
                        doc.getDouble("totalPrice"),
                        parseTimestamp(doc)
                    );
                    if (doc.getBoolean("cancelled", false)) booking.cancel();
                    if (doc.containsKey("paymentMethod")) booking.setPaymentMethod(doc.getString("paymentMethod"));
                    if (doc.containsKey("transactionId")) booking.setTransactionId(doc.getString("transactionId"));
                    bookings.add(booking);
                } catch (Exception e) {
                    System.err.println("[DB ERROR] Error parsing booking: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error fetching bookings: " + e.getMessage());
        }
        return bookings;
    }
    
    public List<Booking> getBookingsForEventsOnDate(LocalDate date) {
        List<Booking> bookings = new ArrayList<>();
        Bson filter = Filters.and(
            Filters.eq("eventDate", date.toString()),
            Filters.eq("cancelled", false)
        );
        for (Document doc : bookingsCollection.find(filter)) {
            bookings.add(documentToBooking(doc));
        }
        return bookings;
    }

    public boolean updateEvent(Event event) {
        try {
            eventsCollection.replaceOne(Filters.eq("id", event.getId()), eventToDocument(event));
            return true;
        } catch (Exception e) { 
            return false; 
        }
    }

    public boolean deleteEvent(int eventId) {
        try {
            eventsCollection.deleteOne(Filters.eq("id", eventId));
            seatMapsCollection.deleteOne(Filters.eq("eventId", eventId));
            return true;
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error deleting event: " + e.getMessage());
            return false;
        }
    }

    public Event getEventById(int eventId) {
        Document doc = eventsCollection.find(Filters.eq("id", eventId)).first();
        return doc != null ? documentToEvent(doc) : null;
    }

    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        try (MongoCursor<Document> cursor = eventsCollection.find().iterator()) {
            while (cursor.hasNext()) events.add(documentToEvent(cursor.next())); 
        }
        return events;
    }

    public List<Event> searchEvents(String searchText, String category, Double minPrice, Double maxPrice) {
        List<Bson> filters = new ArrayList<>();
        if (searchText != null && !searchText.trim().isEmpty()) {
            Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
            filters.add(Filters.or(Filters.regex("name", pattern), Filters.regex("venue", pattern)));
        }
        if (category != null && !category.equals("All")) {
            filters.add(Filters.eq("type", category));
        }
        if (minPrice != null && maxPrice != null && maxPrice > minPrice) {
            filters.add(Filters.and(Filters.gte("price", minPrice), Filters.lte("price", maxPrice)));
        }
        Bson finalFilter = filters.isEmpty() ? new Document() : Filters.and(filters);
        List<Event> events = new ArrayList<>();
        try (MongoCursor<Document> cursor = eventsCollection.find(finalFilter).iterator()) {
            while (cursor.hasNext()) events.add(documentToEvent(cursor.next())); 
        }
        return events;
    }

    public List<String> getUniqueEventTypes() {
        return eventsCollection.distinct("type", String.class).into(new ArrayList<>());
    }
    
    public int getNextEventId() {
        Document doc = eventsCollection.find().sort(new Document("id", -1)).limit(1).first();
        return doc != null ? doc.getInteger("id") + 1 : 1;
    }

    // ==========================================================
    // == BOOKING OPERATIONS WITH REFUND SUPPORT ==
    // ==========================================================

    public Booking getBookingById(int bookingId) {
        try {
            Document result = bookingsCollection.find(new Document("id", bookingId)).first();
            if (result != null) {
                Booking booking = new Booking(
                    result.getInteger("id"),
                    result.getString("username"),
                    result.getInteger("eventId"),
                    result.getString("eventName"),
                    result.getInteger("seatsBooked"),
                    result.getDouble("totalPrice"),
                    parseTimestamp(result)
                );
                if (result.containsKey("paymentMethod")) booking.setPaymentMethod(result.getString("paymentMethod"));
                if (result.containsKey("transactionId")) booking.setTransactionId(result.getString("transactionId"));
                return booking;
            }
            return null;
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error getting booking by ID: " + e.getMessage());
            return null;
        }
    }

    public SeatMap getSeatMapForEvent(int eventId) {
        Document doc = seatMapsCollection.find(Filters.eq("eventId", eventId)).first();
        if (doc == null) {
            createDefaultSeatMap(eventId);
            doc = seatMapsCollection.find(Filters.eq("eventId", eventId)).first();
        }
        List<String> rows = doc.getList("rows", String.class);
        int seatsPerRow = doc.getInteger("seatsPerRow");
        SeatMap seatMap = new SeatMap(eventId, rows, seatsPerRow);
        List<Document> seats = doc.getList("seats", Document.class);
        for (Document seatDoc : seats) {
            Seat seat = seatMap.getSeat(seatDoc.getString("seatId"));
            if (seat != null && "BOOKED".equals(seatDoc.getString("status"))) {
                seat.book(seatDoc.getString("bookedBy"));
            }
        }
        return seatMap;
    }

    public Booking createBookingWithSeats(String username, int eventId, List<String> seatIds, 
                                         String paymentMethod, String transactionId) {
        Event event = getEventById(eventId);
        if (event == null || seatIds.isEmpty()) return null;
        if (!bookSeatsInMap(eventId, username, seatIds)) return null;
        event.allocateSeats(seatIds.size());
        updateEvent(event);
        int bookingId = getNextBookingId();
        double totalPrice = seatIds.size() * event.getPrice();
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking(bookingId, username, eventId, event.getName(), seatIds.size(), totalPrice, now);
        Document bookingDoc = bookingToDocument(booking);
        bookingDoc.append("seatIds", seatIds)
                  .append("paymentMethod", paymentMethod)
                  .append("transactionId", transactionId)
                  .append("paymentStatus", "Completed")
                  .append("eventDate", event.getDate().toString());
        bookingsCollection.insertOne(bookingDoc);
        usersCollection.updateOne(Filters.eq("username", username), 
            Updates.combine(
                Updates.inc("ticketsBought", seatIds.size()),
                Updates.inc("monthlyTickets", seatIds.size()),
                Updates.inc("eventsAttended", 1),
                Updates.inc("monthlyEvents", 1),
                Updates.inc("monthlySpent", totalPrice)
            ));
        return booking;
    }
    
    public Booking createSimpleBooking(String username, int eventId, int seatsCount, 
                                      String paymentMethod, String transactionId) {
        System.out.println("[DB] createSimpleBooking called for user: " + username);
        Event event = getEventById(eventId);
        if (event == null) {
            System.err.println("[DB ERROR] Event not found with ID: " + eventId);
            return null;
        }
        if (event.getSeatsAvailable() < seatsCount) {
            System.err.println("[DB ERROR] Not enough seats available");
            return null;
        }
        if (!event.allocateSeats(seatsCount) || !updateEvent(event)) {
            System.err.println("[DB ERROR] Failed to allocate/update event");
            event.releaseSeats(seatsCount);
            return null;
        }
        int bookingId = getNextBookingId();
        double totalPrice = seatsCount * event.getPrice();
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking(bookingId, username, eventId, event.getName(), seatsCount, totalPrice, now);
        Document bookingDoc = bookingToDocument(booking);
        bookingDoc.append("paymentMethod", paymentMethod)
                  .append("transactionId", transactionId)
                  .append("paymentStatus", "Completed")
                  .append("eventDate", event.getDate().toString());
        try {
            bookingsCollection.insertOne(bookingDoc);
            usersCollection.updateOne(
                Filters.eq("username", username),
                Updates.combine(
                    Updates.inc("ticketsBought", seatsCount),
                    Updates.inc("monthlyTickets", seatsCount),
                    Updates.inc("eventsAttended", 1),
                    Updates.inc("monthlyEvents", 1),
                    Updates.inc("monthlySpent", totalPrice)
                )
            );
            System.out.println("[DB SUCCESS] Simple booking created - ID: " + bookingId);
            return booking;
        } catch (Exception e) {
            System.err.println("[DB ERROR] Failed to save booking: " + e.getMessage());
            event.releaseSeats(seatsCount);
            updateEvent(event);
            return null;
        }
    }
    
    public List<Booking> getUserBookings(String username) {
        List<Booking> bookings = new ArrayList<>();
        try (MongoCursor<Document> cursor = bookingsCollection.find(Filters.eq("username", username)).iterator()) {
            while (cursor.hasNext()) bookings.add(documentToBooking(cursor.next())); 
        }
        return bookings;
    }

    public boolean cancelBookingForEventCancellation(int bookingId, double refundAmount, String reason) {
        try {
            Document bookingDoc = bookingsCollection.find(Filters.eq("id", bookingId)).first();
            if (bookingDoc == null || bookingDoc.getBoolean("cancelled", false)) {
                return false;
            }
            
            String username = bookingDoc.getString("username");
            int eventId = bookingDoc.getInteger("eventId");
            int seats = bookingDoc.getInteger("seatsBooked");
            
            bookingsCollection.updateOne(
                Filters.eq("id", bookingId),
                Updates.combine(
                    Updates.set("cancelled", true),
                    Updates.set("status", "Cancelled - Event Cancelled by Organizer"),
                    Updates.set("paymentStatus", "Full Refund Processed"),
                    Updates.set("refundAmount", refundAmount),
                    Updates.set("refundPercentage", 100.0),
                    Updates.set("refundReason", reason),
                    Updates.set("refundDate", LocalDateTime.now().toString()),
                    Updates.set("cancellationType", "Admin Event Cancellation")
                )
            );
            
            List<String> seatIds = bookingDoc.getList("seatIds", String.class);
            if (seatIds != null && !seatIds.isEmpty()) {
                releaseSeatsInMap(eventId, seatIds);
            }
            
            Event event = getEventById(eventId);
            if (event != null) {
                event.releaseSeats(seats);
                updateEvent(event);
            }
            
            usersCollection.updateOne(
                Filters.eq("username", username),
                Updates.combine(
                    Updates.inc("ticketsBought", -seats),
                    Updates.inc("monthlyTickets", -seats)
                )
            );
            
            return true;
        
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error cancelling booking for event cancellation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancelBooking(int bookingId) {
        Document bookingDoc = bookingsCollection.find(Filters.eq("id", bookingId)).first();
        if (bookingDoc == null || bookingDoc.getBoolean("cancelled", false)) return false;
        try {
            String username = bookingDoc.getString("username");
            int eventId = bookingDoc.getInteger("eventId");
            int seats = bookingDoc.getInteger("seatsBooked");
            double totalPrice = bookingDoc.getDouble("totalPrice");
            String eventDateStr = bookingDoc.getString("eventDate");
            
            System.out.println("\n--- USER BOOKING CANCELLATION PROCESS ---");
            System.out.println("Booking ID: " + bookingId + " | User: " + username);
            System.out.println("----------------------------------------------------------------");
            
            RefundService refundService = RefundService.getInstance();
            RefundService.RefundDetails refund = null;
            if (eventDateStr != null) {
                LocalDate eventDate = LocalDate.parse(eventDateStr);
                refund = refundService.calculateRefund(totalPrice, eventDate);
            } else {
                refund = new RefundService.RefundDetails(totalPrice, 0.0, 0.0, -1, "No refund - Date not found");
            }
            
            System.out.println("Refund: $" + String.format("%.2f", refund.getRefundAmount()) + 
                " (" + String.format("%.0f%%", refund.getRefundPercentage()) + ")");
            System.out.println("----------------------------------------------------------------");
            
            bookingsCollection.updateOne(
                Filters.eq("id", bookingId),
                Updates.combine(
                    Updates.set("cancelled", true),
                    Updates.set("status", "Cancelled"),
                    Updates.set("paymentStatus", refund.getRefundAmount() > 0 ? "Refunded" : "No Refund"),
                    Updates.set("refundAmount", refund.getRefundAmount()),
                    Updates.set("refundPercentage", refund.getRefundPercentage()),
                    Updates.set("refundDate", LocalDateTime.now().toString())
                )
            );
            
            if (refund.getRefundAmount() > 0) {
                String paymentMethod = bookingDoc.getString("paymentMethod");
                refundService.processRefund(username, bookingId, refund.getRefundAmount(), 
                    paymentMethod != null ? paymentMethod : "Original Payment Method");
            }
            
            List<String> seatIds = bookingDoc.getList("seatIds", String.class);
            if (seatIds != null && !seatIds.isEmpty()) releaseSeatsInMap(eventId, seatIds);
            
            Event event = getEventById(eventId);
            if (event != null) {
                event.releaseSeats(seats);
                updateEvent(event);
            }
            
            usersCollection.updateOne(
                Filters.eq("username", username),
                Updates.combine(Updates.inc("ticketsBought", -seats), Updates.inc("monthlyTickets", -seats))
            );
            
            EmailService emailService = EmailService.getInstance();
            Document userDoc = getUserDetails(username);
            if (userDoc != null) {
                String email = userDoc.getString("email");
                if (email != null && !email.isEmpty()) {
                    emailService.sendBookingCancellationConfirmation(
                        email, username, bookingDoc.getString("eventName"), seats, 
                        refund.getRefundAmount(), refund.getDaysUntilEvent()
                    );
                    System.out.println("Success: Email sent to: " + email);
                }
            }
            
            addUserNotification(username,
                String.format("Success: Booking cancelled. Refund of $%.2f (%.0f%%) will be processed.",
                    refund.getRefundAmount(), refund.getRefundPercentage()));
            
            System.out.println("Success: Refund processed successfully");
            System.out.println("----------------------------------------------------------------\n");
            return true;
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error cancelling booking: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean partiallyCancelBooking(int bookingId, int seatsToCancel) {
        Document bookingDoc = bookingsCollection.find(Filters.eq("id", bookingId)).first();
        if (bookingDoc == null || bookingDoc.getBoolean("cancelled", false) || seatsToCancel <= 0) {
            return false;
        }

        int currentSeats = bookingDoc.getInteger("seatsBooked");
        if (seatsToCancel > currentSeats) {
            return false;
        }
        
        if (seatsToCancel == currentSeats) {
            return cancelBooking(bookingId);
        }

        double currentPrice = bookingDoc.getDouble("totalPrice");
        String username = bookingDoc.getString("username");
        int eventId = bookingDoc.getInteger("eventId");

        try {
            double pricePerTicket = currentPrice / currentSeats;
            double originalValueToCancel = seatsToCancel * pricePerTicket;

            Event event = getEventById(eventId);
            if (event == null) return false;

            RefundService.RefundDetails refundDetails = RefundService.getInstance().calculateRefund(originalValueToCancel, event.getDate());
            double finalRefundAmount = refundDetails.getRefundAmount();

            bookingsCollection.updateOne(
                Filters.eq("id", bookingId),
                Updates.combine(
                    Updates.inc("seatsBooked", -seatsToCancel),
                    Updates.inc("totalPrice", -originalValueToCancel)
                )
            );

            if (finalRefundAmount > 0) {
                String paymentMethod = bookingDoc.getString("paymentMethod");
                RefundService.getInstance().processRefund(username, bookingId, finalRefundAmount, paymentMethod != null ? paymentMethod : "Original Method");
            }
            
            event.releaseSeats(seatsToCancel);
            updateEvent(event);

            usersCollection.updateOne(
                Filters.eq("username", username),
                Updates.combine(
                    Updates.inc("ticketsBought", -seatsToCancel),
                    Updates.inc("monthlyTickets", -seatsToCancel)
                )
            );
            
            EmailService emailService = EmailService.getInstance();
            Document userDoc = getUserDetails(username);
            String email = userDoc.getString("email");
            if (email != null && !email.isEmpty()){
                 emailService.sendBookingCancellationConfirmation(
                     email, username, bookingDoc.getString("eventName"), seatsToCancel,
                     finalRefundAmount, refundDetails.getDaysUntilEvent()
                 );
            }
            
            addUserNotification(username,
                String.format("Success: Partially cancelled booking. %d seats released. Refund of $%.2f processed.",
                    seatsToCancel, finalRefundAmount));

            return true;

        } catch (Exception e) {
            System.err.println("[DB ERROR] Error during partial cancellation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private boolean bookSeatsInMap(int eventId, String username, List<String> seatIds) {
        Bson update = Updates.combine(seatIds.stream()
            .map(seatId -> Updates.set("seats.$[elem].status", "BOOKED"))
            .toList());
        com.mongodb.client.model.UpdateOptions options = new com.mongodb.client.model.UpdateOptions()
            .arrayFilters(Arrays.asList(Filters.in("elem.seatId", seatIds)));
        return seatMapsCollection.updateOne(Filters.eq("eventId", eventId), update, options).getModifiedCount() > 0;
    }
    
    private void releaseSeatsInMap(int eventId, List<String> seatIds) {
        Bson update = Updates.combine(
            Updates.set("seats.$[elem].status", "AVAILABLE"),
            Updates.set("seats.$[elem].bookedBy", null)
        );
        com.mongodb.client.model.UpdateOptions options = new com.mongodb.client.model.UpdateOptions()
            .arrayFilters(Arrays.asList(Filters.in("elem.seatId", seatIds)));
        seatMapsCollection.updateOne(Filters.eq("eventId", eventId), update, options);
    }
    
    private void createDefaultSeatMap(int eventId) {
        List<String> rows = Arrays.asList("A", "B", "C", "D", "E");
        int seatsPerRow = 10;
        List<Document> seats = new ArrayList<>();
        for (String row : rows) {
            for (int i = 1; i <= seatsPerRow; i++) {
                seats.add(new Document("seatId", row + i).append("status", "AVAILABLE").append("bookedBy", null));
            }
        }
        seatMapsCollection.insertOne(new Document("eventId", eventId)
            .append("rows", rows).append("seatsPerRow", seatsPerRow).append("seats", seats));
    }

    private int getNextBookingId() {
        Document doc = bookingsCollection.find().sort(new Document("id", -1)).limit(1).first();
        return doc != null ? doc.getInteger("id") + 1 : 1;
    }

    public List<Booking> getAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        try (MongoCursor<Document> cursor = bookingsCollection.find().iterator()) {
            while (cursor.hasNext()) bookings.add(documentToBooking(cursor.next()));
        }
        return bookings;
    }

    public List<String> getBookedSeatsForUser(int eventId, String username) {
        List<String> bookedSeats = new ArrayList<>();
        try (MongoCursor<Document> cursor = bookingsCollection.find(
            Filters.and(Filters.eq("eventId", eventId), Filters.eq("username", username), Filters.eq("cancelled", false))
        ).iterator()) {
            while (cursor.hasNext()) {
                List<String> seatIds = cursor.next().getList("seatIds", String.class);
                if (seatIds != null) bookedSeats.addAll(seatIds);
            }
        }
        return bookedSeats;
    }

    // ==========================================================
    // == ANALYTICS & REPORTING ==
    // ==========================================================
    
    /**
     * NEW: Gets the top attendees based on bookings made today.
     * This method is required for the "Award Daily Credits" feature.
     */
    public List<Document> getDailyTopAttendees() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return bookingsCollection.aggregate(Arrays.asList(
            new Document("$match", new Document("cancelled", new Document("$ne", true))
                .append("timestamp", new Document("$gte", startOfDay.toString())
                .append("$lte", endOfDay.toString()))),
            new Document("$group", new Document("_id", "$username")
                .append("dailyTickets", new Document("$sum", "$seatsBooked"))
                .append("dailySpent", new Document("$sum", "$totalPrice"))),
            new Document("$project", new Document("username", "$_id")
                .append("dailyTickets", 1)
                .append("dailySpent", 1)
                .append("_id", 0)),
            new Document("$sort", new Document("dailyTickets", -1))
        )).into(new ArrayList<>());
    }

    public List<Document> getTopEventsByRevenue(int limit) {
        try {
            return bookingsCollection.aggregate(Arrays.asList(
                new Document("$match", new Document("cancelled", new Document("$ne", true))),
                new Document("$group", new Document("_id", "$eventName")
                    .append("revenue", new Document("$sum", "$totalPrice"))
                    .append("ticketsSold", new Document("$sum", "$seatsBooked"))),
                new Document("$project", new Document("eventName", "$_id")
                    .append("revenue", 1).append("ticketsSold", 1).append("_id", 0)),
                new Document("$sort", new Document("revenue", -1)),
                new Document("$limit", limit)
            )).into(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error getting top events: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Document> getRevenueByCategory() {
        try {
            return bookingsCollection.aggregate(Arrays.asList(
                new Document("$match", new Document("cancelled", new Document("$ne", true))),
                new Document("$lookup", new Document("from", "events")
                    .append("localField", "eventId")
                    .append("foreignField", "id")
                    .append("as", "eventDetails")),
                new Document("$unwind", "$eventDetails"),
                new Document("$group", new Document("_id", "$eventDetails.type")
                    .append("totalRevenue", new Document("$sum", "$totalPrice"))),
                new Document("$project", new Document("category", "$_id")
                    .append("revenue", "$totalRevenue")
                    .append("_id", 0)),
                new Document("$sort", new Document("revenue", -1))
            )).into(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error getting revenue by category: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Document> getMonthlyTopAttendees(int limit) {
        try {
            LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);
            return bookingsCollection.aggregate(Arrays.asList(
                new Document("$match", new Document("cancelled", new Document("$ne", true))
                    .append("timestamp", new Document("$gte", monthStart.toString()))),
                new Document("$group", new Document("_id", "$username")
                    .append("monthlyTickets", new Document("$sum", "$seatsBooked"))
                    .append("monthlyEvents", new Document("$sum", 1))
                    .append("monthlySpent", new Document("$sum", "$totalPrice"))),
                new Document("$project", new Document("username", "$_id")
                    .append("monthlyTickets", 1).append("monthlyEvents", 1)
                    .append("monthlySpent", 1).append("_id", 0)),
                new Document("$sort", new Document("monthlyTickets", -1)),
                new Document("$limit", limit)
            )).into(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error getting monthly attendees: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Document> getTopAttendees(int limit) {
        try {
            List<Document> attendees = usersCollection.find(Filters.eq("role", "Attendee"))
                .sort(Sorts.descending("ticketsBought")).limit(limit).into(new ArrayList<>());
            for (Document attendee : attendees) {
                attendee.append("totalSpent", calculateTotalSpent(attendee.getString("username")));
            }
            return attendees;
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error getting top attendees: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private double calculateTotalSpent(String username) {
        try {
            List<Document> results = bookingsCollection.aggregate(Arrays.asList(
                new Document("$match", new Document("username", username)
                    .append("cancelled", new Document("$ne", true))),
                new Document("$group", new Document("_id", null)
                    .append("total", new Document("$sum", "$totalPrice")))
            )).into(new ArrayList<>());
            return !results.isEmpty() && results.get(0).containsKey("total") ? 
                results.get(0).getDouble("total") : 0.0;
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error calculating total spent: " + e.getMessage());
            return 0.0;
        }
    }

    // ==========================================================
    // == NOTIFICATION SYSTEM ==
    // ==========================================================

    public void addAdminNotification(String username, String message) {
        try {
            notificationsCollection.insertOne(new Document("username", username)
                .append("message", message)
                .append("timestamp", LocalDateTime.now().toString()) // Store as String for consistency
                .append("read", false)
                .append("type", "admin"));
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error adding admin notification: " + e.getMessage());
        }
    }

    public List<Document> getAdminNotifications(String username) {
        try {
            return notificationsCollection.find(Filters.and(Filters.eq("username", username), Filters.eq("type", "admin")))
                .sort(Sorts.descending("timestamp")).limit(50).into(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error getting admin notifications: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public int getUnreadNotificationCount(String username) {
        try {
            return (int) notificationsCollection.countDocuments(
                Filters.and(Filters.eq("username", username), Filters.eq("read", false)));
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error getting unread count: " + e.getMessage());
            return 0;
        }
    }

    public void markNotificationAsRead(String username, String timestamp) {
        try {
            notificationsCollection.updateOne(
                Filters.and(Filters.eq("username", username), Filters.eq("timestamp", timestamp)),
                Updates.set("read", true));
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error marking notification as read: " + e.getMessage());
        }
    }

    public void markAllNotificationsAsRead(String username) {
        try {
            notificationsCollection.updateMany(Filters.eq("username", username), Updates.set("read", true));
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error marking all as read: " + e.getMessage());
        }
    }

    public void deleteNotification(String username, String timestamp) {
        try {
            notificationsCollection.deleteOne(
                Filters.and(Filters.eq("username", username), Filters.eq("timestamp", timestamp)));
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error deleting notification: " + e.getMessage());
        }
    }

    public void addUserNotification(String username, String message) {
        try {
            notificationsCollection.insertOne(new Document()
                .append("username", username)
                .append("message", message)
                .append("timestamp", LocalDateTime.now().toString())
                .append("read", false)
                .append("type", "user"));
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error adding user notification: " + e.getMessage());
        }
    }

    public List<Document> getUserNotifications(String username) {
        try {
            return notificationsCollection.find(Filters.and(Filters.eq("username", username), Filters.eq("type", "user")))
                .sort(new Document("timestamp", -1)).into(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error getting user notifications: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==========================================================
    // == HELPER & CONVERSION METHODS ==
    // ==========================================================
    
    private LocalDateTime parseTimestamp(Document doc) {
        if (doc.containsKey("timestamp")) {
            Object timestampObj = doc.get("timestamp");
            if (timestampObj instanceof Date) {
                return ((Date) timestampObj).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            } else if (timestampObj instanceof String) {
                return LocalDateTime.parse((String) timestampObj);
            }
        }
        return LocalDateTime.now();
    }

    private Event documentToEvent(Document doc) {
        Event event = new Event(
            doc.getInteger("id"), 
            doc.getString("name"), 
            doc.getString("type"), 
            LocalDate.parse(doc.getString("date")), 
            doc.getString("venue"), 
            doc.getInteger("totalSeats"), 
            doc.getDouble("price")
        );
        
        if (doc.containsKey("seatsAvailable")) {
            event.setSeatsAvailable(doc.getInteger("seatsAvailable"));
        }
        if (doc.containsKey("status")) {
            event.setStatus(doc.getString("status"));
        }
        if (doc.containsKey("cancellationReason")) {
            event.setCancellationReason(doc.getString("cancellationReason"));
        }
        if (doc.containsKey("originalDate")) {
            event.setOriginalDate(LocalDate.parse(doc.getString("originalDate")));
        }
        
        return event;
    }
    
    private Document eventToDocument(Event event) {
        Document doc = new Document("id", event.getId())
            .append("name", event.getName())
            .append("type", event.getType())
            .append("date", event.getDate().toString())
            .append("venue", event.getVenue())
            .append("totalSeats", event.getTotalSeats())
            .append("seatsAvailable", event.getSeatsAvailable())
            .append("price", event.getPrice())
            .append("status", event.getStatus());
        
        if (event.getCancellationReason() != null) {
            doc.append("cancellationReason", event.getCancellationReason());
        }
        if (event.getOriginalDate() != null) {
            doc.append("originalDate", event.getOriginalDate().toString());
        }
        
        return doc;
    }

    private Booking documentToBooking(Document doc) {
        Booking booking = new Booking(
            doc.getInteger("id"), 
            doc.getString("username"), 
            doc.getInteger("eventId"), 
            doc.getString("eventName"), 
            doc.getInteger("seatsBooked"), 
            doc.getDouble("totalPrice"), 
            parseTimestamp(doc)
        );
        
        if (doc.getBoolean("cancelled", false)) {
            booking.cancel();
        }
        if (doc.containsKey("paymentMethod")) {
            booking.setPaymentMethod(doc.getString("paymentMethod"));
        }
        if (doc.containsKey("transactionId")) {
            booking.setTransactionId(doc.getString("transactionId"));
        }
        
        return booking;
    }
    
    private Document bookingToDocument(Booking booking) {
        return new Document("id", booking.getId())
            .append("username", booking.getUsername())
            .append("eventId", booking.getEventId())
            .append("eventName", booking.getEventName())
            .append("seatsBooked", booking.getSeatsBooked())
            .append("totalPrice", booking.getTotalPrice())
            .append("timestamp", booking.getTimestamp().toString())
            .append("cancelled", booking.isCancelled())
            .append("status", booking.getStatus());
    }

    // ==========================================================
    // == CREDIT POINTS MANAGEMENT ==
    // ==========================================================

    public boolean addCreditPoints(String username, int points) {
        try {
            usersCollection.updateOne(new Document("username", username), 
                new Document("$inc", new Document("creditPoints", points)));
            System.out.println("[DB] Added " + points + " credit points to " + username);
            return true;
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error adding credit points: " + e.getMessage());
            return false;
        }
    }

    public boolean deductCreditPoints(String username, int points) {
        try {
            Document userDoc = usersCollection.find(new Document("username", username)).first();
            if (userDoc == null) return false;
            
            int currentPoints = userDoc.getInteger("creditPoints", 0);
            if (currentPoints < points) return false;
            
            usersCollection.updateOne(new Document("username", username), 
                new Document("$inc", new Document("creditPoints", -points)));
            System.out.println("[DB] Deducted " + points + " credit points from " + username);
            return true;
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error deducting credit points: " + e.getMessage());
            return false;
        }
    }

    public int getUserCreditPoints(String username) {
        try {
            Document userDoc = usersCollection.find(new Document("username", username)).first();
            return userDoc != null ? userDoc.getInteger("creditPoints", 0) : 0;
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error getting credit points: " + e.getMessage());
            return 0;
        }
    }

    public void resetMonthlyStats() {
        try {
            usersCollection.updateMany(new Document(), 
                new Document("$set", new Document()
                    .append("monthlyTickets", 0)
                    .append("monthlyEvents", 0)
                    .append("monthlySpent", 0.0)));
            System.out.println("[DB] Monthly statistics reset for all users");
        } catch (Exception e) {
            System.err.println("[DB ERROR] Error resetting monthly stats: " + e.getMessage());
        }
    }
}
package com.eventbooking.ui;

import com.eventbooking.database.DatabaseManager;
import com.eventbooking.models.Event;
import com.eventbooking.models.Booking;
import com.eventbooking.services.EmailService;
import com.eventbooking.services.RefundService;
import org.bson.Document;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class EventFormDialog extends JDialog {
    private JTextField nameField, typeField, venueField, seatsField, priceField;
    private JSpinner dateSpinner;
    private DatabaseManager dbManager;
    private EmailService emailService;
    private RefundService refundService;
    private Event editingEvent;
    private AdminDashboard parentDashboard;

    public EventFormDialog(AdminDashboard parent, Event event, DatabaseManager dbManager) {
        super(parent, event == null ? "Add New Event" : "Edit Event", true);
        this.parentDashboard = parent;
        this.editingEvent = event;
        this.dbManager = dbManager;
        this.emailService = EmailService.getInstance();
        this.refundService = RefundService.getInstance();
        initializeUI();
    }

    private void initializeUI() {
        setSize(450, 550);
        setLocationRelativeTo(getParent());
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel(editingEvent == null ? "Add New Event" : "Edit Event");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);
        int row = 0;
        
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(createLabel("Event Name:"), gbc);
        gbc.gridx = 1; nameField = createTextField(); formPanel.add(nameField, gbc); row++;
        
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(createLabel("Event Type:"), gbc);
        gbc.gridx = 1; typeField = createTextField(); formPanel.add(typeField, gbc); row++;
        
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(createLabel("Venue:"), gbc);
        gbc.gridx = 1; venueField = createTextField(); formPanel.add(venueField, gbc); row++;
        
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(createLabel("Date:"), gbc);
        gbc.gridx = 1;
        SpinnerDateModel dateModel = new SpinnerDateModel();
        dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        formPanel.add(dateSpinner, gbc); row++;
        
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(createLabel("Total Seats:"), gbc);
        gbc.gridx = 1; seatsField = createTextField(); formPanel.add(seatsField, gbc); row++;
        
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(createLabel("Price ($):"), gbc);
        gbc.gridx = 1; priceField = createTextField(); formPanel.add(priceField, gbc);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        boolean isCompleted = false;
        
        if (editingEvent != null) {
            nameField.setText(editingEvent.getName());
            typeField.setText(editingEvent.getType());
            venueField.setText(editingEvent.getVenue());
            seatsField.setText(String.valueOf(editingEvent.getTotalSeats()));
            priceField.setText(String.valueOf(editingEvent.getPrice()));
            dateSpinner.setValue(java.sql.Date.valueOf(editingEvent.getDate()));
            
            editingEvent.updateStatus();
            isCompleted = editingEvent.isCompleted();
            
            if (isCompleted) {
                nameField.setEnabled(false); typeField.setEnabled(false);
                venueField.setEnabled(false); seatsField.setEnabled(false);
                priceField.setEnabled(false); dateSpinner.setEnabled(false);
                
                JLabel warningLabel = new JLabel("⚠️ This event is completed and cannot be modified");
                warningLabel.setHorizontalAlignment(JLabel.CENTER);
                mainPanel.add(warningLabel, BorderLayout.AFTER_LAST_LINE);
            }
        }
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton saveBtn = createStyledButton("Save", new Color(46, 204, 113));
        JButton deleteBtn = createStyledButton("Delete", new Color(231, 76, 60));
        JButton cancelBtn = createStyledButton("Cancel", new Color(149, 165, 166));
        
        saveBtn.addActionListener(e -> saveEvent());
        deleteBtn.addActionListener(e -> deleteEvent());
        cancelBtn.addActionListener(e -> dispose());
        
        if (isCompleted) {
            saveBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
        }
        
        if (editingEvent != null) {
            buttonPanel.add(saveBtn);
            buttonPanel.add(deleteBtn);
        } else {
            buttonPanel.add(saveBtn);
        }
        buttonPanel.add(cancelBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private void saveEvent() {
        try {
            String name = nameField.getText().trim();
            String type = typeField.getText().trim();
            String venue = venueField.getText().trim();
            int seats = Integer.parseInt(seatsField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            LocalDate newDate = new java.sql.Date(((Date) dateSpinner.getValue()).getTime()).toLocalDate();
            
            if (newDate.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "Cannot create/update events with past dates!", "Invalid Date", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (name.isEmpty() || type.isEmpty() || venue.isEmpty() || seats <= 0 || price < 0) {
                JOptionPane.showMessageDialog(this, "Please fill all fields with valid data.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (editingEvent != null) {
                LocalDate oldDate = editingEvent.getDate();
                boolean dateChanged = !oldDate.equals(newDate);
                
                editingEvent.setName(name); editingEvent.setType(type); editingEvent.setVenue(venue);
                editingEvent.setDate(newDate); editingEvent.setTotalSeats(seats); editingEvent.setPrice(price);
                dbManager.updateEvent(editingEvent);
                
                if (dateChanged) {
                    sendPostponementEmails(editingEvent.getId(), name, oldDate, newDate);
                }
                JOptionPane.showMessageDialog(this, "Event updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int id = dbManager.getNextEventId();
                Event newEvent = new Event(id, name, type, newDate, venue, seats, price);
                dbManager.addEvent(newEvent);
                JOptionPane.showMessageDialog(this, "Event added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            parentDashboard.refreshTable();
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for seats and price.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteEvent() {
        editingEvent.updateStatus();
        if (editingEvent.isCompleted()) {
            JOptionPane.showMessageDialog(this, "Cannot delete completed events.", "Action Not Allowed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        List<Booking> bookings = dbManager.getBookingsForEvent(editingEvent.getId());
        long activeBookingsCount = bookings.stream().filter(b -> !b.isCancelled()).count();
        
        String confirmMessage = "Are you sure you want to delete this event?\n" +
            "Event: " + editingEvent.getName() + "\n" +
            "Active Bookings: " + activeBookingsCount + "\n\n";
        
        if (activeBookingsCount > 0) {
            confirmMessage += "⚠️ All " + activeBookingsCount + " attendees will be refunded 100% and notified via email.\n";
        }
        confirmMessage += "This action cannot be undone!";
        
        int confirm = JOptionPane.showConfirmDialog(this, confirmMessage, "Confirm Delete Event", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            System.out.println("--- ADMIN EVENT DELETION & REFUND PROCESS ---");
            
            // Process refunds and send emails for all active bookings
            for (Booking booking : bookings) {
                if (!booking.isCancelled()) {
                    double refundAmount = booking.getTotalPrice();
                    String reason = "Event has been cancelled by the organizer";
                    
                    if (dbManager.cancelBookingForEventCancellation(booking.getId(), refundAmount, reason)) {
                        Document userDoc = dbManager.getUserDetails(booking.getUsername());
                        String email = userDoc != null ? userDoc.getString("email") : null;
                        
                        if (email != null && !email.isEmpty()) {
                            emailService.sendEventCancellationNotification(email, booking.getUsername(), editingEvent.getName(), refundAmount, reason);
                            System.out.println("✓ Email sent to: " + email);
                        }
                        dbManager.addUserNotification(booking.getUsername(),
                            String.format("❌ Event '%s' has been cancelled. Full refund of $%.2f processed.", editingEvent.getName(), refundAmount));
                    }
                }
            }
            
            // Finally, delete the event from the database
            dbManager.deleteEvent(editingEvent.getId());
            System.out.println("✓ Event deleted successfully.");
            
            JOptionPane.showMessageDialog(this, "Event deleted. All attendees have been notified and refunded.", "Success", JOptionPane.INFORMATION_MESSAGE);
            parentDashboard.refreshTable();
            dispose();
        }
    }

    private void sendPostponementEmails(int eventId, String eventName, LocalDate oldDate, LocalDate newDate) {
        System.out.println("--- EVENT POSTPONEMENT - SENDING NOTIFICATIONS ---");
        List<Booking> bookings = dbManager.getBookingsForEvent(eventId);
        for (Booking booking : bookings) {
            if (!booking.isCancelled()) {
                Document userDoc = dbManager.getUserDetails(booking.getUsername());
                String email = userDoc != null ? userDoc.getString("email") : null;
                if (email != null && !email.isEmpty()) {
                    emailService.sendEventPostponementNotification(email, booking.getUsername(), eventName, oldDate.toString(), newDate.toString(), "Schedule change by organizer");
                    System.out.println("✓ Postponement email sent to: " + email);
                }
                dbManager.addUserNotification(booking.getUsername(),
                    String.format("📅 Event '%s' has been rescheduled from %s to %s.", eventName, oldDate, newDate));
            }
        }
    }

    // Helper methods from your file
    private JLabel createLabel(String text) { return new JLabel(text); }
    private JTextField createTextField() { return new JTextField(); }
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(130, 35));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { if (button.isEnabled()) { button.setBackground(bgColor.darker()); } }
            public void mouseExited(java.awt.event.MouseEvent evt) { button.setBackground(bgColor); }
        });
        return button;
    }
}
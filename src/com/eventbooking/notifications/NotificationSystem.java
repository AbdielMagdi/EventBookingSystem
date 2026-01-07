// REPLACE src/com/eventbooking/notifications/NotificationSystem.java with this

package com.eventbooking.notifications;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;

/**
 * Notification System - demonstrates OBSERVER PATTERN
 * Manages notifications for booking events
 */
public class NotificationSystem {
    private static NotificationSystem instance;
    private List<NotificationObserver> observers;

    private NotificationSystem() {
        observers = new ArrayList<>();
    }

    public static NotificationSystem getInstance() {
        if (instance == null) {
            instance = new NotificationSystem();
        }
        return instance;
    }

    /**
     * Register an observer
     */
    public void addObserver(NotificationObserver observer) {
        observers.add(observer);
    }

    /**
     * Remove an observer
     */
    public void removeObserver(NotificationObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all observers
     */
    public void notifyObservers(String message, NotificationType type) {
        for (NotificationObserver observer : observers) {
            observer.update(message, type);
        }
    }

    /**
     * Send booking confirmation notification
     */
    public void sendBookingConfirmation(String username, String eventName, int seats, double price) {
        String message = String.format(
            "Booking Confirmed!\n\nEvent: %s\nSeats: %d\nTotal Price: $%.2f\n\nThank you for your booking!",
            eventName, seats, price
        );
        notifyObservers(message, NotificationType.SUCCESS);
        showDialog(message, "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Send booking cancellation notification
     */
    public void sendCancellationConfirmation(String username, String eventName, int seats) {
        String message = String.format(
            "Booking Cancelled\n\nEvent: %s\nSeats Released: %d\n\nRefund will be processed as per event policy.",
            eventName, seats
        );
        notifyObservers(message, NotificationType.INFO);
        showDialog(message, "Booking Cancelled", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Send payment confirmation notification
     */
    public void sendPaymentConfirmation(String username, double amount, String paymentMethod) {
        String message = String.format(
            "Payment Successful!\n\nAmount: $%.2f\nMethod: %s\nUser: %s\n\nTransaction completed successfully.",
            amount, paymentMethod, username
        );
        notifyObservers(message, NotificationType.SUCCESS);
        System.out.println("✓ Payment notification sent to " + username);
    }

    /**
     * Send info notification
     */
    public void sendInfoNotification(String message) {
        notifyObservers(message, NotificationType.INFO);
        System.out.println("ℹ Info: " + message);
    }

    /**
     * Send error notification
     */
    public void sendError(String errorMessage) {
        notifyObservers(errorMessage, NotificationType.ERROR);
        showDialog(errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Send admin notification
     */
    public void sendAdminNotification(String message) {
        notifyObservers(message, NotificationType.ADMIN);
        System.out.println("📢 Admin: " + message);
    }

    /**
     * Show dialog notification
     */
    private void showDialog(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(null, message, title, messageType);
    }

    /**
     * Notification types
     */
    public enum NotificationType {
        SUCCESS, ERROR, INFO, ADMIN
    }
}

/**
 * Observer interface for notification pattern
 */
interface NotificationObserver {
    void update(String message, NotificationSystem.NotificationType type);
}
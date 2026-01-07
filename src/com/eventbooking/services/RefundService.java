
package com.eventbooking.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RefundService {
    private static RefundService instance;

    private RefundService() {}

    public static synchronized RefundService getInstance() {
        if (instance == null) {
            instance = new RefundService();
        }
        return instance;
    }

    /**
     * Calculate refund amount based on days until event
     * This is used for USER cancellations
     * 
     * REFUND POLICY (CORRECTED):
     * - More than 3 days before event: 100% refund
     * - Exactly 3 days before event: 50% refund
     * - Exactly 2 days before event: 25% refund
     * - 1 day before event: 0% refund
     * - Event day (0 days): 0% refund
     * - Past events: 0% refund
     * 
     * @param originalAmount The original booking amount
     * @param eventDate The event date
     * @return RefundDetails object containing refund amount and percentage
     */
    public RefundDetails calculateRefund(double originalAmount, LocalDate eventDate) {
        LocalDate today = LocalDate.now();
        long daysUntilEvent = ChronoUnit.DAYS.between(today, eventDate);
        
        double refundPercentage;
        String policyApplied;
        
        // Check if event is already past
        if (daysUntilEvent < 0) {
            // Event already happened - no refund allowed
            refundPercentage = 0.0;
            policyApplied = "Event Already Completed - No Refund";
        }
        // Event day (0 days)
        else if (daysUntilEvent == 0) {
            refundPercentage = 0.0; // 0% refund
            policyApplied = "No Refund (Event Day - Same Day Cancellation)";
        }
        // 1 day before event
        else if (daysUntilEvent == 1) {
            refundPercentage = 0.25; // 0% refund
            policyApplied = "25% refund (1 day before event)";
        }
        // Exactly 2 days before event
        else if (daysUntilEvent == 2) {
            refundPercentage = 0.50; // 25%
            policyApplied = "50% Refund (2 days before event)";
        }
        // Exactly 3 days before event
        else if (daysUntilEvent == 3) {
            refundPercentage = 0.75; // 50%
            policyApplied = "75% Refund (3 days before event)";
        }
        // More than 3 days before event
        else {
            refundPercentage = 1.0; // 100%
            policyApplied = "Full Refund (More than 3 days before event)";
        }
        
        double refundAmount = originalAmount * refundPercentage;
        
        return new RefundDetails(
            originalAmount,
            refundAmount,
            refundPercentage * 100,
            (int) daysUntilEvent,
            policyApplied
        );
    }

    /**
     * Calculate refund for event cancellation by ADMIN
     * Always returns 100% refund when admin cancels event
     * 
     * @param originalAmount The original booking amount
     * @return RefundDetails with 100% refund
     */
    public RefundDetails calculateEventCancellationRefund(double originalAmount) {
        return new RefundDetails(
            originalAmount,
            originalAmount,
            100.0,
            0,
            "Event Cancelled by Organizer - Full Refund (100%)"
        );
    }

    /**
     * Process refund simulation
     * In production, this would integrate with payment gateway
     */
    public boolean processRefund(String username, int bookingId, double refundAmount, 
                                String paymentMethod) {
        System.out.println("\n┌────────────────────────────────────────────────────────────┐");
        System.out.println("│              REFUND PROCESSED                              │");
        System.out.println("├────────────────────────────────────────────────────────────┤");
        System.out.println("│ User: " + String.format("%-51s", username) + " │");
        System.out.println("│ Booking ID: " + String.format("%-46d", bookingId) + " │");
        System.out.println("│ Refund Amount: $" + String.format("%-42.2f", refundAmount) + " │");
        System.out.println("│ Payment Method: " + String.format("%-43s", paymentMethod) + " │");
        System.out.println("│ Status: APPROVED                                             │");
        System.out.println("│ Processing Time: 5-7 business days                          │");
        System.out.println("└────────────────────────────────────────────────────────────┘");
        
        // In production, integrate with actual payment gateway
        // For now, return true to indicate successful processing
        return true;
    }

    /**
     * Check if refund is allowed for a booking
     * Used to prevent refunds for completed events
     * 
     * @param eventDate The event date
     * @return true if refund is allowed, false otherwise
     */
    public boolean isRefundAllowed(LocalDate eventDate) {
        LocalDate today = LocalDate.now();
        // Refund is allowed only if event hasn't completed yet
        return !eventDate.isBefore(today);
    }

    /**
     * Inner class to hold refund calculation details
     */
    public static class RefundDetails {
        private double originalAmount;
        private double refundAmount;
        private double refundPercentage;
        private int daysUntilEvent;
        private String policyApplied;

        public RefundDetails(double originalAmount, double refundAmount, 
                           double refundPercentage, int daysUntilEvent, 
                           String policyApplied) {
            this.originalAmount = originalAmount;
            this.refundAmount = refundAmount;
            this.refundPercentage = refundPercentage;
            this.daysUntilEvent = daysUntilEvent;
            this.policyApplied = policyApplied;
        }

        public double getOriginalAmount() { 
            return originalAmount; 
        }
        
        public double getRefundAmount() { 
            return refundAmount; 
        }
        
        public double getRefundPercentage() { 
            return refundPercentage; 
        }
        
        public int getDaysUntilEvent() { 
            return daysUntilEvent; 
        }
        
        public String getPolicyApplied() { 
            return policyApplied; 
        }

        /**
         * Get formatted summary of refund details
         */
        public String getFormattedSummary() {
            return String.format(
                "Original Amount: $%.2f\n" +
                "Refund Amount: $%.2f (%.0f%%)\n" +
                "Days Until Event: %d\n" +
                "Policy: %s",
                originalAmount, refundAmount, refundPercentage, 
                daysUntilEvent, policyApplied
            );
        }

        /**
         * Get detailed refund information for display
         */
        public String getDetailedInfo() {
            return String.format(
                "╔═══════════════════════════════════════╗\n" +
                "║      REFUND DETAILS                    ║\n" +
                "╠═══════════════════════════════════════╣\n" +
                "║ Original Amount:    $%-18.2f  ║\n" +
                "║ Refund Percentage:  %-18.0f%% ║\n" +
                "║ Refund Amount:      $%-18.2f  ║\n" +
                "║ Days Until Event:   %-18d   ║\n" +
                "╠═══════════════════════════════════════╣\n" +
                "║ Policy: %-32s ║\n" +
                "╚═══════════════════════════════════════╝",
                originalAmount, refundPercentage, refundAmount,
                daysUntilEvent, policyApplied
            );
        }
    }
}
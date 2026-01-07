package com.eventbooking.services;

import com.eventbooking.models.Event; // Added import for Event model
import javax.mail.*;
import javax.mail.internet.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {
    private static EmailService instance;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");
    
    // Toggle between real emails and simulation
    private static final boolean ENABLE_REAL_EMAILS = true; // SET TO true FOR REAL EMAILS
    
    // ========== EMAIL CONFIGURATION ==========
    // IMPORTANT: Update these with your actual SMTP credentials
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String FROM_EMAIL = "arhi.24.2007@gmail.com"; // CHANGE THIS
    private static final String EMAIL_PASSWORD = "hylookuhzsmxadgx"; // CHANGE THIS (use App Password for Gmail)
    private static final String FROM_NAME = "Event Booking System";

    private EmailService() {
        if (ENABLE_REAL_EMAILS) {
            System.out.println("========================================");
            System.out.println("[EMAIL] Real Email Mode ENABLED");
            System.out.println("[EMAIL] SMTP Host: " + SMTP_HOST);
            System.out.println("[EMAIL] From: " + FROM_EMAIL);
            System.out.println("========================================");
            
            if (FROM_EMAIL.contains("your-email") || EMAIL_PASSWORD.contains("your-app-password") || EMAIL_PASSWORD.length() < 10) {
                System.err.println("[WARNING] Email credentials not configured! Update FROM_EMAIL and EMAIL_PASSWORD in EmailService.java");
            } else {
                System.out.println("[SUCCESS] Email credentials configured!");
                System.out.println("========================================");
            }
        } else {
            System.out.println("[EMAIL] Simulation Mode - Emails will be logged to the console only.");
        }
    }

    public static synchronized EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    /**
     * Send booking confirmation email
     */
    public boolean sendBookingConfirmation(String toEmail, String username, 
                                          String eventName, int seats, 
                                          double totalPrice, String bookingId) {
        String subject = "Booking Confirmation - " + eventName;
        String body = String.format(
            "Dear %s,\n\n" +
            "Your booking has been confirmed!\n\n" +
            "BOOKING DETAILS:\n" +
            "=================\n" +
            "Booking ID: %s\n" +
            "Event: %s\n" +
            "Number of Seats: %d\n" +
            "Total Amount Paid: $%.2f\n" +
            "Booking Time: %s\n\n" +
            "Thank you for booking with us!\n\n" +
            "Best regards,\n" +
            "Event Booking Team",
            username, bookingId, eventName, seats, totalPrice,
            LocalDateTime.now().format(TIME_FORMATTER)
        );
        
        return sendEmail(toEmail, subject, body);
    }

    /**
     * Send event cancellation notification
     */
    public boolean sendEventCancellationNotification(String toEmail, String username,
                                                     String eventName, double refundAmount,
                                                     String reason) {
        String subject = "Event Cancelled - " + eventName + " - Refund Processed";
        String body = String.format(
            "Dear %s,\n\n" +
            "We regret to inform you that the following event has been CANCELLED:\n\n" +
            "EVENT: %s\n\n" +
            "REASON: %s\n\n" +
            "REFUND DETAILS:\n" +
            "=================\n" +
            "Refund Amount: $%.2f\n" +
            "Refund Status: PROCESSED\n" +
            "Refund will be credited to your original payment method within 5-7 business days.\n\n" +
            "We sincerely apologize for any inconvenience caused.\n" +
            "Please check our website for alternative events.\n\n" +
            "If you have any questions, please contact our support team.\n\n" +
            "Best regards,\n" +
            "Event Booking Team",
            username, eventName, reason, refundAmount
        );
        
        return sendEmail(toEmail, subject, body);
    }

    /**
     * Send event postponement notification
     */
    public boolean sendEventPostponementNotification(String toEmail, String username,
                                                     String eventName, String oldDate,
                                                     String newDate, String reason) {
        String subject = "Event Postponed - " + eventName + " - New Date Announced";
        String body = String.format(
            "Dear %s,\n\n" +
            "We would like to inform you that the following event has been POSTPONED:\n\n" +
            "EVENT: %s\n\n" +
            "ORIGINAL DATE: %s\n" +
            "NEW DATE: %s\n\n" +
            "REASON: %s\n\n" +
            "Your booking remains valid for the new date.\n\n" +
            "REFUND OPTION:\n" +
            "If you cannot attend on the new date, you may cancel your booking " +
            "from your dashboard and receive a full refund.\n\n" +
            "We apologize for any inconvenience and look forward to seeing you at the event!\n\n" +
            "Best regards,\n" +
            "Event Booking Team",
            username, eventName, oldDate, newDate, reason
        );
        
        return sendEmail(toEmail, subject, body);
    }

    /**
     * Send booking cancellation confirmation (user-initiated)
     */
    public boolean sendBookingCancellationConfirmation(String toEmail, String username,
                                                       String eventName, int seats,
                                                       double refundAmount, int daysUntilEvent) {
        String refundPercentage = calculateRefundPercentage(daysUntilEvent);
        
        String subject = "Booking Cancelled - " + eventName;
        String body = String.format(
            "Dear %s,\n\n" +
            "Your booking cancellation has been processed.\n\n" +
            "CANCELLATION DETAILS:\n" +
            "=================\n" +
            "Event: %s\n" +
            "Seats Cancelled: %d\n" +
            "Days Until Event: %d\n" +
            "Refund Percentage: %s\n" +
            "Refund Amount: $%.2f\n" +
            "Cancellation Time: %s\n\n" +
            "The refund will be credited to your original payment method within 5-7 business days.\n\n" +
            "We hope to see you at our future events!\n\n" +
            "Best regards,\n" +
            "Event Booking Team",
            username, eventName, seats, daysUntilEvent, refundPercentage,
            refundAmount, LocalDateTime.now().format(TIME_FORMATTER)
        );
        
        return sendEmail(toEmail, subject, body);
    }

    /**
     * Send monthly credit points notification
     */
    public boolean sendCreditPointsNotification(String toEmail, String username,
                                               int rank, int creditPoints, 
                                               int ticketsPurchased) {
        String subject = "Monthly Credit Points Awarded - Rank #" + rank;
        String body = String.format(
            "Dear %s,\n\n" +
            "Congratulations! Your monthly rewards have been credited!\n\n" +
            "MONTHLY REWARDS SUMMARY:\n" +
            "=================\n" +
            "Your Rank: #%d\n" +
            "Credit Points Earned: %d points\n" +
            "Tickets Purchased This Month: %d\n" +
            "Month: %s\n\n" +
            "These credit points can be used for discounts on future bookings.\n\n" +
            "Keep booking to earn more rewards next month!\n\n" +
            "View your total points balance in your profile dashboard.\n\n" +
            "Best regards,\n" +
            "Event Booking Team",
            username, rank, creditPoints, ticketsPurchased,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        );
        
        return sendEmail(toEmail, subject, body);
    }
    
    /**
     * NEW: Send event reminder email for events happening today.
     * This method was missing from the UI version.
     */
    public boolean sendEventReminderEmail(String toEmail, String username, Event event) {
        String subject = "Event Reminder: '" + event.getName() + "' is TODAY!";
        String body = String.format(
            "Dear %s,\n\n" +
            "This is a friendly reminder that the event '%s' is taking place today.\n\n" +
            "EVENT DETAILS:\n" +
            "=================\n" +
            "Event: %s\n" +
            "Date: %s\n" +
            "Venue: %s\n\n" +
            "We look forward to seeing you there!\n\n" +
            "Best regards,\n" +
            "The Event Booking Team",
            username, event.getName(), event.getName(), event.getDate().toString(), event.getVenue()
        );
        return sendEmail(toEmail, subject, body);
    }

    /**
     * Core email sending method - REAL IMPLEMENTATION
     */
    private boolean sendEmail(String toEmail, String subject, String body) {
        if (!ENABLE_REAL_EMAILS) {
            // Simulation mode - just log to console
            return logEmailToConsole(toEmail, subject, body);
        }
        
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            
            Authenticator auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
                }
            };
            
            Session session = Session.getInstance(props, auth);
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);
            message.setSentDate(new java.util.Date());
            
            Transport.send(message);
            
            System.out.println("[EMAIL SUCCESS] Email sent to: " + toEmail);
            System.out.println("[EMAIL] Subject: " + subject);
            return true;
            
        } catch (AuthenticationFailedException e) {
            System.err.println("[EMAIL FATAL ERROR] Authentication failed. This is the most common error.");
            System.err.println("--> Please verify your FROM_EMAIL and EMAIL_PASSWORD in EmailService.java.");
            System.err.println("--> If using Gmail, you MUST enable 2-Factor Authentication and generate an 'App Password'.");
            System.err.println("--> Your regular Gmail password will NOT work.");
            System.err.println("--> Guide: https://myaccount.google.com/apppasswords");
            e.printStackTrace();
            return false;
            
        } catch (MessagingException e) {
            System.err.println("[EMAIL ERROR] Failed to send email. This could be a network issue.");
            System.err.println("--> Check your internet connection and firewall settings.");
            System.err.println("--> Ensure the SMTP_HOST and SMTP_PORT are correct.");
            e.printStackTrace();
            return false;
            
        } catch (Exception e) {
            System.err.println("[EMAIL UNEXPECTED ERROR] An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Log email to console (simulation mode)
     */
    private boolean logEmailToConsole(String toEmail, String subject, String body) {
        try {
            System.out.println("\n======================================================================");
            System.out.println("[EMAIL] EMAIL NOTIFICATION (SIMULATION MODE)");
            System.out.println("======================================================================");
            System.out.println("To: " + toEmail);
            System.out.println("From: " + FROM_EMAIL);
            System.out.println("Subject: " + subject);
            System.out.println("----------------------------------------------------------------------");
            System.out.println(body);
            System.out.println("======================================================================\n");
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Error logging email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calculate refund percentage based on days until event
     */
    private String calculateRefundPercentage(int daysUntilEvent) {
        if (daysUntilEvent > 3) return "100%";
        if (daysUntilEvent == 3) return "75%";
        if (daysUntilEvent == 2) return "50%";
        if (daysUntilEvent == 1) return "25%";
        return "0%";
    }

    /**
     * Test email configuration and connectivity
     */
    public boolean testEmailConfiguration() {
        System.out.println("\n========================================");
        System.out.println("[EMAIL] Testing Email Configuration");
        System.out.println("========================================");
        System.out.println("Mode: " + (ENABLE_REAL_EMAILS ? "REAL EMAILS" : "SIMULATION"));
        System.out.println("SMTP Host: " + SMTP_HOST);
        System.out.println("SMTP Port: " + SMTP_PORT);
        System.out.println("From Email: " + FROM_EMAIL);
        System.out.println("========================================");
        
        if (!ENABLE_REAL_EMAILS) {
            System.out.println("[EMAIL] Simulation mode - no actual test performed. Logging is active.");
            return true;
        }
        
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
                }
            });
            
            Transport transport = session.getTransport("smtp");
            transport.connect(SMTP_HOST, FROM_EMAIL, EMAIL_PASSWORD);
            transport.close();
            
            System.out.println("[SUCCESS] SMTP connection successful!");
            System.out.println("[EMAIL] Email service is ready to send emails.");
            return true;
            
        } catch (AuthenticationFailedException e) {
            System.err.println("[FAILED] Authentication failed! Check credentials and ensure you are using a Gmail App Password.");
            System.err.println("Visit: https://myaccount.google.com/apppasswords");
            return false;
            
        } catch (Exception e) {
            System.err.println("[FAILED] Connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send a test email to verify configuration
     */
    public boolean sendTestEmail(String toEmail) {
        String subject = "Test Email - Event Booking System";
        String body = "This is a test email from the Event Booking System.\n\n" +
                     "If you receive this, your email configuration is working correctly!\n\n" +
                     "Sent at: " + LocalDateTime.now().format(TIME_FORMATTER);
        
        System.out.println("[EMAIL] Sending test email to: " + toEmail);
        return sendEmail(toEmail, subject, body);
    }
}
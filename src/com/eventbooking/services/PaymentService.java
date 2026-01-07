package com.eventbooking.services;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * Payment Service - Fixed to support all payment methods
 */
public class PaymentService {
    private static PaymentService instance;
    private Random random = new Random();

    private PaymentService() {}

    public static synchronized PaymentService getInstance() {
        if (instance == null) {
            instance = new PaymentService();
        }
        return instance;
    }

    /**
     * Process payment with proper dialogs for each payment method
     * @return Transaction ID on success, null on failure/cancellation
     */
    public String processPayment(String username, double amount, String method) {
        System.out.println("\n=== PAYMENT PROCESSING STARTED ===");
        System.out.println("User: " + username);
        System.out.println("Amount: $" + String.format("%.2f", amount));
        System.out.println("Method: " + method);
        
        String transactionId = null;
        
        switch (method) {
            case "UPI":
                transactionId = processUPI(username, amount);
                break;
            case "Credit Card":
                transactionId = processCreditCard(username, amount);
                break;
            case "Debit Card":
                transactionId = processDebitCard(username, amount);
                break;
            case "PayPal":
                transactionId = processPayPal(username, amount);
                break;
            case "Net Banking":
                transactionId = processNetBanking(username, amount);
                break;
            default:
                JOptionPane.showMessageDialog(null,
                    "Invalid payment method selected!",
                    "Payment Error",
                    JOptionPane.ERROR_MESSAGE);
                return null;
        }
        
        if (transactionId != null) {
            System.out.println("✓ Payment Successful - Transaction ID: " + transactionId);
        } else {
            System.out.println("✗ Payment Failed or Cancelled");
        }
        System.out.println("=== PAYMENT PROCESSING ENDED ===\n");
        
        return transactionId;
    }

    private String processUPI(String username, double amount) {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField upiField = new JTextField(username.toLowerCase() + "@upi");
        JPasswordField pinField = new JPasswordField();
        
        panel.add(new JLabel("UPI ID:")); panel.add(upiField);
        panel.add(new JLabel("UPI PIN:")); panel.add(pinField);
        panel.add(new JLabel("Amount:")); panel.add(new JLabel(String.format("$%.2f", amount)));
        
        int result = JOptionPane.showConfirmDialog(null, panel, "UPI Payment", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            if (upiField.getText().trim().isEmpty() || new String(pinField.getPassword()).isEmpty()) {
                JOptionPane.showMessageDialog(null, "UPI ID and PIN cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            simulateProcessing("Processing UPI Payment...");
            return "UPI" + System.currentTimeMillis();
        }
        return null;
    }

    private String processCreditCard(String username, double amount) {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        JTextField cardField = new JTextField("4532 1234 5678 9010");
        JTextField nameField = new JTextField(username);
        JTextField expiryField = new JTextField("12/25");
        JPasswordField cvvField = new JPasswordField();
        
        panel.add(new JLabel("Card Number:")); panel.add(cardField);
        panel.add(new JLabel("Cardholder Name:")); panel.add(nameField);
        panel.add(new JLabel("Expiry (MM/YY):")); panel.add(expiryField);
        panel.add(new JLabel("CVV:")); panel.add(cvvField);
        panel.add(new JLabel("Amount:")); panel.add(new JLabel(String.format("$%.2f", amount)));
        
        int result = JOptionPane.showConfirmDialog(null, panel, "Credit Card Payment", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            simulateProcessing("Authorizing Credit Card...");
            return "CC" + System.currentTimeMillis();
        }
        return null;
    }

    private String processDebitCard(String username, double amount) {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        JTextField cardField = new JTextField("5412 7534 8901 2345");
        JTextField nameField = new JTextField(username);
        JTextField expiryField = new JTextField("11/26");
        JPasswordField pinField = new JPasswordField();
        
        panel.add(new JLabel("Debit Card Number:")); panel.add(cardField);
        panel.add(new JLabel("Cardholder Name:")); panel.add(nameField);
        panel.add(new JLabel("Expiry (MM/YY):")); panel.add(expiryField);
        panel.add(new JLabel("PIN:")); panel.add(pinField);
        panel.add(new JLabel("Amount:")); panel.add(new JLabel(String.format("$%.2f", amount)));
        
        int result = JOptionPane.showConfirmDialog(null, panel, "Debit Card Payment", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            simulateProcessing("Processing Debit Card Payment...");
            return "DC" + System.currentTimeMillis();
        }
        return null;
    }

    private String processPayPal(String username, double amount) {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField emailField = new JTextField(username + "@example.com");
        JPasswordField passwordField = new JPasswordField();
        
        panel.add(new JLabel("PayPal Email:")); panel.add(emailField);
        panel.add(new JLabel("Password:")); panel.add(passwordField);
        panel.add(new JLabel("Amount:")); panel.add(new JLabel(String.format("$%.2f", amount)));
        
        int result = JOptionPane.showConfirmDialog(null, panel, "PayPal Payment", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            simulateProcessing("Redirecting to PayPal...");
            return "PP" + System.currentTimeMillis();
        }
        return null;
    }

    private String processNetBanking(String username, double amount) {
        String[] banks = {"State Bank", "HDFC Bank", "ICICI Bank", "Axis Bank", "Kotak Bank"};
        JComboBox<String> bankCombo = new JComboBox<>(banks);
        
        int result = JOptionPane.showConfirmDialog(null, bankCombo, "Select Your Bank", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String bank = (String) bankCombo.getSelectedItem();
            simulateProcessing("Connecting to " + bank + "...");
            return "NB" + System.currentTimeMillis();
        }
        return null;
    }

    private void simulateProcessing(String message) {
        JDialog loadingDialog = new JDialog((Frame) null, "Processing...", true);
        JLabel label = new JLabel(message, JLabel.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        loadingDialog.add(label);
        loadingDialog.pack();
        loadingDialog.setLocationRelativeTo(null);
        
        Timer timer = new Timer(1500, e -> loadingDialog.dispose());
        timer.setRepeats(false);
        timer.start();
        
        loadingDialog.setVisible(true);
    }
}
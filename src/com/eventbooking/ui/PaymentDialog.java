package com.eventbooking.ui;

import com.eventbooking.database.DatabaseManager;
import com.eventbooking.services.PaymentService;
import com.eventbooking.services.CreditPointsService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Payment Dialog with Credit Points (Wallet) redemption
 * COMPLETE AND WORKING VERSION WITH SCROLL WHEEL
 */
public class PaymentDialog extends JDialog {
    private String username;
    private double totalAmount;
    private double finalAmount;
    private int pointsToRedeem = 0;
    private String transactionId = null;
    
    private DatabaseManager dbManager;
    private PaymentService paymentService;
    private CreditPointsService creditService;
    
    private JLabel originalAmountLabel;
    private JLabel discountLabel;
    private JLabel finalAmountLabel;
    private JLabel availablePointsLabel;
    private JSpinner pointsSpinner;
    private JComboBox<String> paymentMethodCombo;
    private JButton applyPointsBtn;
    private JButton confirmPaymentBtn;
    private JButton resetPointsBtn;
    
    private int availablePoints;

    public PaymentDialog(Frame parent, String username, double amount) {
        super(parent, "Payment & Checkout", true);
        this.username = username;
        this.totalAmount = amount;
        this.finalAmount = amount;
        
        this.dbManager = new DatabaseManager();
        this.paymentService = PaymentService.getInstance();
        this.creditService = CreditPointsService.getInstance();
        
        initializeUI();
    }

    private void initializeUI() {
        setSize(550, 650);
        setLocationRelativeTo(getParent());
        setResizable(false);
        
        // Main panel with scroll pane
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // Title
        JLabel titleLabel = new JLabel("Payment & Checkout");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(52, 152, 219));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Scrollable content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);

        // Amount Summary Panel
        JPanel amountPanel = createTitledPanel("Payment Summary");
        amountPanel.setLayout(new GridLayout(3, 2, 10, 15));
        
        amountPanel.add(createLabel("Original Amount:"));
        originalAmountLabel = new JLabel(String.format("$%.2f", totalAmount));
        originalAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        amountPanel.add(originalAmountLabel);
        
        amountPanel.add(createLabel("Discount (Wallet Points):"));
        discountLabel = new JLabel("$0.00");
        discountLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        discountLabel.setForeground(new Color(46, 204, 113));
        amountPanel.add(discountLabel);
        
        amountPanel.add(createLabel("Final Amount:"));
        finalAmountLabel = new JLabel(String.format("$%.2f", finalAmount));
        finalAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        finalAmountLabel.setForeground(new Color(52, 152, 219));
        amountPanel.add(finalAmountLabel);
        
        contentPanel.add(amountPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Credit Points (Wallet) Panel
        availablePoints = creditService.getUserPoints(username);
        
        JPanel pointsPanel = createTitledPanel("Wallet Points (Redeem for Discount)");
        pointsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        pointsPanel.add(createLabel("Available Points:"), gbc);
        
        gbc.gridx = 1;
        availablePointsLabel = new JLabel(String.valueOf(availablePoints) + " points");
        availablePointsLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        availablePointsLabel.setForeground(new Color(155, 89, 182));
        pointsPanel.add(availablePointsLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        pointsPanel.add(createLabel("Points to Redeem:"), gbc);
        
        gbc.gridx = 1;
        int maxRedeemable = Math.min(availablePoints, (int)(totalAmount / 0.10));
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, maxRedeemable, 10);
        pointsSpinner = new JSpinner(spinnerModel);
        pointsSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ((JSpinner.DefaultEditor) pointsSpinner.getEditor()).getTextField().setColumns(10);
        pointsPanel.add(pointsSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonRow.setOpaque(false);
        
        // CHANGED: Updated button colors to blue shades
        applyPointsBtn = createStyledButton("Apply Points", new Color(52, 152, 219));
        applyPointsBtn.setEnabled(availablePoints > 0);
        applyPointsBtn.setPreferredSize(new Dimension(140, 35));
        applyPointsBtn.addActionListener(e -> applyPoints());
        
        resetPointsBtn = createStyledButton("Reset", new Color(41, 128, 185));
        resetPointsBtn.setPreferredSize(new Dimension(100, 35));
        resetPointsBtn.setEnabled(false);
        resetPointsBtn.addActionListener(e -> resetPoints());
        
        buttonRow.add(applyPointsBtn);
        buttonRow.add(resetPointsBtn);
        pointsPanel.add(buttonRow, gbc);
        
        JLabel pointsInfoLabel = new JLabel("[INFO] 1 point = $0.10 discount | Max redeemable: " + maxRedeemable + " points");
        pointsInfoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        pointsInfoLabel.setForeground(Color.GRAY);
        gbc.gridy = 3;
        pointsPanel.add(pointsInfoLabel, gbc);
        
        contentPanel.add(pointsPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Payment Method Panel
        JPanel methodPanel = createTitledPanel("Payment Method");
        methodPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));
        
        methodPanel.add(createLabel("Select Method:"));
        String[] paymentMethods = {"UPI", "Credit Card", "Debit Card", "PayPal", "Net Banking"};
        paymentMethodCombo = new JComboBox<>(paymentMethods);
        paymentMethodCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        paymentMethodCombo.setPreferredSize(new Dimension(220, 35));
        methodPanel.add(paymentMethodCombo);
        
        contentPanel.add(methodPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // ADDED: Wrap content in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        confirmPaymentBtn = createStyledButton("Confirm Payment", new Color(46, 204, 113));
        confirmPaymentBtn.setPreferredSize(new Dimension(180, 45));
        confirmPaymentBtn.addActionListener(e -> processPayment());
        
        JButton cancelBtn = createStyledButton("Cancel", new Color(231, 76, 60));
        cancelBtn.setPreferredSize(new Dimension(120, 45));
        cancelBtn.addActionListener(e -> {
            transactionId = null;
            dispose();
        });
        
        buttonPanel.add(confirmPaymentBtn);
        buttonPanel.add(cancelBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }

    private void applyPoints() {
        int points = (Integer) pointsSpinner.getValue();
        
        if (points <= 0) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid number of points to redeem.",
                "Invalid Points",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        double discount = creditService.calculateDiscount(points);
        
        // Don't allow discount greater than total amount
        if (discount > totalAmount) {
            int maxPoints = creditService.getPointsForDiscount(totalAmount);
            JOptionPane.showMessageDialog(this,
                String.format("Maximum redeemable points for this amount: %d points\n($%.2f discount)",
                    maxPoints, totalAmount),
                "Points Limit Exceeded",
                JOptionPane.WARNING_MESSAGE);
            pointsSpinner.setValue(maxPoints);
            return;
        }
        
        pointsToRedeem = points;
        finalAmount = Math.max(0, totalAmount - discount);
        
        discountLabel.setText(String.format("-$%.2f", discount));
        finalAmountLabel.setText(String.format("$%.2f", finalAmount));
        
        // Update button states
        applyPointsBtn.setEnabled(false);
        resetPointsBtn.setEnabled(true);
        pointsSpinner.setEnabled(false);
        
        JOptionPane.showMessageDialog(this,
            String.format("Points Applied Successfully!\n\n" +
                "Points Redeemed: %d\n" +
                "Discount: $%.2f\n" +
                "New Total: $%.2f",
                points, discount, finalAmount),
            "Points Applied",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetPoints() {
        pointsToRedeem = 0;
        finalAmount = totalAmount;
        
        discountLabel.setText("$0.00");
        finalAmountLabel.setText(String.format("$%.2f", finalAmount));
        
        pointsSpinner.setValue(0);
        pointsSpinner.setEnabled(true);
        applyPointsBtn.setEnabled(true);
        resetPointsBtn.setEnabled(false);
    }

    private void processPayment() {
        String paymentMethod = (String) paymentMethodCombo.getSelectedItem();
        
        // Build confirmation message
        StringBuilder confirmMsg = new StringBuilder();
        confirmMsg.append("Please confirm your payment:\n\n");
        confirmMsg.append("===================================\n");
        confirmMsg.append(String.format("Original Amount: $%.2f\n", totalAmount));
        
        if (pointsToRedeem > 0) {
            double discount = creditService.calculateDiscount(pointsToRedeem);
            confirmMsg.append(String.format("Wallet Points Used: %d points\n", pointsToRedeem));
            confirmMsg.append(String.format("Discount Applied: -$%.2f\n", discount));
            confirmMsg.append("-----------------------------------\n");
        }
        
        confirmMsg.append(String.format("Final Amount: $%.2f\n", finalAmount));
        confirmMsg.append(String.format("Payment Method: %s\n", paymentMethod));
        confirmMsg.append("===================================\n\n");
        confirmMsg.append("Proceed with payment?");
        
        int confirm = JOptionPane.showConfirmDialog(this,
            confirmMsg.toString(),
            "Confirm Payment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Process payment
            transactionId = paymentService.processPayment(username, finalAmount, paymentMethod);
            
            if (transactionId != null) {
                // Payment successful - deduct credit points if used
                if (pointsToRedeem > 0) {
                    boolean pointsDeducted = creditService.redeemPoints(username, pointsToRedeem);
                    if (!pointsDeducted) {
                        System.err.println("Warning: Failed to deduct credit points");
                    }
                }
                
                // Show success message
                StringBuilder successMsg = new StringBuilder();
                successMsg.append("Payment Successful!\n\n");
                successMsg.append("===================================\n");
                successMsg.append(String.format("Transaction ID: %s\n", transactionId));
                successMsg.append(String.format("Amount Paid: $%.2f\n", finalAmount));
                
                if (pointsToRedeem > 0) {
                    double discount = creditService.calculateDiscount(pointsToRedeem);
                    successMsg.append(String.format("Points Redeemed: %d\n", pointsToRedeem));
                    successMsg.append(String.format("Discount: $%.2f\n", discount));
                    int remainingPoints = creditService.getUserPoints(username);
                    successMsg.append(String.format("Remaining Points: %d\n", remainingPoints));
                }
                
                successMsg.append(String.format("Payment Method: %s\n", paymentMethod));
                successMsg.append("===================================\n\n");
                successMsg.append("Your booking will be confirmed shortly.");
                
                JOptionPane.showMessageDialog(this,
                    successMsg.toString(),
                    "Payment Success",
                    JOptionPane.INFORMATION_MESSAGE);
                
                dispose();
            } else {
                // Payment failed
                JOptionPane.showMessageDialog(this,
                    "Payment Failed or Cancelled!\n\n" +
                    "Your booking has not been processed.\n" +
                    "Please try again with a different payment method.",
                    "Payment Failed",
                    JOptionPane.ERROR_MESSAGE);
                
                // Reset points if they were applied
                if (pointsToRedeem > 0) {
                    resetPoints();
                }
            }
        }
    }

    public String getTransactionId() {
        return transactionId;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public int getPointsRedeemed() {
        return pointsToRedeem;
    }

    private JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder(
                new LineBorder(new Color(52, 152, 219), 2),
                new EmptyBorder(10, 10, 10, 10)
            ),
            title,
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(52, 73, 94)
        ));
        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.darker());
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
}
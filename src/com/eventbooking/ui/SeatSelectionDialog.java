// PASTE THIS INTO src/com/eventbooking/ui/SeatSelectionDialog.java

package com.eventbooking.ui;

import com.eventbooking.database.DatabaseManager;
import com.eventbooking.models.Booking;
import com.eventbooking.models.Event;
import com.eventbooking.models.Seat;
import com.eventbooking.models.SeatMap;
import com.eventbooking.services.PaymentService;
import com.eventbooking.notifications.NotificationSystem;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Theatre-style seat selection dialog
 */
public class SeatSelectionDialog extends JDialog {
    private DatabaseManager dbManager;
    private PaymentService paymentService;
    private NotificationSystem notificationSystem;
    private String username;
    private Event event;
    private SeatMap seatMap;
    private boolean viewOnly;
    
    private JPanel seatPanel;
    private JLabel selectedSeatsLabel;
    private JLabel totalPriceLabel;
    private JButton confirmBtn;
    
    private List<String> selectedSeats;
    private List<JButton> seatButtons;

    public SeatSelectionDialog(JFrame parent, String username, Event event, DatabaseManager dbManager) {
        this(parent, username, event, dbManager, false);
    }

    public SeatSelectionDialog(JFrame parent, String username, Event event, 
                              DatabaseManager dbManager, boolean viewOnly) {
        super(parent, viewOnly ? "My Booked Seats" : "Select Your Seats", true);
        this.username = username;
        this.event = event;
        this.dbManager = dbManager;
        this.viewOnly = viewOnly;
        this.paymentService = PaymentService.getInstance();
        this.notificationSystem = NotificationSystem.getInstance();
        this.selectedSeats = new ArrayList<>();
        this.seatButtons = new ArrayList<>();
        
        initializeUI();
    }

    private void initializeUI() {
        setSize(900, 700);
        setLocationRelativeTo(getParent());
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(52, 152, 219));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel eventLabel = new JLabel(event.getName());
        eventLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        eventLabel.setForeground(Color.WHITE);
        
        JLabel infoLabel = new JLabel(String.format("Price per seat: $%.2f | Available: %d", 
            event.getPrice(), event.getSeatsAvailable()));
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoLabel.setForeground(Color.WHITE);
        
        JPanel headerTextPanel = new JPanel();
        headerTextPanel.setLayout(new BoxLayout(headerTextPanel, BoxLayout.Y_AXIS));
        headerTextPanel.setOpaque(false);
        headerTextPanel.add(eventLabel);
        headerTextPanel.add(Box.createVerticalStrut(5));
        headerTextPanel.add(infoLabel);
        
        headerPanel.add(headerTextPanel, BorderLayout.WEST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center panel with screen and seats
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(Color.WHITE);
        
        // Screen
        JPanel screenPanel = new JPanel();
        screenPanel.setBackground(Color.WHITE);
        screenPanel.setBorder(new EmptyBorder(10, 0, 20, 0));
        
        JLabel screenLabel = new JLabel("SCREEN");
        screenLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        screenLabel.setForeground(new Color(100, 100, 100));
        screenLabel.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 3, 0, new Color(52, 152, 219)),
            new EmptyBorder(5, 50, 5, 50)
        ));
        screenPanel.add(screenLabel);
        centerPanel.add(screenPanel, BorderLayout.NORTH);

        // Seats
        seatMap = dbManager.getSeatMapForEvent(event.getId());
        List<String> userBookedSeats = viewOnly ? 
            dbManager.getBookedSeatsForUser(event.getId(), username) : new ArrayList<>();
        
        seatPanel = new JPanel(new GridBagLayout());
        seatPanel.setBackground(Color.WHITE);
        seatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        List<String> rows = seatMap.getRows();
        int seatsPerRow = seatMap.getSeatsPerRow();
        
        for (int r = 0; r < rows.size(); r++) {
            String row = rows.get(r);
            
            // Row label
            gbc.gridx = 0;
            gbc.gridy = r;
            JLabel rowLabel = new JLabel(row);
            rowLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            rowLabel.setPreferredSize(new Dimension(30, 40));
            rowLabel.setHorizontalAlignment(JLabel.CENTER);
            seatPanel.add(rowLabel, gbc);
            
            // Seats in row
            for (int s = 1; s <= seatsPerRow; s++) {
                gbc.gridx = s;
                String seatId = row + s;
                Seat seat = seatMap.getSeat(seatId);
                
                JButton seatBtn = createSeatButton(seat, userBookedSeats.contains(seatId));
                seatButtons.add(seatBtn);
                seatPanel.add(seatBtn, gbc);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(seatPanel);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        // Legend
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        legendPanel.setBackground(Color.WHITE);
        legendPanel.setBorder(new TitledBorder("Legend"));
        
        legendPanel.add(createLegendItem("Available", new Color(46, 204, 113)));
        legendPanel.add(createLegendItem("Selected", new Color(52, 152, 219)));
        legendPanel.add(createLegendItem("Booked", new Color(231, 76, 60)));
        if (viewOnly) {
            legendPanel.add(createLegendItem("Your Seats", new Color(241, 196, 15)));
        }
        
        bottomPanel.add(legendPanel, BorderLayout.NORTH);
        
        // Selection info and buttons
        JPanel actionPanel = new JPanel(new BorderLayout(10, 10));
        actionPanel.setBackground(Color.WHITE);
        
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setBackground(Color.WHITE);
        
        selectedSeatsLabel = new JLabel("Selected Seats: None");
        selectedSeatsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        totalPriceLabel = new JLabel("Total Price: $0.00");
        totalPriceLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalPriceLabel.setForeground(new Color(52, 152, 219));
        
        infoPanel.add(selectedSeatsLabel);
        infoPanel.add(totalPriceLabel);
        actionPanel.add(infoPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        if (!viewOnly) {
            confirmBtn = createStyledButton("Confirm Booking", new Color(46, 204, 113));
            confirmBtn.setEnabled(false);
            confirmBtn.addActionListener(e -> confirmBooking());
            buttonPanel.add(confirmBtn);
        }
        
        JButton closeBtn = createStyledButton(viewOnly ? "Close" : "Cancel", new Color(149, 165, 166));
        closeBtn.addActionListener(e -> dispose());
        buttonPanel.add(closeBtn);
        
        actionPanel.add(buttonPanel, BorderLayout.EAST);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }

    private JButton createSeatButton(Seat seat, boolean isUserSeat) {
        JButton btn = new JButton(String.valueOf(seat.getNumber()));
        btn.setPreferredSize(new Dimension(45, 40));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (seat.isBooked()) {
            if (isUserSeat) {
                // User's own booked seats (view only mode)
                btn.setBackground(new Color(241, 196, 15));
                btn.setForeground(Color.WHITE);
                btn.setBorder(new LineBorder(new Color(241, 196, 15).darker(), 2));
                btn.setEnabled(false);
                btn.setToolTipText("Your booked seat: " + seat.getSeatId());
            } else {
                // Other users' booked seats
                btn.setBackground(new Color(231, 76, 60));
                btn.setForeground(Color.WHITE);
                btn.setBorder(new LineBorder(new Color(231, 76, 60).darker(), 2));
                btn.setEnabled(false);
                btn.setToolTipText("Booked");
            }
        } else {
            // Available seats
            btn.setBackground(new Color(46, 204, 113));
            btn.setForeground(Color.WHITE);
            btn.setBorder(new LineBorder(new Color(46, 204, 113).darker(), 2));
            btn.setEnabled(!viewOnly);
            btn.setToolTipText("Click to select: " + seat.getSeatId());
            
            if (!viewOnly) {
                btn.addActionListener(e -> toggleSeatSelection(seat.getSeatId(), btn));
            }
        }
        
        return btn;
    }

    private void toggleSeatSelection(String seatId, JButton btn) {
        if (selectedSeats.contains(seatId)) {
            // Deselect
            selectedSeats.remove(seatId);
            btn.setBackground(new Color(46, 204, 113));
            btn.setBorder(new LineBorder(new Color(46, 204, 113).darker(), 2));
        } else {
            // Select
            selectedSeats.add(seatId);
            btn.setBackground(new Color(52, 152, 219));
            btn.setBorder(new LineBorder(new Color(52, 152, 219).darker(), 2));
        }
        
        updateSelectionInfo();
    }

    private void updateSelectionInfo() {
        if (selectedSeats.isEmpty()) {
            selectedSeatsLabel.setText("Selected Seats: None");
            totalPriceLabel.setText("Total Price: $0.00");
            confirmBtn.setEnabled(false);
        } else {
            String seatsText = String.join(", ", selectedSeats);
            selectedSeatsLabel.setText("Selected Seats: " + seatsText);
            
            double totalPrice = selectedSeats.size() * event.getPrice();
            totalPriceLabel.setText(String.format("Total Price: $%.2f", totalPrice));
            confirmBtn.setEnabled(true);
        }
    }

    private void confirmBooking() {
        if (selectedSeats.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least one seat!", 
                "No Seats Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        double totalPrice = selectedSeats.size() * event.getPrice();
        
        // Show payment dialog
        String[] paymentMethods = {"Credit Card", "Debit Card", "PayPal", "UPI", "Net Banking"};
        String paymentMethod = (String) JOptionPane.showInputDialog(
            this,
            String.format("Select payment method for $%.2f:", totalPrice),
            "Payment Method",
            JOptionPane.QUESTION_MESSAGE,
            null,
            paymentMethods,
            paymentMethods[0]
        );
        
        if (paymentMethod == null) {
            return; // User cancelled
        }
        
        // REPLACE the payment block inside confirmBooking() in SeatSelectionDialog.java

        // Process payment
        String transactionId = paymentService.processPayment(username, totalPrice, paymentMethod);
        
        // Check if payment was successful (transactionId is not null)
        if (transactionId == null) {
            JOptionPane.showMessageDialog(this, 
                "Payment failed or was cancelled! Please try again.", 
                "Payment Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Book seats
        // REPLACE the dbManager.bookSeatsForEvent call in SeatSelectionDialog.java

        // Book seats using the correct consolidated method
        Booking newBooking = dbManager.createBookingWithSeats(
            username, event.getId(), selectedSeats, paymentMethod, transactionId);

        boolean bookingSuccess = (newBooking != null); // Success if a booking object was returned
        
        if (bookingSuccess) {
            notificationSystem.sendBookingConfirmation(
                username, event.getName(), selectedSeats.size(), totalPrice);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Booking failed! Some seats may have been taken.", 
                "Booking Error", 
                JOptionPane.ERROR_MESSAGE);
            
            // Refresh seat display
            dispose();
            new SeatSelectionDialog((JFrame) getParent(), username, event, dbManager).setVisible(true);
        }
    }

    private JPanel createLegendItem(String text, Color color) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBackground(Color.WHITE);
        
        JLabel colorBox = new JLabel("  ");
        colorBox.setOpaque(true);
        colorBox.setBackground(color);
        colorBox.setBorder(new LineBorder(color.darker(), 2));
        colorBox.setPreferredSize(new Dimension(20, 20));
        
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        panel.add(colorBox);
        panel.add(label);
        
        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(150, 40));
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
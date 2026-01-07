package com.eventbooking.ui;

import com.eventbooking.database.DatabaseManager;
import com.eventbooking.models.Booking;
import com.eventbooking.models.Event;
import com.eventbooking.notifications.NotificationSystem;
import com.eventbooking.services.RefundService;
import com.eventbooking.services.EmailService;
import org.bson.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class AttendeeDashboard extends JFrame {
    private String username;
    private DatabaseManager dbManager;
    private NotificationSystem notificationSystem;
    private RefundService refundService;
    private EmailService emailService;

    private JTabbedPane tabbedPane;
    private JLabel unreadCountLabel;

    private JTable eventsTable;
    private DefaultTableModel eventsTableModel;
    private EventSearchPanel eventSearchPanel;
    
    private JTable bookingsTable;
    private DefaultTableModel bookingsTableModel;

    private JTable notificationsTable;
    private DefaultTableModel notificationsTableModel;
    
    private JProgressBar eventsLoadingBar;
    private JProgressBar bookingsLoadingBar;
    private JProgressBar notificationsLoadingBar;

    public AttendeeDashboard(String username) {
        this.username = username;
        this.dbManager = new DatabaseManager();
        this.notificationSystem = NotificationSystem.getInstance();
        this.refundService = RefundService.getInstance();
        this.emailService = EmailService.getInstance();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Attendee Dashboard");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem bookingHelp = new JMenuItem("How to Book Tickets");
        bookingHelp.addActionListener(e -> showBookingHelp());
        JMenuItem cancelHelp = new JMenuItem("Cancellation Policy");
        cancelHelp.addActionListener(e -> showCancellationPolicy());
        JMenuItem pointsHelp = new JMenuItem("About Credit Points");
        pointsHelp.addActionListener(e -> showPointsHelp());
        helpMenu.add(bookingHelp);
        helpMenu.add(cancelHelp);
        helpMenu.add(pointsHelp);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel welcomeLabel = new JLabel("Welcome, " + username);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcomeLabel.setForeground(Color.WHITE);
        
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        navPanel.setOpaque(false);

        unreadCountLabel = new JLabel("0 New");
        unreadCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        unreadCountLabel.setForeground(new Color(255, 193, 7));
        unreadCountLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        unreadCountLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tabbedPane.setSelectedIndex(2);
            }
        });

        JButton profileBtn = new JButton("My Profile");
        profileBtn.setBackground(new Color(52, 152, 219));
        profileBtn.setForeground(Color.WHITE);
        profileBtn.setFocusPainted(false);
        profileBtn.setBorderPainted(false);
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBtn.addActionListener(e -> new ProfileFrame(this, username).setVisible(true));
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(231, 76, 60));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        navPanel.add(unreadCountLabel);
        navPanel.add(profileBtn);
        navPanel.add(logoutBtn);
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        headerPanel.add(navPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("Available Events", createEventsPanel());
        tabbedPane.addTab("My Bookings", createBookingsPanel());
        tabbedPane.addTab("My Notifications", createNotificationsPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        updateNotificationCount();
        setVisible(true);
    }
    
    private void showBookingHelp() {
        String message = "<html><h2>How to Book Tickets</h2>" +
                         "1. Select an event from the 'Available Events' table.<br>" +
                         "2. Click the 'Book Tickets' button.<br>" +
                         "3. Enter the number of tickets you wish to purchase.<br>" +
                         "4. Complete the payment process.<br>" +
                         "5. Your booking will appear in the 'My Bookings' tab.</html>";
        JOptionPane.showMessageDialog(this, message, "Booking Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showCancellationPolicy() {
        String message = "<html><h2>Our Cancellation Policy</h2>" +
                         "Refunds are based on when you cancel:<br>" +
                         "<ul>" +
                         "<li><b>More than 3 days before event:</b> 100% refund</li>" +
                         "<li><b>3 days before event:</b> 75% refund</li>" +
                         "<li><b>2 days before event:</b> 50% refund</li>" +
                         "<li><b>1 day before event:</b> 25% refund</li>" +
                         "<li><b>On the event day:</b> No refund</li>" +
                         "</ul>" +
                         "You can cancel all or some of your tickets for a booking.</html>";
        JOptionPane.showMessageDialog(this, message, "Cancellation Policy", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showPointsHelp() {
        String message = "<html><h2>About Credit Points</h2>" +
                         "Earn points for being an active user!<br>" +
                         "<ul>" +
                         "<li>Points are awarded at the end of each month based on tickets purchased.</li>" +
                         "<li>Top bookers get the most points!</li>" +
                         "<li><b>1 point = $0.10 discount</b> on future bookings.</li>" +
                         "<li>Redeem your points during the payment checkout process.</li>" +
                         "</ul>" +
                         "Check your points balance in 'My Profile'.</html>";
        JOptionPane.showMessageDialog(this, message, "Credit Points Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);

        String[] columns = {"ID", "Event Name", "Type", "Date", "Venue", "Available", "Price ($)", "Status"};
        eventsTableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        eventsTable = new JTable(eventsTableModel);
        styleTable(eventsTable, new Color(64,64,64));
        
        eventsTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                String status = value.toString();
                setHorizontalAlignment(JLabel.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                
                if (!isSelected) {
                    if (status.contains("Upcoming")) {
                        c.setBackground(new Color(230, 247, 255));
                        setForeground(new Color(3, 102, 214));
                    } else if (status.contains("Event Day")) {
                        c.setBackground(new Color(255, 244, 229));
                        setForeground(new Color(230, 126, 34));
                    } else if (status.contains("Completed")) {
                        c.setBackground(new Color(232, 245, 233));
                        setForeground(new Color(46, 125, 50));
                    }
                }
                
                return c;
            }
        });
        
        eventSearchPanel = new EventSearchPanel(eventsTableModel, eventsTable);
        panel.add(eventSearchPanel, BorderLayout.NORTH);

        eventsLoadingBar = new JProgressBar();
        eventsLoadingBar.setIndeterminate(true);
        eventsLoadingBar.setString("Loading events...");
        eventsLoadingBar.setStringPainted(true);
        eventsLoadingBar.setVisible(false);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(eventsLoadingBar, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(eventsTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);
        JButton viewDetailsBtn = createActionButton("View Details", new Color(52, 152, 219));
        viewDetailsBtn.addActionListener(e -> viewEventDetails());
        JButton bookBtn = createActionButton("Book Tickets", new Color(46, 204, 113));
        bookBtn.addActionListener(e -> bookTickets());
        buttonPanel.add(viewDetailsBtn);
        buttonPanel.add(bookBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void viewEventDetails() {
        Event selectedEvent = eventSearchPanel.getSelectedEvent();
        if (selectedEvent == null) {
            JOptionPane.showMessageDialog(this, "Please select an event to view details.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        selectedEvent.updateStatus();

        String details = String.format(
            "EVENT DETAILS\n\n" +
            "Name: %s\nType: %s\nDate: %s\nVenue: %s\n" +
            "Available Seats: %d / %d\nPrice: $%.2f\n" +
            "Occupancy: %.1f%%\n" +
            "Status: %s",
            selectedEvent.getName(), selectedEvent.getType(), selectedEvent.getDate(),
            selectedEvent.getVenue(), selectedEvent.getSeatsAvailable(), selectedEvent.getTotalSeats(),
            selectedEvent.getPrice(), selectedEvent.getOccupancyRate(), selectedEvent.getStatusWithIcon()
        );
        JOptionPane.showMessageDialog(this, details, "Event Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void bookTickets() {
        Event selectedEvent = eventSearchPanel.getSelectedEvent();
        if (selectedEvent == null) {
            JOptionPane.showMessageDialog(this, "Please select an event to book.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        selectedEvent.updateStatus();

        if (selectedEvent.isCompleted()) {
            JOptionPane.showMessageDialog(this, "This event has already been completed.", "Event Completed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!selectedEvent.isBookable()) {
            JOptionPane.showMessageDialog(this, "Sorry, this event is sold out or no longer available for booking.", "Booking Not Available", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String seatsStr = JOptionPane.showInputDialog(this,
            String.format("Event: %s\nAvailable Seats: %d\nPrice per ticket: $%.2f\n\nEnter number of tickets:",
            selectedEvent.getName(), selectedEvent.getSeatsAvailable(), selectedEvent.getPrice()),
            "Book Tickets", JOptionPane.QUESTION_MESSAGE);

        if (seatsStr == null || seatsStr.trim().isEmpty()) return;

        try {
            int seatsToBook = Integer.parseInt(seatsStr.trim());
            if (seatsToBook <= 0) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number of tickets.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (seatsToBook > selectedEvent.getSeatsAvailable()) {
                JOptionPane.showMessageDialog(this, String.format("Not enough seats available! Only %d seats remaining.", selectedEvent.getSeatsAvailable()), "Booking Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            double totalPrice = seatsToBook * selectedEvent.getPrice();
            PaymentDialog paymentDialog = new PaymentDialog(this, username, totalPrice);
            paymentDialog.setVisible(true);
            String transactionId = paymentDialog.getTransactionId();
            
            if (transactionId != null && !transactionId.isEmpty()) {
                processBookingInBackground(selectedEvent, seatsToBook, paymentDialog.getFinalAmount(), transactionId);
            } else {
                JOptionPane.showMessageDialog(this, "Booking cancelled. No payment was processed.", "Booking Cancelled", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void processBookingInBackground(Event event, int seatsToBook, double finalPrice, String transactionId) {
        SwingWorker<Booking, Void> worker = new SwingWorker<>() {
            @Override
            protected Booking doInBackground() throws Exception {
                return dbManager.createSimpleBooking(username, event.getId(), seatsToBook, "Online Payment", transactionId);
            }
            
            @Override
            protected void done() {
                try {
                    Booking booking = get();
                    if (booking != null) {
                        dbManager.addUserNotification(username, String.format("Booking confirmed for '%s' (%d tickets, $%.2f). Booking ID: %d.", event.getName(), seatsToBook, finalPrice, booking.getId()));
                        Document userDoc = dbManager.getUserDetails(username);
                        if(userDoc != null) {
                            emailService.sendBookingConfirmation(userDoc.getString("email"), username, event.getName(), seatsToBook, finalPrice, String.valueOf(booking.getId()));
                        }
                        JOptionPane.showMessageDialog(AttendeeDashboard.this, "Booking Successful! Your booking ID is " + booking.getId(), "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
                        eventSearchPanel.loadInitialData();
                        loadBookings();
                    } else {
                        JOptionPane.showMessageDialog(AttendeeDashboard.this, "Booking Failed! The event might be sold out.", "Booking Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private JPanel createBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);
        
        String[] columns = {"Booking ID", "Event", "Seats", "Total Price", "Date", "Status"};
        bookingsTableModel = new DefaultTableModel(columns, 0) { 
            public boolean isCellEditable(int r, int c) { return false; } 
        };
        bookingsTable = new JTable(bookingsTableModel);
        styleTable(bookingsTable, new Color(64,64,64));
        
        bookingsLoadingBar = new JProgressBar();
        bookingsLoadingBar.setIndeterminate(true);
        bookingsLoadingBar.setString("Loading bookings...");
        bookingsLoadingBar.setStringPainted(true);
        bookingsLoadingBar.setVisible(false);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(bookingsLoadingBar, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(bookingsTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton refreshBtn = createActionButton("Refresh", new Color(52, 152, 219));
        refreshBtn.addActionListener(e -> loadBookings());
        
        JButton cancelBtn = createActionButton("Cancel Booking", new Color(231, 76, 60));
        cancelBtn.addActionListener(e -> cancelBooking());
        
        buttonPanel.add(refreshBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        loadBookings();
        return panel;
    }

    private void loadBookings() {
        SwingWorker<List<Booking>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Booking> doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    bookingsLoadingBar.setVisible(true);
                    bookingsTable.setEnabled(false);
                });
                return dbManager.getUserBookings(username);
            }
            
            @Override
            protected void done() {
                try {
                    List<Booking> bookings = get();
                    bookingsTableModel.setRowCount(0);
                    for (Booking booking : bookings) {
                        bookingsTableModel.addRow(new Object[]{
                            booking.getId(), 
                            booking.getEventName(), 
                            booking.getSeatsBooked(),
                            String.format("$%.2f", booking.getTotalPrice()), 
                            booking.getTimestamp().toLocalDate(),
                            booking.getStatus()
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Error loading bookings: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    bookingsLoadingBar.setVisible(false);
                    bookingsTable.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void cancelBooking() {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking to cancel.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String status = (String) bookingsTableModel.getValueAt(selectedRow, 5);
        if ("Cancelled".equals(status)) {
            JOptionPane.showMessageDialog(this, "This booking is already cancelled.", "Already Cancelled", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int bookingId = (int) bookingsTableModel.getValueAt(selectedRow, 0);
        Booking booking = dbManager.getBookingById(bookingId);
        if (booking == null) return;
        
        Event event = dbManager.getEventById(booking.getEventId());
        if (event == null) return;
        
        event.updateStatus();
        if (event.isCompleted()) {
            JOptionPane.showMessageDialog(this, "Cannot cancel booking for completed events.", "Action Not Allowed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int currentSeats = booking.getSeatsBooked();
        String seatsToCancelStr = JOptionPane.showInputDialog(this, "Booking for '" + event.getName() + "' has " + currentSeats + " seat(s).\nHow many seats would you like to cancel?", "Cancel Tickets", JOptionPane.QUESTION_MESSAGE);
        if (seatsToCancelStr == null || seatsToCancelStr.trim().isEmpty()) return;

        int seatsToCancel;
        try {
            seatsToCancel = Integer.parseInt(seatsToCancelStr.trim());
            if (seatsToCancel <= 0 || seatsToCancel > currentSeats) {
                JOptionPane.showMessageDialog(this, "Please enter a number between 1 and " + currentSeats + ".", "Invalid Number", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        double pricePerTicket = booking.getTotalPrice() / booking.getSeatsBooked();
        double originalAmountForCancellation = pricePerTicket * seatsToCancel;
        
        RefundService.RefundDetails refundDetails = refundService.calculateRefund(originalAmountForCancellation, event.getDate());

        String confirmMessage = String.format("You are about to cancel %d seat(s) for '%s'.\n\n--- REFUND DETAILS ---\nOriginal Price for %d seats: $%.2f\nRefund Amount: $%.2f (%.0f%%)\nPolicy Applied: %s\n\nAre you sure you want to proceed?", seatsToCancel, event.getName(), seatsToCancel, originalAmountForCancellation, refundDetails.getRefundAmount(), refundDetails.getRefundPercentage(), refundDetails.getPolicyApplied());
        int confirm = JOptionPane.showConfirmDialog(this, confirmMessage, "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            processPartialCancellationInBackground(bookingId, seatsToCancel);
        }
    }
    
    private void processPartialCancellationInBackground(int bookingId, int seatsToCancel) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return dbManager.partiallyCancelBooking(bookingId, seatsToCancel);
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(AttendeeDashboard.this, "Cancellation successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(AttendeeDashboard.this, "Cancellation failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    loadBookings();
                    eventSearchPanel.loadInitialData();
                } catch (Exception e) {
                     e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private JPanel createNotificationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);

        String[] columns = {"Status", "Message", "Timestamp"};
        notificationsTableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        notificationsTable = new JTable(notificationsTableModel);
        styleTable(notificationsTable, new Color(64,64,64));

        notificationsLoadingBar = new JProgressBar();
        notificationsLoadingBar.setIndeterminate(true);
        notificationsLoadingBar.setString("Loading notifications...");
        notificationsLoadingBar.setStringPainted(true);
        notificationsLoadingBar.setVisible(false);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(notificationsLoadingBar, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(notificationsTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);
        JButton markAllReadBtn = createActionButton("Mark All as Read", new Color(52, 152, 219));
        markAllReadBtn.addActionListener(e -> markAllAsRead());
        JButton deleteBtn = createActionButton("Delete Notification", new Color(231, 76, 60));
        deleteBtn.addActionListener(e -> deleteSelectedNotification());
        JButton refreshBtn = createActionButton("Refresh", new Color(127, 140, 141));
        refreshBtn.addActionListener(e -> loadNotifications());

        buttonPanel.add(markAllReadBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        loadNotifications();
        return panel;
    }
    
    private void loadNotifications() {
        SwingWorker<List<Document>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Document> doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    notificationsLoadingBar.setVisible(true);
                    notificationsTable.setEnabled(false);
                });
                return dbManager.getUserNotifications(username);
            }

            @Override
            protected void done() {
                try {
                    List<Document> notifications = get();
                    notificationsTableModel.setRowCount(0);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    for (Document notif : notifications) {
                        Object timestampObj = notif.get("timestamp");
                        String timestampStr;

                        // FIX: Handle both Date and String types for timestamp to prevent ClassCastException
                        if (timestampObj instanceof Date) {
                            timestampStr = ((Date) timestampObj).toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime().format(formatter);
                        } else {
                            timestampStr = LocalDateTime.parse(timestampObj.toString()).format(formatter);
                        }
                        
                        notificationsTableModel.addRow(new Object[]{
                            notif.getBoolean("read", false) ? "Read" : "UNREAD",
                            notif.getString("message"),
                            timestampStr
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Error loading notifications: " + e.getClass().getName() + " - " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    notificationsLoadingBar.setVisible(false);
                    notificationsTable.setEnabled(true);
                    updateNotificationCount();
                }
            }
        };
        worker.execute();
    }

    private void markAllAsRead() {
        dbManager.markAllNotificationsAsRead(username);
        loadNotifications();
    }

    private void deleteSelectedNotification() {
        int selectedRow = notificationsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a notification to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String timestampToDelete = dbManager.getUserNotifications(username).get(selectedRow).getString("timestamp");
        dbManager.deleteNotification(username, timestampToDelete);
        loadNotifications();
    }
    
    private void updateNotificationCount() {
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return dbManager.getUnreadNotificationCount(username);
            }
            
            @Override
            protected void done() {
                try {
                    int unreadCount = get();
                    unreadCountLabel.setText(unreadCount + " New");
                    tabbedPane.setTitleAt(2, "My Notifications" + (unreadCount > 0 ? " (" + unreadCount + ")" : ""));
                } catch (Exception e) {
                    System.err.println("Error updating notification count: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void styleTable(JTable table, Color headerBg) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(headerBg);
        table.getTableHeader().setForeground(Color.BLACK); // FIX: Header text color
        table.setSelectionBackground(headerBg.brighter().brighter());
        table.setGridColor(new Color(220, 220, 220));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != 1) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }
    }

    private JButton createActionButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(170, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { 
                if (button.isEnabled()) button.setBackground(bgColor.darker()); 
            }
            public void mouseExited(java.awt.event.MouseEvent evt) { 
                button.setBackground(bgColor); 
            }
        });
        return button;
    }
}
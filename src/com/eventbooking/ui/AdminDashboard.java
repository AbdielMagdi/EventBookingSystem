package com.eventbooking.ui;

import com.eventbooking.database.DatabaseManager;
import com.eventbooking.models.Event;
import com.eventbooking.models.Booking;
import com.eventbooking.services.CreditPointsService;
import com.eventbooking.services.EmailService;
import com.eventbooking.ui.PieChartPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.bson.Document;

public class AdminDashboard extends JFrame {
    private DatabaseManager dbManager;
    private EmailService emailService;
    private String username;
    private JTabbedPane tabbedPane;
    
    private JTable eventTable;
    private DefaultTableModel eventTableModel;
    
    private JTextArea reportsArea;
    
    private JTable leaderboardTable;
    private DefaultTableModel leaderboardModel;
    
    private JTable notificationsTable;
    private DefaultTableModel notificationsModel;
    private JLabel unreadCountLabel;
    
    private JProgressBar eventsLoadingBar;
    private JProgressBar reportsLoadingBar;
    private JProgressBar leaderboardLoadingBar;
    private JProgressBar notificationsLoadingBar;

    public AdminDashboard(String username) {
        this.username = username;
        this.dbManager = new DatabaseManager();
        this.emailService = EmailService.getInstance();
        initializeUI();
    }
    
    public void refreshTable() {
        loadEvents();
    }

    private void initializeUI() {
        setTitle("Admin Dashboard");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(52, 73, 94));
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel welcomeLabel = new JLabel("Welcome, " + username + " (Admin)");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcomeLabel.setForeground(Color.WHITE);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        unreadCountLabel = new JLabel("0 New");
        unreadCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        unreadCountLabel.setForeground(new Color(255, 193, 7));
        unreadCountLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        unreadCountLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tabbedPane.setSelectedIndex(3);
            }
        });
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to logout?", 
                "Confirm Logout", 
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        rightPanel.add(unreadCountLabel);
        rightPanel.add(logoutBtn);
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        tabbedPane.addTab("Manage Events", createEventsPanel());
        tabbedPane.addTab("Reports & Analytics", createReportsPanel());
        tabbedPane.addTab("Leaderboard", createLeaderboardPanel());
        tabbedPane.addTab("Notifications", createNotificationsPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        loadEvents();
        updateNotificationCount();
        
        setVisible(true);
    }

    private JPanel createEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Event Management");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(52, 73, 94));
        titlePanel.add(titleLabel);
        
        panel.add(titlePanel, BorderLayout.NORTH);

        String[] columns = {"ID", "Event Name", "Type", "Date", "Venue", "Available", "Total", "Price ($)", "Status"};
        eventTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        eventTable = new JTable(eventTableModel);
        eventTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        eventTable.setRowHeight(30);
        eventTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        eventTable.getTableHeader().setBackground(new Color(41, 128, 185));
        eventTable.getTableHeader().setForeground(Color.BLACK); 
        eventTable.setSelectionBackground(new Color(52, 73, 94));
        eventTable.setGridColor(new Color(220, 220, 220));
        
        eventTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
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
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        eventTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        eventTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        eventTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        eventTable.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        eventsLoadingBar = new JProgressBar();
        eventsLoadingBar.setIndeterminate(true);
        eventsLoadingBar.setString("Loading events...");
        eventsLoadingBar.setStringPainted(true);
        eventsLoadingBar.setVisible(false);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(eventsLoadingBar, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(eventTable);
        scrollPane.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);

        JButton addBtn = createActionButton("Add Event", new Color(39, 174, 96));
        JButton editBtn = createActionButton("Edit Event", new Color(243, 156, 18));
        JButton refreshBtn = createActionButton("Refresh", new Color(52, 152, 219));
        JButton reminderBtn = createActionButton("Send Reminders", new Color(142, 68, 173));

        addBtn.addActionListener(e -> addEvent());
        editBtn.addActionListener(e -> editEvent());
        refreshBtn.addActionListener(e -> loadEvents());
        reminderBtn.addActionListener(e -> sendEventDayReminders());

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(reminderBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadEvents() {
        SwingWorker<List<Event>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Event> doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    eventsLoadingBar.setVisible(true);
                    eventTable.setEnabled(false);
                });
                return dbManager.getAllEvents();
            }
            
            @Override
            protected void done() {
                try {
                    List<Event> events = get();
                    eventTableModel.setRowCount(0);
                    for (Event event : events) {
                        event.updateStatus();
                        eventTableModel.addRow(new Object[]{
                            event.getId(),
                            event.getName(),
                            event.getType(),
                            event.getDate(),
                            event.getVenue(),
                            event.getSeatsAvailable(),
                            event.getTotalSeats(),
                            String.format("%.2f", event.getPrice()),
                            event.getStatusWithIcon()
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Error loading events: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    eventsLoadingBar.setVisible(false);
                    eventTable.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void addEvent() {
        EventFormDialog dialog = new EventFormDialog(this, null, dbManager);
        dialog.setVisible(true);
        loadEvents();
    }

    private void editEvent() {
        int row = eventTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an event to edit!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int eventId = (Integer) eventTableModel.getValueAt(row, 0);
        Event event = dbManager.getEventById(eventId);
        
        if (event != null) {
            event.updateStatus();
            if (event.isCompleted()) {
                JOptionPane.showMessageDialog(this, "Cannot edit completed events.", "Action Not Allowed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            EventFormDialog dialog = new EventFormDialog(this, event, dbManager);
            dialog.setVisible(true);
            loadEvents();
        }
    }
    
    private void sendEventDayReminders() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "This will send reminder emails to all attendees of events scheduled for today.\nAre you sure you want to proceed?",
            "Confirm Reminders",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                List<Booking> todaysBookings = dbManager.getBookingsForEventsOnDate(LocalDate.now());
                if (todaysBookings.isEmpty()) {
                    return 0;
                }

                int count = 0;
                for (Booking booking : todaysBookings) {
                    Document userDoc = dbManager.getUserDetails(booking.getUsername());
                    Event event = dbManager.getEventById(booking.getEventId());
                    if (userDoc != null && event != null) {
                        emailService.sendEventReminderEmail(userDoc.getString("email"), booking.getUsername(), event);
                        count++;
                    }
                }
                return count;
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    if (count > 0) {
                        JOptionPane.showMessageDialog(AdminDashboard.this,
                            "Successfully sent " + count + " event-day reminders.",
                            "Reminders Sent",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(AdminDashboard.this,
                            "No active bookings found for any events today. No reminders were sent.",
                            "No Bookings Today",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                        "An error occurred while sending reminders: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Sales Reports & Analytics");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(52, 73, 94));
        titlePanel.add(titleLabel);
        panel.add(titlePanel, BorderLayout.NORTH);

        reportsArea = new JTextArea();
        reportsArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        reportsArea.setEditable(false);
        reportsArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        reportsLoadingBar = new JProgressBar();
        reportsLoadingBar.setIndeterminate(true);
        reportsLoadingBar.setString("Generating report...");
        reportsLoadingBar.setStringPainted(true);
        reportsLoadingBar.setVisible(false);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(reportsLoadingBar, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(reportsArea);
        scrollPane.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton generateBtn = createActionButton("Generate Report", new Color(52, 152, 219));
        JButton exportBtn = createActionButton("Export", new Color(39, 174, 96));
        JButton revenueBtn = createActionButton("Revenue Analysis", new Color(142, 68, 173));
        JButton chartBtn = createActionButton("Show Revenue Chart", new Color(26, 188, 156));
        
        generateBtn.addActionListener(e -> generateReport());
        exportBtn.addActionListener(e -> exportReport());
        revenueBtn.addActionListener(e -> showRevenueAnalysis());
        chartBtn.addActionListener(e -> showRevenueChart());
        
        buttonPanel.add(generateBtn);
        buttonPanel.add(exportBtn);
        buttonPanel.add(revenueBtn);
        buttonPanel.add(chartBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void exportReport() {
        if (reportsArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please generate a report first!", "No Report", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String filename = "EventBooking_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
            java.io.FileWriter writer = new java.io.FileWriter(filename, java.nio.charset.StandardCharsets.UTF_8);
            writer.write("EVENT BOOKING SYSTEM - SALES REPORT\n");
            writer.write("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss")) + "\n");
            writer.write("================================================================\n\n");
            writer.write(reportsArea.getText());
            writer.write("\n\n================================================================\nEnd of Report\n================================================================\n");
            writer.close();
            String absolutePath = new java.io.File(filename).getAbsolutePath();
            JOptionPane.showMessageDialog(this, "Report exported successfully!\n\nFile: " + filename + "\nLocation: " + absolutePath, "Export Success", JOptionPane.INFORMATION_MESSAGE);
            dbManager.addAdminNotification(username, "Report exported: " + filename);
            updateNotificationCount();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export report:\n" + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void generateReport() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    reportsLoadingBar.setVisible(true);
                    reportsArea.setText("Generating report, please wait...");
                });
                
                StringBuilder report = new StringBuilder();
                String topBorder = "================================================================\n";
                String separator = "----------------------------------------------------------------\n";
                
                report.append(topBorder).append("                  SALES & ANALYTICS REPORT\n").append(topBorder);
                report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss"))).append("\n\n");

                List<Event> events = dbManager.getAllEvents();
                List<Booking> bookings = dbManager.getAllBookings();
                double totalRevenue = 0;
                int totalTicketsSold = 0;
                int totalActiveBookings = 0;

                report.append("EVENT PERFORMANCE:\n").append(separator);
                for (Event event : events) {
                    event.updateStatus();
                    report.append(String.format("Event: %s [%s]\n", event.getName(), event.getStatus()));
                    report.append(String.format("  - Tickets Sold: %d / %d\n", event.getTicketsSold(), event.getTotalSeats()));
                    report.append(String.format("  - Occupancy Rate: %.1f%%\n", event.getOccupancyRate()));
                    report.append(String.format("  - Revenue: $%.2f\n\n", event.getTotalRevenue()));
                    totalRevenue += event.getTotalRevenue();
                    totalTicketsSold += event.getTicketsSold();
                }

                report.append(topBorder).append("BOOKING STATISTICS:\n").append(separator);
                for (Booking booking : bookings) {
                    if (!booking.isCancelled()) totalActiveBookings++;
                }
                report.append(String.format("Total Bookings: %d\n", bookings.size()));
                report.append(String.format("Active Bookings: %d\n", totalActiveBookings));
                report.append(String.format("Cancelled Bookings: %d\n", bookings.size() - totalActiveBookings));

                report.append("\n").append(topBorder).append("FINANCIAL SUMMARY:\n").append(separator);
                report.append(String.format("Total Tickets Sold: %d\n", totalTicketsSold));
                report.append(String.format("Total Revenue: $%.2f\n", totalRevenue));
                report.append(String.format("Average Revenue per Event: $%.2f\n", events.size() > 0 ? totalRevenue / events.size() : 0));
                report.append(topBorder);
                
                return report.toString();
            }
            
            @Override
            protected void done() {
                try {
                    reportsArea.setText(get());
                    dbManager.addAdminNotification(username, "Sales report generated successfully");
                    updateNotificationCount();
                } catch (Exception e) {
                    reportsArea.setText("Error generating report: " + e.getMessage());
                } finally {
                    reportsLoadingBar.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private void showRevenueAnalysis() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    reportsLoadingBar.setVisible(true);
                    reportsArea.setText("Analyzing revenue data...");
                });
                
                List<Document> topEvents = dbManager.getTopEventsByRevenue(10);
                StringBuilder analysis = new StringBuilder();
                String topBorder = "================================================================\n";
                String separator = "----------------------------------------------------------------\n";
                
                analysis.append(topBorder).append("                     REVENUE ANALYSIS\n").append(topBorder).append("\n");
                analysis.append("TOP 10 EVENTS BY REVENUE:\n").append(separator);
                
                int rank = 1;
                double totalRevenue = 0;
                for (Document doc : topEvents) {
                    analysis.append(String.format("%2d. %-40s $%10.2f (%d tickets)\n", 
                        rank++, doc.getString("eventName"), doc.getDouble("revenue"), doc.getInteger("ticketsSold", 0)));
                    totalRevenue += doc.getDouble("revenue");
                }
                
                analysis.append("\n").append(topBorder);
                analysis.append(String.format("Total Revenue (Top 10): $%.2f\n", totalRevenue));
                analysis.append(String.format("Average Revenue per Event: $%.2f\n", topEvents.size() > 0 ? totalRevenue / topEvents.size() : 0));
                analysis.append(topBorder);
                
                return analysis.toString();
            }
            
            @Override
            protected void done() {
                try {
                    reportsArea.setText(get());
                } catch (Exception e) {
                    reportsArea.setText("Error analyzing revenue: " + e.getMessage());
                } finally {
                    reportsLoadingBar.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private void showRevenueChart() {
        SwingWorker<List<Document>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Document> doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    reportsLoadingBar.setVisible(true);
                    reportsArea.setText("Generating chart data...");
                });
                return dbManager.getRevenueByCategory();
            }

            @Override
            protected void done() {
                try {
                    List<Document> revenueData = get();
                    if (revenueData.isEmpty()) {
                        JOptionPane.showMessageDialog(AdminDashboard.this, "No revenue data found to generate a chart.", "No Data", JOptionPane.INFORMATION_MESSAGE);
                        reportsArea.setText("No revenue data available.");
                        return;
                    }

                    PieChartPanel chartPanel = new PieChartPanel(revenueData);
                    JDialog chartDialog = new JDialog(AdminDashboard.this, "Revenue by Category", true);
                    chartDialog.setContentPane(chartPanel);
                    chartDialog.pack();
                    chartDialog.setLocationRelativeTo(AdminDashboard.this);
                    chartDialog.setVisible(true);
                    reportsArea.setText("Chart displayed successfully.");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AdminDashboard.this, "Failed to generate chart: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    reportsArea.setText("Error generating chart: " + e.getMessage());
                } finally {
                    reportsLoadingBar.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private JPanel createLeaderboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Top Attendees Leaderboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(52, 73, 94));
        titlePanel.add(titleLabel);
        panel.add(titlePanel, BorderLayout.NORTH);

        String[] columns = {"Rank", "Username", "Tickets Purchased", "Events Attended", "Total Spent ($)"};
        leaderboardModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        leaderboardTable = new JTable(leaderboardModel);
        leaderboardTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        leaderboardTable.setRowHeight(35);
        leaderboardTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        leaderboardTable.getTableHeader().setBackground(new Color(230, 126, 34));
        leaderboardTable.getTableHeader().setForeground(Color.BLACK); 
        leaderboardTable.setSelectionBackground(new Color(52, 73, 94));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < leaderboardTable.getColumnCount(); i++) {
            leaderboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        leaderboardLoadingBar = new JProgressBar();
        leaderboardLoadingBar.setIndeterminate(true);
        leaderboardLoadingBar.setString("Loading leaderboard...");
        leaderboardLoadingBar.setStringPainted(true);
        leaderboardLoadingBar.setVisible(false);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(leaderboardLoadingBar, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        scrollPane.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton loadBtn = createActionButton("Load All-Time", new Color(230, 126, 34));
        JButton monthlyBtn = createActionButton("Load Monthly", new Color(52, 152, 219));
        JButton awardDailyBtn = createActionButton("Award Daily Credits", new Color(39, 174, 96));
        
        loadBtn.addActionListener(e -> loadLeaderboard());
        monthlyBtn.addActionListener(e -> showMonthlyRankings());
        awardDailyBtn.addActionListener(e -> awardDailyCreditsManually());
        
        buttonPanel.add(loadBtn);
        buttonPanel.add(monthlyBtn);
        buttonPanel.add(awardDailyBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void awardDailyCreditsManually() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "This will calculate today's top attendees and award them credit points.\nThis task also runs automatically at midnight.\n\nDo you want to proceed with a manual award now?",
            "Confirm Manual Award",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                CreditPointsService.getInstance().awardDailyCredits();
                return null;
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(AdminDashboard.this,
                    "Daily credit points distribution has been completed.\nCheck the console for details.",
                    "Task Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        };
        worker.execute();
    }

    private void loadLeaderboard() {
        SwingWorker<List<Document>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Document> doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    leaderboardLoadingBar.setVisible(true);
                    leaderboardTable.setEnabled(false);
                });
                return dbManager.getTopAttendees(20);
            }
            
            @Override
            protected void done() {
                try {
                    List<Document> topAttendees = get();
                    leaderboardModel.setRowCount(0);
                    int rank = 1;
                    for (Document doc : topAttendees) {
                        // FIX: Safely get double value
                        Double totalSpent = doc.get("totalSpent", Double.class);
                        if (totalSpent == null) totalSpent = 0.0;
                        
                        leaderboardModel.addRow(new Object[]{
                            String.valueOf(rank++),
                            doc.getString("username"),
                            doc.getInteger("ticketsBought", 0),
                            doc.getInteger("eventsAttended", 0),
                            String.format("%.2f", totalSpent)
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    leaderboardLoadingBar.setVisible(false);
                    leaderboardTable.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void showMonthlyRankings() {
        SwingWorker<List<Document>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Document> doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    leaderboardLoadingBar.setVisible(true);
                    leaderboardTable.setEnabled(false);
                });
                return dbManager.getMonthlyTopAttendees(10);
            }
            
            @Override
            protected void done() {
                try {
                    List<Document> monthlyTop = get();
                    leaderboardModel.setRowCount(0);
                    int rank = 1;
                    for (Document doc : monthlyTop) {
                        // FIX: Safely get double value
                        Double monthlySpent = doc.get("monthlySpent", Double.class);
                        if (monthlySpent == null) monthlySpent = 0.0;
                        
                        leaderboardModel.addRow(new Object[]{
                            String.valueOf(rank++),
                            doc.getString("username"),
                            doc.getInteger("monthlyTickets", 0),
                            doc.getInteger("monthlyEvents", 0),
                            String.format("%.2f", monthlySpent)
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    leaderboardLoadingBar.setVisible(false);
                    leaderboardTable.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private JPanel createNotificationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Notification Center");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(52, 73, 94));
        titlePanel.add(titleLabel);
        panel.add(titlePanel, BorderLayout.NORTH);

        String[] columns = {"Status", "Message", "Timestamp"};
        notificationsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        notificationsTable = new JTable(notificationsModel);
        notificationsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        notificationsTable.setRowHeight(40);
        notificationsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        notificationsTable.getTableHeader().setBackground(new Color(155, 89, 182));
        notificationsTable.getTableHeader().setForeground(Color.BLACK); 
        notificationsTable.setSelectionBackground(new Color(52, 73, 94));

        notificationsLoadingBar = new JProgressBar();
        notificationsLoadingBar.setIndeterminate(true);
        notificationsLoadingBar.setString("Loading notifications...");
        notificationsLoadingBar.setStringPainted(true);
        notificationsLoadingBar.setVisible(false);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(notificationsLoadingBar, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(notificationsTable);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220), 1));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton markReadBtn = createActionButton("Mark as Read", new Color(39, 174, 96));
        JButton markAllReadBtn = createActionButton("Mark All Read", new Color(52, 152, 219));
        JButton deleteBtn = createActionButton("Delete", new Color(192, 57, 43));
        JButton refreshBtn = createActionButton("Refresh", new Color(149, 165, 166));
        
        markReadBtn.addActionListener(e -> markSelectedAsRead());
        markAllReadBtn.addActionListener(e -> markAllAsRead());
        deleteBtn.addActionListener(e -> deleteSelectedNotification());
        refreshBtn.addActionListener(e -> loadNotifications());
        
        buttonPanel.add(markReadBtn);
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
                return dbManager.getAdminNotifications(username);
            }
            
            @Override
            protected void done() {
                try {
                    List<Document> notifications = get();
                    notificationsModel.setRowCount(0);
                    for (Document notif : notifications) {
                        Object timestampObj = notif.get("timestamp");
                        String timestampStr;

                        // FIX: Handle both Date and String types for timestamp to prevent ClassCastException
                        if (timestampObj instanceof Date) {
                            timestampStr = ((Date) timestampObj).toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime().toString();
                        } else {
                            timestampStr = timestampObj.toString();
                        }

                        notificationsModel.addRow(new Object[]{
                            notif.getBoolean("read", false) ? "Read" : "New",
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
                }
            }
        };
        worker.execute();
    }

    private void markSelectedAsRead() {
        int row = notificationsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a notification!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String timestamp = (String) notificationsModel.getValueAt(row, 2);
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                dbManager.markNotificationAsRead(username, timestamp);
                return null;
            }
            
            @Override
            protected void done() {
                loadNotifications();
                updateNotificationCount();
            }
        };
        worker.execute();
    }

    private void markAllAsRead() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                dbManager.markAllNotificationsAsRead(username);
                return null;
            }
            
            @Override
            protected void done() {
                loadNotifications();
                updateNotificationCount();
            }
        };
        worker.execute();
    }

    private void deleteSelectedNotification() {
        int row = notificationsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a notification to delete!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String timestamp = (String) notificationsModel.getValueAt(row, 2);
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                dbManager.deleteNotification(username, timestamp);
                return null;
            }
            
            @Override
            protected void done() {
                loadNotifications();
                updateNotificationCount();
            }
        };
        worker.execute();
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
                } catch (Exception e) {
                    System.err.println("Error updating notification count: " + e.getMessage());
                }
            }
        };
        worker.execute();
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
                if(button.isEnabled()) button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
}
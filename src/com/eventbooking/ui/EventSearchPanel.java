// PASTE THIS UPDATED VERSION WITH THREADING INTO EventSearchPanel.java

package com.eventbooking.ui;

import com.eventbooking.database.DatabaseManager;
import com.eventbooking.models.Event;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * A panel that provides search and filter controls for an external JTable.
 * This class is designed to be integrated into another frame (like AttendeeDashboard).
 * UPDATED: Now includes event status in the table AND threading for database operations
 */
public class EventSearchPanel extends JPanel {

    // Controls for filtering
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    
    // References to external components that this panel will control
    private DefaultTableModel tableModel;
    private JTable eventTable;
    
    private DatabaseManager dbManager;
    
    // Loading indicator
    private JLabel statusLabel;

    /**
     * Constructor for the search panel.
     * @param tableModel The table model to update with search results.
     * @param eventTable The table to get the selected row from.
     */
    public EventSearchPanel(DefaultTableModel tableModel, JTable eventTable) {
        this.dbManager = new DatabaseManager();
        this.tableModel = tableModel;
        this.eventTable = eventTable;
        
        initializeUI();
        loadInitialData(); // Load categories and initial event list
    }

    private void initializeUI() {
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        this.setBorder(BorderFactory.createTitledBorder(
            null, "Search & Filter", TitledBorder.DEFAULT_JUSTIFICATION, 
            TitledBorder.DEFAULT_POSITION, new Font("Segoe UI", Font.BOLD, 12))
        );
        this.setBackground(Color.WHITE);

        // Search text field
        this.add(new JLabel("Search by Name/Venue:"));
        searchField = new JTextField(20);
        this.add(searchField);

        // Category filter dropdown
        this.add(new JLabel("Category:"));
        categoryFilter = new JComboBox<>();
        this.add(categoryFilter);

        // Search button
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> applyFilters());
        this.add(searchBtn);

        // Clear button
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilter.setSelectedIndex(0);
            applyFilters();
        });
        this.add(clearBtn);
        
        // Status label for loading indication
        statusLabel = new JLabel("");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(new Color(52, 152, 219));
        this.add(statusLabel);
    }

    /**
     * THREADED: Loads the unique event types into the category filter dropdown
     * and performs an initial search to populate the table.
     */
    public void loadInitialData() {
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            private List<String> types;
            
            @Override
            protected Void doInBackground() throws Exception {
                // Update status
                publish("Loading categories...");
                
                // Fetch event types from database
                types = dbManager.getUniqueEventTypes();
                
                publish("Loading events...");
                
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                // Update status label with progress messages
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }
            
            @Override
            protected void done() {
                try {
                    // Populate category filter on EDT
                    categoryFilter.removeAllItems();
                    categoryFilter.addItem("All");
                    for (String type : types) {
                        categoryFilter.addItem(type);
                    }
                    
                    // Load all events initially
                    applyFilters();
                    
                } catch (Exception e) {
                    System.err.println("Error loading initial data: " + e.getMessage());
                    e.printStackTrace();
                    statusLabel.setText("Error loading data");
                } finally {
                    // Clear status after a delay
                    Timer timer = new Timer(2000, evt -> statusLabel.setText(""));
                    timer.setRepeats(false);
                    timer.start();
                }
            }
        };
        
        worker.execute();
    }

    /**
     * THREADED: Executes the search based on the current filter settings 
     * and updates the table model.
     */
    private void applyFilters() {
        String searchText = searchField.getText();
        String category = (String) categoryFilter.getSelectedItem();
        
        SwingWorker<List<Event>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Event> doInBackground() throws Exception {
                // Update UI to show loading state
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Searching...");
                    eventTable.setEnabled(false);
                });
                
                // Perform search in background
                return dbManager.searchEvents(searchText, category, null, null);
            }
            
            @Override
            protected void done() {
                try {
                    List<Event> events = get();
                    
                    // Clear the existing table data
                    tableModel.setRowCount(0);
                    
                    // Repopulate the table with the search results
                    for (Event event : events) {
                        // IMPORTANT: Update event status before displaying
                        event.updateStatus();
                        
                        tableModel.addRow(new Object[]{
                            event.getId(),
                            event.getName(),
                            event.getType(),
                            event.getDate(),
                            event.getVenue(),
                            event.getSeatsAvailable(),
                            String.format("%.2f", event.getPrice()),
                            event.getStatusWithIcon() // Added status column
                        });
                    }
                    
                    statusLabel.setText(events.size() + " event(s) found");
                    
                    // Clear status after a delay
                    Timer timer = new Timer(3000, evt -> statusLabel.setText(""));
                    timer.setRepeats(false);
                    timer.start();
                    
                } catch (Exception e) {
                    System.err.println("Error applying filters: " + e.getMessage());
                    e.printStackTrace();
                    statusLabel.setText("Search failed");
                } finally {
                    eventTable.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }

    /**
     * Gets the currently selected Event object from the JTable.
     * @return The selected Event, or null if no row is selected.
     */
    public Event getSelectedEvent() {
        int selectedRow = eventTable.getSelectedRow();
        if (selectedRow != -1) {
            // Get the ID from the first column of the selected row
            int eventId = (int) tableModel.getValueAt(selectedRow, 0);
            // Fetch the full event object from the database using its ID
            Event event = dbManager.getEventById(eventId);
            
            // Update status before returning
            if (event != null) {
                event.updateStatus();
            }
            
            return event;
        }
        return null; // No row selected
    }
}
package com.eventbooking.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import com.mongodb.client.*;
import org.bson.Document;
import com.eventbooking.MongoDBConnection;

public class EventForm extends JDialog {
    private JTextField idField, nameField, typeField, dateField, venueField, seatsField, priceField;
    private JButton saveBtn;
    private MongoCollection<Document> eventsCollection;
    private AdminDashboard parent;
    private Document editingDoc;

    public EventForm(AdminDashboard parent, Document doc) {
        super(parent, "Event Form", true);
        this.parent = parent;
        this.editingDoc = doc;

        setSize(400, 400);
        setLayout(new GridLayout(8,2,5,5));
        setLocationRelativeTo(parent);

        eventsCollection = MongoDBConnection.getDatabase().getCollection("events");

        add(new JLabel("ID:"));
        idField = new JTextField();
        add(idField);

        add(new JLabel("Name:"));
        nameField = new JTextField();
        add(nameField);

        add(new JLabel("Type:"));
        typeField = new JTextField();
        add(typeField);

        add(new JLabel("Date (YYYY-MM-DD):"));
        dateField = new JTextField();
        add(dateField);

        add(new JLabel("Venue:"));
        venueField = new JTextField();
        add(venueField);

        add(new JLabel("Total Seats:"));
        seatsField = new JTextField();
        add(seatsField);

        add(new JLabel("Price:"));
        priceField = new JTextField();
        add(priceField);

        saveBtn = new JButton("Save");
        add(saveBtn);

        if(editingDoc != null) { // populate fields for edit
            idField.setText(editingDoc.getInteger("id").toString());
            idField.setEnabled(false);
            nameField.setText(editingDoc.getString("name"));
            typeField.setText(editingDoc.getString("type"));
            dateField.setText(editingDoc.getString("date"));
            venueField.setText(editingDoc.getString("venue"));
            seatsField.setText(editingDoc.getInteger("totalSeats").toString());
            priceField.setText(editingDoc.getDouble("price").toString());
        }

        saveBtn.addActionListener(e -> saveEvent());
    }

    private void saveEvent() {
        try {
            int id = Integer.parseInt(idField.getText());
            String name = nameField.getText();
            String type = typeField.getText();
            String date = dateField.getText();
            String venue = venueField.getText();
            int seats = Integer.parseInt(seatsField.getText());
            double price = Double.parseDouble(priceField.getText());

            Document doc = new Document("id", id)
                    .append("name", name)
                    .append("type", type)
                    .append("date", date)
                    .append("venue", venue)
                    .append("totalSeats", seats)
                    .append("price", price);

            if(editingDoc != null) {
                eventsCollection.replaceOne(new Document("id", id), doc);
                JOptionPane.showMessageDialog(this, "Event updated!");
            } else {
                eventsCollection.insertOne(doc);
                JOptionPane.showMessageDialog(this, "Event added!");
            }

            parent.refreshTable();
            dispose();
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}

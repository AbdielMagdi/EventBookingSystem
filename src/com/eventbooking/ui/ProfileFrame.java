package com.eventbooking.ui;

import com.eventbooking.database.DatabaseManager;
import com.eventbooking.services.CreditPointsService;

// ADDED: Explicit import for clarity, though it should work without it in the same package.
// This can help resolve environment/compiler issues.
import com.eventbooking.ui.CircularImagePanel; 

import org.bson.Document;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class ProfileFrame extends JDialog {

    private String username;
    private DatabaseManager dbManager;
    private String currentProfileImagePath;

    // UI Components
    private CircularImagePanel imagePanel;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JLabel ticketsBoughtLabel;
    private JLabel eventsAttendedLabel;
    private JLabel creditPointsLabel;
    private JLabel discountLabel;

    public ProfileFrame(Frame owner, String username) {
        super(owner, "My Profile", true);
        this.username = username;
        this.dbManager = new DatabaseManager();

        initializeUI();
        loadUserData();
    }

    private void initializeUI() {
        setSize(550, 700);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // --- Profile Image Section ---
        JPanel profileImageSection = new JPanel();
        profileImageSection.setLayout(new BoxLayout(profileImageSection, BoxLayout.Y_AXIS));
        profileImageSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        profileImageSection.setOpaque(false);

        imagePanel = new CircularImagePanel(150);
        imagePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton changePictureBtn = createStyledButton("Change Picture", new Color(149, 165, 166));
        changePictureBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changePictureBtn.addActionListener(e -> changeProfilePicture());

        profileImageSection.add(imagePanel);
        profileImageSection.add(Box.createVerticalStrut(10));
        profileImageSection.add(changePictureBtn);

        mainPanel.add(profileImageSection);
        mainPanel.add(Box.createVerticalStrut(20));

        // --- Edit Profile Details Panel ---
        JPanel detailsPanel = createTitledPanel("Edit Profile");
        detailsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        detailsPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        emailField = new JTextField(20);
        detailsPanel.add(emailField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        detailsPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        phoneField = new JTextField(20);
        detailsPanel.add(phoneField, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        JButton updateDetailsBtn = createStyledButton("Update Details", new Color(52, 152, 219));
        detailsPanel.add(updateDetailsBtn, gbc);

        // --- Change Password Panel ---
        JPanel passwordPanel = createTitledPanel("Change Password");
        passwordPanel.setLayout(new GridBagLayout());
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.WEST;
        passwordPanel.add(new JLabel("Old Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        oldPasswordField = new JPasswordField(20);
        passwordPanel.add(oldPasswordField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        passwordPanel.add(new JLabel("New Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        newPasswordField = new JPasswordField(20);
        passwordPanel.add(newPasswordField, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        passwordPanel.add(new JLabel("Confirm New Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        confirmPasswordField = new JPasswordField(20);
        passwordPanel.add(confirmPasswordField, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        JButton changePasswordBtn = createStyledButton("Change Password", new Color(231, 76, 60));
        passwordPanel.add(changePasswordBtn, gbc);

        // --- Statistics Panel ---
        JPanel statsPanel = createTitledPanel("My Statistics");
        statsPanel.setLayout(new GridLayout(4, 2, 10, 10));
        statsPanel.add(new JLabel("Total Tickets Purchased:"));
        ticketsBoughtLabel = new JLabel("0");
        ticketsBoughtLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statsPanel.add(ticketsBoughtLabel);
        statsPanel.add(new JLabel("Events Attended:"));
        eventsAttendedLabel = new JLabel("0");
        eventsAttendedLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statsPanel.add(eventsAttendedLabel);
        statsPanel.add(new JLabel("Credit Points Balance:"));
        creditPointsLabel = new JLabel("0");
        creditPointsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        creditPointsLabel.setForeground(new Color(155, 89, 182));
        statsPanel.add(creditPointsLabel);
        statsPanel.add(new JLabel("Available Discount:"));
        discountLabel = new JLabel("$0.00");
        discountLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        discountLabel.setForeground(new Color(46, 204, 113));
        statsPanel.add(discountLabel);

        mainPanel.add(detailsPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(passwordPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(statsPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        JPanel creditInfoPanel = createTitledPanel("Credit Points Information");
        creditInfoPanel.setLayout(new BorderLayout(10, 10));

        JTextArea creditInfo = new JTextArea(
            "HOW TO EARN CREDIT POINTS:\n\n" +
            "• Points are awarded monthly based on your ranking\n" +
            "• Top purchasers get more points\n" +
            "• Rank #1: 1000 points | Rank #2: 750 points | Rank #3: 500 points\n\n" +
            "HOW TO USE POINTS:\n\n" +
            "• 1 point = $0.10 discount on bookings\n" +
            "• Apply points during checkout\n" +
            "• No expiration on points!"
        );
        creditInfo.setEditable(false);
        creditInfo.setWrapStyleWord(true);
        creditInfo.setLineWrap(true);
        creditInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        creditInfo.setBackground(new Color(240, 248, 255));
        creditInfo.setBorder(new EmptyBorder(10, 10, 10, 10));

        creditInfoPanel.add(creditInfo, BorderLayout.CENTER);
        mainPanel.add(creditInfoPanel);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);

        updateDetailsBtn.addActionListener(e -> updateUserDetails());
        changePasswordBtn.addActionListener(e -> changePassword());
    }

    private void loadUserData() {
        Document userDoc = dbManager.getUserDetails(username);
        if (userDoc != null) {
            emailField.setText(userDoc.getString("email"));
            phoneField.setText(userDoc.getString("phone"));

            // FIXED: Changed .toString() to String.valueOf() to convert primitive int to String
            ticketsBoughtLabel.setText(String.valueOf(userDoc.getInteger("ticketsBought", 0)));
            eventsAttendedLabel.setText(String.valueOf(userDoc.getInteger("eventsAttended", 0)));
            
            int creditPoints = userDoc.getInteger("creditPoints", 0);
            creditPointsLabel.setText(String.valueOf(creditPoints));
            
            CreditPointsService creditService = CreditPointsService.getInstance();
            double availableDiscount = creditService.calculateDiscount(creditPoints);
            discountLabel.setText(String.format("$%.2f", availableDiscount));
            
            currentProfileImagePath = userDoc.getString("profileImagePath");
            imagePanel.loadImage(currentProfileImagePath);
            
        } else {
            JOptionPane.showMessageDialog(this, "Could not load user data for '" + username + "'.", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void updateUserDetails() {
        String newEmail = emailField.getText().trim();
        String newPhone = phoneField.getText().trim();

        if (newEmail.isEmpty() || !newEmail.contains("@")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPhone.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid 10-digit phone number.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (dbManager.isEmailInUse(newEmail, username)) {
            JOptionPane.showMessageDialog(this, "This email is already registered to another account.", "Email In Use", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // CHANGED: Added currentProfileImagePath to the method call to match the updated DatabaseManager
        boolean success = dbManager.updateUserProfile(username, newEmail, newPhone, currentProfileImagePath);
        if (success) {
            JOptionPane.showMessageDialog(this, "Profile details updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update profile.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void changePassword() {
        String oldPassword = new String(oldPasswordField.getPassword());
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all password fields.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newPassword.length() < 4) {
            JOptionPane.showMessageDialog(this, "New password must be at least 4 characters long.", "Password Too Short", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Password Mismatch", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (dbManager.changePassword(username, oldPassword, newPassword)) {
            JOptionPane.showMessageDialog(this, "Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            oldPasswordField.setText("");
            newPasswordField.setText("");
            confirmPasswordField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to change password. Please check your old password.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void changeProfilePicture() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg", "gif");
        fileChooser.setFileFilter(filter);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            currentProfileImagePath = selectedFile.getAbsolutePath();
            
            imagePanel.loadImage(currentProfileImagePath);
            
            JOptionPane.showMessageDialog(this, "Image selected. Click 'Update Details' to save your new profile picture.", "Image Selected", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(220, 220, 220)),
            title,
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(70, 70, 70)
        ));
        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 35));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { button.setBackground(bgColor.darker()); }
            public void mouseExited(java.awt.event.MouseEvent evt) { button.setBackground(bgColor); }
        });
        return button;
    }
}
// PASTE THIS FULL CODE INTO LoginFrame.java

package com.eventbooking.ui;

import com.eventbooking.database.DatabaseManager;
import com.eventbooking.models.User;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Enhanced Login Frame with modern UI
 */
public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;
    private DatabaseManager dbManager;

    public LoginFrame() {
        dbManager = new DatabaseManager();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Event Booking System");
        // MODIFIED: Increased window size for better layout
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Main panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(74, 144, 226), 0, h, new Color(97, 178, 239));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new GridBagLayout());
        
        // Login card panel
        JPanel loginPanel = new JPanel();
        loginPanel.setBackground(Color.WHITE);
        loginPanel.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 200, 200), 1, true),
            new EmptyBorder(30, 40, 30, 40)
        ));
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setPreferredSize(new Dimension(350, 450));

        // Title
        JLabel titleLabel = new JLabel("Event Booking System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(74, 144, 226));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Login to your account");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginPanel.add(titleLabel);
        loginPanel.add(Box.createVerticalStrut(5));
        loginPanel.add(subtitleLabel);
        loginPanel.add(Box.createVerticalStrut(30));

        // Username field
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(270, 35));
        usernameField.setMaximumSize(new Dimension(270, 35));
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        usernameField.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(5, 10, 5, 10)
        ));

        loginPanel.add(usernameLabel);
        loginPanel.add(Box.createVerticalStrut(5));
        loginPanel.add(usernameField);
        loginPanel.add(Box.createVerticalStrut(15));

        // Password field
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(270, 35));
        passwordField.setMaximumSize(new Dimension(270, 35));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(5, 10, 5, 10)
        ));

        loginPanel.add(passwordLabel);
        loginPanel.add(Box.createVerticalStrut(5));
        loginPanel.add(passwordField);
        loginPanel.add(Box.createVerticalStrut(15));

        // Role selection
        JLabel roleLabel = new JLabel("Login As");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        roleCombo = new JComboBox<>(new String[] {"Attendee", "Admin"});
        roleCombo.setPreferredSize(new Dimension(270, 35));
        roleCombo.setMaximumSize(new Dimension(270, 35));
        roleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        loginPanel.add(roleLabel);
        loginPanel.add(Box.createVerticalStrut(5));
        loginPanel.add(roleCombo);
        loginPanel.add(Box.createVerticalStrut(25));

        // Login button
        JButton loginBtn = createStyledButton("Login", new Color(74, 144, 226));
        loginBtn.addActionListener(e -> login());

        loginPanel.add(loginBtn);
        loginPanel.add(Box.createVerticalStrut(15));

        // Register button
        JButton registerBtn = createStyledButton("Register New Account", new Color(46, 204, 113));
        registerBtn.addActionListener(e -> openRegistration());

        loginPanel.add(registerBtn);

        mainPanel.add(loginPanel);
        add(mainPanel);
        setVisible(true);
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(270, 40));
        button.setMaximumSize(new Dimension(270, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter username and password!", 
                "Input Required", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = dbManager.authenticateUser(username, password, role);
        
        if (user != null) {
            JOptionPane.showMessageDialog(this, 
                "Login successful! Welcome " + username, 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            user.openDashboard();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Invalid credentials! Please try again.", 
                "Login Failed", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegistration() {
        new RegistrationFrame(this);
        setVisible(false);
    }

    public void showLoginFrame() {
        setVisible(true);
    }
}
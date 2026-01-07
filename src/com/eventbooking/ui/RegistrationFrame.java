// PASTE THIS FULL AND CORRECTED CODE INTO RegistrationFrame.java

package com.eventbooking.ui;

import com.eventbooking.database.DatabaseManager;
import com.eventbooking.services.OTPService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Registration Frame with OTP Verification
 */
public class RegistrationFrame extends JFrame {
    private JTextField usernameField, emailField, phoneField, otpField;
    private JPasswordField passwordField, confirmPasswordField;
    private JButton sendOTPBtn, verifyOTPBtn, registerBtn;
    private DatabaseManager dbManager;
    private OTPService otpService;
    private LoginFrame loginFrame;
    private boolean otpVerified = false;

    public RegistrationFrame(LoginFrame loginFrame) {
        this.loginFrame = loginFrame;
        this.dbManager = new DatabaseManager();
        this.otpService = OTPService.getInstance();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Register New Account");
        setSize(500, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                loginFrame.showLoginFrame();
            }
        });

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(46, 204, 113), 0, getHeight(), new Color(39, 174, 96));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new GridBagLayout());

        JPanel regPanel = new JPanel();
        regPanel.setBackground(Color.WHITE);
        regPanel.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 200, 200), 1, true),
            new EmptyBorder(30, 40, 30, 40)
        ));
        regPanel.setLayout(new BoxLayout(regPanel, BoxLayout.Y_AXIS));
        regPanel.setPreferredSize(new Dimension(350, 650));

        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(46, 204, 113));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        regPanel.add(titleLabel);
        regPanel.add(Box.createVerticalStrut(20));

        // Username
        regPanel.add(createLabel("Username"));
        usernameField = createTextField();
        regPanel.add(usernameField);
        regPanel.add(Box.createVerticalStrut(10));

        // Email
        regPanel.add(createLabel("Email"));
        emailField = createTextField();
        regPanel.add(emailField);
        regPanel.add(Box.createVerticalStrut(10));

        // --- LAYOUT CORRECTION START ---
        // Using a more robust BoxLayout with X_AXIS for horizontal alignment
        
        // Phone number + Send OTP Button
        regPanel.add(createLabel("Phone Number (10 digits)"));
        JPanel phonePanel = new JPanel();
        phonePanel.setLayout(new BoxLayout(phonePanel, BoxLayout.X_AXIS));
        phonePanel.setBackground(Color.WHITE);
        phonePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        phoneField = new JTextField(15);
        styleTextField(phoneField);
        
        sendOTPBtn = createSmallButton("Send OTP", new Color(52, 152, 219));
        
        phonePanel.add(phoneField);
        phonePanel.add(Box.createHorizontalStrut(5)); // Adds a small gap
        phonePanel.add(sendOTPBtn);
        regPanel.add(phonePanel);
        regPanel.add(Box.createVerticalStrut(10));

        // OTP Field + Verify Button
        regPanel.add(createLabel("Enter OTP"));
        JPanel otpPanel = new JPanel();
        otpPanel.setLayout(new BoxLayout(otpPanel, BoxLayout.X_AXIS));
        otpPanel.setBackground(Color.WHITE);
        otpPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        otpField = new JTextField(15);
        styleTextField(otpField);
        otpField.setEnabled(false);

        verifyOTPBtn = createSmallButton("Verify", new Color(46, 204, 113));
        verifyOTPBtn.setEnabled(false);
        
        otpPanel.add(otpField);
        otpPanel.add(Box.createHorizontalStrut(5)); // Adds a small gap
        otpPanel.add(verifyOTPBtn);
        regPanel.add(otpPanel);
        regPanel.add(Box.createVerticalStrut(10));
        
        // --- LAYOUT CORRECTION END ---

        // Add action listeners after components are created
        sendOTPBtn.addActionListener(e -> sendOTP());
        verifyOTPBtn.addActionListener(e -> verifyOTP());
        
        // Password
        regPanel.add(createLabel("Password"));
        passwordField = new JPasswordField();
        styleTextField(passwordField);
        regPanel.add(passwordField);
        regPanel.add(Box.createVerticalStrut(10));

        // Confirm Password
        regPanel.add(createLabel("Confirm Password"));
        confirmPasswordField = new JPasswordField();
        styleTextField(confirmPasswordField);
        regPanel.add(confirmPasswordField);
        regPanel.add(Box.createVerticalStrut(20));

        // Register button
        registerBtn = createStyledButton("Register", new Color(46, 204, 113));
        registerBtn.setEnabled(false); // Starts disabled
        registerBtn.addActionListener(e -> register());
        regPanel.add(registerBtn);
        regPanel.add(Box.createVerticalStrut(10));

        // Back button
        JButton backBtn = createStyledButton("Back to Login", new Color(149, 165, 166));
        backBtn.addActionListener(e -> {
            loginFrame.showLoginFrame();
            dispose();
        });
        regPanel.add(backBtn);

        mainPanel.add(regPanel);
        add(mainPanel);
        setVisible(true);
    }
    
    // All the logic methods (sendOTP, verifyOTP, register) and helper methods are the same
    private void sendOTP() {
        String phone = phoneField.getText().trim();
        if (!phone.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid 10-digit phone number!", "Invalid Phone", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (otpService.sendOTP(phone)) {
            JOptionPane.showMessageDialog(this, "OTP sent to " + phone + "\n(Check console for OTP in this simulation)", "OTP Sent", JOptionPane.INFORMATION_MESSAGE);
            otpField.setEnabled(true);
            verifyOTPBtn.setEnabled(true);
            sendOTPBtn.setEnabled(false);
            phoneField.setEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to send OTP. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void verifyOTP() {
        String phone = phoneField.getText().trim();
        String otp = otpField.getText().trim();
        if (otp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the OTP!", "OTP Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (otpService.verifyOTP(phone, otp)) {
            otpVerified = true;
            JOptionPane.showMessageDialog(this, "Phone number verified successfully!", "Verification Success", JOptionPane.INFORMATION_MESSAGE);
            registerBtn.setEnabled(true);
            otpField.setEnabled(false);
            verifyOTPBtn.setEnabled(false);
            otpField.setBackground(new Color(212, 237, 218));
        } else {
            JOptionPane.showMessageDialog(this, "Invalid or expired OTP! Please try again.", "Verification Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void register() {
        if (!otpVerified) {
            JOptionPane.showMessageDialog(this, "Please verify your phone number first!", "Verification Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match!", "Password Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password must be at least 4 characters!", "Password Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (dbManager.registerUser(username, password, email, phone)) {
            JOptionPane.showMessageDialog(this, "Registration successful! You can now login.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loginFrame.showLoginFrame();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Username already exists! Please choose another.", "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
    private JTextField createTextField() {
        JTextField field = new JTextField();
        styleTextField(field);
        return field;
    }
    private void styleTextField(JComponent field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(new LineBorder(new Color(200, 200, 200), 1), new EmptyBorder(5, 10, 5, 10)));
        // Setting a max size is important for BoxLayout
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
    }
    private JButton createSmallButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Set fixed size for the small buttons
        Dimension size = new Dimension(90, 35);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        return button;
    }
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Dimension size = new Dimension(270, 40);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { if (button.isEnabled()) button.setBackground(bgColor.darker()); }
            public void mouseExited(java.awt.event.MouseEvent evt) { button.setBackground(bgColor); }
        });
        return button;
    }
}
package com.eventbooking.models;

public abstract class User {
    protected String username;
    protected String password;
    protected String email;
    protected String phone;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String username, String password, String email, String phone) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
    }

    // Getters demonstrating ENCAPSULATION
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    // Setters
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean checkPassword(String input) {
        return password.equals(input);
    }

    // Abstract method to be overridden - demonstrates POLYMORPHISM
    public abstract void openDashboard();
    
    // Abstract method to get user role
    public abstract String getRole();
}
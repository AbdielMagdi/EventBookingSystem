package com.eventbooking.models;

import com.eventbooking.ui.AdminDashboard;

public class Admin extends User {
    
    public Admin(String username, String password) {
        super(username, password);
    }
    
    public Admin(String username, String password, String email, String phone) {
        super(username, password, email, phone);
    }

    @Override
    public String getRole() {
        return "Admin";
    }

    @Override
    public void openDashboard() {
        new AdminDashboard(username);
    }
}
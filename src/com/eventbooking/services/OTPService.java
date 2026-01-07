package com.eventbooking.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.time.LocalDateTime;

/**
 * OTP Service for mobile verification
 * This is a simulation. For production, integrate with Twilio, AWS SNS, or similar
 */
public class OTPService {
    private static OTPService instance;
    private Map<String, OTPData> otpStore;
    private Random random;
    private static final int OTP_EXPIRY_MINUTES = 5;

    private OTPService() {
        otpStore = new HashMap<>();
        random = new Random();
    }

    public static OTPService getInstance() {
        if (instance == null) {
            instance = new OTPService();
        }
        return instance;
    }

    /**
     * Generate and send OTP to mobile number
     * @param phoneNumber - 10 digit mobile number
     * @return true if OTP sent successfully
     */
    public boolean sendOTP(String phoneNumber) {
        // Validate phone number
        if (phoneNumber == null || !phoneNumber.matches("\\d{10}")) {
            return false;
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1000000));
        
        // Store OTP with timestamp
        otpStore.put(phoneNumber, new OTPData(otp, LocalDateTime.now()));

        // SIMULATION: Print OTP to console
        // In production, replace this with actual SMS API call
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║     OTP VERIFICATION MESSAGE     ║");
        System.out.println("╠══════════════════════════════════╣");
        System.out.println("║ Phone: " + phoneNumber + "               ║");
        System.out.println("║ OTP Code: " + otp + "                  ║");
        System.out.println("║ Valid for: " + OTP_EXPIRY_MINUTES + " minutes             ║");
        System.out.println("╚══════════════════════════════════╝");

        return true;
    }

    /**
     * Verify OTP entered by user
     * @param phoneNumber - mobile number
     * @param enteredOTP - OTP entered by user
     * @return true if OTP is valid and not expired
     */
    public boolean verifyOTP(String phoneNumber, String enteredOTP) {
        if (!otpStore.containsKey(phoneNumber)) {
            return false;
        }

        OTPData otpData = otpStore.get(phoneNumber);
        
        // Check if OTP expired
        if (otpData.timestamp.plusMinutes(OTP_EXPIRY_MINUTES).isBefore(LocalDateTime.now())) {
            otpStore.remove(phoneNumber);
            return false;
        }

        // Verify OTP
        boolean isValid = otpData.otp.equals(enteredOTP);
        
        if (isValid) {
            // Remove OTP after successful verification
            otpStore.remove(phoneNumber);
        }

        return isValid;
    }

    /**
     * Clear expired OTPs (can be called periodically)
     */
    public void clearExpiredOTPs() {
        LocalDateTime now = LocalDateTime.now();
        otpStore.entrySet().removeIf(entry -> 
            entry.getValue().timestamp.plusMinutes(OTP_EXPIRY_MINUTES).isBefore(now));
    }

    /**
     * Inner class to store OTP with timestamp
     */
    private static class OTPData {
        String otp;
        LocalDateTime timestamp;

        OTPData(String otp, LocalDateTime timestamp) {
            this.otp = otp;
            this.timestamp = timestamp;
        }
    }
}
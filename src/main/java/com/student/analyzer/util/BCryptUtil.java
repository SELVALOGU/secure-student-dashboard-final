package com.student.analyzer.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCryptUtil - Utility class for password hashing using BCrypt.
 * BCrypt is slow by design, providing strong protection against brute force attacks.
 */
public class BCryptUtil {

    // Cost factor: 12 means 2^12 hashing rounds (balance between security and speed)
    private static final int COST = 12;

    /**
     * Hashes a plain-text password using BCrypt.
     *
     * @param plainPassword The raw password to hash
     * @return BCrypt hashed password string
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST));
    }

    /**
     * Verifies a plain-text password against a BCrypt hash.
     *
     * @param plainPassword  The raw password entered by user
     * @param hashedPassword The stored BCrypt hash
     * @return true if passwords match, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            System.err.println("[BCryptUtil] Error verifying password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Main method for testing — generate hashes for sample data.
     * Usage: java BCryptUtil
     */
    public static void main(String[] args) {
        String[] passwords = {"Admin@123", "Staff@123", "Student@123"};
        for (String pw : passwords) {
            System.out.println("Password: " + pw + " -> Hash: " + hashPassword(pw));
        }
    }
}

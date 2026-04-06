package com.student.analyzer.test;

import com.student.analyzer.dao.UserDAO;
import com.student.analyzer.model.User;
import com.student.analyzer.util.BCryptUtil;

public class LoginTest {
    public static void main(String[] args) {
        try {
            UserDAO userDAO = new UserDAO();
            String email = "admin@school.edu";
            String rawPassword = "Admin@123";
            
            System.out.println("Testing login for: " + email);
            User user = userDAO.findByEmail(email);
            
            if (user == null) {
                System.out.println("ERROR: User not found in database.");
                return;
            }
            
            System.out.println("Found user: " + user.getName());
            System.out.println("Stored Hash: " + user.getPasswordHash());
            
            boolean match = BCryptUtil.verifyPassword(rawPassword, user.getPasswordHash());
            System.out.println("Password Match: " + match);
            
            if (!match) {
                System.out.println("Generating a new hash for '" + rawPassword + "' to see what it should look like:");
                System.out.println(BCryptUtil.hashPassword(rawPassword));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

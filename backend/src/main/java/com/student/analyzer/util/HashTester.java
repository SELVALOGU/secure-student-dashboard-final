package com.student.analyzer.util;

import org.mindrot.jbcrypt.BCrypt;

public class HashTester {
    public static void main(String[] args) {
        String password = "Admin@123";
        String hash = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.s5uhom";
        
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        boolean match = BCrypt.checkpw(password, hash);
        System.out.println("Match: " + match);
        
        String generated = BCrypt.hashpw(password, BCrypt.gensalt(12));
        System.out.println("Generated check: " + BCrypt.checkpw(password, generated));
    }
}

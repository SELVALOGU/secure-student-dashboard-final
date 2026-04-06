package com.student.analyzer.test;

import org.mindrot.jbcrypt.BCrypt;

public class HashGenerator {
    public static void main(String[] args) {
        String[] passwords = {"Admin@123", "Staff@123", "Student@123"};
        for(String pwd : passwords) {
            System.out.println(pwd + " -> " + BCrypt.hashpw(pwd, BCrypt.gensalt(12)));
        }
    }
}

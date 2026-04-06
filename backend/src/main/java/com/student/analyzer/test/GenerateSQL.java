package com.student.analyzer.test;

import java.io.FileWriter;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class GenerateSQL {
    public static void main(String[] args) {
        try {
            String adminHash = BCrypt.hashpw("Admin@123", BCrypt.gensalt(12));
            String staffHash = BCrypt.hashpw("Staff@123", BCrypt.gensalt(12));
            String studentHash = BCrypt.hashpw("Student@123", BCrypt.gensalt(12));

            String sql = "-- ============================================================\n" +
                         "-- SecureEdu Master Password Reset Script\n" +
                         "-- ============================================================\n\n" +
                         "USE student_analyzer;\n\n" +
                         "SET FOREIGN_KEY_CHECKS = 0;\n" +
                         "TRUNCATE TABLE unblock_requests;\n" +
                         "TRUNCATE TABLE marks;\n" +
                         "TRUNCATE TABLE student_subjects;\n" +
                         "TRUNCATE TABLE subjects;\n" +
                         "TRUNCATE TABLE students;\n" +
                         "TRUNCATE TABLE staff;\n" +
                         "TRUNCATE TABLE login_attempts;\n" +
                         "TRUNCATE TABLE users;\n" +
                         "SET FOREIGN_KEY_CHECKS = 1;\n\n" +
                         "INSERT INTO users (id, name, email, password_hash, role, login_score, status) VALUES\n" +
                         "(1, 'System Admin', 'admin@school.edu', '" + adminHash + "', 'admin', 100, 'active'),\n" +
                         "(2, 'Dr. Priya Sharma', 'priya.sharma@school.edu', '" + staffHash + "', 'staff', 100, 'active'),\n" +
                         "(3, 'Arjun Nair', 'arjun.nair@student.edu', '" + studentHash + "', 'student', 100, 'active');\n\n" +
                         "INSERT INTO staff (user_id, department, designation) VALUES (2, 'Computer Science', 'Associate Professor');\n" +
                         "INSERT INTO students (user_id, roll_number, department, semester) VALUES (3, 'CS2021001', 'Computer Science', 4);\n\n" +
                         "SELECT 'PASSWORDS RESET! Use Admin@123, Staff@123, or Student@123.' AS message;\n";

            FileWriter writer = new FileWriter("master_reset.sql");
            writer.write(sql);
            writer.close();

            System.out.println("master_reset.sql generated successfully with valid BCrypt hashes.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

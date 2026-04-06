-- ============================================================
-- SecureEdu Master Password Reset Script
-- ============================================================

USE student_analyzer;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE unblock_requests;
TRUNCATE TABLE marks;
TRUNCATE TABLE student_subjects;
TRUNCATE TABLE subjects;
TRUNCATE TABLE students;
TRUNCATE TABLE staff;
TRUNCATE TABLE login_attempts;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (id, name, email, password_hash, role, login_score, status) VALUES
(1, 'System Admin', 'admin@school.edu', '$2a$12$bNqJeXD58AUa0Ke2lrtQUO74s2p1T/R3j5i5qfmcfZ56Aj5Sua5Ym', 'admin', 100, 'active'),
(2, 'Dr. Priya Sharma', 'priya.sharma@school.edu', '$2a$12$tLHI381exlVJ2Lg0FTlezeZY8WLvmJPd9H.wj5ZvEJXk5qojiph1W', 'staff', 100, 'active'),
(3, 'Arjun Nair', 'arjun.nair@student.edu', '$2a$12$W/uf/Akla9jPzYShOnEUYuKiyxxNwzvd8wHWWPwj.9ggVNTc5h/Ri', 'student', 100, 'active');

INSERT INTO staff (user_id, department, designation) VALUES (2, 'Computer Science', 'Associate Professor');
INSERT INTO students (user_id, roll_number, department, semester) VALUES (3, 'CS2021001', 'Computer Science', 4);

SELECT 'PASSWORDS RESET! Use Admin@123, Staff@123, or Student@123.' AS message;

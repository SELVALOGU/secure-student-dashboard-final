-- ============================================================
-- Secure Student Login Behavior Analyzer - Database Setup
-- ============================================================
-- Run this script in MySQL as root or admin user
-- Usage: mysql -u root -p < setup.sql
-- ============================================================

DROP DATABASE IF EXISTS student_analyzer;
CREATE DATABASE student_analyzer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE student_analyzer;

-- ============================================================
-- TABLE: users (base authentication table for all roles)
-- ============================================================
CREATE TABLE users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        ENUM('student','staff','admin') NOT NULL DEFAULT 'student',
    login_score INT NOT NULL DEFAULT 100,
    status      ENUM('active','blocked','suspended') NOT NULL DEFAULT 'active',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login  DATETIME,
    INDEX idx_email (email),
    INDEX idx_role  (role),
    INDEX idx_status (status)
);

-- ============================================================
-- TABLE: login_attempts (tracks every login attempt)
-- ============================================================
CREATE TABLE login_attempts (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    email       VARCHAR(150),
    timestamp   DATETIME DEFAULT CURRENT_TIMESTAMP,
    ip_address  VARCHAR(45),
    device      VARCHAR(255),
    status      ENUM('success','failure','blocked') NOT NULL,
    risk_level  ENUM('low','medium','high') NOT NULL DEFAULT 'low',
    fail_reason VARCHAR(255),
    INDEX idx_user_id   (user_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_risk_level (risk_level),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ============================================================
-- TABLE: students (extended student profile)
-- ============================================================
CREATE TABLE students (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL UNIQUE,
    staff_id    INT,
    attendance  DECIMAL(5,2) DEFAULT 0.00,
    grade       VARCHAR(5) DEFAULT 'N/A',
    roll_number VARCHAR(20),
    department  VARCHAR(100),
    semester    INT DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: staff (extended staff profile)
-- ============================================================
CREATE TABLE staff (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL UNIQUE,
    department  VARCHAR(100),
    designation VARCHAR(100) DEFAULT 'Lecturer',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: subjects
-- ============================================================
CREATE TABLE subjects (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    subject_name VARCHAR(150) NOT NULL,
    subject_code VARCHAR(20) NOT NULL UNIQUE,
    staff_id     INT NOT NULL,
    credits      INT DEFAULT 3,
    INDEX idx_staff_id (staff_id),
    FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: student_subjects (enrollment)
-- ============================================================
CREATE TABLE student_subjects (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    subject_id INT NOT NULL,
    status     ENUM('pending','accepted','rejected') DEFAULT 'pending',
    enrolled_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_enrollment (student_id, subject_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: marks
-- ============================================================
CREATE TABLE marks (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    student_id  INT NOT NULL,
    subject_id  INT NOT NULL,
    marks       DECIMAL(5,2) DEFAULT 0.00,
    max_marks   DECIMAL(5,2) DEFAULT 100.00,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_marks (student_id, subject_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: unblock_requests
-- ============================================================
CREATE TABLE unblock_requests (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    student_id  INT NOT NULL,
    reason      TEXT,
    status      ENUM('pending','approved','rejected') DEFAULT 'pending',
    requested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    reviewed_at  DATETIME,
    reviewed_by  INT,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES staff(id) ON DELETE SET NULL
);

-- ============================================================
-- SAMPLE DATA
-- ============================================================

-- Admin user (password: Admin@123)
-- BCrypt hash for "Admin@123"
INSERT INTO users (name, email, password_hash, role, login_score, status) VALUES
('System Admin', 'admin@school.edu', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.s5uhom', 'admin', 100, 'active');

-- Staff users (password: Staff@123)
INSERT INTO users (name, email, password_hash, role, login_score, status) VALUES
('Dr. Priya Sharma',   'priya.sharma@school.edu',  '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'staff', 100, 'active'),
('Prof. Rajesh Kumar', 'rajesh.kumar@school.edu',  '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'staff', 100, 'active'),
('Dr. Meena Iyer',     'meena.iyer@school.edu',    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'staff', 100, 'active');

-- Student users (password: Student@123)
INSERT INTO users (name, email, password_hash, role, login_score, status) VALUES
('Arjun Nair',       'arjun.nair@student.edu',    '$2a$12$TyTGkZNa8DpKJHXQ9hjLqOVS8YDMnfhGrNhCJa00XDWNvgEPHIi7C', 'student', 100, 'active'),
('Divya Patel',      'divya.patel@student.edu',   '$2a$12$TyTGkZNa8DpKJHXQ9hjLqOVS8YDMnfhGrNhCJa00XDWNvgEPHIi7C', 'student', 100, 'active'),
('Karthik Rajan',    'karthik.rajan@student.edu', '$2a$12$TyTGkZNa8DpKJHXQ9hjLqOVS8YDMnfhGrNhCJa00XDWNvgEPHIi7C', 'student', 85,  'active'),
('Sneha Menon',      'sneha.menon@student.edu',   '$2a$12$TyTGkZNa8DpKJHXQ9hjLqOVS8YDMnfhGrNhCJa00XDWNvgEPHIi7C', 'student', 60,  'active'),
('Rahul Verma',      'rahul.verma@student.edu',   '$2a$12$TyTGkZNa8DpKJHXQ9hjLqOVS8YDMnfhGrNhCJa00XDWNvgEPHIi7C', 'student', 20,  'blocked'),
('Ananya Krishnan',  'ananya.k@student.edu',      '$2a$12$TyTGkZNa8DpKJHXQ9hjLqOVS8YDMnfhGrNhCJa00XDWNvgEPHIi7C', 'student', 100, 'active');

-- Staff profiles
INSERT INTO staff (user_id, department, designation) VALUES
(2, 'Computer Science', 'Associate Professor'),
(3, 'Mathematics',      'Professor'),
(4, 'Physics',          'Assistant Professor');

-- Student profiles
INSERT INTO students (user_id, staff_id, attendance, grade, roll_number, department, semester) VALUES
(5,  1, 88.50, 'A',   'CS2021001', 'Computer Science', 4),
(6,  1, 92.00, 'A+',  'CS2021002', 'Computer Science', 4),
(7,  2, 75.00, 'B',   'MA2021001', 'Mathematics',      3),
(8,  1, 65.50, 'C',   'CS2021003', 'Computer Science', 4),
(9,  2, 45.00, 'D',   'MA2021002', 'Mathematics',      3),
(10, 3, 95.00, 'A+',  'PH2021001', 'Physics',          2);

-- Subjects
INSERT INTO subjects (subject_name, subject_code, staff_id, credits) VALUES
('Data Structures',         'CS301', 1, 4),
('Database Management',     'CS302', 1, 3),
('Operating Systems',       'CS303', 1, 3),
('Calculus II',             'MA201', 2, 4),
('Linear Algebra',          'MA202', 2, 3),
('Mechanics',               'PH101', 3, 4),
('Quantum Physics',         'PH201', 3, 3);

-- Enrollment
INSERT INTO student_subjects (student_id, subject_id, status) VALUES
(1, 1, 'accepted'), (1, 2, 'accepted'), (1, 3, 'accepted'),
(2, 1, 'accepted'), (2, 2, 'accepted'), (2, 3, 'accepted'),
(3, 4, 'accepted'), (3, 5, 'accepted'),
(4, 1, 'accepted'), (4, 2, 'accepted'),
(5, 4, 'accepted'), (5, 5, 'accepted'),
(6, 6, 'accepted'), (6, 7, 'accepted');

-- Marks
INSERT INTO marks (student_id, subject_id, marks, max_marks) VALUES
(1, 1, 82.0, 100), (1, 2, 91.0, 100), (1, 3, 76.0, 100),
(2, 1, 95.0, 100), (2, 2, 88.0, 100), (2, 3, 92.0, 100),
(3, 4, 70.0, 100), (3, 5, 65.0, 100),
(4, 1, 55.0, 100), (4, 2, 62.0, 100),
(5, 4, 45.0, 100), (5, 5, 50.0, 100),
(6, 6, 98.0, 100), (6, 7, 94.0, 100);

-- Sample login attempts
INSERT INTO login_attempts (user_id, email, ip_address, device, status, risk_level) VALUES
(5,  'arjun.nair@student.edu',    '192.168.1.10', 'Chrome/Windows', 'success', 'low'),
(5,  'arjun.nair@student.edu',    '192.168.1.10', 'Chrome/Windows', 'success', 'low'),
(5,  'arjun.nair@student.edu',    '192.168.1.10', 'Firefox/Windows','failure', 'medium'),
(6,  'divya.patel@student.edu',   '192.168.1.11', 'Chrome/Mac',     'success', 'low'),
(7,  'karthik.rajan@student.edu', '10.0.0.5',     'Edge/Windows',   'failure', 'medium'),
(7,  'karthik.rajan@student.edu', '10.0.0.6',     'Edge/Android',   'failure', 'high'),
(9,  'rahul.verma@student.edu',   '203.0.113.1',  'Unknown/Linux',  'blocked', 'high'),
(9,  'rahul.verma@student.edu',   '203.0.113.2',  'Unknown/Linux',  'blocked', 'high'),
(2,  'priya.sharma@school.edu',   '192.168.1.1',  'Chrome/Windows', 'success', 'low'),
(1,  'admin@school.edu',          '127.0.0.1',    'Chrome/Windows', 'success', 'low');

-- Unblock request from blocked student
INSERT INTO unblock_requests (student_id, reason, status) VALUES
(5, 'I forgot my password and tried multiple times by mistake. Please unblock my account.', 'pending');

-- ============================================================
-- VIEWS for reporting
-- ============================================================
CREATE VIEW v_student_details AS
SELECT 
    u.id AS user_id, u.name, u.email, u.login_score, u.status,
    u.last_login, s.id AS student_id, s.roll_number, s.department,
    s.semester, s.attendance, s.grade,
    st_user.name AS staff_name
FROM users u
JOIN students s ON u.id = s.user_id
LEFT JOIN staff st ON s.staff_id = st.id
LEFT JOIN users st_user ON st.user_id = st_user.id
WHERE u.role = 'student';

CREATE VIEW v_login_risk_summary AS
SELECT 
    u.name, u.email, u.role,
    COUNT(la.id) AS total_attempts,
    SUM(CASE WHEN la.status = 'success' THEN 1 ELSE 0 END) AS successful,
    SUM(CASE WHEN la.status = 'failure' THEN 1 ELSE 0 END) AS failed,
    SUM(CASE WHEN la.risk_level = 'high' THEN 1 ELSE 0 END) AS high_risk,
    SUM(CASE WHEN la.risk_level = 'medium' THEN 1 ELSE 0 END) AS medium_risk,
    u.login_score, u.status
FROM users u
LEFT JOIN login_attempts la ON u.id = la.user_id
GROUP BY u.id, u.name, u.email, u.role, u.login_score, u.status;

SELECT 'Database setup complete!' AS message;
SELECT 'Sample credentials:' AS info;
SELECT 'Admin:   admin@school.edu    / Admin@123'   AS credentials;
SELECT 'Staff:   priya.sharma@school.edu / Staff@123' AS credentials;
SELECT 'Student: arjun.nair@student.edu  / Student@123' AS credentials;

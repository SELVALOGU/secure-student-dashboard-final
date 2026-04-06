-- ============================================================
-- Secure Student Login Behavior Analyzer - PostgreSQL Setup
-- ============================================================

-- Clean up existing tables
DROP VIEW IF EXISTS v_student_details CASCADE;
DROP VIEW IF EXISTS v_login_risk_summary CASCADE;
DROP TABLE IF EXISTS unblock_requests CASCADE;
DROP TABLE IF EXISTS marks CASCADE;
DROP TABLE IF EXISTS student_subjects CASCADE;
DROP TABLE IF EXISTS subjects CASCADE;
DROP TABLE IF EXISTS students CASCADE;
DROP TABLE IF EXISTS staff CASCADE;
DROP TABLE IF EXISTS login_attempts CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================================
-- TABLE: users (base authentication table for all roles)
-- ============================================================
CREATE TABLE users (
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL DEFAULT 'student' CHECK (role IN ('student','staff','admin')),
    login_score   INT NOT NULL DEFAULT 100,
    status        VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active','blocked','suspended')),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login    TIMESTAMP
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_status ON users (status);

-- ============================================================
-- TABLE: login_attempts (tracks every login attempt)
-- ============================================================
CREATE TABLE login_attempts (
    id          SERIAL PRIMARY KEY,
    user_id     INT,
    email       VARCHAR(150),
    timestamp   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address  VARCHAR(45),
    device      VARCHAR(255),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('success','failure','blocked')),
    risk_level  VARCHAR(20) NOT NULL DEFAULT 'low' CHECK (risk_level IN ('low','medium','high')),
    fail_reason VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_login_user_id   ON login_attempts (user_id);
CREATE INDEX idx_login_timestamp ON login_attempts (timestamp);
CREATE INDEX idx_login_risk      ON login_attempts (risk_level);

-- ============================================================
-- TABLE: students (extended student profile)
-- ============================================================
CREATE TABLE students (
    id          SERIAL PRIMARY KEY,
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
    id          SERIAL PRIMARY KEY,
    user_id     INT NOT NULL UNIQUE,
    department  VARCHAR(100),
    designation VARCHAR(100) DEFAULT 'Lecturer',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: subjects
-- ============================================================
CREATE TABLE subjects (
    id           SERIAL PRIMARY KEY,
    subject_name VARCHAR(150) NOT NULL,
    subject_code VARCHAR(20) NOT NULL UNIQUE,
    staff_id     INT NOT NULL,
    credits      INT DEFAULT 3,
    FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
);

CREATE INDEX idx_subjects_staff_id ON subjects (staff_id);

-- ============================================================
-- TABLE: student_subjects (enrollment)
-- ============================================================
CREATE TABLE student_subjects (
    id         SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    subject_id INT NOT NULL,
    status     VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending','accepted','rejected')),
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, subject_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: marks
-- ============================================================
CREATE TABLE marks (
    id          SERIAL PRIMARY KEY,
    student_id  INT NOT NULL,
    subject_id  INT NOT NULL,
    marks       DECIMAL(5,2) DEFAULT 0.00,
    max_marks   DECIMAL(5,2) DEFAULT 100.00,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, subject_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Create a trigger for updated_at in marks (equivalent to ON UPDATE CURRENT_TIMESTAMP)
CREATE OR REPLACE FUNCTION update_marks_updated_at()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = CURRENT_TIMESTAMP;
   RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_marks_updated_at
BEFORE UPDATE ON marks
FOR EACH ROW
EXECUTE FUNCTION update_marks_updated_at();

-- ============================================================
-- TABLE: unblock_requests
-- ============================================================
CREATE TABLE unblock_requests (
    id          SERIAL PRIMARY KEY,
    student_id  INT NOT NULL,
    reason      TEXT,
    status      VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending','approved','rejected')),
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at  TIMESTAMP,
    reviewed_by  INT,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES staff(id) ON DELETE SET NULL
);

-- ============================================================
-- SAMPLE DATA
-- ============================================================

INSERT INTO users (name, email, password_hash, role, login_score, status) VALUES
('System Admin', 'admin@school.edu', 'Admin@123', 'admin', 100, 'active'),
('Dr. Priya Sharma',   'priya.sharma@school.edu',  'Staff@123', 'staff', 100, 'active'),
('Prof. Rajesh Kumar', 'rajesh.kumar@school.edu',  'Staff@123', 'staff', 100, 'active'),
('Dr. Meena Iyer',     'meena.iyer@school.edu',    'Staff@123', 'staff', 100, 'active'),
('Arjun Nair',       'arjun.nair@student.edu',    'Student@123', 'student', 100, 'active'),
('Divya Patel',      'divya.patel@student.edu',   'Student@123', 'student', 100, 'active'),
('Karthik Rajan',    'karthik.rajan@student.edu', 'Student@123', 'student', 85,  'active'),
('Sneha Menon',      'sneha.menon@student.edu',   'Student@123', 'student', 60,  'active'),
('Rahul Verma',      'rahul.verma@student.edu',   'Student@123', 'student', 20,  'blocked'),
('Ananya Krishnan',  'ananya.k@student.edu',      'Student@123', 'student', 100, 'active');

INSERT INTO staff (user_id, department, designation) VALUES
(2, 'Computer Science', 'Associate Professor'),
(3, 'Mathematics',      'Professor'),
(4, 'Physics',          'Assistant Professor');

INSERT INTO students (user_id, staff_id, attendance, grade, roll_number, department, semester) VALUES
(5,  1, 88.50, 'A',   'CS2021001', 'Computer Science', 4),
(6,  1, 92.00, 'A+',  'CS2021002', 'Computer Science', 4),
(7,  2, 75.00, 'B',   'MA2021001', 'Mathematics',      3),
(8,  1, 65.50, 'C',   'CS2021003', 'Computer Science', 4),
(9,  2, 45.00, 'D',   'MA2021002', 'Mathematics',      3),
(10, 3, 95.00, 'A+',  'PH2021001', 'Physics',          2);

INSERT INTO subjects (subject_name, subject_code, staff_id, credits) VALUES
('Data Structures',         'CS301', 1, 4),
('Database Management',     'CS302', 1, 3),
('Operating Systems',       'CS303', 1, 3),
('Calculus II',             'MA201', 2, 4),
('Linear Algebra',          'MA202', 2, 3),
('Mechanics',               'PH101', 3, 4),
('Quantum Physics',         'PH201', 3, 3);

INSERT INTO student_subjects (student_id, subject_id, status) VALUES
(1, 1, 'accepted'), (1, 2, 'accepted'), (1, 3, 'accepted'),
(2, 1, 'accepted'), (2, 2, 'accepted'), (2, 3, 'accepted'),
(3, 4, 'accepted'), (3, 5, 'accepted'),
(4, 1, 'accepted'), (4, 2, 'accepted'),
(5, 4, 'accepted'), (5, 5, 'accepted'),
(6, 6, 'accepted'), (6, 7, 'accepted');

INSERT INTO marks (student_id, subject_id, marks, max_marks) VALUES
(1, 1, 82.0, 100), (1, 2, 91.0, 100), (1, 3, 76.0, 100),
(2, 1, 95.0, 100), (2, 2, 88.0, 100), (2, 3, 92.0, 100),
(3, 4, 70.0, 100), (3, 5, 65.0, 100),
(4, 1, 55.0, 100), (4, 2, 62.0, 100),
(5, 4, 45.0, 100), (5, 5, 50.0, 100),
(6, 6, 98.0, 100), (6, 7, 94.0, 100);

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

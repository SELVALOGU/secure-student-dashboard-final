package com.student.analyzer.dao;

import com.student.analyzer.model.Student;
import com.student.analyzer.model.Mark;
import com.student.analyzer.model.Subject;
import com.student.analyzer.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentDAO — Data Access Object for student-related operations.
 */
public class StudentDAO {

    /**
     * Finds a student record by their user_id.
     */
    public Student findByUserId(int userId) throws SQLException {
        String sql = "SELECT s.*, u.name, u.email, u.login_score, u.status, u.last_login, " +
                     "st_user.name AS staff_name " +
                     "FROM students s " +
                     "JOIN users u ON s.user_id = u.id " +
                     "LEFT JOIN staff st ON s.staff_id = st.id " +
                     "LEFT JOIN users st_user ON st.user_id = st_user.id " +
                     "WHERE s.user_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a student by their student (not user) ID.
     */
    public Student findById(int studentId) throws SQLException {
        String sql = "SELECT s.*, u.name, u.email, u.login_score, u.status, u.last_login, " +
                     "st_user.name AS staff_name " +
                     "FROM students s " +
                     "JOIN users u ON s.user_id = u.id " +
                     "LEFT JOIN staff st ON s.staff_id = st.id " +
                     "LEFT JOIN users st_user ON st.user_id = st_user.id " +
                     "WHERE s.id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Gets all students (for staff/admin view).
     */
    public List<Student> getAllStudents() throws SQLException {
        String sql = "SELECT s.*, u.name, u.email, u.login_score, u.status, u.last_login, " +
                     "st_user.name AS staff_name " +
                     "FROM students s " +
                     "JOIN users u ON s.user_id = u.id " +
                     "LEFT JOIN staff st ON s.staff_id = st.id " +
                     "LEFT JOIN users st_user ON st.user_id = st_user.id " +
                     "ORDER BY u.name";
        List<Student> students = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                students.add(mapResultSet(rs));
            }
        }
        return students;
    }

    /**
     * Gets marks for a student across all enrolled subjects.
     */
    public List<Mark> getMarksForStudent(int studentId) throws SQLException {
        String sql = "SELECT m.*, sub.subject_name, sub.subject_code " +
                     "FROM marks m " +
                     "JOIN subjects sub ON m.subject_id = sub.id " +
                     "WHERE m.student_id = ? " +
                     "ORDER BY sub.subject_name";
        List<Mark> marks = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mark mark = new Mark();
                    mark.setId(rs.getInt("id"));
                    mark.setStudentId(rs.getInt("student_id"));
                    mark.setSubjectId(rs.getInt("subject_id"));
                    mark.setMarks(rs.getDouble("marks"));
                    mark.setMaxMarks(rs.getDouble("max_marks"));
                    mark.setSubjectName(rs.getString("subject_name"));
                    mark.setSubjectCode(rs.getString("subject_code"));
                    marks.add(mark);
                }
            }
        }
        return marks;
    }

    /**
     * Gets subjects a student is enrolled in.
     */
    public List<Subject> getEnrolledSubjects(int studentId) throws SQLException {
        String sql = "SELECT sub.*, u.name AS staff_name, ss.status AS enrollment_status " +
                     "FROM student_subjects ss " +
                     "JOIN subjects sub ON ss.subject_id = sub.id " +
                     "JOIN staff st ON sub.staff_id = st.id " +
                     "JOIN users u ON st.user_id = u.id " +
                     "WHERE ss.student_id = ?";
        List<Subject> subjects = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Subject sub = new Subject();
                    sub.setId(rs.getInt("id"));
                    sub.setSubjectName(rs.getString("subject_name"));
                    sub.setSubjectCode(rs.getString("subject_code"));
                    sub.setStaffId(rs.getInt("staff_id"));
                    sub.setCredits(rs.getInt("credits"));
                    sub.setStaffName(rs.getString("staff_name"));
                    sub.setEnrollmentStatus(rs.getString("enrollment_status"));
                    subjects.add(sub);
                }
            }
        }
        return subjects;
    }

    /**
     * Updates a student's attendance and grade.
     */
    public void updateStudent(int studentId, double attendance, String grade) throws SQLException {
        String sql = "UPDATE students SET attendance = ?, grade = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, attendance);
            ps.setString(2, grade);
            ps.setInt(3, studentId);
            ps.executeUpdate();
        }
    }

    /**
     * Maps a ResultSet row to a Student object.
     */
    private Student mapResultSet(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setStaffId(rs.getInt("staff_id"));
        s.setAttendance(rs.getDouble("attendance"));
        s.setGrade(rs.getString("grade"));
        s.setRollNumber(rs.getString("roll_number"));
        s.setDepartment(rs.getString("department"));
        s.setSemester(rs.getInt("semester"));
        s.setName(rs.getString("name"));
        s.setEmail(rs.getString("email"));
        s.setLoginScore(rs.getInt("login_score"));
        s.setStatus(rs.getString("status"));
        s.setStaffName(rs.getString("staff_name"));
        Timestamp lastLogin = rs.getTimestamp("last_login");
        s.setLastLogin(lastLogin != null ? lastLogin.toString() : "Never");
        return s;
    }
}

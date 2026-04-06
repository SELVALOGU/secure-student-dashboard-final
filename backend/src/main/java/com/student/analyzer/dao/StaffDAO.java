package com.student.analyzer.dao;

import com.student.analyzer.model.Staff;
import com.student.analyzer.model.Mark;
import com.student.analyzer.model.Subject;
import com.student.analyzer.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * StaffDAO — Data Access Object for staff operations.
 */
public class StaffDAO {

    /**
     * Finds staff record by user_id.
     */
    public Staff findByUserId(int userId) throws SQLException {
        String sql = "SELECT st.*, u.name, u.email, u.login_score, u.status " +
                     "FROM staff st JOIN users u ON st.user_id = u.id " +
                     "WHERE st.user_id = ?";
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
     * Gets all staff members (for admin view).
     */
    public List<Staff> getAllStaff() throws SQLException {
        String sql = "SELECT st.*, u.name, u.email, u.login_score, u.status " +
                     "FROM staff st JOIN users u ON st.user_id = u.id ORDER BY u.name";
        List<Staff> staffList = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                staffList.add(mapResultSet(rs));
            }
        }
        return staffList;
    }

    /**
     * Gets subjects taught by a staff member.
     */
    public List<Subject> getSubjectsByStaff(int staffId) throws SQLException {
        String sql = "SELECT sub.*, u.name AS staff_name " +
                     "FROM subjects sub " +
                     "JOIN staff st ON sub.staff_id = st.id " +
                     "JOIN users u ON st.user_id = u.id " +
                     "WHERE sub.staff_id = ?";
        List<Subject> subjects = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Subject sub = new Subject();
                    sub.setId(rs.getInt("id"));
                    sub.setSubjectName(rs.getString("subject_name"));
                    sub.setSubjectCode(rs.getString("subject_code"));
                    sub.setStaffId(rs.getInt("staff_id"));
                    sub.setCredits(rs.getInt("credits"));
                    sub.setStaffName(rs.getString("staff_name"));
                    subjects.add(sub);
                }
            }
        }
        return subjects;
    }

    /**
     * Gets all subjects (for all-subjects view).
     */
    public List<Subject> getAllSubjects() throws SQLException {
        String sql = "SELECT sub.*, u.name AS staff_name " +
                     "FROM subjects sub " +
                     "JOIN staff st ON sub.staff_id = st.id " +
                     "JOIN users u ON st.user_id = u.id " +
                     "ORDER BY sub.subject_name";
        List<Subject> subjects = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Subject sub = new Subject();
                sub.setId(rs.getInt("id"));
                sub.setSubjectName(rs.getString("subject_name"));
                sub.setSubjectCode(rs.getString("subject_code"));
                sub.setStaffId(rs.getInt("staff_id"));
                sub.setCredits(rs.getInt("credits"));
                sub.setStaffName(rs.getString("staff_name"));
                subjects.add(sub);
            }
        }
        return subjects;
    }

    /**
     * Adds a new subject (staff creates a subject).
     */
    public void addSubject(String name, String code, int staffId, int credits) throws SQLException {
        String sql = "INSERT INTO subjects (subject_name, subject_code, staff_id, credits) VALUES (?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, code);
            ps.setInt(3, staffId);
            ps.setInt(4, credits);
            ps.executeUpdate();
        }
    }

    /**
     * Updates or inserts marks for a student in a subject.
     */
    public void updateMarks(int studentId, int subjectId, double marks, double maxMarks) throws SQLException {
        // Use INSERT ... ON DUPLICATE KEY UPDATE for upsert
        String sql = "INSERT INTO marks (student_id, subject_id, marks, max_marks) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE marks = VALUES(marks), max_marks = VALUES(max_marks)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            ps.setDouble(3, marks);
            ps.setDouble(4, maxMarks);
            ps.executeUpdate();
        }
    }

    /**
     * Gets marks for all students in a subject.
     */
    public List<Mark> getMarksBySubject(int subjectId) throws SQLException {
        String sql = "SELECT m.*, u.name AS student_name, sub.subject_name, sub.subject_code " +
                     "FROM marks m " +
                     "JOIN students st ON m.student_id = st.id " +
                     "JOIN users u ON st.user_id = u.id " +
                     "JOIN subjects sub ON m.subject_id = sub.id " +
                     "WHERE m.subject_id = ? " +
                     "ORDER BY u.name";
        List<Mark> marks = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
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
                    mark.setStudentName(rs.getString("student_name"));
                    marks.add(mark);
                }
            }
        }
        return marks;
    }

    /**
     * Maps a ResultSet row to a Staff object.
     */
    private Staff mapResultSet(ResultSet rs) throws SQLException {
        Staff staff = new Staff();
        staff.setId(rs.getInt("id"));
        staff.setUserId(rs.getInt("user_id"));
        staff.setDepartment(rs.getString("department"));
        staff.setDesignation(rs.getString("designation"));
        staff.setName(rs.getString("name"));
        staff.setEmail(rs.getString("email"));
        staff.setLoginScore(rs.getInt("login_score"));
        staff.setStatus(rs.getString("status"));
        return staff;
    }
}

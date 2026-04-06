package com.student.analyzer.dao;

import com.student.analyzer.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * SubjectDAO — Handles student subject enrollment operations.
 */
public class SubjectDAO {

    /**
     * Enrolls a student in a subject (creates pending enrollment).
     */
    public void enrollStudent(int studentId, int subjectId) throws SQLException {
        String sql = "INSERT IGNORE INTO student_subjects (student_id, subject_id, status) VALUES (?, ?, 'accepted')";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            ps.executeUpdate();
        }
    }

    /**
     * Gets all subjects with enrollment count (for staff view).
     */
    public List<Map<String, Object>> getSubjectsWithEnrollmentCount(int staffId) throws SQLException {
        String sql = "SELECT sub.id, sub.subject_name, sub.subject_code, sub.credits, " +
                     "COUNT(ss.id) AS enrolled_count " +
                     "FROM subjects sub " +
                     "LEFT JOIN student_subjects ss ON sub.id = ss.subject_id AND ss.status = 'accepted' " +
                     "WHERE sub.staff_id = ? " +
                     "GROUP BY sub.id, sub.subject_name, sub.subject_code, sub.credits";
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("subjectName", rs.getString("subject_name"));
                    row.put("subjectCode", rs.getString("subject_code"));
                    row.put("credits", rs.getInt("credits"));
                    row.put("enrolledCount", rs.getInt("enrolled_count"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    /**
     * Gets students enrolled in a specific subject.
     */
    public List<Map<String, Object>> getStudentsInSubject(int subjectId) throws SQLException {
        String sql = "SELECT u.name, u.email, s.roll_number, ss.status, ss.enrolled_at " +
                     "FROM student_subjects ss " +
                     "JOIN students s ON ss.student_id = s.id " +
                     "JOIN users u ON s.user_id = u.id " +
                     "WHERE ss.subject_id = ? AND ss.status = 'accepted'";
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", rs.getString("name"));
                    row.put("email", rs.getString("email"));
                    row.put("rollNumber", rs.getString("roll_number"));
                    row.put("status", rs.getString("status"));
                    row.put("enrolledAt", rs.getTimestamp("enrolled_at").toString());
                    result.add(row);
                }
            }
        }
        return result;
    }

    /**
     * Gets system-wide enrollment summary.
     */
    public List<Map<String, Object>> getAllSubjectsWithEnrollment() throws SQLException {
        String sql = "SELECT sub.id, sub.subject_name, sub.subject_code, sub.credits, " +
                     "u.name AS staff_name, COUNT(ss.id) AS enrolled_count " +
                     "FROM subjects sub " +
                     "JOIN staff st ON sub.staff_id = st.id " +
                     "JOIN users u ON st.user_id = u.id " +
                     "LEFT JOIN student_subjects ss ON sub.id = ss.subject_id AND ss.status = 'accepted' " +
                     "GROUP BY sub.id, sub.subject_name, sub.subject_code, sub.credits, u.name " +
                     "ORDER BY sub.subject_name";
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("subjectName", rs.getString("subject_name"));
                row.put("subjectCode", rs.getString("subject_code"));
                row.put("credits", rs.getInt("credits"));
                row.put("staffName", rs.getString("staff_name"));
                row.put("enrolledCount", rs.getInt("enrolled_count"));
                result.add(row);
            }
        }
        return result;
    }
}

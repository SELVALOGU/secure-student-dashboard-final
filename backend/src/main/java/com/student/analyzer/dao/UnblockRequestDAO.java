package com.student.analyzer.dao;

import com.student.analyzer.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * UnblockRequestDAO — Manages account unblock requests from students.
 */
public class UnblockRequestDAO {

    /**
     * Creates a new unblock request for a blocked student.
     *
     * @param studentId The student's ID (from students table)
     * @param reason    The reason for the unblock request
     */
    public void createRequest(int studentId, String reason) throws SQLException {
        // First cancel any existing pending request
        String cancelSql = "UPDATE unblock_requests SET status = 'rejected' " +
                           "WHERE student_id = ? AND status = 'pending'";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(cancelSql)) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        }

        // Insert new request
        String sql = "INSERT INTO unblock_requests (student_id, reason, status) VALUES (?, ?, 'pending')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, reason);
            ps.executeUpdate();
        }
    }

    /**
     * Gets all pending unblock requests (for staff view).
     */
    public List<Map<String, Object>> getPendingRequests() throws SQLException {
        String sql = "SELECT ur.id, ur.student_id, ur.reason, ur.requested_at, ur.status, " +
                     "u.name AS student_name, u.email AS student_email, u.login_score " +
                     "FROM unblock_requests ur " +
                     "JOIN students s ON ur.student_id = s.id " +
                     "JOIN users u ON s.user_id = u.id " +
                     "WHERE ur.status = 'pending' " +
                     "ORDER BY ur.requested_at DESC";
        List<Map<String, Object>> requests = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> req = new HashMap<>();
                req.put("id", rs.getInt("id"));
                req.put("studentId", rs.getInt("student_id"));
                req.put("reason", rs.getString("reason"));
                req.put("requestedAt", rs.getTimestamp("requested_at").toString());
                req.put("status", rs.getString("status"));
                req.put("studentName", rs.getString("student_name"));
                req.put("studentEmail", rs.getString("student_email"));
                req.put("loginScore", rs.getInt("login_score"));
                requests.add(req);
            }
        }
        return requests;
    }

    /**
     * Approves an unblock request.
     *
     * @param requestId  The request ID
     * @param reviewerId The staff ID who approved it
     */
    public void approveRequest(int requestId, int reviewerId) throws SQLException {
        String sql = "UPDATE unblock_requests SET status = 'approved', reviewed_at = NOW(), " +
                     "reviewed_by = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewerId);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        }
    }

    /**
     * Rejects an unblock request.
     */
    public void rejectRequest(int requestId, int reviewerId) throws SQLException {
        String sql = "UPDATE unblock_requests SET status = 'rejected', reviewed_at = NOW(), " +
                     "reviewed_by = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewerId);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        }
    }

    /**
     * Gets student_id from an unblock request.
     */
    public int getStudentIdFromRequest(int requestId) throws SQLException {
        String sql = "SELECT student_id FROM unblock_requests WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("student_id");
            }
        }
        return -1;
    }

    /**
     * Checks if a student has a pending unblock request.
     */
    public boolean hasPendingRequest(int studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM unblock_requests WHERE student_id = ? AND status = 'pending'";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
}

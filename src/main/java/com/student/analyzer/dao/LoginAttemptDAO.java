package com.student.analyzer.dao;

import com.student.analyzer.model.LoginAttempt;
import com.student.analyzer.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * LoginAttemptDAO — Manages all login attempt records.
 */
public class LoginAttemptDAO {

    /**
     * Records a new login attempt to the database.
     */
    public void recordAttempt(LoginAttempt attempt) throws SQLException {
        String sql = "INSERT INTO login_attempts (user_id, email, ip_address, device, status, risk_level, fail_reason) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (attempt.getUserId() > 0) {
                ps.setInt(1, attempt.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER); // Unknown user (wrong email)
            }
            ps.setString(2, attempt.getEmail());
            ps.setString(3, attempt.getIpAddress());
            ps.setString(4, attempt.getDevice());
            ps.setString(5, attempt.getStatus());
            ps.setString(6, attempt.getRiskLevel());
            ps.setString(7, attempt.getFailReason());
            ps.executeUpdate();
        }
    }

    /**
     * Gets the most recent N login attempts for a user.
     * Used for risk analysis and dashboard display.
     *
     * @param userId The user's ID
     * @param limit  Maximum number of records to return
     * @return List of LoginAttempt objects, newest first
     */
    public List<LoginAttempt> getRecentAttempts(int userId, int limit) throws SQLException {
        String sql = "SELECT * FROM login_attempts WHERE user_id = ? ORDER BY timestamp DESC LIMIT ?";
        List<LoginAttempt> attempts = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attempts.add(mapResultSet(rs));
                }
            }
        }
        return attempts;
    }

    /**
     * Gets all login attempts for a user (for full history view).
     */
    public List<LoginAttempt> getAllAttemptsForUser(int userId) throws SQLException {
        String sql = "SELECT * FROM login_attempts WHERE user_id = ? ORDER BY timestamp DESC";
        List<LoginAttempt> attempts = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attempts.add(mapResultSet(rs));
                }
            }
        }
        return attempts;
    }

    /**
     * Gets all login attempts system-wide (admin view).
     * Can filter by risk level.
     *
     * @param riskFilter "all", "low", "medium", or "high"
     * @param limit      Maximum rows to return
     */
    public List<LoginAttempt> getAllAttempts(String riskFilter, int limit) throws SQLException {
        String sql;
        if ("all".equals(riskFilter) || riskFilter == null) {
            sql = "SELECT la.*, u.name FROM login_attempts la " +
                  "LEFT JOIN users u ON la.user_id = u.id " +
                  "ORDER BY la.timestamp DESC LIMIT ?";
        } else {
            sql = "SELECT la.*, u.name FROM login_attempts la " +
                  "LEFT JOIN users u ON la.user_id = u.id " +
                  "WHERE la.risk_level = ? ORDER BY la.timestamp DESC LIMIT ?";
        }

        List<LoginAttempt> attempts = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if ("all".equals(riskFilter) || riskFilter == null) {
                ps.setInt(1, limit);
            } else {
                ps.setString(1, riskFilter);
                ps.setInt(2, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attempts.add(mapResultSet(rs));
                }
            }
        }
        return attempts;
    }

    /**
     * Counts consecutive failed attempts for a user (most recent first).
     * Stops counting once a success is encountered.
     */
    public int countConsecutiveFails(int userId) throws SQLException {
        String sql = "SELECT status FROM login_attempts WHERE user_id = ? ORDER BY timestamp DESC LIMIT 20";
        int count = 0;
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    if ("failure".equals(status) || "blocked".equals(status)) {
                        count++;
                    } else {
                        break; // Stop at first success
                    }
                }
            }
        }
        return count;
    }

    /**
     * Gets count of failed attempts for a user.
     */
    public int countTotalFails(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM login_attempts WHERE user_id = ? AND status = 'failure'";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Maps a ResultSet row to a LoginAttempt object.
     */
    private LoginAttempt mapResultSet(ResultSet rs) throws SQLException {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setId(rs.getInt("id"));
        attempt.setEmail(rs.getString("email"));
        attempt.setIpAddress(rs.getString("ip_address"));
        attempt.setDevice(rs.getString("device"));
        attempt.setStatus(rs.getString("status"));
        attempt.setRiskLevel(rs.getString("risk_level"));
        attempt.setTimestamp(rs.getTimestamp("timestamp"));
        // user_id might be null for unknown email attempts
        try {
            attempt.setUserId(rs.getInt("user_id"));
        } catch (Exception e) {
            attempt.setUserId(0);
        }
        try {
            attempt.setFailReason(rs.getString("fail_reason"));
        } catch (Exception e) { /* column might not be in all queries */ }
        return attempt;
    }
}

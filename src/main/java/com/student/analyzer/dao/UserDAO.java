package com.student.analyzer.dao;

import com.student.analyzer.model.User;
import com.student.analyzer.util.DBConnection;

import java.sql.*;

/**
 * UserDAO — Data Access Object for users table.
 * Uses PreparedStatements to prevent SQL injection.
 */
public class UserDAO {

    /**
     * Finds a user by their email address.
     *
     * @param email The email to search for
     * @return User object if found, null otherwise
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a user by their ID.
     */
    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates a user's login score and status.
     * Called after each login attempt.
     *
     * @param userId     The user's ID
     * @param loginScore New login score
     * @param status     New status ("active" or "blocked")
     * @param lastLogin  Timestamp of last successful login (or null)
     */
    public void updateLoginInfo(int userId, int loginScore, String status, Timestamp lastLogin) throws SQLException {
        String sql = "UPDATE users SET login_score = ?, status = ?, last_login = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loginScore);
            ps.setString(2, status);
            ps.setTimestamp(3, lastLogin);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Updates a user's status only (e.g., block/unblock).
     */
    public void updateStatus(int userId, String status) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Resets login score (used when staff unblocks a student).
     */
    public void resetLoginScore(int userId) throws SQLException {
        String sql = "UPDATE users SET login_score = 100, status = 'active' WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Updates login score only.
     */
    public void updateLoginScore(int userId, int newScore) throws SQLException {
        String sql = "UPDATE users SET login_score = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newScore);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Creates a new user in the database.
     * Returns the generated user ID.
     */
    public int createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, role, login_score, status) VALUES (?, ?, ?, ?, 100, 'active')";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Deletes a user by ID.
     */
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Updates user details (name, email).
     */
    public void updateUser(int userId, String name, String email) throws SQLException {
        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Maps a ResultSet row to a User object.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setLoginScore(rs.getInt("login_score"));
        user.setStatus(rs.getString("status"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        return user;
    }
}

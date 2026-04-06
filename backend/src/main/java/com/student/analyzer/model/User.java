package com.student.analyzer.model;

import java.util.Date;

/**
 * User model — represents any user in the system (student, staff, admin).
 */
public class User {
    private int    id;
    private String name;
    private String email;
    private String passwordHash;
    private String role;          // "student", "staff", "admin"
    private int    loginScore;    // 0-100, decreases on failed attempts
    private String status;        // "active", "blocked", "suspended"
    private Date   createdAt;
    private Date   lastLogin;

    // Default constructor
    public User() {}

    // Constructor for creating new users
    public User(String name, String email, String passwordHash, String role) {
        this.name         = name;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.loginScore   = 100;
        this.status       = "active";
    }

    // Getters and Setters
    public int getId()                   { return id; }
    public void setId(int id)            { this.id = id; }

    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    public String getEmail()             { return email; }
    public void setEmail(String email)   { this.email = email; }

    public String getPasswordHash()      { return passwordHash; }
    public void setPasswordHash(String h){ this.passwordHash = h; }

    public String getRole()              { return role; }
    public void setRole(String role)     { this.role = role; }

    public int getLoginScore()           { return loginScore; }
    public void setLoginScore(int score) { this.loginScore = score; }

    public String getStatus()            { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt()           { return createdAt; }
    public void setCreatedAt(Date d)     { this.createdAt = d; }

    public Date getLastLogin()           { return lastLogin; }
    public void setLastLogin(Date d)     { this.lastLogin = d; }

    public boolean isBlocked()           { return "blocked".equals(status); }
    public boolean isActive()            { return "active".equals(status); }

    @Override
    public String toString() {
        return "User{id=" + id + ", name=" + name + ", role=" + role + ", status=" + status + "}";
    }
}

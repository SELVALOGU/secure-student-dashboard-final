package com.student.analyzer.model;

import java.util.Date;

/**
 * LoginAttempt model — records every login event.
 */
public class LoginAttempt {
    private int    id;
    private int    userId;
    private String email;
    private Date   timestamp;
    private String ipAddress;
    private String device;       // e.g. "Chrome/Windows", "Firefox/Mac"
    private String status;       // "success", "failure", "blocked"
    private String riskLevel;    // "low", "medium", "high"
    private String failReason;   // Reason for failure

    public LoginAttempt() {}

    public LoginAttempt(int userId, String email, String ipAddress, 
                        String device, String status, String riskLevel) {
        this.userId    = userId;
        this.email     = email;
        this.ipAddress = ipAddress;
        this.device    = device;
        this.status    = status;
        this.riskLevel = riskLevel;
    }

    public int getId()                   { return id; }
    public void setId(int id)            { this.id = id; }

    public int getUserId()               { return userId; }
    public void setUserId(int u)         { this.userId = u; }

    public String getEmail()             { return email; }
    public void setEmail(String e)       { this.email = e; }

    public Date getTimestamp()           { return timestamp; }
    public void setTimestamp(Date t)     { this.timestamp = t; }

    public String getIpAddress()         { return ipAddress; }
    public void setIpAddress(String ip)  { this.ipAddress = ip; }

    public String getDevice()            { return device; }
    public void setDevice(String d)      { this.device = d; }

    public String getStatus()            { return status; }
    public void setStatus(String s)      { this.status = s; }

    public String getRiskLevel()         { return riskLevel; }
    public void setRiskLevel(String r)   { this.riskLevel = r; }

    public String getFailReason()        { return failReason; }
    public void setFailReason(String r)  { this.failReason = r; }
}

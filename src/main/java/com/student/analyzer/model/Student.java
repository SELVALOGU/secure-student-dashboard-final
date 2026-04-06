package com.student.analyzer.model;

/**
 * Student model — extends user with academic details.
 */
public class Student {
    private int    id;
    private int    userId;
    private int    staffId;
    private double attendance;    // Percentage (0-100)
    private String grade;         // A+, A, B, C, D, F
    private String rollNumber;
    private String department;
    private int    semester;

    // Joined fields (from users table via JOIN)
    private String name;
    private String email;
    private int    loginScore;
    private String status;
    private String staffName;     // Name of assigned staff
    private String lastLogin;

    // Default constructor
    public Student() {}

    // Getters and Setters
    public int getId()                    { return id; }
    public void setId(int id)             { this.id = id; }

    public int getUserId()                { return userId; }
    public void setUserId(int userId)     { this.userId = userId; }

    public int getStaffId()               { return staffId; }
    public void setStaffId(int staffId)   { this.staffId = staffId; }

    public double getAttendance()         { return attendance; }
    public void setAttendance(double a)   { this.attendance = a; }

    public String getGrade()              { return grade; }
    public void setGrade(String grade)    { this.grade = grade; }

    public String getRollNumber()         { return rollNumber; }
    public void setRollNumber(String r)   { this.rollNumber = r; }

    public String getDepartment()         { return department; }
    public void setDepartment(String d)   { this.department = d; }

    public int getSemester()              { return semester; }
    public void setSemester(int s)        { this.semester = s; }

    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    public String getEmail()             { return email; }
    public void setEmail(String email)   { this.email = email; }

    public int getLoginScore()           { return loginScore; }
    public void setLoginScore(int s)     { this.loginScore = s; }

    public String getStatus()           { return status; }
    public void setStatus(String s)     { this.status = s; }

    public String getStaffName()        { return staffName; }
    public void setStaffName(String s)  { this.staffName = s; }

    public String getLastLogin()        { return lastLogin; }
    public void setLastLogin(String l)  { this.lastLogin = l; }
}

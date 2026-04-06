package com.student.analyzer.model;

/**
 * Staff model — represents a teacher/lecturer.
 */
public class Staff {
    private int    id;
    private int    userId;
    private String department;
    private String designation;

    // Joined fields
    private String name;
    private String email;
    private int    loginScore;
    private String status;

    public Staff() {}

    public int getId()                   { return id; }
    public void setId(int id)            { this.id = id; }

    public int getUserId()               { return userId; }
    public void setUserId(int u)         { this.userId = u; }

    public String getDepartment()        { return department; }
    public void setDepartment(String d)  { this.department = d; }

    public String getDesignation()       { return designation; }
    public void setDesignation(String d) { this.designation = d; }

    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    public String getEmail()             { return email; }
    public void setEmail(String e)       { this.email = e; }

    public int getLoginScore()           { return loginScore; }
    public void setLoginScore(int s)     { this.loginScore = s; }

    public String getStatus()            { return status; }
    public void setStatus(String s)      { this.status = s; }
}

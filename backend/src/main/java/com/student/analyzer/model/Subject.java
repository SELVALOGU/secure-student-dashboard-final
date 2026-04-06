package com.student.analyzer.model;

/**
 * Subject model — a course taught by a staff member.
 */
public class Subject {
    private int    id;
    private String subjectName;
    private String subjectCode;
    private int    staffId;
    private int    credits;

    // Joined fields
    private String staffName;       // Name of teaching staff
    private String enrollmentStatus; // Student's enrollment status (for student view)

    public Subject() {}

    public int getId()                   { return id; }
    public void setId(int id)            { this.id = id; }

    public String getSubjectName()       { return subjectName; }
    public void setSubjectName(String n) { this.subjectName = n; }

    public String getSubjectCode()       { return subjectCode; }
    public void setSubjectCode(String c) { this.subjectCode = c; }

    public int getStaffId()              { return staffId; }
    public void setStaffId(int s)        { this.staffId = s; }

    public int getCredits()              { return credits; }
    public void setCredits(int c)        { this.credits = c; }

    public String getStaffName()         { return staffName; }
    public void setStaffName(String s)   { this.staffName = s; }

    public String getEnrollmentStatus()  { return enrollmentStatus; }
    public void setEnrollmentStatus(String s){ this.enrollmentStatus = s; }
}

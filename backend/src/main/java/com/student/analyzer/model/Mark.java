package com.student.analyzer.model;

/**
 * Mark model — stores marks for a student in a subject.
 */
public class Mark {
    private int    id;
    private int    studentId;
    private int    subjectId;
    private double marks;
    private double maxMarks;
    private String updatedAt;

    // Joined fields
    private String subjectName;
    private String subjectCode;
    private String studentName;

    public Mark() {}

    public int getId()                     { return id; }
    public void setId(int id)              { this.id = id; }

    public int getStudentId()              { return studentId; }
    public void setStudentId(int s)        { this.studentId = s; }

    public int getSubjectId()              { return subjectId; }
    public void setSubjectId(int s)        { this.subjectId = s; }

    public double getMarks()               { return marks; }
    public void setMarks(double m)         { this.marks = m; }

    public double getMaxMarks()            { return maxMarks; }
    public void setMaxMarks(double m)      { this.maxMarks = m; }

    public String getUpdatedAt()           { return updatedAt; }
    public void setUpdatedAt(String u)     { this.updatedAt = u; }

    public String getSubjectName()         { return subjectName; }
    public void setSubjectName(String s)   { this.subjectName = s; }

    public String getSubjectCode()         { return subjectCode; }
    public void setSubjectCode(String s)   { this.subjectCode = s; }

    public String getStudentName()         { return studentName; }
    public void setStudentName(String s)   { this.studentName = s; }

    /** Returns percentage score as a double */
    public double getPercentage() {
        if (maxMarks <= 0) return 0;
        return (marks / maxMarks) * 100.0;
    }
}

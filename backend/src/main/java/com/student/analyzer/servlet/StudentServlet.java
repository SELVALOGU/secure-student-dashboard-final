package com.student.analyzer.servlet;

import com.student.analyzer.dao.LoginAttemptDAO;
import com.student.analyzer.dao.StudentDAO;
import com.student.analyzer.dao.SubjectDAO;
import com.student.analyzer.dao.UnblockRequestDAO;
import com.student.analyzer.model.LoginAttempt;
import com.student.analyzer.model.Mark;
import com.student.analyzer.model.Student;
import com.student.analyzer.model.Subject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * StudentServlet — API endpoint for student dashboard data.
 *
 * GET /api/student?action=dashboard  → Full dashboard data (JSON)
 * GET /api/student?action=history    → Login history
 * GET /api/student?action=marks      → Marks data for Chart.js
 * GET /api/student?action=subjects   → Enrolled subjects
 * POST /api/student?action=unblock   → Request account unblock
 * POST /api/student?action=enroll    → Enroll in a subject
 */
@WebServlet("/api/student")
public class StudentServlet extends HttpServlet {

    private StudentDAO       studentDAO       = new StudentDAO();
    private LoginAttemptDAO  loginAttemptDAO  = new LoginAttemptDAO();
    private UnblockRequestDAO unblockDAO      = new UnblockRequestDAO();
    private SubjectDAO       subjectDAO       = new SubjectDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Check session
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            sendUnauthorized(resp);
            return;
        }

        String role = (String) session.getAttribute("role");
        // Admin and staff can also access student data
        int userId = (Integer) session.getAttribute("userId");
        
        // If staff/admin is viewing a specific student
        String targetStudentIdParam = req.getParameter("studentId");
        if (targetStudentIdParam != null && ("staff".equals(role) || "admin".equals(role))) {
            // Staff/admin viewing specific student
            userId = -1; // Will use studentId directly
        }

        String action = req.getParameter("action");
        if (action == null) action = "dashboard";

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            Student student;
            if (targetStudentIdParam != null && ("staff".equals(role) || "admin".equals(role))) {
                int targetStudentId = Integer.parseInt(targetStudentIdParam);
                student = studentDAO.findById(targetStudentId);
            } else {
                student = studentDAO.findByUserId(userId);
            }

            if (student == null) {
                out.print("{\"success\":false,\"message\":\"Student not found\"}");
                return;
            }

            switch (action) {
                case "dashboard":
                    out.print(buildDashboardJson(student).toString());
                    break;
                case "history":
                    out.print(buildHistoryJson(student).toString());
                    break;
                case "marks":
                    out.print(buildMarksJson(student).toString());
                    break;
                case "subjects":
                    out.print(buildSubjectsJson(student).toString());
                    break;
                default:
                    out.print(buildDashboardJson(student).toString());
            }

        } catch (Exception e) {
            System.err.println("[StudentServlet] Error: " + e.getMessage());
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            sendUnauthorized(resp);
            return;
        }

        String action = req.getParameter("action");
        int userId  = (Integer) session.getAttribute("userId");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            Student student = studentDAO.findByUserId(userId);
            if (student == null) {
                out.print("{\"success\":false,\"message\":\"Student not found\"}");
                return;
            }

            if ("unblock".equals(action)) {
                // Check student is actually blocked
                if (!"blocked".equals(student.getStatus())) {
                    out.print("{\"success\":false,\"message\":\"Your account is not blocked\"}");
                    return;
                }

                // Check no existing pending request
                if (unblockDAO.hasPendingRequest(student.getId())) {
                    out.print("{\"success\":false,\"message\":\"You already have a pending unblock request\"}");
                    return;
                }

                String reason = req.getParameter("reason");
                if (reason == null || reason.trim().isEmpty()) {
                    reason = "No reason provided";
                }

                unblockDAO.createRequest(student.getId(), reason.trim());
                out.print("{\"success\":true,\"message\":\"Unblock request submitted successfully\"}");

            } else if ("enroll".equals(action)) {
                String subjectIdParam = req.getParameter("subjectId");
                if (subjectIdParam == null) {
                    out.print("{\"success\":false,\"message\":\"Subject ID required\"}");
                    return;
                }
                int subjectId = Integer.parseInt(subjectIdParam);
                subjectDAO.enrollStudent(student.getId(), subjectId);
                out.print("{\"success\":true,\"message\":\"Enrolled successfully\"}");

            } else {
                out.print("{\"success\":false,\"message\":\"Unknown action\"}");
            }

        } catch (Exception e) {
            System.err.println("[StudentServlet] POST Error: " + e.getMessage());
            out.print("{\"success\":false,\"message\":\"Server error\"}");
        }
    }

    // ================================================================
    // JSON BUILDERS
    // ================================================================

    private JSONObject buildDashboardJson(Student student) throws Exception {
        JSONObject data = new JSONObject();
        data.put("success", true);

        // Student info
        JSONObject info = new JSONObject();
        info.put("id",           student.getId());
        info.put("userId",       student.getUserId());
        info.put("name",         student.getName());
        info.put("email",        student.getEmail());
        info.put("rollNumber",   student.getRollNumber());
        info.put("department",   student.getDepartment());
        info.put("semester",     student.getSemester());
        info.put("attendance",   student.getAttendance());
        info.put("grade",        student.getGrade());
        info.put("loginScore",   student.getLoginScore());
        info.put("status",       student.getStatus());
        info.put("staffName",    student.getStaffName() != null ? student.getStaffName() : "Unassigned");
        info.put("lastLogin",    student.getLastLogin());
        data.put("student", info);

        // Login statistics
        int totalFails = loginAttemptDAO.countTotalFails(student.getUserId());
        int consecutiveFails = loginAttemptDAO.countConsecutiveFails(student.getUserId());
        List<LoginAttempt> recentAttempts = loginAttemptDAO.getRecentAttempts(student.getUserId(), 5);

        JSONObject stats = new JSONObject();
        stats.put("totalFails",       totalFails);
        stats.put("consecutiveFails", consecutiveFails);
        stats.put("recentAttempts",   recentAttempts.size());
        data.put("loginStats", stats);

        // Check pending unblock request
        boolean hasPendingUnblock = false;
        if ("blocked".equals(student.getStatus())) {
            hasPendingUnblock = unblockDAO.hasPendingRequest(student.getId());
        }
        data.put("hasPendingUnblock", hasPendingUnblock);

        // Marks summary
        List<Mark> marks = studentDAO.getMarksForStudent(student.getId());
        double totalMarks = 0;
        for (Mark m : marks) totalMarks += m.getMarks();
        double avgMarks = marks.isEmpty() ? 0 : totalMarks / marks.size();

        JSONObject marksSummary = new JSONObject();
        marksSummary.put("subjectCount", marks.size());
        marksSummary.put("average",      Math.round(avgMarks * 10.0) / 10.0);
        data.put("marksSummary", marksSummary);

        return data;
    }

    private JSONObject buildHistoryJson(Student student) throws Exception {
        JSONObject data = new JSONObject();
        data.put("success", true);

        List<LoginAttempt> attempts = loginAttemptDAO.getAllAttemptsForUser(student.getUserId());
        JSONArray arr = new JSONArray();
        for (LoginAttempt a : attempts) {
            JSONObject obj = new JSONObject();
            obj.put("id",        a.getId());
            obj.put("timestamp", a.getTimestamp() != null ? a.getTimestamp().toString() : "");
            obj.put("ipAddress", a.getIpAddress());
            obj.put("device",    a.getDevice());
            obj.put("status",    a.getStatus());
            obj.put("riskLevel", a.getRiskLevel());
            arr.put(obj);
        }
        data.put("history", arr);
        data.put("totalAttempts", attempts.size());
        data.put("failedAttempts", loginAttemptDAO.countTotalFails(student.getUserId()));
        return data;
    }

    private JSONObject buildMarksJson(Student student) throws Exception {
        JSONObject data = new JSONObject();
        data.put("success", true);

        List<Mark> marks = studentDAO.getMarksForStudent(student.getId());
        JSONArray labels = new JSONArray();
        JSONArray values = new JSONArray();
        JSONArray maxValues = new JSONArray();

        for (Mark m : marks) {
            labels.put(m.getSubjectCode() + ": " + m.getSubjectName());
            values.put(m.getMarks());
            maxValues.put(m.getMaxMarks());
        }

        data.put("labels",    labels);
        data.put("marks",     values);
        data.put("maxMarks",  maxValues);
        return data;
    }

    private JSONObject buildSubjectsJson(Student student) throws Exception {
        JSONObject data = new JSONObject();
        data.put("success", true);

        List<Subject> subjects = studentDAO.getEnrolledSubjects(student.getId());
        JSONArray arr = new JSONArray();
        for (Subject s : subjects) {
            JSONObject obj = new JSONObject();
            obj.put("id",          s.getId());
            obj.put("name",        s.getSubjectName());
            obj.put("code",        s.getSubjectCode());
            obj.put("credits",     s.getCredits());
            obj.put("staffName",   s.getStaffName());
            obj.put("status",      s.getEnrollmentStatus());
            arr.put(obj);
        }
        data.put("subjects", arr);
        return data;
    }

    private void sendUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json");
        resp.getWriter().print("{\"success\":false,\"message\":\"Not authenticated\"}");
    }
}

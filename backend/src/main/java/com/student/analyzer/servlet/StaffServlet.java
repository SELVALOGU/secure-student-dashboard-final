package com.student.analyzer.servlet;

import com.student.analyzer.dao.*;
import com.student.analyzer.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * StaffServlet — API for staff dashboard operations.
 *
 * GET  /api/staff?action=dashboard         → Staff info + summary
 * GET  /api/staff?action=students          → All students list
 * GET  /api/staff?action=subjects          → Staff's subjects
 * GET  /api/staff?action=marksBySubject    → Marks for a subject
 * GET  /api/staff?action=unblockRequests   → Pending unblock requests
 * POST /api/staff?action=addSubject        → Create new subject
 * POST /api/staff?action=updateMarks       → Update student marks
 * POST /api/staff?action=blockStudent      → Block a student
 * POST /api/staff?action=unblockStudent    → Unblock/approve request
 * POST /api/staff?action=rejectUnblock     → Reject unblock request
 */
@WebServlet("/api/staff")
public class StaffServlet extends HttpServlet {

    private StaffDAO          staffDAO          = new StaffDAO();
    private StudentDAO        studentDAO        = new StudentDAO();
    private LoginAttemptDAO   loginAttemptDAO   = new LoginAttemptDAO();
    private UnblockRequestDAO unblockDAO        = new UnblockRequestDAO();
    private SubjectDAO        subjectDAO        = new SubjectDAO();
    private UserDAO           userDAO           = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (!isAuthorized(session, "staff", "admin")) {
            sendUnauthorized(resp);
            return;
        }

        String action = req.getParameter("action");
        if (action == null) action = "dashboard";

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = (Integer) session.getAttribute("userId");

        try {
            Staff staff = staffDAO.findByUserId(userId);

            switch (action) {
                case "dashboard":
                    out.print(buildDashboard(staff).toString());
                    break;

                case "students":
                    out.print(buildStudentList().toString());
                    break;

                case "subjects":
                    List<Map<String, Object>> subjectData = subjectDAO.getSubjectsWithEnrollmentCount(staff.getId());
                    JSONObject subResp = new JSONObject();
                    subResp.put("success", true);
                    JSONArray subArr = new JSONArray();
                    for (Map<String, Object> s : subjectData) subArr.put(new JSONObject(s));
                    subResp.put("subjects", subArr);
                    out.print(subResp.toString());
                    break;

                case "marksBySubject":
                    String subjectIdParam = req.getParameter("subjectId");
                    if (subjectIdParam == null) {
                        out.print("{\"success\":false,\"message\":\"subjectId required\"}");
                        return;
                    }
                    int subjectId = Integer.parseInt(subjectIdParam);
                    List<Mark> marks = staffDAO.getMarksBySubject(subjectId);
                    JSONObject marksResp = new JSONObject();
                    marksResp.put("success", true);
                    JSONArray marksArr = new JSONArray();
                    for (Mark m : marks) {
                        JSONObject mo = new JSONObject();
                        mo.put("studentId",   m.getStudentId());
                        mo.put("studentName", m.getStudentName());
                        mo.put("subjectName", m.getSubjectName());
                        mo.put("subjectCode", m.getSubjectCode());
                        mo.put("marks",       m.getMarks());
                        mo.put("maxMarks",    m.getMaxMarks());
                        mo.put("percentage",  Math.round(m.getPercentage() * 10.0) / 10.0);
                        marksArr.put(mo);
                    }
                    marksResp.put("marks", marksArr);
                    out.print(marksResp.toString());
                    break;

                case "unblockRequests":
                    List<Map<String, Object>> requests = unblockDAO.getPendingRequests();
                    JSONObject reqResp = new JSONObject();
                    reqResp.put("success", true);
                    JSONArray reqArr = new JSONArray();
                    for (Map<String, Object> r : requests) reqArr.put(new JSONObject(r));
                    reqResp.put("requests", reqArr);
                    out.print(reqResp.toString());
                    break;

                case "studentHistory":
                    String studentIdParam = req.getParameter("studentId");
                    if (studentIdParam == null) {
                        out.print("{\"success\":false,\"message\":\"studentId required\"}");
                        return;
                    }
                    int studentId = Integer.parseInt(studentIdParam);
                    Student student = studentDAO.findById(studentId);
                    if (student == null) {
                        out.print("{\"success\":false,\"message\":\"Student not found\"}");
                        return;
                    }
                    List<LoginAttempt> history = loginAttemptDAO.getAllAttemptsForUser(student.getUserId());
                    JSONObject histResp = new JSONObject();
                    histResp.put("success", true);
                    histResp.put("studentName", student.getName());
                    JSONArray histArr = new JSONArray();
                    for (LoginAttempt a : history) {
                        JSONObject ao = new JSONObject();
                        ao.put("timestamp", a.getTimestamp() != null ? a.getTimestamp().toString() : "");
                        ao.put("ipAddress", a.getIpAddress());
                        ao.put("device",    a.getDevice());
                        ao.put("status",    a.getStatus());
                        ao.put("riskLevel", a.getRiskLevel());
                        histArr.put(ao);
                    }
                    histResp.put("history", histArr);
                    out.print(histResp.toString());
                    break;

                default:
                    out.print("{\"success\":false,\"message\":\"Unknown action\"}");
            }

        } catch (Exception e) {
            System.err.println("[StaffServlet] GET Error: " + e.getMessage());
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (!isAuthorized(session, "staff", "admin")) {
            sendUnauthorized(resp);
            return;
        }

        String action = req.getParameter("action");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = (Integer) session.getAttribute("userId");

        try {
            Staff staff = staffDAO.findByUserId(userId);

            switch (action != null ? action : "") {

                case "addSubject":
                    String subjectName = req.getParameter("subjectName");
                    String subjectCode = req.getParameter("subjectCode");
                    String creditsStr  = req.getParameter("credits");
                    if (subjectName == null || subjectCode == null) {
                        out.print("{\"success\":false,\"message\":\"Subject name and code required\"}");
                        return;
                    }
                    int credits = (creditsStr != null) ? Integer.parseInt(creditsStr) : 3;
                    staffDAO.addSubject(subjectName, subjectCode, staff.getId(), credits);
                    out.print("{\"success\":true,\"message\":\"Subject added successfully\"}");
                    break;

                case "updateMarks":
                    String studentIdStr = req.getParameter("studentId");
                    String subjectIdStr = req.getParameter("subjectId");
                    String marksStr     = req.getParameter("marks");
                    String maxMarksStr  = req.getParameter("maxMarks");
                    if (studentIdStr == null || subjectIdStr == null || marksStr == null) {
                        out.print("{\"success\":false,\"message\":\"studentId, subjectId, and marks required\"}");
                        return;
                    }
                    double maxM = (maxMarksStr != null) ? Double.parseDouble(maxMarksStr) : 100.0;
                    staffDAO.updateMarks(Integer.parseInt(studentIdStr),
                                        Integer.parseInt(subjectIdStr),
                                        Double.parseDouble(marksStr), maxM);
                    out.print("{\"success\":true,\"message\":\"Marks updated successfully\"}");
                    break;

                case "blockStudent":
                    String blockStudentId = req.getParameter("studentId");
                    if (blockStudentId == null) {
                        out.print("{\"success\":false,\"message\":\"studentId required\"}");
                        return;
                    }
                    Student toBlock = studentDAO.findById(Integer.parseInt(blockStudentId));
                    if (toBlock != null) {
                        userDAO.updateStatus(toBlock.getUserId(), "blocked");
                        out.print("{\"success\":true,\"message\":\"Student blocked successfully\"}");
                    } else {
                        out.print("{\"success\":false,\"message\":\"Student not found\"}");
                    }
                    break;

                case "unblockStudent":
                    String requestIdStr = req.getParameter("requestId");
                    if (requestIdStr == null) {
                        out.print("{\"success\":false,\"message\":\"requestId required\"}");
                        return;
                    }
                    int requestId = Integer.parseInt(requestIdStr);
                    int studentId = unblockDAO.getStudentIdFromRequest(requestId);
                    if (studentId < 0) {
                        out.print("{\"success\":false,\"message\":\"Request not found\"}");
                        return;
                    }
                    // Approve the request
                    unblockDAO.approveRequest(requestId, staff.getId());
                    // Find student and reset their account
                    Student toUnblock = studentDAO.findById(studentId);
                    if (toUnblock != null) {
                        userDAO.resetLoginScore(toUnblock.getUserId());
                    }
                    out.print("{\"success\":true,\"message\":\"Student unblocked successfully\"}");
                    break;

                case "rejectUnblock":
                    String rejectIdStr = req.getParameter("requestId");
                    if (rejectIdStr == null) {
                        out.print("{\"success\":false,\"message\":\"requestId required\"}");
                        return;
                    }
                    unblockDAO.rejectRequest(Integer.parseInt(rejectIdStr), staff.getId());
                    out.print("{\"success\":true,\"message\":\"Request rejected\"}");
                    break;

                default:
                    out.print("{\"success\":false,\"message\":\"Unknown action\"}");
            }

        } catch (Exception e) {
            System.err.println("[StaffServlet] POST Error: " + e.getMessage());
            out.print("{\"success\":false,\"message\":\"Server error: " + e.getMessage() + "\"}");
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private JSONObject buildDashboard(Staff staff) throws Exception {
        JSONObject data = new JSONObject();
        data.put("success", true);

        JSONObject info = new JSONObject();
        info.put("id",          staff.getId());
        info.put("name",        staff.getName());
        info.put("email",       staff.getEmail());
        info.put("department",  staff.getDepartment());
        info.put("designation", staff.getDesignation());
        info.put("loginScore",  staff.getLoginScore());
        info.put("status",      staff.getStatus());
        data.put("staff", info);

        // Summary stats
        List<Student> students = studentDAO.getAllStudents();
        int blocked = 0, lowScore = 0;
        for (Student s : students) {
            if ("blocked".equals(s.getStatus())) blocked++;
            if (s.getLoginScore() < 50) lowScore++;
        }

        JSONObject summary = new JSONObject();
        summary.put("totalStudents",   students.size());
        summary.put("blockedStudents", blocked);
        summary.put("lowScoreStudents", lowScore);
        summary.put("pendingRequests", unblockDAO.getPendingRequests().size());
        data.put("summary", summary);

        return data;
    }

    private JSONObject buildStudentList() throws Exception {
        JSONObject data = new JSONObject();
        data.put("success", true);

        List<Student> students = studentDAO.getAllStudents();
        JSONArray arr = new JSONArray();
        for (Student s : students) {
            JSONObject so = new JSONObject();
            so.put("id",         s.getId());
            so.put("userId",     s.getUserId());
            so.put("name",       s.getName());
            so.put("email",      s.getEmail());
            so.put("rollNumber", s.getRollNumber());
            so.put("department", s.getDepartment());
            so.put("semester",   s.getSemester());
            so.put("attendance", s.getAttendance());
            so.put("grade",      s.getGrade());
            so.put("loginScore", s.getLoginScore());
            so.put("status",     s.getStatus());
            so.put("staffName",  s.getStaffName() != null ? s.getStaffName() : "Unassigned");
            so.put("lastLogin",  s.getLastLogin());

            // Count fails
            int fails = loginAttemptDAO.countTotalFails(s.getUserId());
            so.put("totalFails", fails);
            arr.put(so);
        }
        data.put("students", arr);
        return data;
    }

    private boolean isAuthorized(HttpSession session, String... roles) {
        if (session == null || session.getAttribute("userId") == null) return false;
        String userRole = (String) session.getAttribute("role");
        for (String r : roles) {
            if (r.equals(userRole)) return true;
        }
        return false;
    }

    private void sendUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json");
        resp.getWriter().print("{\"success\":false,\"message\":\"Not authorized\"}");
    }
}

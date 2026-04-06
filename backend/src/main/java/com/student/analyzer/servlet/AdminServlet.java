package com.student.analyzer.servlet;

import com.student.analyzer.dao.*;
import com.student.analyzer.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.student.analyzer.util.DBConnection;
import com.student.analyzer.util.BCryptUtil;

/**
 * AdminServlet — API for admin dashboard with full system access.
 *
 * GET  /api/admin?action=dashboard        → System overview stats
 * GET  /api/admin?action=allUsers         → All users list
 * GET  /api/admin?action=loginActivity    → All login attempts (filterable)
 * GET  /api/admin?action=riskReport       → Risk analysis report
 * GET  /api/admin?action=staffList        → All staff members
 * POST /api/admin?action=createUser       → Add new user
 * POST /api/admin?action=deleteUser       → Remove user
 * POST /api/admin?action=updateUser       → Update user details
 * POST /api/admin?action=blockUser        → Block any user
 * POST /api/admin?action=unblockUser      → Unblock any user
 */
@WebServlet("/api/admin")
public class AdminServlet extends HttpServlet {

    private UserDAO         userDAO         = new UserDAO();
    private StudentDAO      studentDAO      = new StudentDAO();
    private StaffDAO        staffDAO        = new StaffDAO();
    private LoginAttemptDAO loginAttemptDAO = new LoginAttemptDAO();
    private SubjectDAO      subjectDAO      = new SubjectDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (!isAdmin(session)) {
            sendUnauthorized(resp);
            return;
        }

        String action = req.getParameter("action");
        if (action == null) action = "dashboard";

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            switch (action) {

                case "dashboard":
                    out.print(buildSystemDashboard().toString());
                    break;

                case "allUsers":
                    out.print(buildAllUsersList().toString());
                    break;

                case "staffList":
                    List<Staff> staffList = staffDAO.getAllStaff();
                    JSONObject staffResp = new JSONObject();
                    staffResp.put("success", true);
                    JSONArray staffArr = new JSONArray();
                    for (Staff s : staffList) {
                        JSONObject so = new JSONObject();
                        so.put("id",          s.getId());
                        so.put("userId",      s.getUserId());
                        so.put("name",        s.getName());
                        so.put("email",       s.getEmail());
                        so.put("department",  s.getDepartment());
                        so.put("designation", s.getDesignation());
                        so.put("loginScore",  s.getLoginScore());
                        so.put("status",      s.getStatus());
                        staffArr.put(so);
                    }
                    staffResp.put("staff", staffArr);
                    out.print(staffResp.toString());
                    break;

                case "loginActivity":
                    String riskFilter = req.getParameter("risk");
                    String limitStr   = req.getParameter("limit");
                    int limit = (limitStr != null) ? Integer.parseInt(limitStr) : 100;
                    List<LoginAttempt> attempts = loginAttemptDAO.getAllAttempts(riskFilter, limit);
                    JSONObject actResp = new JSONObject();
                    actResp.put("success", true);
                    JSONArray actArr = new JSONArray();
                    for (LoginAttempt a : attempts) {
                        JSONObject ao = new JSONObject();
                        ao.put("id",        a.getId());
                        ao.put("email",     a.getEmail() != null ? a.getEmail() : "");
                        ao.put("userId",    a.getUserId());
                        ao.put("timestamp", a.getTimestamp() != null ? a.getTimestamp().toString() : "");
                        ao.put("ipAddress", a.getIpAddress() != null ? a.getIpAddress() : "");
                        ao.put("device",    a.getDevice() != null ? a.getDevice() : "");
                        ao.put("status",    a.getStatus());
                        ao.put("riskLevel", a.getRiskLevel());
                        actArr.put(ao);
                    }
                    actResp.put("attempts", actArr);
                    actResp.put("total", attempts.size());
                    out.print(actResp.toString());
                    break;

                case "riskReport":
                    out.print(buildRiskReport().toString());
                    break;

                default:
                    out.print("{\"success\":false,\"message\":\"Unknown action\"}");
            }

        } catch (Exception e) {
            System.err.println("[AdminServlet] GET Error: " + e.getMessage());
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (!isAdmin(session)) {
            sendUnauthorized(resp);
            return;
        }

        String action = req.getParameter("action");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            switch (action != null ? action : "") {

                case "createUser":
                    String name     = req.getParameter("name");
                    String email    = req.getParameter("email");
                    String password = req.getParameter("password");
                    String role     = req.getParameter("role");
                    if (name == null || email == null || password == null || role == null) {
                        out.print("{\"success\":false,\"message\":\"All fields required\"}");
                        return;
                    }
                    // Check email uniqueness
                    if (userDAO.findByEmail(email.toLowerCase()) != null) {
                        out.print("{\"success\":false,\"message\":\"Email already exists\"}");
                        return;
                    }
                    User newUser = new User(name, email.toLowerCase(), password, role);
                    int newUserId = userDAO.createUser(newUser);
                    // Create role-specific record
                    if (newUserId > 0) {
                        if ("student".equals(role)) {
                            createStudentRecord(newUserId, req.getParameter("department"));
                        } else if ("staff".equals(role)) {
                            createStaffRecord(newUserId, req.getParameter("department"), req.getParameter("designation"));
                        }
                    }
                    out.print("{\"success\":true,\"message\":\"User created successfully\",\"userId\":" + newUserId + "}");
                    break;

                case "deleteUser":
                    String deleteId = req.getParameter("userId");
                    if (deleteId == null) {
                        out.print("{\"success\":false,\"message\":\"userId required\"}");
                        return;
                    }
                    userDAO.deleteUser(Integer.parseInt(deleteId));
                    out.print("{\"success\":true,\"message\":\"User deleted successfully\"}");
                    break;

                case "updateUser":
                    String updateId   = req.getParameter("userId");
                    String updateName  = req.getParameter("name");
                    String updateEmail = req.getParameter("email");
                    if (updateId == null) {
                        out.print("{\"success\":false,\"message\":\"userId required\"}");
                        return;
                    }
                    userDAO.updateUser(Integer.parseInt(updateId), updateName, updateEmail);
                    out.print("{\"success\":true,\"message\":\"User updated successfully\"}");
                    break;

                case "blockUser":
                    String blockId = req.getParameter("userId");
                    if (blockId == null) {
                        out.print("{\"success\":false,\"message\":\"userId required\"}");
                        return;
                    }
                    userDAO.updateStatus(Integer.parseInt(blockId), "blocked");
                    out.print("{\"success\":true,\"message\":\"User blocked\"}");
                    break;

                case "unblockUser":
                    String unblockId = req.getParameter("userId");
                    if (unblockId == null) {
                        out.print("{\"success\":false,\"message\":\"userId required\"}");
                        return;
                    }
                    userDAO.resetLoginScore(Integer.parseInt(unblockId));
                    out.print("{\"success\":true,\"message\":\"User unblocked\"}");
                    break;

                default:
                    out.print("{\"success\":false,\"message\":\"Unknown action\"}");
            }

        } catch (Exception e) {
            System.err.println("[AdminServlet] POST Error: " + e.getMessage());
            out.print("{\"success\":false,\"message\":\"Server error: " + e.getMessage() + "\"}");
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private JSONObject buildSystemDashboard() throws Exception {
        JSONObject data = new JSONObject();
        data.put("success", true);

        // Count users by role using raw SQL
        String sql = "SELECT role, COUNT(*) as cnt, " +
                     "SUM(CASE WHEN status='blocked' THEN 1 ELSE 0 END) as blocked_cnt, " +
                     "AVG(login_score) as avg_score " +
                     "FROM users GROUP BY role";

        int totalStudents=0, totalStaff=0, totalAdmins=0;
        int blockedStudents=0, blockedStaff=0;
        double avgStudentScore=0, avgStaffScore=0;

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String r = rs.getString("role");
                int cnt  = rs.getInt("cnt");
                int blk  = rs.getInt("blocked_cnt");
                double avg = rs.getDouble("avg_score");
                if ("student".equals(r))      { totalStudents=cnt; blockedStudents=blk; avgStudentScore=avg; }
                else if ("staff".equals(r))   { totalStaff=cnt;    blockedStaff=blk;    avgStaffScore=avg; }
                else if ("admin".equals(r))   { totalAdmins=cnt; }
            }
        }

        // Login attempt stats
        String loginSql = "SELECT COUNT(*) as total, " +
                          "SUM(CASE WHEN status='success' THEN 1 ELSE 0 END) as success_cnt, " +
                          "SUM(CASE WHEN status='failure' THEN 1 ELSE 0 END) as fail_cnt, " +
                          "SUM(CASE WHEN risk_level='high' THEN 1 ELSE 0 END) as high_risk " +
                          "FROM login_attempts WHERE timestamp >= NOW() - INTERVAL '7 days'";

        int totalAttempts=0, successCnt=0, failCnt=0, highRisk=0;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(loginSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                totalAttempts = rs.getInt("total");
                successCnt    = rs.getInt("success_cnt");
                failCnt       = rs.getInt("fail_cnt");
                highRisk      = rs.getInt("high_risk");
            }
        }

        JSONObject stats = new JSONObject();
        stats.put("totalStudents",     totalStudents);
        stats.put("totalStaff",        totalStaff);
        stats.put("totalAdmins",       totalAdmins);
        stats.put("blockedStudents",   blockedStudents);
        stats.put("blockedStaff",      blockedStaff);
        stats.put("avgStudentScore",   Math.round(avgStudentScore));
        stats.put("avgStaffScore",     Math.round(avgStaffScore));
        stats.put("weeklyAttempts",    totalAttempts);
        stats.put("weeklySuccess",     successCnt);
        stats.put("weeklyFails",       failCnt);
        stats.put("weeklyHighRisk",    highRisk);
        data.put("stats", stats);

        // Subjects count
        List<Map<String, Object>> subjects = subjectDAO.getAllSubjectsWithEnrollment();
        data.put("totalSubjects", subjects.size());

        return data;
    }

    private JSONObject buildAllUsersList() throws Exception {
        JSONObject data = new JSONObject();
        data.put("success", true);

        String sql = "SELECT u.*, " +
                     "CASE WHEN u.role='student' THEN s.roll_number ELSE '' END AS roll_number, " +
                     "CASE WHEN u.role='student' THEN s.department WHEN u.role='staff' THEN st.department ELSE '' END AS department " +
                     "FROM users u " +
                     "LEFT JOIN students s ON u.id = s.user_id " +
                     "LEFT JOIN staff st ON u.id = st.user_id " +
                     "ORDER BY u.role, u.name";

        JSONArray arr = new JSONArray();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject uo = new JSONObject();
                uo.put("id",          rs.getInt("id"));
                uo.put("name",        rs.getString("name"));
                uo.put("email",       rs.getString("email"));
                uo.put("role",        rs.getString("role"));
                uo.put("loginScore",  rs.getInt("login_score"));
                uo.put("status",      rs.getString("status"));
                uo.put("rollNumber",  rs.getString("roll_number") != null ? rs.getString("roll_number") : "");
                uo.put("department",  rs.getString("department") != null ? rs.getString("department") : "");
                uo.put("createdAt",   rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "");
                uo.put("lastLogin",   rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toString() : "Never");
                arr.put(uo);
            }
        }
        data.put("users", arr);
        return data;
    }

    private JSONObject buildRiskReport() throws Exception {
        JSONObject data = new JSONObject();
        data.put("success", true);

        // Risk distribution
        String riskSql = "SELECT risk_level, COUNT(*) as cnt FROM login_attempts GROUP BY risk_level";
        JSONObject riskDist = new JSONObject();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(riskSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                riskDist.put(rs.getString("risk_level"), rs.getInt("cnt"));
            }
        }
        data.put("riskDistribution", riskDist);

        // Top risky users
        String topRiskSql = "SELECT u.name, u.email, u.role, u.login_score, u.status, " +
                            "COUNT(la.id) as total_attempts, " +
                            "SUM(CASE WHEN la.risk_level='high' THEN 1 ELSE 0 END) as high_risk_count " +
                            "FROM users u " +
                            "LEFT JOIN login_attempts la ON u.id = la.user_id " +
                            "GROUP BY u.id, u.name, u.email, u.role, u.login_score, u.status " +
                            "HAVING high_risk_count > 0 " +
                            "ORDER BY high_risk_count DESC LIMIT 10";
        JSONArray riskyUsers = new JSONArray();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(topRiskSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject uo = new JSONObject();
                uo.put("name",          rs.getString("name"));
                uo.put("email",         rs.getString("email"));
                uo.put("role",          rs.getString("role"));
                uo.put("loginScore",    rs.getInt("login_score"));
                uo.put("status",        rs.getString("status"));
                uo.put("totalAttempts", rs.getInt("total_attempts"));
                uo.put("highRiskCount", rs.getInt("high_risk_count"));
                riskyUsers.put(uo);
            }
        }
        data.put("riskyUsers", riskyUsers);

        // Daily login trend (last 7 days)
        String trendSql = "SELECT DATE(timestamp) as day, COUNT(*) as total, " +
                          "SUM(CASE WHEN status='success' THEN 1 ELSE 0 END) as success, " +
                          "SUM(CASE WHEN status='failure' THEN 1 ELSE 0 END) as failure " +
                          "FROM login_attempts " +
                          "WHERE timestamp >= NOW() - INTERVAL '7 days' " +
                          "GROUP BY DATE(timestamp) ORDER BY day";
        JSONArray trend = new JSONArray();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(trendSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject row = new JSONObject();
                row.put("day",     rs.getString("day"));
                row.put("total",   rs.getInt("total"));
                row.put("success", rs.getInt("success"));
                row.put("failure", rs.getInt("failure"));
                trend.put(row);
            }
        }
        data.put("dailyTrend", trend);

        return data;
    }

    private void createStudentRecord(int userId, String department) {
        try {
            String sql = "INSERT INTO students (user_id, department, attendance, grade, roll_number, semester) " +
                         "VALUES (?, ?, 0, 'N/A', ?, 1)";
            String dept = (department != null) ? department : "General";
            String rollNo = "STU" + (1000 + userId);
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, dept);
                ps.setString(3, rollNo);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[AdminServlet] Error creating student record: " + e.getMessage());
        }
    }

    private void createStaffRecord(int userId, String department, String designation) {
        try {
            String sql = "INSERT INTO staff (user_id, department, designation) VALUES (?, ?, ?)";
            String dept  = (department != null)   ? department   : "General";
            String desig = (designation != null)  ? designation  : "Lecturer";
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, dept);
                ps.setString(3, desig);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[AdminServlet] Error creating staff record: " + e.getMessage());
        }
    }

    private boolean isAdmin(HttpSession session) {
        if (session == null || session.getAttribute("userId") == null) return false;
        return "admin".equals(session.getAttribute("role"));
    }

    private void sendUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json");
        resp.getWriter().print("{\"success\":false,\"message\":\"Admin access required\"}");
    }
}

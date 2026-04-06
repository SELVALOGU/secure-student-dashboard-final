package com.student.analyzer.servlet;

import com.student.analyzer.dao.StudentDAO;
import com.student.analyzer.dao.UnblockRequestDAO;
import com.student.analyzer.dao.UserDAO;
import com.student.analyzer.model.Student;
import com.student.analyzer.model.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * PublicUnblockServlet — Allows BLOCKED students to request unblock
 * WITHOUT being logged in. Only requires their email + reason.
 *
 * POST /api/public-unblock?email=...&reason=...
 */
@WebServlet("/api/public-unblock")
public class PublicUnblockServlet extends HttpServlet {

    private UserDAO           userDAO     = new UserDAO();
    private StudentDAO        studentDAO  = new StudentDAO();
    private UnblockRequestDAO unblockDAO  = new UnblockRequestDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String email  = req.getParameter("email");
        String reason = req.getParameter("reason");

        if (email == null || email.trim().isEmpty()) {
            out.print("{\"success\":false,\"message\":\"Email is required\"}");
            return;
        }

        email = email.trim().toLowerCase();

        try {
            // Find user by email
            User user = userDAO.findByEmail(email);
            if (user == null) {
                out.print("{\"success\":false,\"message\":\"No account found with this email address\"}");
                return;
            }

            // Must be a student
            if (!"student".equals(user.getRole())) {
                out.print("{\"success\":false,\"message\":\"Unblock requests are only for student accounts\"}");
                return;
            }

            // Must be blocked
            if (!"blocked".equals(user.getStatus())) {
                out.print("{\"success\":false,\"message\":\"Your account is not blocked. Please login normally.\"}");
                return;
            }

            // Find student record
            Student student = studentDAO.findByUserId(user.getId());
            if (student == null) {
                out.print("{\"success\":false,\"message\":\"Student record not found\"}");
                return;
            }

            // Check for existing pending request
            if (unblockDAO.hasPendingRequest(student.getId())) {
                out.print("{\"success\":false,\"message\":\"You already have a pending unblock request. Please wait for staff review.\"}");
                return;
            }

            // Create the request
            String finalReason = (reason != null && !reason.trim().isEmpty())
                                  ? reason.trim()
                                  : "No reason provided - submitted from login page";
            unblockDAO.createRequest(student.getId(), finalReason);

            out.print("{\"success\":true,\"message\":\"Unblock request submitted successfully! Your staff will review it shortly.\",\"studentName\":\"" + escapeJson(user.getName()) + "\"}");

        } catch (Exception e) {
            System.err.println("[PublicUnblockServlet] Error: " + e.getMessage());
            out.print("{\"success\":false,\"message\":\"Server error. Please try again.\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        // Check if an email belongs to a blocked account (used by JS to show the panel)
        String email = req.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            resp.getWriter().print("{\"blocked\":false}");
            return;
        }
        try {
            User user = userDAO.findByEmail(email.trim().toLowerCase());
            if (user != null && "blocked".equals(user.getStatus()) && "student".equals(user.getRole())) {
                resp.getWriter().print("{\"blocked\":true,\"name\":\"" + escapeJson(user.getName()) + "\"}");
            } else {
                resp.getWriter().print("{\"blocked\":false}");
            }
        } catch (Exception e) {
            resp.getWriter().print("{\"blocked\":false}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

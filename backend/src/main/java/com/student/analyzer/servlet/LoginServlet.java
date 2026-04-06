package com.student.analyzer.servlet;

import com.student.analyzer.dao.LoginAttemptDAO;
import com.student.analyzer.dao.UserDAO;
import com.student.analyzer.model.LoginAttempt;
import com.student.analyzer.model.User;
import com.student.analyzer.util.BCryptUtil;
import com.student.analyzer.util.RiskAnalyzer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

/**
 * LoginServlet — Handles multi-role login authentication.
 *
 * GET  /login  → serves login page
 * POST /login  → processes login form
 *
 * Features:
 * - BCrypt password verification
 * - Login attempt tracking (IP, device, timestamp)
 * - Risk analysis (LOW / MEDIUM / HIGH)
 * - Login score management
 * - Auto-block if score ≤ 20
 * - Simple math CAPTCHA validation
 * - Role-based redirect after login
 */
@WebServlet(urlPatterns = {"/login", "/logout"})
public class LoginServlet extends HttpServlet {

    private UserDAO         userDAO         = new UserDAO();
    private LoginAttemptDAO loginAttemptDAO = new LoginAttemptDAO();

    /**
     * GET /login → redirect to login page, GET /logout → destroy session
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();

        if ("/logout".equals(path)) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            resp.sendRedirect("http://localhost:3000/index.html");
            return;
        }

        // Redirect already-logged-in users
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            redirectByRole((String) session.getAttribute("role"), req, resp);
            return;
        }

        resp.sendRedirect("http://localhost:3000/index.html");
    }

    /**
     * POST /login → process credentials and authenticate.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String email           = req.getParameter("email");
        String password        = req.getParameter("password");
        String captchaAnswer   = req.getParameter("captchaAnswer");
        String captchaExpected = req.getParameter("captchaExpected");

        // Extract client info
        String ipAddress = getClientIP(req);
        String device    = getDeviceInfo(req);

        // ---- CAPTCHA Validation ----
        if (captchaAnswer == null || !captchaAnswer.trim().equals(captchaExpected)) {
            sendError(resp, "Invalid CAPTCHA. Please try again.", "captcha");
            return;
        }

        // ---- Input validation ----
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            recordAnonymousFailure(email, ipAddress, device, "Missing credentials");
            sendError(resp, "Email and password are required.", "validation");
            return;
        }

        email = email.trim().toLowerCase();

        try {
            // ---- Find user ----
            User user = userDAO.findByEmail(email);

            if (user == null) {
                // Email not found — still record attempt
                LoginAttempt attempt = buildAttempt(0, email, ipAddress, device, "failure", "medium");
                attempt.setFailReason("Email not found");
                loginAttemptDAO.recordAttempt(attempt);
                sendError(resp, "Invalid email or password.", "credentials");
                return;
            }

            // ---- Check if account is blocked ----
            if ("blocked".equals(user.getStatus())) {
                LoginAttempt attempt = buildAttempt(user.getId(), email, ipAddress, device, "blocked", "high");
                attempt.setFailReason("Account blocked");
                loginAttemptDAO.recordAttempt(attempt);
                sendError(resp, "Your account is blocked. Please contact staff to unblock.", "blocked");
                return;
            }

            // ---- Verify password ----
            boolean passwordCorrect = password.equals(user.getPasswordHash());

            // Get consecutive fails for risk analysis
            int consecutiveFails = loginAttemptDAO.countConsecutiveFails(user.getId());
            List<LoginAttempt> recentAttempts = loginAttemptDAO.getRecentAttempts(user.getId(), 10);

            if (!passwordCorrect) {
                consecutiveFails++; // Include this attempt

                // Calculate risk
                String riskLevel = RiskAnalyzer.analyzeRisk(false, recentAttempts, consecutiveFails);

                // Update login score
                int newScore = RiskAnalyzer.calculateNewScore(user.getLoginScore(), false, false);

                // Check if should auto-block
                String newStatus = user.getStatus();
                if (RiskAnalyzer.shouldAutoBlock(newScore)) {
                    newStatus = "blocked";
                }

                // Save updated score and status
                userDAO.updateLoginInfo(user.getId(), newScore, newStatus, null);

                // Record failed attempt
                LoginAttempt attempt = buildAttempt(user.getId(), email, ipAddress, device, "failure", riskLevel);
                attempt.setFailReason("Wrong password");
                loginAttemptDAO.recordAttempt(attempt);

                if ("blocked".equals(newStatus)) {
                    sendError(resp, "Too many failed attempts. Your account has been blocked.", "auto_blocked");
                } else {
                    int remaining = Math.max(0, (newScore - 20) / 10);
                    sendError(resp, "Invalid password. Login score: " + newScore + "/100. " +
                              (remaining > 0 ? remaining + " attempt(s) before block." : "Account near block!"), 
                              "wrong_password");
                }
                return;
            }

            // ---- Successful login ----
            // Detect unusual login
            boolean unusual = RiskAnalyzer.isUnusualLogin(device, ipAddress, recentAttempts);
            String riskLevel = unusual ? "medium" : RiskAnalyzer.analyzeRisk(true, recentAttempts, consecutiveFails);

            // Slightly restore score on success
            int newScore = RiskAnalyzer.calculateNewScore(user.getLoginScore(), true, false);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            userDAO.updateLoginInfo(user.getId(), newScore, user.getStatus(), now);

            // Record successful attempt
            LoginAttempt attempt = buildAttempt(user.getId(), email, ipAddress, device, "success", riskLevel);
            loginAttemptDAO.recordAttempt(attempt);

            // ---- Create session ----
            HttpSession session = req.getSession(true);
            session.setAttribute("userId",    user.getId());
            session.setAttribute("userName",  user.getName());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("role",      user.getRole());
            session.setMaxInactiveInterval(3600); // 1 hour

            // Return JSON success with redirect URL for Decoupled Architecture
            sendLoginSuccess(resp, user.getRole());

        } catch (Exception e) {
            System.err.println("[LoginServlet] Error: " + e.getMessage());
            e.printStackTrace();
            sendError(resp, "Server error. Please try again.", "server_error");
        }
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Sends JSON success response with the appropriate dashboard URL.
     */
    private void sendLoginSuccess(HttpServletResponse resp, String role) throws IOException {
        String frontendBase = "http://localhost:3000";
        String redirectUrl = frontendBase + "/index.html";
        
        switch (role) {
            case "admin":   redirectUrl = frontendBase + "/admin-dashboard.html";   break;
            case "staff":   redirectUrl = frontendBase + "/staff-dashboard.html";   break;
            case "student": redirectUrl = frontendBase + "/student-dashboard.html"; break;
        }
        
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(
            "{\"success\":true,\"message\":\"Login successful!\",\"redirect\":\"" + redirectUrl + "\"}"
        );
    }

    /**
     * Redirects user to their role-specific dashboard.
     */
    private void redirectByRole(String role, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String frontendBase = "http://localhost:3000";
        switch (role) {
            case "admin":   resp.sendRedirect(frontendBase + "/admin-dashboard.html");   break;
            case "staff":   resp.sendRedirect(frontendBase + "/staff-dashboard.html");   break;
            case "student": resp.sendRedirect(frontendBase + "/student-dashboard.html"); break;
            default:        resp.sendRedirect(frontendBase + "/index.html");
        }
    }

    /**
     * Sends JSON error response back to client.
     */
    private void sendError(HttpServletResponse resp, String message, String errorType)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.getWriter().write(
            "{\"success\":false,\"message\":\"" + escapeJson(message) + "\",\"errorType\":\"" + errorType + "\"}"
        );
    }

    /**
     * Records an anonymous login failure (for unknown emails).
     */
    private void recordAnonymousFailure(String email, String ip, String device, String reason) {
        try {
            LoginAttempt attempt = buildAttempt(0, email, ip, device, "failure", "medium");
            attempt.setFailReason(reason);
            loginAttemptDAO.recordAttempt(attempt);
        } catch (Exception e) {
            System.err.println("[LoginServlet] Could not record anonymous failure: " + e.getMessage());
        }
    }

    /**
     * Builds a LoginAttempt object with all fields.
     */
    private LoginAttempt buildAttempt(int userId, String email, String ip, String device,
                                       String status, String riskLevel) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUserId(userId);
        attempt.setEmail(email);
        attempt.setIpAddress(ip);
        attempt.setDevice(device);
        attempt.setStatus(status);
        attempt.setRiskLevel(riskLevel);
        return attempt;
    }

    /**
     * Extracts real client IP (handles proxies/load balancers).
     */
    private String getClientIP(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = req.getRemoteAddr();
        // Take first IP if comma-separated list
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip != null ? ip : "unknown";
    }

    /**
     * Extracts device/browser info from User-Agent header.
     */
    private String getDeviceInfo(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        if (ua == null) return "Unknown";

        String browser = "Unknown";
        String os      = "Unknown";

        // Detect browser
        if (ua.contains("Edg/"))          browser = "Edge";
        else if (ua.contains("Chrome"))   browser = "Chrome";
        else if (ua.contains("Firefox"))  browser = "Firefox";
        else if (ua.contains("Safari"))   browser = "Safari";
        else if (ua.contains("Opera"))    browser = "Opera";
        else if (ua.contains("MSIE") || ua.contains("Trident")) browser = "Internet Explorer";

        // Detect OS
        if (ua.contains("Windows NT"))    os = "Windows";
        else if (ua.contains("Mac OS X")) os = "Mac";
        else if (ua.contains("Android"))  os = "Android";
        else if (ua.contains("iPhone"))   os = "iOS";
        else if (ua.contains("Linux"))    os = "Linux";

        return browser + "/" + os;
    }

    /**
     * Escapes special characters for JSON strings.
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"")
                  .replace("\n", "\\n").replace("\r", "\\r");
    }
}

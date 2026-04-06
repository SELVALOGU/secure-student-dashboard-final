package com.student.analyzer.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import org.json.JSONObject;

/**
 * AuthServlet — Endpoint to check current session status.
 * Used by frontend dashboards to verify authentication before loading.
 */
@WebServlet("/api/auth/status")
public class AuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        resp.setContentType("application/json;charset=UTF-8");
        
        JSONObject json = new JSONObject();
        if (session != null && session.getAttribute("userId") != null) {
            json.put("authenticated", true);
            json.put("role",      session.getAttribute("role"));
            json.put("userName",  session.getAttribute("userName"));
            json.put("userId",    session.getAttribute("userId"));
        } else {
            json.put("authenticated", false);
        }
        
        resp.getWriter().write(json.toString());
    }
}

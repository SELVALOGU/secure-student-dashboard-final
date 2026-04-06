package com.student.analyzer.test;

import com.student.analyzer.util.DBConnection;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

public class InitializeNeonDB {
    public static void main(String[] args) throws Exception {
        System.out.println("Initializing Neon PostgreSQL Database...");
        
        String sqlFilePath = "setup_postgres.sql";
        File f = new File(sqlFilePath);
        if (!f.exists()) {
            throw new RuntimeException("ERROR: setup_postgres.sql not found at " + f.getAbsolutePath());
        }
        
        String fullSql = new String(Files.readAllBytes(Paths.get(sqlFilePath)));
        
        Connection conn = DBConnection.getConnection();
        Statement stmt = conn.createStatement();
        
        System.out.println("Executing SQL statements...");
        
        // Simple manual split to avoid regex StackOverflow
        StringBuilder sb = new StringBuilder();
        boolean inDollarBlock = false;
        
        for (int i = 0; i < fullSql.length(); i++) {
            char c = fullSql.charAt(i);
            
            // Check for $$ block start/end
            if (c == '$' && i + 1 < fullSql.length() && fullSql.charAt(i+1) == '$') {
                inDollarBlock = !inDollarBlock;
                sb.append("$$");
                i++;
                continue;
            }
            
            if (c == ';' && !inDollarBlock) {
                String cmd = sb.toString().trim();
                if (!cmd.isEmpty()) {
                    try {
                        stmt.execute(cmd);
                    } catch (Exception e) {
                        System.err.println("FAILED on: " + (cmd.length() > 100 ? cmd.substring(0, 100) + "..." : cmd));
                        throw e;
                    }
                }
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        
        // Final command if no trailing semicolon
        String finalCmd = sb.toString().trim();
        if (!finalCmd.isEmpty()) {
            stmt.execute(finalCmd);
        }
        
        System.out.println("SUCCESS! Database tables created and data seeded successfully.");
        stmt.close();
        DBConnection.closeConnection();
    }
}

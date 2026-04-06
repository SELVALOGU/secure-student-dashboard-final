package com.student.analyzer.util;

import com.student.analyzer.model.LoginAttempt;
import java.util.List;

/**
 * RiskAnalyzer - Analyzes login behavior and calculates risk levels.
 *
 * Risk scoring logic:
 * - HIGH risk:   3+ consecutive failures, logins from different IPs rapidly, score < 30
 * - MEDIUM risk: 1-2 failures, unusual device, score 30-60
 * - LOW risk:    Successful login, consistent device/IP, score > 60
 */
public class RiskAnalyzer {

    // Score penalties and thresholds
    public static final int PENALTY_WRONG_PASSWORD = 10;
    public static final int PENALTY_BLOCKED_ATTEMPT = 5;
    public static final int MIN_SCORE_TO_ALLOW      = 20;
    public static final int MIN_SCORE_AUTO_BLOCK    = 20;
    public static final int HIGH_RISK_THRESHOLD     = 3;   // 3+ failures = high risk
    public static final int MEDIUM_RISK_THRESHOLD   = 1;   // 1+ failures = medium risk

    /**
     * Determines risk level for a login event.
     *
     * @param isSuccess       Whether this login was successful
     * @param recentAttempts  Recent login attempts for this user (last 10)
     * @param consecutiveFails Number of consecutive failed attempts
     * @return "low", "medium", or "high"
     */
    public static String analyzeRisk(boolean isSuccess, List<LoginAttempt> recentAttempts, int consecutiveFails) {
        // Successful login — check if there were previous failures
        if (isSuccess) {
            if (consecutiveFails >= HIGH_RISK_THRESHOLD) {
                return "medium"; // Was having issues but now succeeded
            }
            return "low";
        }

        // Failed login — escalate based on failure count
        if (consecutiveFails >= HIGH_RISK_THRESHOLD) {
            return "high";
        } else if (consecutiveFails >= MEDIUM_RISK_THRESHOLD) {
            return "medium";
        }
        return "medium"; // Any failure is at least medium
    }

    /**
     * Calculates new login score after an event.
     *
     * @param currentScore  Current user login score (0-100)
     * @param isSuccess     Whether login succeeded
     * @param isBlocked     Whether the account was blocked
     * @return New score (clamped between 0 and 100)
     */
    public static int calculateNewScore(int currentScore, boolean isSuccess, boolean isBlocked) {
        if (isSuccess) {
            // Reward successful login — slowly restore score (max 100)
            return Math.min(100, currentScore + 2);
        } else if (isBlocked) {
            return Math.max(0, currentScore - PENALTY_BLOCKED_ATTEMPT);
        } else {
            // Penalize failed attempt
            return Math.max(0, currentScore - PENALTY_WRONG_PASSWORD);
        }
    }

    /**
     * Checks if a user should be automatically blocked based on score.
     *
     * @param loginScore Current login score
     * @return true if user should be blocked
     */
    public static boolean shouldAutoBlock(int loginScore) {
        return loginScore <= MIN_SCORE_AUTO_BLOCK;
    }

    /**
     * Detects if login is from unusual device/IP compared to history.
     * If user typically uses Chrome but now using Firefox from a different IP → suspicious.
     *
     * @param currentDevice    Device string of current attempt
     * @param currentIp        IP address of current attempt
     * @param recentAttempts   Recent successful login attempts
     * @return true if the login looks unusual
     */
    public static boolean isUnusualLogin(String currentDevice, String currentIp, 
                                          List<LoginAttempt> recentAttempts) {
        if (recentAttempts == null || recentAttempts.isEmpty()) {
            return false; // No history to compare
        }

        // Count how many recent logins came from same device
        long sameDeviceCount = recentAttempts.stream()
            .filter(a -> a.getDevice() != null && a.getDevice().equals(currentDevice))
            .count();

        long sameIpCount = recentAttempts.stream()
            .filter(a -> a.getIpAddress() != null && a.getIpAddress().equals(currentIp))
            .count();

        // If device OR IP is brand new and we have history → unusual
        return (sameDeviceCount == 0 || sameIpCount == 0) && recentAttempts.size() >= 3;
    }

    /**
     * Counts consecutive failed attempts (most recent first).
     */
    public static int countConsecutiveFails(List<LoginAttempt> attempts) {
        int count = 0;
        for (LoginAttempt attempt : attempts) {
            if ("failure".equals(attempt.getStatus()) || "blocked".equals(attempt.getStatus())) {
                count++;
            } else {
                break; // Stop at first success
            }
        }
        return count;
    }
}

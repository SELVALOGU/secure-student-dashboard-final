# SecureEdu — Secure Student Login Behavior Analyzer

A complete full-stack web application built with **Java Servlets + MySQL + Apache Tomcat** featuring multi-role login, behavior analytics, risk scoring, and role-based dashboards.

---

## 🔐 Features

| Feature | Details |
|---|---|
| Multi-role Login | Student, Staff, Admin with BCrypt passwords |
| Risk Analysis | LOW / MEDIUM / HIGH based on login behavior |
| Login Score | Reduces on failed attempts, auto-blocks at ≤20 |
| CAPTCHA | Math-based CAPTCHA on every login |
| Device Tracking | Browser + OS detection from User-Agent |
| Student Dashboard | Marks chart (Chart.js), attendance, login history |
| Staff Dashboard | Manage students, grades, block/unblock accounts |
| Admin Dashboard | System analytics, risk reports, user management |
| SQL Injection Prevention | All queries use PreparedStatements |

---

## ⚡ Quick Start

### Prerequisites (all already on your machine)
- ✅ Java 8 — `C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot`
- ✅ Maven 3.8.8 — `C:\tools\apache-maven-3.8.8`
- ✅ Tomcat 9 — `C:\tools\apache-tomcat-9.0.80` (optional, use embedded)
- ⚠️ MySQL — must be running on port 3306

### Step 1: Set up MySQL Database
```sql
-- Connect to MySQL:
mysql -u root -p

-- Then run the setup script:
source C:/Users/SELVA LOGU N/dummy 3/setup.sql
```
Or via MySQL Workbench: **File → Run SQL Script** → select `setup.sql`

> **MySQL Password:** Edit `src/main/java/com/student/analyzer/util/DBConnection.java`
> Change `DB_PASS = "root"` to your actual MySQL root password.

### Step 2: Run the Application

**Option A — Double-click** `run.bat`

**Option B — PowerShell:**
```powershell
cd "c:\Users\SELVA LOGU N\dummy 3"
.\run.ps1
```

**Option C — Manual Maven:**
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot"
C:\tools\apache-maven-3.8.8\bin\mvn.cmd clean package tomcat7:run -DskipTests
```

App starts at: **http://localhost:8080/**

---

## 🔑 Demo Credentials

| Role | Email | Password |
|---|---|---|
| Admin | admin@school.edu | Admin@123 |
| Staff | priya.sharma@school.edu | Staff@123 |
| Staff | rajesh.kumar@school.edu | Staff@123 |
| Student | arjun.nair@student.edu | Student@123 |
| Student | divya.patel@student.edu | Student@123 |
| Student (blocked) | rahul.verma@student.edu | Student@123 |

---

## 📁 Project Structure

```
secure-student-analyzer/
├── pom.xml                          Maven configuration
├── setup.sql                        Database schema + sample data
├── run.bat                          Windows launcher
├── run.ps1                          PowerShell launcher
└── src/main/
    ├── java/com/student/analyzer/
    │   ├── model/                   Data models
    │   │   ├── User.java
    │   │   ├── Student.java
    │   │   ├── Staff.java
    │   │   ├── LoginAttempt.java
    │   │   ├── Subject.java
    │   │   └── Mark.java
    │   ├── dao/                     Database access (PreparedStatements)
    │   │   ├── UserDAO.java
    │   │   ├── StudentDAO.java
    │   │   ├── StaffDAO.java
    │   │   ├── LoginAttemptDAO.java
    │   │   ├── SubjectDAO.java
    │   │   └── UnblockRequestDAO.java
    │   ├── servlet/                 HTTP request handlers
    │   │   ├── LoginServlet.java    (multi-role auth + risk analysis)
    │   │   ├── StudentServlet.java  (student API)
    │   │   ├── StaffServlet.java    (staff API)
    │   │   └── AdminServlet.java    (admin API)
    │   └── util/
    │       ├── DBConnection.java    Singleton JDBC connection
    │       ├── BCryptUtil.java      Password hashing
    │       └── RiskAnalyzer.java    Risk scoring logic
    └── webapp/
        ├── index.html               Login page (CAPTCHA + glassmorphism UI)
        ├── student-dashboard.html   Student portal
        ├── staff-dashboard.html     Staff portal
        ├── admin-dashboard.html     Admin portal
        ├── css/style.css            Dark theme stylesheet
        └── WEB-INF/web.xml          Servlet configuration
```

---

## 🗄️ Database Schema

```
users              → Base auth table (all roles)
  ├── students      → Extended student profile
  ├── staff         → Extended staff profile
  └── login_attempts → Every login tracked

subjects           → Courses taught by staff
student_subjects   → Enrollment (student ↔ subject)
marks              → Marks per student per subject
unblock_requests   → Student → Staff unblock flow
```

---

## 🔒 Security Architecture

```
Login Request
    ↓
Math CAPTCHA Check
    ↓
Email Lookup (PreparedStatement)
    ↓
Account Status Check (blocked?)
    ↓
BCrypt Password Verify
    ↓
Risk Analysis (consecutive fails, IP, device)
    ↓
Score Update (−10 per fail, +2 on success)
    ↓
Auto-block if score ≤ 20
    ↓
Record Attempt (IP, device, risk level, timestamp)
    ↓
Session Create → Role-based redirect
```

---

## 🛠️ Configuration

### Change MySQL Password
Edit `DBConnection.java`:
```java
private static final String DB_PASS = "your_password_here";
```

### Change Server Port
Edit `pom.xml` in the tomcat7-maven-plugin section:
```xml
<port>8080</port>  <!-- Change to 9090 or any free port -->
```

### Risk Score Thresholds
Edit `RiskAnalyzer.java`:
```java
public static final int PENALTY_WRONG_PASSWORD = 10;  // Points deducted per fail
public static final int MIN_SCORE_AUTO_BLOCK    = 20;  // Block threshold
```

---

## 📊 Login Risk Logic

| Consecutive Fails | Risk Level | Action |
|---|---|---|
| 0 (success) | LOW | Normal login, score +2 |
| 1-2 | MEDIUM | Warning, score −10 |
| 3+ | HIGH | High alert, score −10 |
| Score ≤ 20 | CRITICAL | Account auto-blocked |

---

## 🆘 Troubleshooting

| Problem | Solution |
|---|---|
| `Communications link failure` | MySQL is not running. Start MySQL service. |
| Port 8080 in use | Change port in pom.xml or kill process using port |
| `Build FAILED` | Check Java version is 8, run `mvn -version` to verify |
| Wrong password error on fresh install | Re-run `setup.sql` to reset the database |
| Blank dashboard after login | Check browser console (F12) for API errors |

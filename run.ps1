# ============================================================
#  SecureEdu - Run Script (PowerShell - Auto-configured)
# ============================================================

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot"
$MVN = "C:\tools\apache-maven-3.8.8\bin\mvn.cmd"

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  SecureEdu - Secure Student Analyzer" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check Java
Write-Host "[1/4] Checking Java..." -ForegroundColor Yellow
if (Test-Path "$env:JAVA_HOME\bin\java.exe") {
    Write-Host "      OK: $env:JAVA_HOME" -ForegroundColor Green
} else {
    Write-Host "      ERROR: Java not found at $env:JAVA_HOME" -ForegroundColor Red
    Read-Host "Press Enter to exit"; exit 1
}

# Check Maven
Write-Host "[2/4] Checking Maven..." -ForegroundColor Yellow
if (Test-Path $MVN) {
    Write-Host "      OK: $MVN" -ForegroundColor Green
} else {
    Write-Host "      ERROR: Maven not found at $MVN" -ForegroundColor Red
    Read-Host "Press Enter to exit"; exit 1
}

# Build
Write-Host "[3/4] Building project..." -ForegroundColor Yellow
Set-Location $PSScriptRoot
& $MVN clean package -DskipTests --no-transfer-progress
if ($LASTEXITCODE -ne 0) {
    Write-Host "BUILD FAILED." -ForegroundColor Red
    Read-Host "Press Enter to exit"; exit 1
}
Write-Host "      Build SUCCESS!" -ForegroundColor Green

# Start
Write-Host ""
Write-Host "[4/4] Starting embedded Tomcat..." -ForegroundColor Yellow
Write-Host ""
Write-Host "  Login Page : http://localhost:8080/" -ForegroundColor White
Write-Host "  Admin      : admin@school.edu / Admin@123" -ForegroundColor White
Write-Host "  Staff      : priya.sharma@school.edu / Staff@123" -ForegroundColor White
Write-Host "  Student    : arjun.nair@student.edu / Student@123" -ForegroundColor White
Write-Host ""
Write-Host "  Press Ctrl+C to stop." -ForegroundColor DarkGray
Write-Host "=========================================" -ForegroundColor Cyan

& $MVN tomcat7:run

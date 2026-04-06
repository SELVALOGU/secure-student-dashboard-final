@echo off
title SecureEdu - Secure Student Analyzer
color 0B

:: ============================================================
::  SecureEdu - Auto-configured run script
::  Uses tools found at C:\tools\apache-maven-3.8.8
:: ============================================================

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot"
set "MVN=C:\tools\apache-maven-3.8.8\bin\mvn.cmd"

echo.
echo =========================================
echo   SecureEdu - Secure Student Analyzer
echo =========================================
echo.

:: Check Java
echo [1/4] Checking Java...
"%JAVA_HOME%\bin\java.exe" -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found at %JAVA_HOME%
    echo Please edit JAVA_HOME in this file.
    pause & exit /b 1
)
echo       Java OK: %JAVA_HOME%

:: Check Maven
echo [2/4] Checking Maven...
if not exist "%MVN%" (
    echo ERROR: Maven not found at %MVN%
    echo Please edit MVN path in this file.
    pause & exit /b 1
)
echo       Maven OK: %MVN%

:: Build
echo [3/4] Building project...
cd /d "%~dp0"
call "%MVN%" clean package -DskipTests --no-transfer-progress
if errorlevel 1 (
    echo.
    echo BUILD FAILED. Check errors above and fix them.
    pause & exit /b 1
)
echo       Build SUCCESS!

:: Start Tomcat
echo.
echo [4/4] Starting embedded Tomcat on http://localhost:8080
echo.
echo   ============================================
echo   Application URLs:
echo   Login Page : http://localhost:8080/
echo   ============================================
echo   Demo Credentials:
echo   Admin   : admin@school.edu / Admin@123
echo   Staff   : priya.sharma@school.edu / Staff@123
echo   Student : arjun.nair@student.edu / Student@123
echo   ============================================
echo.
echo   Press Ctrl+C to stop the server.
echo.

call "%MVN%" tomcat7:run
pause

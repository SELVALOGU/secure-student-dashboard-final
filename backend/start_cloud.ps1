# ============================================================
# SecureEdu — Master Cloud Start Script (Final Fix)
# ============================================================
# This script handles the connection between your code and Aiven.

$ErrorActionPreference = "Stop"

Write-Host " "
Write-Host "SecureEdu Master Cloud Setup"
Write-Host "============================"

# 1. Get Aiven Details
$AivenHost = Read-Host "Enter Aiven Host (e.g. mysql-xxx.aivencloud.com)"
$AivenPort = Read-Host "Enter Aiven Port (usually 23306)"
$AivenUser = Read-Host "Enter Aiven User (usually avnadmin)"
$AivenPass = Read-Host "Enter Aiven Password" -AsSecureString
$AivenDB   = Read-Host "Enter Database Name (usually defaultdb)"

# Convert password correctly
$p = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($AivenPass)
$FinalPass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($p)

Write-Host " "
Write-Host "Configuring Cloud Environment..."

# 2. Set Environment Variables
# Using ${Var} syntax to fix the "Parser Error"
$env:DB_URL  = "jdbc:mysql://${AivenHost}:${AivenPort}/${AivenDB}?sslMode=REQUIRED&allowPublicKeyRetrieval=true"
$env:DB_USER = $AivenUser
$env:DB_PASS = $FinalPass
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot"

Write-Host "Cloud connected! Starting Server now..."
Write-Host "---------------------------------------"

# 3. Start the Server
& "C:\tools\apache-maven-3.8.8\bin\mvn.cmd" clean package tomcat7:run -DskipTests

# Cleanup memory
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p)

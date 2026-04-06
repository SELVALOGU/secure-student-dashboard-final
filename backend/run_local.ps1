$ErrorActionPreference = "SilentlyContinue"
Stop-Process -Name "java" -Force
Stop-Process -Name "mvn.cmd" -Force
$ErrorActionPreference = "Stop"

Write-Host "Starting SecureEdu Locally..." -ForegroundColor Green

# Remove any existing cloud env vars to force local mode
Remove-Item Env:\DB_URL -ErrorAction SilentlyContinue
Remove-Item Env:\DB_USER -ErrorAction SilentlyContinue
Remove-Item Env:\DB_PASS -ErrorAction SilentlyContinue

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot"

# Run project
& "C:\tools\apache-maven-3.8.8\bin\mvn.cmd" clean package tomcat7:run -DskipTests

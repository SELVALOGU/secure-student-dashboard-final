# ============================================================
# SecureEdu — Aiven Cloud MySQL Migration Script (v3)
# ============================================================
# This script pushes your local database (setup.sql) to Aiven.
# ============================================================

$ErrorActionPreference = "Stop"

Write-Host " "
Write-Host "SecureEdu Cloud Migration Tool"
Write-Host "=============================="

# 1. Get Aiven Details
$AivenHost = Read-Host "Enter Aiven Host (e.g. mysql-xxx.aivencloud.com)"
$AivenPort = Read-Host "Enter Aiven Port (usually 23306)"
$AivenUser = Read-Host "Enter Aiven User (usually avnadmin)"
$AivenPass = Read-Host "Enter Aiven Password" -AsSecureString
$AivenDB   = Read-Host "Enter Database Name (e.g. defaultdb)"

# Convert SecureString to plain text for the command
$PassPtr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($AivenPass)
$PlainPass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($PassPtr)

# 2. Path to SQL and MySQL Client
$SqlFile = "c:\Users\SELVA LOGU N\dummy 3\setup.sql"
$MySqlExe = "C:\xampp\mysql\bin\mysql.exe"

if (-not (Test-Path $SqlFile)) {
    Write-Host "ERROR: Could not find setup.sql"
    return
}

if (-not (Test-Path $MySqlExe)) {
    Write-Host "ERROR: Could not find mysql.exe. Is XAMPP installed?"
    return
}

Write-Host " "
Write-Host "Connecting to Aiven and migrating data... (Please wait)"

try {
    # Read the SQL file
    $SqlContent = Get-Content $SqlFile -Raw
    
    # Filter out local database creation lines (Regex approach)
    $FilteredSql = $SqlContent -replace '(?m)^DROP DATABASE IF EXISTS.*$', '-- skipped'
    $FilteredSql = $FilteredSql -replace '(?m)^CREATE DATABASE.*$', '-- skipped'
    $FilteredSql = $FilteredSql -replace '(?m)^USE student_analyzer;.*$', '-- skipped'
    
    # Save filtered SQL to a temporary file
    $TempSql = [System.IO.Path]::GetTempFileName()
    $FilteredSql | Out-File -FilePath $TempSql -Encoding utf8 -Force
    
    # Use PowerShell Pipe instead of CMD redirection to avoid syntax errors
    # Note: Password must be passed carefully.
    Get-Content $TempSql | & "$MySqlExe" --host=$AivenHost --port=$AivenPort --user=$AivenUser "--password=$PlainPass" --database=$AivenDB --ssl-mode=REQUIRED
    
    # Clean up
    Remove-Item $TempSql -Force
    
    Write-Host " "
    Write-Host "SUCCESS! Your data has been pushed to Aiven Cloud."
}
catch {
    Write-Host " "
    Write-Host "MIGRATION FAILED!"
    Write-Host $_.Exception.Message
}
finally {
    if ($PassPtr) { [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($PassPtr) }
}

Write-Host "=============================="
Read-Host "Press Enter to exit"

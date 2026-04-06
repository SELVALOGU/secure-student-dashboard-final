$ErrorActionPreference = "Stop"

Write-Host " "
Write-Host "🚀 SecureEdu GitHub Upload Tool" -ForegroundColor Cyan
Write-Host "===============================" -ForegroundColor Gray

# Ask user for their URL
$RepoUrl = Read-Host "Enter your new GitHub Repository URL (e.g. https://github.com/username/my-project.git)"

if ([string]::IsNullOrWhiteSpace($RepoUrl)) {
    Write-Host "URL cannot be empty! Run the script again." -ForegroundColor Red
    exit
}

Write-Host " "
Write-Host "⚙️ Linking project to GitHub..." -ForegroundColor Yellow

# Try to remove old origin if it exists to prevent errors
git remote remove origin 2>$null

# Add new origin
git remote add origin $RepoUrl
git branch -M main

Write-Host "⬆️ Pushing code to GitHub... (You may be asked to log in to GitHub)" -ForegroundColor Yellow
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host " "
    Write-Host "✅ SUCCESS! Your project is now fully uploaded to GitHub!" -ForegroundColor Green
    Write-Host "You can view it here: $RepoUrl" -ForegroundColor Cyan
} else {
    Write-Host " "
    Write-Host "❌ PUSH FAILED!" -ForegroundColor Red
    Write-Host "- Ensure your GitHub repository exists and is empty." -ForegroundColor Gray
    Write-Host "- Make sure you are authenticated with GitHub." -ForegroundColor Gray
}

Write-Host " "
Read-Host "Press Enter to exit"

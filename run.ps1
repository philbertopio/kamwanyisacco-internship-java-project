# run.ps1 — Safe restart script for Kimwanyi SACCO
# Usage: .\run.ps1
# Kills any running cargo:run Tomcat, clears the lock, then starts fresh.

Write-Host "`n=== Kimwanyi SACCO — Safe Restart ===" -ForegroundColor Cyan

# 1. Kill any java.exe that has this project in its command line (cargo:run)
$victims = Get-WmiObject Win32_Process |
    Where-Object { $_.Name -eq "java.exe" -and $_.CommandLine -like "*kamwanyisacco*" }

if ($victims) {
    foreach ($p in $victims) {
        Write-Host "  Stopping Tomcat (PID $($p.ProcessId))..." -ForegroundColor Yellow
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  Waiting 3 s for OS to release file locks..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
} else {
    Write-Host "  No running Tomcat found." -ForegroundColor Green
}

# 2. Force-delete the target directory so maven-clean-plugin never hits a lock
$targetDir = "$PSScriptRoot\target"
if (Test-Path $targetDir) {
    Write-Host "  Deleting $targetDir ..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force $targetDir -ErrorAction SilentlyContinue
    Write-Host "  target/ cleared." -ForegroundColor Green
}

# 3. Build and start
Write-Host "`n  Starting: mvn clean package cargo:run`n" -ForegroundColor Cyan
mvn clean package cargo:run

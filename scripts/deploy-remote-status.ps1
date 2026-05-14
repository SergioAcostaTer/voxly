param(
    [string]$BackendEnvPath = "backend\.env",
    [string]$ComposeFilePath = "backend\docker-compose.remote.yml"
)

$ErrorActionPreference = "Stop"

$serverPort = "8080"
$frontendPort = "3000"
$appBase = ""

if (Test-Path $BackendEnvPath) {
    $envLines = Get-Content $BackendEnvPath
    foreach ($line in $envLines) {
        if ($line -match '^(SERVER_PORT)=(.+)$') {
            $serverPort = $matches[2]
        }
        if ($line -match '^(APP_BASE_URL)=(.+)$') {
            $appBase = $matches[2]
        }
        if ($line -match '^(FRONTEND_PORT)=(.+)$') {
            $frontendPort = $matches[2]
        }
    }
}

Write-Host ""
Write-Host "=== VoxLy Remote Deployment Status ===" -ForegroundColor Cyan
docker compose -f $ComposeFilePath ps

Write-Host ""
Write-Host "Services exposed by this stack:" -ForegroundColor Cyan
Write-Host ("Frontend UI:         http://localhost:{0}" -f $frontendPort)
Write-Host ("Backend API base:    http://localhost:{0}" -f $serverPort)
Write-Host ("Backend readiness:   http://localhost:{0}/actuator/health/readiness" -f $serverPort)
Write-Host ("Backend health:      http://localhost:{0}/actuator/health" -f $serverPort)
Write-Host "AI analyzer base:    http://localhost:8001"
Write-Host "AI analyzer health:  http://localhost:8001/health"
if ($appBase -ne "") {
    Write-Host ("Configured app URL:  {0}" -f $appBase)
}
Write-Host ""
Write-Host "Open the app using the Frontend UI URL above." -ForegroundColor Yellow

Write-Host ""
try {
    $frontendCheck = Invoke-WebRequest -UseBasicParsing ("http://localhost:{0}" -f $frontendPort) -TimeoutSec 5
    Write-Host "Frontend health check: OK" -ForegroundColor Green
    Write-Host ("HTTP status: {0}" -f [int]$frontendCheck.StatusCode)
} catch {
    Write-Host "Frontend health check: FAILED" -ForegroundColor Yellow
    if ($_.Exception.Response) {
        Write-Host ("HTTP status: {0}" -f [int]$_.Exception.Response.StatusCode)
    }
}

try {
    $backendRoot = Invoke-WebRequest -UseBasicParsing ("http://localhost:{0}" -f $serverPort) -TimeoutSec 5
    Write-Host "Backend root check: OK" -ForegroundColor Green
    Write-Host ("HTTP status: {0}" -f [int]$backendRoot.StatusCode)
} catch {
    if ($_.Exception.Response) {
        $backendRootStatus = [int]$_.Exception.Response.StatusCode
        if ($backendRootStatus -eq 401) {
            Write-Host "Backend root check: OK (protected as expected)" -ForegroundColor Green
            Write-Host ("HTTP status: {0}" -f $backendRootStatus)
        } else {
            Write-Host "Backend root check: FAILED" -ForegroundColor Yellow
            Write-Host ("HTTP status: {0}" -f $backendRootStatus)
        }
    } else {
        Write-Host "Backend root check: FAILED" -ForegroundColor Yellow
    }
}

try {
    $backendHealth = Invoke-WebRequest -UseBasicParsing ("http://localhost:{0}/actuator/health/readiness" -f $serverPort) -TimeoutSec 5
    Write-Host "Backend readiness check: OK" -ForegroundColor Green
    Write-Host $backendHealth.Content
} catch {
    Write-Host "Backend readiness check: FAILED" -ForegroundColor Yellow
    if ($_.Exception.Response) {
        Write-Host ("HTTP status: {0}" -f [int]$_.Exception.Response.StatusCode)
    }
}

try {
    $aiHealth = Invoke-WebRequest -UseBasicParsing "http://localhost:8001/health" -TimeoutSec 5
    Write-Host "AI analyzer health check: OK" -ForegroundColor Green
    Write-Host $aiHealth.Content
} catch {
    Write-Host "AI analyzer health check: FAILED" -ForegroundColor Yellow
    if ($_.Exception.Response) {
        Write-Host ("HTTP status: {0}" -f [int]$_.Exception.Response.StatusCode)
    }
}

Write-Host ""
Write-Host "To stop everything: make deploy-remote-down" -ForegroundColor Cyan
Write-Host "To tail logs only:   make deploy-remote-logs" -ForegroundColor Cyan
Write-Host ""

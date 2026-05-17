$ROOT = $PSScriptRoot

Write-Host "==> Starting Docker infrastructure..." -ForegroundColor Cyan
docker compose -f "$ROOT\backend\docker-compose.yml" up -d

Write-Host "    Waiting for Postgres to be ready..." -ForegroundColor Gray
$timeout = 30
$elapsed = 0
do {
    Start-Sleep -Seconds 2
    $elapsed += 2
    $ready = docker exec voxly-postgres pg_isready -U voxly_user -q 2>$null
} while ($LASTEXITCODE -ne 0 -and $elapsed -lt $timeout)

if ($LASTEXITCODE -ne 0) {
    Write-Host "    ERROR: Postgres did not start in time." -ForegroundColor Red
    exit 1
}
Write-Host "    Postgres ready." -ForegroundColor Green

Write-Host "==> Starting AI service (port 8000)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT'; & 'C:\Users\gorka\AppData\Local\Python\bin\python.exe' ai/run_api.py --host 0.0.0.0 --port 8000"

Write-Host "==> Starting Whisper server (port 9000)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "& 'C:\Users\gorka\AppData\Local\Python\bin\python.exe' '$ROOT\ai\whisper_server.py' --host 0.0.0.0 --port 9000 --model small"

Write-Host "==> Starting Backend (port 8080)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT\backend'; .\gradlew.bat bootRun"

Write-Host "==> Starting Frontend (port 5173)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT\frontend'; .\node_modules\.bin\vite.cmd"

Write-Host ""
Write-Host "All services launching in separate windows." -ForegroundColor Green
Write-Host ""
Write-Host "  Frontend  -> http://localhost:5173" -ForegroundColor Yellow
Write-Host "  Backend   -> http://localhost:8080" -ForegroundColor Yellow
Write-Host "  Swagger   -> http://localhost:8080/swagger-ui.html" -ForegroundColor Yellow
Write-Host "  AI        -> http://localhost:8000/docs" -ForegroundColor Yellow
Write-Host "  Whisper   -> http://localhost:9000/docs" -ForegroundColor Yellow
Write-Host "  Mailpit   -> http://localhost:18025" -ForegroundColor Yellow
Write-Host "  MinIO     -> http://localhost:19101" -ForegroundColor Yellow

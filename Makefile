.PHONY: frontend backend ai

frontend:
	cd frontend && pnpm run dev

backend:
	cd backend && POSTURE_ANALYZER_URL=http://localhost:8000 POSTURE_ANALYZER_ENABLED=true POSTURE_ANALYZER_RENDER_BASE=http://localhost:8000 ./gradlew.bat bootRun

ai:
	python -m pip install -q -r ai/presentation_analyzer/requirements.txt; powershell -Command "$$pid = (Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue).OwningProcess; if ($$pid) { Stop-Process -Id $$pid -Force; Start-Sleep -Seconds 2 }" 2>/dev/null; python ai/run_api.py --host 0.0.0.0 --port 8000

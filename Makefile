.DEFAULT_GOAL := help

BACKEND_DIR := backend
FRONTEND_DIR := frontend
DOCKER_COMPOSE_FILE := $(BACKEND_DIR)/docker-compose.yml

.PHONY: help \
	install backend-install frontend-install \
	backend frontend up \
	backend-test frontend-test test \
	backend-build frontend-build build \
	lint \
	docker-up docker-down docker-restart docker-logs docker-ps

help:
	@echo "Voxly Makefile"
	@echo ""
	@echo "Development:"
	@echo "  make backend          - Run backend only (Spring Boot)"
	@echo "  make frontend         - Run frontend only (Vite)"
	@echo "  make up               - Run backend + frontend in parallel"
	@echo ""
	@echo "Testing and quality:"
	@echo "  make backend-test     - Run backend tests"
	@echo "  make frontend-test    - Run frontend lint + build"
	@echo "  make test             - Run backend-test + frontend-test"
	@echo "  make lint             - Run frontend lint"
	@echo ""
	@echo "Build:"
	@echo "  make backend-build    - Build backend"
	@echo "  make frontend-build   - Build frontend"
	@echo "  make build            - Build backend + frontend"
	@echo ""
	@echo "Dependencies:"
	@echo "  make backend-install  - Download backend dependencies (Gradle)"
	@echo "  make frontend-install - Install frontend dependencies (npm)"
	@echo "  make install          - Run backend-install + frontend-install"
	@echo ""
	@echo "Docker Compose:"
	@echo "  make docker-up        - Start docker compose in background"
	@echo "  make docker-down      - Stop docker compose"
	@echo "  make docker-restart   - Restart docker compose"
	@echo "  make docker-logs      - Tail docker compose logs"
	@echo "  make docker-ps        - Show docker compose services status"

backend:
	cd $(BACKEND_DIR) && ./gradlew.bat bootRun

frontend:
	cd $(FRONTEND_DIR) && npm run dev

up:
	$(MAKE) -j2 backend frontend

backend-test:
	cd $(BACKEND_DIR) && ./gradlew.bat test

frontend-test:
	cd $(FRONTEND_DIR) && npm run lint && npm run build

test: backend-test frontend-test

lint:
	cd $(FRONTEND_DIR) && npm run lint

backend-build:
	cd $(BACKEND_DIR) && ./gradlew.bat clean build -x test

frontend-build:
	cd $(FRONTEND_DIR) && npm run build

build: backend-build frontend-build

backend-install:
	cd $(BACKEND_DIR) && ./gradlew.bat --refresh-dependencies classes -x test

frontend-install:
	cd $(FRONTEND_DIR) && npm install

install: backend-install frontend-install

docker-up:
	docker compose -f $(DOCKER_COMPOSE_FILE) up -d

docker-down:
	docker compose -f $(DOCKER_COMPOSE_FILE) down

docker-restart: docker-down docker-up

docker-logs:
	docker compose -f $(DOCKER_COMPOSE_FILE) logs -f

docker-ps:
	docker compose -f $(DOCKER_COMPOSE_FILE) ps

.DEFAULT_GOAL := help

BACKEND_DIR := backend
FRONTEND_DIR := frontend
GRADLEW := ./gradlew.bat
NPM := npm

ifeq ($(OS),Windows_NT)
NPM := npm.cmd
endif

.PHONY: help \
	install backend-install frontend-install \
	backend backend-testing frontend up up-testing infra-up infra-down infra-logs \
	backend-test frontend-test test \
	backend-build frontend-build build \
	lint

help:
	@echo "Voxly Makefile"
	@echo ""
	@echo "Development:"
	@echo "  make backend          - Run backend only (Spring Boot)"
	@echo "  make backend-testing  - Run backend with testing profile (no-login mode)"
	@echo "  make frontend         - Run frontend only (Vite)"
	@echo "  make up               - Run backend + frontend in one terminal with prefixed logs"
	@echo "  make up-testing       - Run backend + frontend with testing profile"
	@echo "  make infra-up         - Start local Postgres + MinIO + Mailpit with Docker Compose"
	@echo "  make infra-down       - Stop local Docker infrastructure"
	@echo "  make infra-logs       - Tail local Docker infrastructure logs"
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

backend:
	cd $(BACKEND_DIR) && $(GRADLEW) bootRun

backend-testing:
	cd $(BACKEND_DIR) && $(GRADLEW) bootRun --args=--spring.profiles.active=testing

frontend:
	cd $(FRONTEND_DIR) && $(NPM) run dev

up:
	cd $(FRONTEND_DIR) && $(NPM) run dev:all

up-testing:
	cd $(FRONTEND_DIR) && $(NPM) run dev:all:testing

infra-up:
	cd $(BACKEND_DIR) && docker compose up -d

infra-down:
	cd $(BACKEND_DIR) && docker compose down

infra-logs:
	cd $(BACKEND_DIR) && docker compose logs -f

backend-test:
	cd $(BACKEND_DIR) && $(GRADLEW) test

frontend-test:
	cd $(FRONTEND_DIR) && $(NPM) run lint && $(NPM) run build

test: backend-test frontend-test

lint:
	cd $(FRONTEND_DIR) && $(NPM) run lint

backend-build:
	cd $(BACKEND_DIR) && $(GRADLEW) clean build -x test

frontend-build:
	cd $(FRONTEND_DIR) && $(NPM) run build

build: backend-build frontend-build

backend-install:
	cd $(BACKEND_DIR) && $(GRADLEW) --refresh-dependencies classes -x test

frontend-install:
	cd $(FRONTEND_DIR) && $(NPM) install

install: backend-install frontend-install

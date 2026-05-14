SHELL := /usr/bin/env bash
COMPOSE := docker compose

.PHONY: help infra-up infra-down platform-up platform-down services-up services-down frontend-up frontend-down frontend-dev frontend-build observability-up observability-down obs-logs up down logs ps build-shared build-platform build-services test clean e2e e2e-install

help:
	@echo "NAJA7I-APP — make targets"
	@echo ""
	@echo "  infra-up        Start infra deps (postgres, mongo, redis, kafka, minio)"
	@echo "  infra-down      Stop infra deps"
	@echo "  platform-up     Start platform services (config, eureka, gateway)"
	@echo "  platform-down   Stop platform services"
	@echo "  services-up     Start Phase 1 microservices (auth, profile, notification)"
	@echo "  services-down   Stop Phase 1 microservices"
	@echo "  frontend-up     Start frontend container (depends on api-gateway)"
	@echo "  frontend-down   Stop frontend container"
	@echo "  frontend-dev    Run frontend Vite dev server locally (port 5173)"
	@echo "  frontend-build  Production build of the frontend"
	@echo "  observability-up   Start LGTM stack (Prometheus, Loki, Tempo, Grafana, Promtail)"
	@echo "  observability-down Stop the LGTM stack"
	@echo "  obs-logs           Tail observability stack logs"
	@echo "  up              Start everything (infra + platform + services + frontend + observability)"
	@echo "  down            Stop everything"
	@echo "  logs            Tail all logs"
	@echo "  ps              List running containers"
	@echo "  build-shared    Install shared-libs in local Maven repo"
	@echo "  build-platform  Build platform service images"
	@echo "  build-services  Build Phase 1 service jars"
	@echo "  test            Run all backend tests"
	@echo "  e2e             Run Playwright e2e suite (assumes services + frontend already up)"
	@echo "  e2e-install     Install Playwright browsers (chromium)"
	@echo "  clean           Remove generated artefacts (NOT volumes)"

infra-up:
	$(COMPOSE) --profile infra up -d

infra-down:
	$(COMPOSE) --profile infra down

platform-up:
	$(COMPOSE) --profile platform up -d --build

platform-down:
	$(COMPOSE) --profile platform down

services-up:
	$(COMPOSE) --profile services up -d --build

services-down:
	$(COMPOSE) --profile services down

frontend-up:
	$(COMPOSE) --profile frontend up -d --build

frontend-down:
	$(COMPOSE) --profile frontend down

frontend-dev:
	cd frontend-web && npm install && npm run dev

frontend-build:
	cd frontend-web && npm install && npm run build

observability-up:
	$(COMPOSE) --profile observability up -d

observability-down:
	$(COMPOSE) --profile observability down

obs-logs:
	$(COMPOSE) logs -f --tail=100 prometheus loki tempo grafana promtail

up:
	$(COMPOSE) --profile infra --profile platform --profile services --profile frontend --profile observability up -d --build

down:
	$(COMPOSE) --profile infra --profile platform --profile services --profile frontend --profile observability down

logs:
	$(COMPOSE) logs -f --tail=100

ps:
	$(COMPOSE) ps

build-shared:
	cd backend && mvn -pl shared-libs -am clean install -DskipTests

build-platform:
	cd backend && mvn -pl config-server,service-discovery,api-gateway -am clean package -DskipTests

build-services:
	cd backend && mvn -pl auth-service,user-profile-service,notification-service -am clean package -DskipTests

test:
	cd backend && mvn clean verify

# Playwright e2e — assumes `make up` has been run with NAJA7I_AUTH_TEST_MODE=true.
e2e:
	cd frontend-web && npm run e2e

e2e-install:
	cd frontend-web && npm install && npm run e2e:install

clean:
	cd backend && mvn clean

SHELL := /usr/bin/env bash
COMPOSE := docker compose

.PHONY: help infra-up infra-down platform-up platform-down services-up services-down up down logs ps build-shared build-platform build-services test clean

help:
	@echo "NAJA7I-APP — make targets"
	@echo ""
	@echo "  infra-up        Start infra deps (postgres, mongo, redis, kafka, minio)"
	@echo "  infra-down      Stop infra deps"
	@echo "  platform-up     Start platform services (config, eureka, gateway)"
	@echo "  platform-down   Stop platform services"
	@echo "  services-up     Start Phase 1 microservices (auth, profile, notification)"
	@echo "  services-down   Stop Phase 1 microservices"
	@echo "  up              Start everything (infra + platform + services)"
	@echo "  down            Stop everything"
	@echo "  logs            Tail all logs"
	@echo "  ps              List running containers"
	@echo "  build-shared    Install shared-libs in local Maven repo"
	@echo "  build-platform  Build platform service images"
	@echo "  build-services  Build Phase 1 service jars"
	@echo "  test            Run all backend tests"
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

up:
	$(COMPOSE) --profile infra --profile platform --profile services up -d --build

down:
	$(COMPOSE) --profile infra --profile platform --profile services down

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

clean:
	cd backend && mvn clean

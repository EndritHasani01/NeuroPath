# NeuroPath — Adaptive Learning Platform (Dockerized, CI/CD-enabled, Kubernetes-ready)

**NeuroPath** is a multi-service web application that delivers AI-assisted, adaptive learning. The system consists of a React SPA (served by Nginx), a Spring Boot backend (Java 21), a Python/FastAPI microservice for LLM-driven insights, and a PostgreSQL database.
This repository provides a complete DevOps setup: per-service Dockerfiles, a Docker Compose topology for local development, CI builds that push images to Docker Hub, and a set of Kubernetes manifests (namespace, Deployments, Services, Ingress; and a StatefulSet for PostgreSQL) suitable for a local k3d cluster or for adaptation to a managed cluster.

> **Note on naming:** Some submodules/READMEs refer to *InsightPath AI*. That is the same application packaged here as **NeuroPath**.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Quick Start (Docker Compose)](#quick-start-docker-compose)
4. [Kubernetes (k3d + Traefik)](#kubernetes-k3d--traefik)
5. [Configuration](#configuration)
6. [Project Structure](#project-structure)
7. [CI/CD and Images](#cicd-and-images)
8. [Tech Stack & Capabilities](#tech-stack--capabilities)
9. [Security & Observability](#security--observability)

---

## Overview

NeuroPath is an adaptive learning platform that personalizes learning paths and feedback. The **frontend** is a modern React app that handles the user experience and authentication. The **backend** is a Spring Boot service that manages domain logic, persistence, and JWT-based access control. The **LLM microservice** exposes a small, schema-driven API (FastAPI) that generates learning paths, insights, and review prompts; it can operate in mock mode or use real LLM providers via API keys. **PostgreSQL** stores users, domains, assessments, and progress.

From a DevOps standpoint, the system demonstrates:

* **Containerization:** minimal, runtime-appropriate Dockerfiles for each service.
* **Local orchestration:** a single `docker-compose.yml` that wires all services.
* **Continuous Integration:** GitHub Actions builds/pushes images to Docker Hub on each push.
* **Kubernetes readiness:** manifests under `deploy/k8s/` with a dedicated namespace, ConfigMaps/Secrets, Deployments, Services, Ingress, and a DB StatefulSet.

---

## Architecture

At runtime, the **frontend** communicates with the **backend** over REST (JWT auth). The **backend** integrates with the **LLM service** via an internal base URL. **PostgreSQL** is the system of record.

* **Compose networking:** services resolve each other by Compose service name (e.g., `backend`, `llm`, `db`).
* **Kubernetes networking:** services communicate over cluster DNS (e.g., `adaptive-learning-backend.neuropath.svc.cluster.local`). Traefik Ingress exposes hostnames such as:

  * `neuropath.localtest.me` → frontend
  * `api.neuropath.localtest.me` → backend
  * `llm.neuropath.localtest.me` → LLM service

> `localtest.me` resolves to `127.0.0.1`, which makes local Ingress testing simple.

---

## Quick Start (Docker Compose)

**Prerequisites:** Docker (and optionally Docker Compose v2, which is bundled with recent Docker Desktop).

1. Clone the repository and move into it:

   ```bash
   git clone https://github.com/EndritHasani01/NeuroPath.git
   cd NeuroPath
   ```
2. Copy environment variables and adjust as needed:

   ```bash
   cp .env.example .env   # if .env.example exists; otherwise create .env
   ```
3. Build and run:

   ```bash
   docker compose up --build
   ```
4. Access the services:

   * Frontend (SPA): [http://localhost:3000/](http://localhost:3000/)
   * Backend health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
   * LLM microservice: [http://localhost:8000/](http://localhost:8000/)

The frontend’s Nginx is templated at container start to proxy `/api/*` to the backend URL specified via `BACKEND_BASE_URL`.

---

## Kubernetes (k3d + Traefik)

**Prerequisites:** Docker, [k3d](https://k3d.io), and `kubectl`.

1. Create a local cluster with Traefik exposed on host ports:

   ```bash
   k3d cluster create neuropath --servers 1 --agents 2 \
     --port "8080:80@loadbalancer" --port "8443:443@loadbalancer"
   ```
2. Option A — Use images built locally (import them into the cluster):

   ```bash
   docker build -t neuropath-backend:local  backend
   docker build -t neuropath-frontend:local frontend
   docker build -t neuropath-llm:local      llm

   k3d image import neuropath-backend:local neuropath-frontend:local neuropath-llm:local -c neuropath
   ```

   Option B — Use images from Docker Hub: update `image:` in the Deployment manifests to the published tags (see **CI/CD and Images** below).
3. Apply the manifests (namespace first, then the rest):

   ```bash
   kubectl apply -f deploy/k8s/namespace.yaml
   kubectl apply -f deploy/k8s
   ```
4. Test via Ingress (Traefik on `127.0.0.1:8080`):

   ```bash
   # Frontend
   curl -H "Host: neuropath.localtest.me" http://127.0.0.1:8080/

   # Backend health
   curl -H "Host: api.neuropath.localtest.me" http://127.0.0.1:8080/actuator/health

   # LLM root
   curl -H "Host: llm.neuropath.localtest.me" http://127.0.0.1:8080/
   ```

> The Kubernetes manifests include: a dedicated `neuropath` namespace; ConfigMaps/Secrets for app and DB settings; Deployments with readiness/liveness probes; ClusterIP Services; Traefik Ingress routes; and a PostgreSQL StatefulSet with persistent storage.

---

## Configuration

Configuration is injected at runtime via Compose environment variables or Kubernetes ConfigMaps/Secrets. The most relevant settings are:

| Variable                                                                            | Used by          | Purpose                                                                                                  |
| ----------------------------------------------------------------------------------- | ---------------- | -------------------------------------------------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | backend          | JDBC configuration for PostgreSQL                                                                        |
| `PYTHON_SERVICE_BASEURL`                                                            | backend          | Base URL for the LLM microservice (e.g., `http://llm:8000/api/ai` in Compose or the cluster FQDN in K8s) |
| `FRONTEND_ORIGIN`                                                                   | backend          | Allowed CORS origin for the SPA                                                                          |
| `JWT_SECRET`, `JWT_EXPIRATION_MS`                                                   | backend          | Authentication token secret and expiration                                                               |
| `BACKEND_BASE_URL`                                                                  | frontend (Nginx) | Where Nginx proxies `/api/*` at runtime                                                                  |
| `PORT`, `LLM_MODE`                                                                  | llm              | Service port and mode (`mock` or provider-backed)                                                        |
| `GEMINI_API_KEY`, `GROQ_API_KEY`                                                    | llm              | Provider keys when not in mock mode                                                                      |

> Do not commit real secrets. Use `.env` locally, Kubernetes Secrets in clusters, and consider SealedSecrets/External Secrets for production.

---

## Project Structure

```
.
├── docker-compose.yml
├── .env                              # local development env (do not commit real secrets)
├── frontend/                         # React + Vite app served by Nginx
│   ├── Dockerfile
│   └── docker/entrypoint.sh, default.conf.template
├── backend/                          # Spring Boot (Java 21) service
│   ├── Dockerfile
│   └── src/main/java/... (JWT security, LLM integration, API)
├── llm/                              # FastAPI microservice for AI features
│   ├── Dockerfile
│   └── app/main.py, services/, core/
├── deploy/
│   └── k8s/                          # Namespace, ConfigMaps/Secrets, Deployments, Services, Ingress, StatefulSet
└── .github/
    └── workflows/docker-image.yml    # CI builds & pushes Docker images
```

---

## CI/CD and Images

A GitHub Actions workflow (`.github/workflows/docker-image.yml`) builds and pushes images on each push. Images are published to Docker Hub:

* `endrithasani/adaptive-learning-frontend`
* `endrithasani/adaptive-learning-backend`
* `endrithasani/adaptive-learning-llm`

Typical tags include the commit SHA and `latest`. Kubernetes Deployments can reference these public tags directly; for local k3d development you can import locally built `:local` images as shown above.

---

## Tech Stack & Capabilities

* **Frontend:** React 19 (Vite), Material-UI, React Router; served by Nginx. JWT-based auth, domain/topic flows, AI-generated insights and reviews, progress tracking. A small Nginx templating step (`envsubst`) injects the backend URL at runtime so the same image works across environments.

* **Backend:** Java 21, Spring Boot 3; Spring Security with JWT; Spring Data JPA (PostgreSQL); Actuator health endpoints; reactive `WebClient` to the LLM microservice; OpenAPI docs enabled. Configuration is externalized via environment variables and K8s ConfigMaps/Secrets.

* **LLM Microservice:** Python 3.12, FastAPI, `pydantic-ai` for schema-driven calls; async endpoints under `/api/ai/*` to generate learning paths, insights, and review content. Operates in mock mode or with real providers (Gemini/Groq) when API keys are present.

* **Database:** PostgreSQL 15. In Kubernetes it runs as a StatefulSet with a headless Service and a PersistentVolumeClaim for durable storage.

---

## Security & Observability

* **Authentication & Authorization:** JWT-based security at the backend with route whitelisting for auth and docs.
* **CORS:** Configured to a specific frontend origin via environment variables.
* **Probes:** Readiness and liveness probes are defined in Deployments; health checks are available at `/actuator/health` (backend) and `/` for the other services.
* **Resource Limits:** Conservative CPU/memory requests/limits are set in the Kubernetes manifests for predictable scheduling in small clusters.

Add screenshots of the running UI, `kubectl get pods -n neuropath`, and Ingress tests here once available.

---

**Contact / Issues:** Please open an issue in this repository for bug reports or suggestions.

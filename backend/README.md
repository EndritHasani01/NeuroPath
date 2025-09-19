# InsightPath AI - Java Backend

This repository contains the Spring Boot service that powers the InsightPath AI learning platform. The application manages authentication, persists user progress and delegates adaptive content generation to a Python microservice. It exposes a REST API that is consumed by the front-end.

## Technology

* **Java 21**, **Spring Boot 3.4.5**
* Spring Data JPA with **PostgreSQL**
* **Spring Security** with JWT authentication
* Reactive **WebClient** for Python service integration
* **Lombok** and **ModelMapper** for boilerplate reduction
* Built with **Maven** (wrapper included)

## Configuration

Two property files exist:

* `src/main/resources/application.properties` - default development settings (localhost database, Python service at `http://localhost:8000/api/ai`).
* `src/main/resources/application-prod.properties` - values are taken from environment variables for production deployments.

Important variables for production:

* `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
* `PYTHON_SERVICE_BASEURL`
* `FRONTEND_ORIGIN` - allowed CORS origin
* `JWT_SECRET` and optional `JWT_EXPIRATION_MS`

## Data Initialisation

`DataInitializer` seeds the database on startup with:

* Two roles (`ROLE_USER`, `ROLE_ADMIN`) and a default `admin` user.
* A catalogue of learning domains grouped by category, each with example assessment questions.

## Running Locally

```bash
# Example environment (override as needed)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/adaptive_learning_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export PYTHON_SERVICE_BASEURL=http://localhost:8000/api/ai
export FRONTEND_ORIGIN=http://localhost:3000
export JWT_SECRET=devsecret

./mvnw spring-boot:run
```

Swagger documentation is available at `http://localhost:8080/swagger-ui.html` once the application is running.

## Docker Compose quick start

1. Copy `.env.example` to `.env` and adjust secrets (especially `POSTGRES_PASSWORD`, `JWT_SECRET`, plus `GEMINI_API_KEY`/`GROQ_API_KEY`).
2. Build and launch the stack:
   ```bash
   docker compose up --build
   ```
3. The React frontend is available at `http://localhost:${FRONTEND_PORT:-3000}`, the Spring API at `http://localhost:${APP_PORT:-8080}`, and the FastAPI LLM service at `http://localhost:${LLM_PORT:-8000}`.

The Compose file builds the backend API, the Vite/React frontend (served by nginx), provisions PostgreSQL 15 with persistent storage, and starts the FastAPI LLM microservice behind the same network with nginx proxying `/api` calls from the SPA.

## Kubernetes deployment

Manifest files live in `deploy/k8s` and target the `neuropath` namespace. They include:

* Namespace, ConfigMaps, and Secrets for the application, LLM service, and database
* PostgreSQL StatefulSet with persistent volume claim
* Backend, LLM, and frontend Deployments/Services alongside dedicated Ingress resources

Apply resources in order:

```bash
# Namespace and shared configuration
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/postgres-configmap.yaml
kubectl apply -f deploy/k8s/postgres-secret.yaml
kubectl apply -f deploy/k8s/backend-configmap.yaml
kubectl apply -f deploy/k8s/backend-secret.yaml
kubectl apply -f deploy/k8s/frontend-configmap.yaml
kubectl apply -f deploy/k8s/llm-configmap.yaml
kubectl apply -f deploy/k8s/llm-secret.yaml

# Workloads and ingress
kubectl apply -f deploy/k8s/postgres-service.yaml
kubectl apply -f deploy/k8s/postgres-statefulset.yaml
kubectl apply -f deploy/k8s/backend-deployment.yaml
kubectl apply -f deploy/k8s/backend-service.yaml
kubectl apply -f deploy/k8s/backend-ingress.yaml
kubectl apply -f deploy/k8s/frontend-deployment.yaml
kubectl apply -f deploy/k8s/frontend-service.yaml
kubectl apply -f deploy/k8s/frontend-ingress.yaml
kubectl apply -f deploy/k8s/llm-deployment.yaml
kubectl apply -f deploy/k8s/llm-service.yaml
kubectl apply -f deploy/k8s/llm-ingress.yaml
```

Before applying, update placeholder values:

* Replace `changeMe` secrets in `postgres-secret.yaml`, `backend-secret.yaml`, and `llm-secret.yaml`
* Set routable hostnames (and optional TLS secrets) in `backend-ingress.yaml`, `frontend-ingress.yaml`, and `llm-ingress.yaml`
* Replace the placeholder container images (`adaptive-learning-backend`, `adaptive-learning-frontend`, `adaptive-learning-llm`) with the tags published by your CI/CD pipeline

Validation commands:

```bash
kubectl -n neuropath get pods
kubectl -n neuropath get svc
kubectl -n neuropath get ingress
kubectl -n neuropath logs deploy/adaptive-learning-backend
```

## REST API

### Authentication

* `POST /api/auth/login` - obtain a JWT token
* `POST /api/auth/register` - create a user
* `GET  /api/auth/me` - profile summary of the authenticated user

### Learning Workflow

* `GET  /api/learning/domains` - list all domains
* `GET  /api/learning/domains/{id}/assessment-questions` - questions for the initial assessment
* `POST /api/learning/domains/start` - submit assessment answers and receive a learning path
* `GET  /api/learning/domains/{id}/next-insight` - fetch the next insight for the current topic
* `POST /api/learning/insights/submit-answer` - submit an answer to a question
* `GET  /api/learning/domains/{id}/progress` - current progress within a domain
* `GET  /api/learning/domains/{id}/review` - generate a review after completing a level
* `POST /api/learning/domains/{id}/complete-review?satisfactoryPerformance=bool` - mark review complete and potentially advance
* `GET  /api/learning/domains/{id}/overview` - high level view of all topics in the learning path
* `POST /api/learning/domains/{id}/select-topic/{topicIdx}` - manually change current topic index
* `GET  /api/learning/domains/status` - list all domains with started/completed status

## Python Integration

`AiIntegrationService` communicates with the external Python service using WebClient. It calls:

* `/generate-learning-path`
* `/generate-insights`
* `/generate-review`

The responses are synchronously retrieved using `block()` and converted into DTOs used by the business layer.

## Building a Container

A multi-stage `Dockerfile` is included. Build and run with:

```bash
docker build -t insightpath-backend .
docker run -p 8080:8080 --env-file env.list insightpath-backend
```

where `env.list` contains the required environment variables described above.

## Repository Structure

```
src/main/java/com/example/adaptivelearningbackend/
  config/         # Security configuration and startup seeding
  controller/     # REST controllers
  dto/            # Request and response models
  entity/         # JPA entities
  repository/     # Spring Data repositories
  security/       # JWT utilities and filter
  service/        # Business logic and AI integration
  enums/          # Enumerations used in entities/DTOs
```

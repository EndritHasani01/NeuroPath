# InsightPath AI – Java Backend

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

* `src/main/resources/application.properties` – default development settings (localhost database, Python service at `http://localhost:8000/api/ai`).
* `src/main/resources/application-prod.properties` – values are taken from environment variables for production deployments.

Important variables for production:

* `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
* `PYTHON_SERVICE_BASEURL`
* `FRONTEND_ORIGIN` – allowed CORS origin
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

## REST API

### Authentication

* `POST /api/auth/login` – obtain a JWT token
* `POST /api/auth/register` – simple registration (username == email)

### User

* `POST /api/users/register` – create a user
* `GET  /api/users/me` – profile summary of the authenticated user

### Learning Workflow

* `GET  /api/learning/domains` – list all domains
* `GET  /api/learning/domains/{id}/assessment-questions` – questions for the initial assessment
* `POST /api/learning/domains/start` – submit assessment answers and receive a learning path
* `GET  /api/learning/domains/{id}/next-insight` – fetch the next insight for the current topic
* `POST /api/learning/insights/submit-answer` – submit an answer to a question
* `GET  /api/learning/domains/{id}/progress` – current progress within a domain
* `GET  /api/learning/domains/{id}/review` – generate a review after completing a level
* `POST /api/learning/domains/{id}/complete-review?satisfactoryPerformance=bool` – mark review complete and potentially advance
* `GET  /api/learning/domains/{id}/overview` – high level view of all topics in the learning path
* `POST /api/learning/domains/{id}/select-topic/{topicIdx}` – manually change current topic index
* `GET  /api/learning/domains/status` – list all domains with started/completed status

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
src/main/java/com/example/adaptivelearningbackend
├─ config/         # Security configuration and startup seeding
├─ controller/     # REST controllers
├─ dto/            # Request and response models
├─ entity/         # JPA entities
├─ repository/     # Spring Data repositories
├─ security/       # JWT utilities and filter
├─ service/        # Business logic and AI integration
└─ enums/          # Enumerations used in entities/DTOs
```

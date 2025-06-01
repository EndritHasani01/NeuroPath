# InsightPath AI – Java Backend

This is the main backend for **InsightPath AI**, a Spring Boot 3.4.5 service that handles authentication, business logic, and communication with a FastAPI AI microservice. It uses JWT for security, JPA for persistence, and exposes a RESTful API consumed by a React frontend.

## Features

* JWT-based authentication and role-based access
* Structured learning workflow via REST endpoints
* Dynamic AI integration for personalized insights and reviews
* JPA/Hibernate-backed domain model with PostgreSQL
* Async WebClient integration with Python microservice

## Tech Stack

* **Spring Boot 3.4.5**
* **Spring Security + JWT**
* **Spring Data JPA**
* **PostgreSQL**
* **Lombok**
* **WebClient (reactive)**
* **Maven**

## Project Structure

```
src/main/java/
├─ config/           # Security config, JWT filter/provider
├─ controller/       # REST API controllers
├─ service/          # Business logic + AI integration
├─ model/            # JPA entities and DTOs
└─ repository/       # Spring Data repositories
```

## Key Endpoints

| Endpoint                                      | Description                          |
| --------------------------------------------- | ------------------------------------ |
| `POST /api/auth/login`                        | Authenticate user, return JWT        |
| `POST /api/auth/register`                     | Register new user                    |
| `GET /api/learning/domains`                   | List available learning domains      |
| `POST /api/learning/domains/start`            | Submit assessment, get learning path |
| `GET /api/learning/domains/{id}/next-insight` | Get next insight                     |
| `POST /api/learning/insights/answer`          | Submit answer and update progress    |
| `GET /api/learning/domains/{id}/review`       | Generate review summary              |

## AI Integration

* Communicates with a FastAPI microservice via **WebClient**
* Endpoints:

  * `/generate-learning-path`
  * `/generate-insights`
  * `/generate-review`
* Uses `.block()` (can be optimized)

## Security

* **JWT** with HMAC-SHA and roles claim
* Stateless session, open CORS (configurable)
* Secure paths: all except `/api/auth/**` and static assets

## Running Locally

```bash
# Required env variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/insightpath
export JWT_SECRET=your_jwt_secret
export PYTHON_SERVICE_BASE_URL=http://localhost:8000

# Build and run
./mvnw spring-boot:run
```

## Key Files

* `SecurityConfig.java` – Spring Security setup
* `LearningServiceImpl.java` – core learning workflow logic
* `AiIntegrationServiceImpl.java` – LLM adapter (WebClient)


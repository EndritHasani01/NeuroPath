# InsightPath AI – Python Microservice

This repository contains the Python service that generates adaptive learning material for **InsightPath AI**. It exposes a small FastAPI application which orchestrates a set of `pydantic-ai` agents to build learning paths, insights and review summaries.

The code base is intentionally compact and the entire business logic lives in the `llm_service.py` module. All request and response models are defined in `models.py` and use camelCase JSON so they can be consumed by the Java/React clients without extra mapping code.

## Features

* Schema-driven AI calls using `pydantic-ai`
* Fallback LLM chains for resilience and continuity
* Adaptive insight generation based on user performance
* Compatible JSON models for seamless Java/React integration (camelCase)
* Fully async FastAPI with rich validation


## Key Endpoints

| Endpoint                              | Description                                        |
| ------------------------------------- | -------------------------------------------------- |
| `POST /api/ai/generate-learning-path` | Creates 10–20 topic path with optional adaptation  |
| `POST /api/ai/generate-insights`      | Returns insights (title + explanation + questions) |
| `POST /api/ai/generate-review`        | Summarizes user strengths/weaknesses + review tips |
| `GET /`                               | Health-check endpoint                              |

## Project Structure

```
app/
├─ core/               # Config & settings
├─ models.py           # DTOs (camelCase) shared with Java/React
├─ services/
│   └─ llm_service.py  # All agent logic lives here
└─ main.py             # FastAPI entrypoint & routers
```

## Agent Design (pydantic-ai)

All logic is powered by declarative `Agent` instances:

* **Curriculum Planner** → `LearningPathResponse`
* **User Data Analyzer** → `UserProficiencyProfile`
* **Adaptation Planner** → `ContentAdaptationPlan`
* **Insight Generator** → `List[InsightDetail]`
* **Question Generator** → `List[QuestionDetail]`
* **Reviewer** → `ReviewResponse`

Agents use **fallback chains** across providers like Gemini, Groq, and Llama3 to ensure high availability.

Functions like **generate_learning_path_logic** and **generate_insights_logic** orchestrate these agents to produce adaptive content.

## Adaptation Workflow

1. **Learning Path**:
   Assessment answers → proficiency profile → learning path

2. **Insights**:
   User performance → adaptation plan → insights + questions

3. **Review**:
   Topic data → summary, strengths, weaknesses

## Running Locally

```bash
# Activate venv and install
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt

# Run server
uvicorn app.main:app --reload --port 8000
```

Set required env variables:

```bash
export GEMINI_API_KEY=...
export GROQ_API_KEY=...
```

## Response Models

Responses align with Java/React via camelCase serialization:

* `LearningPathResponse`
* `List[InsightDetail]` (each includes `title`, `explanation`, `questions`)
* `ReviewResponse` (summary, strengths, weaknesses)

## Key Files

* `llm_service.py`: all business logic + agent orchestration
* `models.py`: shared DTOs for Java ↔ Python
* `main.py`: FastAPI app and endpoint handlers

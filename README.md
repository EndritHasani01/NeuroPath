# InsightPath AI – Python AI Microservice

This is the AI microservice powering **InsightPath AI**, built with **FastAPI** and driven entirely by **pydantic-ai agents**. It handles adaptive learning logic such as curriculum generation, personalized insights, and spaced-repetition reviews.

## Features

* Schema-driven AI calls using `pydantic-ai`
* Fallback LLM chains for resilience and continuity
* Adaptive insight generation based on user performance
* Compatible JSON models for seamless Java/React integration (camelCase)
* Fully async FastAPI with rich validation

## Tech Stack

* **FastAPI**
* **pydantic-ai**
* **Pydantic v2**
* **Uvicorn**
* Optional integration with: OpenAI, Groq, Google Gemini, Llama3/4

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
export OPENAI_API_KEY=...
export GROQ_API_KEY=...
export LLAMA_INDEX_DIR=...
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

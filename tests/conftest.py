# ========== FILE: tests/conftest.py ==========
import asyncio
import json
import pytest
from httpx import AsyncClient
from app.main import app
from app.models import (
    AssessmentAnswer, InsightDetail, QuestionDetail, QuestionType
)

@pytest.fixture
async def client():
    async with AsyncClient(app=app, base_url="http://test") as ac:
        yield ac

# --- Simple factories for valid model instances used across tests ---

@pytest.fixture
def assessment_answer() -> AssessmentAnswer:
    return AssessmentAnswer(
        question_id=1,
        question_text="What is a function in Python?",
        options=["A", "B", "C", "D"],
        selected_answer="A",
    )

@pytest.fixture
def long_explanation() -> str:
    # >= 100 chars to satisfy InsightDetail.explanation min_length
    return (
        "This is a sufficiently long explanation intended to meet the minimum length "
        "requirements for the InsightDetail model. It spans multiple sentences, "
        "offering context and clarity for testing."
    )

@pytest.fixture
def sample_insight(long_explanation) -> InsightDetail:
    return InsightDetail(
        title="Understanding Variables",
        explanation=long_explanation,
        ai_metadata={"source": "unit-test"},
        questions=[],
    )

@pytest.fixture
def sample_questions() -> list[QuestionDetail]:
    return [
        QuestionDetail(
            question_type=QuestionType.MULTIPLE_CHOICE,
            question_text="Which is a valid variable name in Python?",
            options=["1var", "_name", "var-name"],
            correct_answer="_name",
            answer_feedbacks={"_name": "Correct!", "1var": "Cannot start with a digit.", "var-name": "Hyphen not allowed."},
        ),
        QuestionDetail(
            question_type=QuestionType.TRUE_FALSE,
            question_text="Variables can change type at runtime in Python.",
            options=["True", "False"],
            correct_answer="True",
            answer_feedbacks={"True": "Correct.", "False": "Python is dynamically typed."},
        ),
    ]

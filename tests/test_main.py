# ========== FILE: tests/test_main.py ==========
import json
import pytest
from types import SimpleNamespace
from unittest.mock import AsyncMock

from app.models import (
    LearningPathResponse,
    InsightDetail,
    ReviewResponse,
    QuestionDetail,
    QuestionType,
)

pytestmark = pytest.mark.anyio


# -------------------- /api/ai/generate-learning-path --------------------

async def test_generate_learning_path_happy_path(client, mocker):
    # Mock service output
    response_model = LearningPathResponse(
        domain_name="Python", topics=[f"Module {i}" for i in range(1, 11)]
    )
    mocker.patch(
        "app.services.llm_service.generate_learning_path_logic",
        new=AsyncMock(return_value=response_model),
    )
    payload = {
        "domainName": "Python",
        "userId": 42,
        "userTopicPerformanceData": {
            "assessmentAnswers": [
                {
                    "questionId": 1,
                    "questionText": "What is Python?",
                    "options": ["A snake", "A language"],
                    "selectedAnswer": "A language",
                }
            ]
        },
    }
    res = await client.post("/api/ai/generate-learning-path", json=payload)
    assert res.status_code == 200
    body = res.json()
    # camelCase contract
    assert body == {
        "domainName": "Python",
        "topics": [f"Module {i}" for i in range(1, 11)],
    }


async def test_generate_learning_path_validation_error_missing_domain_name(client):
    # Missing required field domainName → 422
    payload = {
        "userId": 42,
        "userTopicPerformanceData": {"assessmentAnswers": []},
    }
    res = await client.post("/api/ai/generate-learning-path", json=payload)
    assert res.status_code == 422
    assert "detail" in res.json()


async def test_generate_learning_path_server_error(client, mocker):
    mocker.patch(
        "app.services.llm_service.generate_learning_path_logic",
        new=AsyncMock(side_effect=Exception("boom")),
    )
    payload = {"domainName": "Python"}
    res = await client.post("/api/ai/generate-learning-path", json=payload)
    assert res.status_code == 500
    assert res.json()["detail"].startswith("Failed to generate learning path: boom")


# -------------------- /api/ai/generate-insights --------------------

async def test_generate_insights_happy_path(client, mocker):
    # Build two valid InsightDetail items (questions empty is fine)
    long_expl = (
        "This is a long explanation that satisfies the length requirement. "
        "It ensures we have more than one hundred characters for schema validation "
        "and represents a realistic insight explanation for testing."
    )
    insights = [
        InsightDetail(title="Intro to Loops", explanation=long_expl, ai_metadata={}, questions=[]),
        InsightDetail(title="For vs While", explanation=long_expl, ai_metadata={}, questions=[]),
    ]
    mocker.patch(
        "app.services.llm_service.generate_insights_logic",
        new=AsyncMock(return_value=insights),
    )
    payload = {"domainName": "Python", "topicName": "Loops", "level": 1, "userId": 123}
    res = await client.post("/api/ai/generate-insights", json=payload)
    assert res.status_code == 200
    body = res.json()
    assert isinstance(body, list) and len(body) == 2
    # Contract conformance (spot-check keys/shape)
    for item in body:
        assert {"title", "explanation", "aiMetadata", "questions"}.issubset(item.keys())


async def test_generate_insights_validation_error_level_string_instead_of_int(client):
    payload = {"domainName": "Python", "topicName": "Loops", "level": "two", "userId": 1}
    res = await client.post("/api/ai/generate-insights", json=payload)
    assert res.status_code == 422


# -------------------- /api/ai/generate-review --------------------

async def test_generate_review_happy_path(client, mocker):
    mocker.patch(
        "app.services.llm_service.generate_review_logic",
        new=AsyncMock(return_value=ReviewResponse(
            summary="Great progress.",
            strengths=["Syntax", "Control Flow"],
            weaknesses=["OOP"],
        ))
    )

    payload = {
        "userId": 9,
        "topicProgressId": 17,
        "performanceData": {
            "topicName": "Functions",
            "level": 2,
            "accuracy": 0.8,
            "commonErrors": ["Parameter order"],
        },
    }
    res = await client.post("/api/ai/generate-review", json=payload)
    assert res.status_code == 200
    body = res.json()
    assert set(body.keys()) == {"summary", "strengths", "weaknesses"}


@pytest.mark.xfail(reason="Model currently allows empty dict; make performance_data non-empty via Field(min_length=1)")
async def test_generate_review_validation_error_empty_performance_data(client):
    # To make this pass, update:
    # class ReviewGenerationRequest(...):
    #     performance_data: Dict[str, Any] = Field(..., min_length=1)
    payload = {"userId": 9, "topicProgressId": 17, "performanceData": {}}
    res = await client.post("/api/ai/generate-review", json=payload)
    assert res.status_code == 422

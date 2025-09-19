# ========== FILE: tests/test_llm_service.py ==========
import asyncio
import json
import pytest
from types import SimpleNamespace
from unittest.mock import AsyncMock

from app.services import llm_service
from app.models import (
    TopicPerformanceData,
    AssessmentAnswer,
    UserProficiencyProfile,
    ContentAdaptationPlan,
    ContentAdaptationFocus,
    LearningPathResponse,
    InsightDetail,
    QuestionDetail,
    QuestionType,
    ReviewResponse,
)

pytestmark = pytest.mark.asyncio  # run async tests with asyncio


# -------------------- generate_learning_path_logic --------------------

async def _mk_learning_path(topics_count=10) -> LearningPathResponse:
    return LearningPathResponse(domain_name="Python Programming",
                                topics=[f"Topic {i}" for i in range(1, topics_count + 1)])


async def test_generate_learning_path_with_assessment_invokes_analyzer_and_passes_profile(mocker, assessment_answer):
    # Arrange: performance data with assessment answers
    perf = TopicPerformanceData(assessment_answers=[assessment_answer])

    # Mock analyzer -> returns proficiency profile
    prof = UserProficiencyProfile(
        user_id=123,
        analyzed_context="InitialAssessment for domain Python Programming",
        overall_understanding_level="Beginner",
        strengths=["Syntax"],
        weaknesses=["OOP"],
        key_observations="Struggles with classes."
    )
    analyzer_mock = mocker.patch.object(
        llm_service._user_data_analyzer_agent, "run",
        new=AsyncMock(return_value=SimpleNamespace(output=prof))
    )

    # Intercept payload to _learning_path_agent.run and assert it contains the embedded profile
    def lp_side_effect(payload_json: str):
        payload = json.loads(payload_json)
        assert payload["domain_name"] == "Python Programming"
        assert "user_proficiency_profile" in payload, "Expected profile to be passed when assessment data exists"
        return SimpleNamespace(
            output=LearningPathResponse(domain_name="Python Programming", topics=[f"Topic {i}" for i in range(1, 11)]))

    lp_mock = mocker.patch.object(
        llm_service._learning_path_agent, "run",
        new=AsyncMock(side_effect=lp_side_effect)
    )

    # Act
    result = await llm_service.generate_learning_path_logic(
        domain_name="Python Programming",
        user_id=123,
        user_topic_performance_data=perf,
    )

    # Assert
    assert isinstance(result, LearningPathResponse)
    assert len(result.topics) == 10
    assert analyzer_mock.await_count == 1
    assert lp_mock.await_count == 1


async def test_generate_learning_path_without_performance_uses_fallback_and_skips_analyzer(mocker):
    # Analyzer should NOT be called
    analyzer_mock = mocker.patch.object(
        llm_service._user_data_analyzer_agent, "run",
        new=AsyncMock(side_effect=AssertionError("Analyzer should not be invoked"))
    )

    # Learning path agent called without profile
    def lp_side_effect(payload_json: str):
        payload = json.loads(payload_json)
        assert "user_proficiency_profile" not in payload, "Did not expect profile without performance data"
        return SimpleNamespace(
            output=LearningPathResponse(domain_name="Data Science", topics=[f"Unit {i}" for i in range(1, 11)]))

    mocker.patch.object(llm_service._learning_path_agent, "run", new=AsyncMock(side_effect=lp_side_effect))

    result = await llm_service.generate_learning_path_logic(domain_name="Data Science")
    assert isinstance(result, LearningPathResponse)
    assert len(result.topics) == 10


async def test_generate_learning_path_analyzer_error_is_tolerated(mocker, assessment_answer):
    perf = TopicPerformanceData(assessment_answers=[assessment_answer])

    # Analyzer raises, logic should continue without profile
    mocker.patch.object(
        llm_service._user_data_analyzer_agent, "run",
        new=AsyncMock(side_effect=Exception("LLM down"))
    )
    mocker.patch.object(
        llm_service._learning_path_agent, "run",
        new=AsyncMock(return_value=SimpleNamespace(output=await _mk_learning_path()))
    )

    result = await llm_service.generate_learning_path_logic("Python Programming", user_id=1,
                                                            user_topic_performance_data=perf)
    assert isinstance(result, LearningPathResponse)


async def test_generate_learning_path_learning_agent_error_bubbles(mocker):
    mocker.patch.object(llm_service._learning_path_agent, "run", new=AsyncMock(side_effect=Exception("LP failed")))
    with pytest.raises(Exception, match="LP failed"):
        await llm_service.generate_learning_path_logic("Python Programming")


# -------------------- generate_insights_logic --------------------

def _insight(title: str, explanation: str) -> InsightDetail:
    return InsightDetail(title=title, explanation=explanation, ai_metadata={"t": "u"}, questions=[])


async def test_generate_insights_happy_path_sequential_calls(mocker, assessment_answer, long_explanation):
    call_order: list[str] = []

    # Analyzer
    prof = UserProficiencyProfile(
        user_id=1,
        analyzed_context="Topic Variables, Level 2",
        overall_understanding_level="Intermediate",
        strengths=["Variables"],
        weaknesses=["Scopes"],
        key_observations="Needs practice with scopes."
    )

    async def analyzer_side_effect(_: str):
        call_order.append("analyzer")
        return SimpleNamespace(output=prof)

    mocker.patch.object(llm_service._user_data_analyzer_agent, "run", new=AsyncMock(side_effect=analyzer_side_effect))

    # Planner
    plan = ContentAdaptationPlan(
        next_topic_name="Variables",
        next_level=2,
        focus=ContentAdaptationFocus.MAINTAIN_PACE,
        specific_instructions_for_insight_generation="Keep explanations concise and example-driven.",
        number_of_insights_to_generate=6,
    )

    async def planner_side_effect(_: str):
        call_order.append("planner")
        return SimpleNamespace(output=plan)

    mocker.patch.object(llm_service._content_adaptation_planner_agent, "run",
                        new=AsyncMock(side_effect=planner_side_effect))

    # Insights agent should be invoked AFTER analyzer/planner with the plan embedded
    def insights_side_effect(payload_json: str):
        call_order.append("insights")
        payload = json.loads(payload_json)
        cap = payload.get("content_adaptation_plan", {})
        assert cap.get("next_topic_name") == "Variables"
        assert cap.get("next_level") == 2
        return SimpleNamespace(output=[
            _insight("Intro to Variables", long_explanation),
            _insight("Variable Assignment", long_explanation),
        ])

    mocker.patch.object(llm_service._insights_agent, "run", new=AsyncMock(side_effect=insights_side_effect))

    # Questions (keep simple; they’ll be attached in add_questions)
    mocker.patch.object(llm_service._question_agent, "run", new=AsyncMock(return_value=SimpleNamespace(output=[])))

    perf = TopicPerformanceData(assessment_answers=[assessment_answer])
    insights = await llm_service.generate_insights_logic(
        domain_name="Python",
        topic_name="Variables",
        level=2,
        user_id=1,
        user_topic_performance_data=perf
    )

    assert [i.title for i in insights] == ["Intro to Variables", "Variable Assignment"]
    assert call_order[:3] == ["analyzer", "planner", "insights"]


async def test_generate_insights_fallback_plan_when_no_perf_data(mocker, long_explanation):
    # Analyzer should not be called
    mocker.patch.object(llm_service._user_data_analyzer_agent, "run",
                        new=AsyncMock(side_effect=AssertionError("No analyzer")))

    # Intercept insights payload to verify fallback plan
    def insights_side_effect(payload_json: str):
        payload = json.loads(payload_json)
        cap = payload["content_adaptation_plan"]
        assert cap["focus"] == ContentAdaptationFocus.MAINTAIN_PACE.value
        assert cap["next_topic_name"] == "Loops"
        assert cap["next_level"] == 1
        return SimpleNamespace(output=[_insight("Loop Basics", long_explanation)])

    mocker.patch.object(llm_service._insights_agent, "run", new=AsyncMock(side_effect=insights_side_effect))

    mocker.patch.object(llm_service._question_agent, "run", new=AsyncMock(return_value=SimpleNamespace(output=[])))

    insights = await llm_service.generate_insights_logic(
        domain_name="Python", topic_name="Loops", level=1, user_id=7, user_topic_performance_data=None
    )
    assert len(insights) == 1
    assert insights[0].title == "Loop Basics"


async def test_generate_insights_empty_agent_output_triggers_generic_fallback(mocker):
    # Insights agent returns empty → we expect "Key Concepts of {topic}" fallback
    mocker.patch.object(llm_service._insights_agent, "run", new=AsyncMock(return_value=SimpleNamespace(output=[])))
    mocker.patch.object(llm_service._question_agent, "run", new=AsyncMock(return_value=SimpleNamespace(output=[])))

    insights = await llm_service.generate_insights_logic(
        domain_name="Python", topic_name="Decorators", level=3, user_id=2, user_topic_performance_data=None
    )
    assert len(insights) == 1
    assert insights[0].title == "Key Concepts of Decorators"
    # Questions empty on fallback ok
    assert insights[0].questions == []


# -------------------- add_questions helper --------------------

async def test_add_questions_attaches_generated_questions(mocker, sample_insight, sample_questions):
    # Mock question agent to return valid QuestionDetail list
    mocker.patch.object(
        llm_service._question_agent, "run",
        new=AsyncMock(return_value=SimpleNamespace(output=sample_questions))
    )

    enriched = await llm_service.add_questions(sample_insight)
    assert len(enriched.questions) == 2
    assert enriched.questions[0].question_type == QuestionType.MULTIPLE_CHOICE

from __future__ import annotations

import asyncio
import json
import os
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field
from pydantic_ai import Agent
from pydantic_ai.models.fallback import FallbackModel
from pydantic_ai.settings import ModelSettings

from app.models import (
    AssessmentAnswer,
    LearningPathResponse,
    ReviewResponse, InsightDetail, QuestionDetail, UserProficiencyProfile, ContentAdaptationPlan, TopicPerformanceData,
    ContentAdaptationFocus,
)

_LLM_MODE = os.getenv("LLM_MODE", "mock").strip().lower()
_USE_REAL_LLM = _LLM_MODE in {"real", "live", "prod", "production", "true", "1"}

# ---------------------------------------------------------------------------
FALLBACK_SEQUENCE_1 = FallbackModel(
    "groq:gpt-oss-120b",                         # huge OSS model, 128k
    "google-gla:gemini-2.5-flash",                 # fast & capable, 1M tokens
    "groq:deepseek-r1-distill-llama-70b",          # 70B distill, strong reasoning
    "groq:llama-3.3-70b-versatile",                # production, solid 70B
    "google-gla:gemini-2.5-flash-lite",            # cheaper large context
    "groq:kimi-k2-0905",                           # 256k context, newer model
    "groq:meta-llama/llama-4-maverick-17b-128e-instruct",  # fallback mid-size
    "google-gla:gemini-2.5-pro", # top reasoning, 1M tokens
)

FALLBACK_SEQUENCE_2 = FallbackModel(
    "google-gla:gemini-2.5-flash",                 # balance of speed/quality
    "groq:gpt-oss-120b",                         # OSS, massive capacity
    "groq:llama-3.3-70b-versatile",                # reliable production Groq
    "groq:deepseek-r1-distill-llama-70b",          # reasoning-focused Groq
    "google-gla:gemini-2.5-flash-lite",            # budget/throughput
    "groq:kimi-k2-0905",                           # 256k context fallback
    "groq:meta-llama/llama-4-maverick-17b-128e-instruct",  # mid-size fallback
    "google-gla:gemini-2.5-pro",                   # higher reasoning
)

# FALLBACK_SEQUENCE_1 = FallbackModel(
#     "google-gla:gemini-2.5-flash",
#     "google-gla:gemini-2.0-flash",
#     "google-gla:gemini-2.5-pro",
#     "groq:deepseek-r1-distill-llama-70b",
#     "google-gla:gemini-2.0-flash-lite",
#     "groq:meta-llama/llama-4-maverick-17b-128e-instruct",
#     "groq:llama3-70b-8192",
# )
#
# FALLBACK_SEQUENCE_2 = FallbackModel(
# "google-gla:gemini-2.0-flash",
# "google-gla:gemini-2.0-flash-lite",
#     "groq:deepseek-r1-distill-llama-70b",
#     "groq:meta-llama/llama-4-maverick-17b-128e-instruct",
#     "groq:llama3-70b-8192",
#     "google-gla:gemini-2.5-flash-preview-05-20",
# )
# print("Using model:", GROQ_MODEL_Deepseek_r1)
_default_groq_settings: ModelSettings = {
    "temperature": 0.8,
    "top_p": 0.95,
}


# ---------- Structured prompt payloads -------------------------------------
async def _maybe_sleep_for_mock():
    if not _USE_REAL_LLM:
        await asyncio.sleep(0)


def _build_mock_learning_path(domain_name: str) -> LearningPathResponse:
    base_topics = [
        "Foundations",
        "Key Concepts",
        "Core Techniques",
        "Guided Practice",
        "Common Pitfalls",
        "Real-World Application",
        "Deep Dive",
        "Project Workshop",
        "Reflection and Review",
        "Next Steps"
    ]
    topics: List[str] = []
    for idx in range(10):
        label = base_topics[idx] if idx < len(base_topics) else f"Advanced Focus {idx + 1}"
        topics.append(f"{domain_name} - {label}")
    return LearningPathResponse(domain_name=domain_name, topics=topics)


def _mock_questions(topic_name: str) -> List[QuestionDetail]:
    options = [
        "It introduces the core idea",
        "It is unrelated background information",
        "It lists tools only",
        "It summarises the entire course"
    ]
    feedbacks = {
        options[0]: "Correct – the explanation focuses on the central idea of the insight.",
        options[1]: "This point actually supports the main concept.",
        options[2]: "Tools appear later; this section sets up the idea.",
        options[3]: "The insight narrows in on one concept rather than the entire course."
    }
    mc_question = QuestionDetail(
        question_type=QuestionType.MULTIPLE_CHOICE,
        question_text=f"Which statement best captures the key lesson from '{topic_name}'?",
        options=options,
        correct_answer=options[0],
        answer_feedbacks=feedbacks,
    )
    tf_question = QuestionDetail(
        question_type=QuestionType.TRUE_FALSE,
        question_text=f"True or False: Practising the ideas from '{topic_name}' with short sessions helps build confidence.",
        options=["True", "False"],
        correct_answer="True",
        answer_feedbacks={
            "True": "Short, deliberate practice reinforces new ideas effectively.",
            "False": "Small practice blocks are recommended to consolidate learning."
        },
    )
    return [mc_question, tf_question]


def _mock_insights(domain_name: str, topic_name: str, level: int) -> List[InsightDetail]:
    paragraphs = [
        f"Focus on the core vocabulary and sounds that define {topic_name}. Begin by repeating short phrases so the mechanics feel natural before adding complexity.",
        f"Link the concept to something you already understand. Making analogies keeps your working memory free while forming strong associations.",
        f"Wrap up by explaining the topic aloud in your own words. Teaching the idea – even to yourself – highlights gaps and builds confidence for the next level."
    ]
    insights: List[InsightDetail] = []
    for idx, paragraph in enumerate(paragraphs, start=1):
        full_explanation = (
            f"Level {level} insight {idx} for {topic_name} emphasises deliberate practice. "
            f"{paragraph} This short reflection should take about a minute to read and sets you up for the next activity."
        )
        insights.append(
            InsightDetail(
                title=f"{topic_name}: Step {idx}",
                explanation=full_explanation,
                ai_metadata={"mode": "mock", "level": level, "domain": domain_name},
                questions=_mock_questions(topic_name),
            )
        )
    return insights


def _mock_review(topic_name: str, level: int) -> ReviewResponse:
    summary = (
        f"You reviewed {topic_name} at level {level} by revisiting the core idea, applying it in a short exercise, and summarising the learning in your own words. "
        "This rhythm keeps the topic approachable while your long-term memory consolidates new patterns."
    )
    strengths = [f"Consistent engagement with {topic_name}", "Willingness to reflect after every session"]
    weaknesses = ["Pause for quick self-tests before moving on", "Write down one actionable takeaway per study block"]
    return ReviewResponse(summary=summary, strengths=strengths, weaknesses=weaknesses)

class LearningPathPrompt(BaseModel):
    domain_name: str = Field(..., description="The name of the learning domain for which the LLM should generate a learning path.")
    assessment_answers: List[AssessmentAnswer] = Field(default_factory=list, description="A list of user's answers from an initial assessment, used by the LLM to potentially tailor the learning path.")


class InsightsPrompt(BaseModel):
    domain_name: str  = Field(..., description="The name of the learning domain relevant to the requested insights.")
    topic_name: str = Field(..., description="The specific topic within the domain for which insights are to be generated.")
    level: int = Field(..., description="The difficulty level for which the insights should be tailored.")
    user_id: Optional[int] = Field(None, description="Optional unique identifier for the user, allowing for context if available for personalized insight generation.")
    user_performance_metrics: Dict[str, Any] = Field(default_factory=dict, description="A dictionary of user performance metrics, such as accuracy or common errors, to guide the LLM in adapting insights. This is usually derived from UserProficiencyProfile and ContentAdaptationPlan.")


class ReviewPrompt(BaseModel):
    topic_name: str = Field(..., description="The name of the topic for which a review is being generated.")
    level: int = Field(..., description="The difficulty level of the topic content being reviewed.")
    performance_data: Dict[str, Any] = Field(..., description="A dictionary containing detailed user performance data (e.g., accuracy, common errors, correctly/incorrectly answered concepts) to inform the review generation by the LLM.")



_user_data_analyzer_agent = Agent(
    model=FALLBACK_SEQUENCE_2,
    output_type=UserProficiencyProfile,
    retries=6,
    system_prompt=(
        "You are an expert learning analyst AI. Your task is to analyze the provided user performance data "
        "(TopicPerformanceData) and generate a UserProficiencyProfile.\n"
        "Input 'TopicPerformanceData' contains 'assessment_answers' (for initial assessment) OR 'insights_performance' (for topic-level adaptation).\n"
        "Identify key strengths (concepts user grasps well) and weaknesses (areas needing improvement).\n"
        "Suggest an 'overall_understanding_level' (e.g., Beginner, Intermediate, Advanced, Needs Reinforcement).\n"
        "For 'analyzed_context', use the input topic_name and level, or 'InitialAssessment for domain X' if analyzing assessment_answers.\n"
        "Provide a concise 'key_observations' summary.\n"
        "Base your analysis on patterns in correct/incorrect answers, types of questions struggled with, and any time_taken data if significant.\n"
        "Return **only** valid JSON for the UserProficiencyProfile model."
    ),
    model_settings=_default_groq_settings,
)

_content_adaptation_planner_agent = Agent(
    model=FALLBACK_SEQUENCE_1,
    output_type=ContentAdaptationPlan,
    retries=6,
    system_prompt=(
        "You are an adaptive learning strategist AI. Based on the UserProficiencyProfile, current topic, and level, "
        "create a ContentAdaptationPlan for the next set of insights.\n"
        "The plan should specify 'next_topic_name' (usually same as current) and 'next_level'.\n"
        "Determine the 'focus' (REINFORCE_WEAKNESSES, EXPLORE_STRENGTHS, MAINTAIN_PACE, ACCELERATE).\n"
        "Provide 'specific_instructions_for_insight_generation' that are actionable for an instructional designer AI. "
        "These instructions should promote general understanding and avoid over-specialization or bias. "
        "E.g., 'Focus on foundational concepts of X using clear analogies.', 'Provide varied examples for Y.', 'Challenge with slightly more complex scenarios for Z, ensuring explanations are thorough'.\n"
        "Set 'number_of_insights_to_generate' (typically 6).\n"
        "If weaknesses are prominent, suggest REINFORCE_WEAKNESSES. If strengths are strong, consider EXPLORE_STRENGTHS or ACCELERATE. Otherwise, MAINTAIN_PACE.\n"
        "Return **only** valid JSON for the ContentAdaptationPlan model."
    ),
    model_settings=_default_groq_settings,
)

_learning_path_agent = Agent(
    model=FALLBACK_SEQUENCE_1,  # Ensure this model is suitable
    output_type=LearningPathResponse,
    retries=6,
    system_prompt=(  # Modified prompt
        "You are a curriculum designer AI.\n"
        "Use the supplied JSON payload (containing 'domain_name' and optionally 'user_proficiency_profile' from an initial assessment) "
        "to build a **LearningPathResponse**.\n"
        "• 'topics' must be 10‑20 unique topic titles that gradually increase in difficulty, covering the domain comprehensively.\n"
        "• If 'user_proficiency_profile' is provided, subtly adjust the emphasis or ordering of early topics based on identified strengths/weaknesses, "
        "but ensure the overall path remains balanced and generally applicable. Do not make the path too niche.\n"
        "• DO NOT invent new fields or change property names. Adhere strictly to the LearningPathResponse schema.\n"
        "Return **only** valid JSON for the response model."
    ),
    model_settings=_default_groq_settings,
)

_insights_agent = Agent(
    model=FALLBACK_SEQUENCE_2,
    output_type=List[InsightDetail],
    retries=6,
    system_prompt=(  # Modified prompt
        "You are an instructional designer AI creating adaptive micro-learning insights.\n"
        "Given the payload (containing 'domain_name', 'topic_name', 'level', and a 'content_adaptation_plan'), "
        "produce a list of **InsightDetail** items as specified by 'number_of_insights_to_generate' in the plan.\n"
        "Strictly follow the 'specific_instructions_for_insight_generation' and 'focus' from the 'content_adaptation_plan'.\n"
        "For each insight, fill 'title' (~5-10 words) and a concise 'explanation' (3-5 sentences, ~1-minute read). Leave 'questions' empty.\n"
        "Ensure content is general, unbiased, and suitable for the specified level, even when adapting. Avoid overly specific or niche examples unless crucial and guided by the plan.\n"
        "Return **only** valid JSON for the List[InsightDetail] response model."
    ),
    model_settings=_default_groq_settings,
)

_question_agent = Agent(
    model=FALLBACK_SEQUENCE_2,
    output_type=List[QuestionDetail],
    retries=6,
    system_prompt=(
        "You are a question-writing AI.\n"
        "Given the insight title & explanation, return a list of around "
        "2 QuestionDetail objects that match this JSON schema:\n"
        f"{QuestionDetail.model_json_schema()}\n"
        "Return only valid JSON for the response model."
    ),
    model_settings=_default_groq_settings,
)

_review_agent = Agent(
    model=FALLBACK_SEQUENCE_2,
    output_type=ReviewResponse,
    retries=6,
    system_prompt=(
        "You are a tutoring AI preparing a spaced repetition review.\n"
        "Given the payload (topic_name, level, and performance_data which includes accuracy, common errors, etc.) "
        "produce a **ReviewResponse** with:\n"
        "• 'summary' (2‑3 paragraphs recapping progress and areas of focus).\n"
        "• around 3‑5 bullet 'strengths' and 'weaknesses' derived from performance_data.\n"
        "Return only valid JSON for the response model."
    ),
    model_settings=_default_groq_settings,
)


async def generate_learning_path_logic(
        domain_name: str,
        user_id: Optional[int] = None,
        user_topic_performance_data: Optional[TopicPerformanceData] = None,
) -> LearningPathResponse:
    if not _USE_REAL_LLM:
        await _maybe_sleep_for_mock()
        return _build_mock_learning_path(domain_name)

    proficiency_profile_dict = None
    if user_topic_performance_data and user_topic_performance_data.assessment_answers:
        try:
            analyzer_payload = user_topic_performance_data.model_copy(
                update={"user_id": user_id, "domain_name": domain_name,
                        "topic_name": None, "current_level": None}  # Clear topic/level for assessment
            )
            # Ensure context is set for assessment
            analyzer_payload_json = json.loads(analyzer_payload.model_dump_json(exclude_none=True))
            analyzer_payload_json["analyzed_context"] = f"InitialAssessment for domain {domain_name}"

            analysis_result = await _user_data_analyzer_agent.run(json.dumps(analyzer_payload_json))
            proficiency_profile: UserProficiencyProfile = analysis_result.output
            proficiency_profile_dict = proficiency_profile.model_dump(exclude_none=True)
            print(f"Initial Proficiency Profile: {json.dumps(proficiency_profile_dict)}")
        except Exception as e:
            print(f"Error during initial proficiency analysis: {e}. Proceeding without profile.")
            proficiency_profile_dict = None

    learning_path_payload = {"domain_name": domain_name}
    if proficiency_profile_dict:
        learning_path_payload["user_proficiency_profile"] = proficiency_profile_dict

    result = await _learning_path_agent.run(json.dumps(learning_path_payload))
    return result.output


async def add_questions(one_insight: InsightDetail) -> InsightDetail:  # Unchanged
    q_payload = {
        "title": one_insight.title,
        "explanation": one_insight.explanation,
    }
    if not _USE_REAL_LLM:
        return one_insight  # mock mode
    try:
        resp = await _question_agent.run(json.dumps(q_payload))
        if resp.output:  # Check if output is not None
            one_insight.questions = resp.output
    except Exception as e:
        print(f"Error generating questions for insight '{one_insight.title}': {e}")
        one_insight.questions = []  # Fallback to empty list
    return one_insight


async def generate_insights_logic(
        domain_name: str,
        topic_name: str,
        level: int,
        user_id: Optional[int],
        user_topic_performance_data: Optional[TopicPerformanceData] = None,
) -> List[InsightDetail]:
    if not _USE_REAL_LLM:
        await _maybe_sleep_for_mock()
        return _mock_insights(domain_name, topic_name, level)

    adaptation_plan_dict = None
    proficiency_profile = None

    if user_topic_performance_data and (
            user_topic_performance_data.insights_performance or user_topic_performance_data.assessment_answers):
        try:
            analyzer_payload = user_topic_performance_data.model_copy(
                update={
                    "user_id": user_id, "domain_name": domain_name,
                    "topic_name": topic_name, "current_level": level
                }
            )
            # Ensure context is set for topic/level
            analyzer_payload_json = json.loads(analyzer_payload.model_dump_json(exclude_none=True))
            analyzer_payload_json["analyzed_context"] = f"Topic {topic_name}, Level {level}"

            analysis_result = await _user_data_analyzer_agent.run(json.dumps(analyzer_payload_json))
            proficiency_profile: UserProficiencyProfile = analysis_result.output
            print(
                f"Proficiency Profile for Insights ({topic_name} L{level}): {proficiency_profile.model_dump_json(exclude_none=True)}")

            # 2. Get Content Adaptation Plan
            planner_payload = {
                "user_proficiency_profile": proficiency_profile.model_dump(exclude_none=True),
                "current_topic_name_from_profile": topic_name,  # Pass explicitly for planner context
                "current_level_from_profile": level,
                "domain_name": domain_name
            }
            plan_result = await _content_adaptation_planner_agent.run(json.dumps(planner_payload))
            adaptation_plan: ContentAdaptationPlan = plan_result.output
            adaptation_plan_dict = adaptation_plan.model_dump(exclude_none=True)
            print(f"Content Adaptation Plan: {json.dumps(adaptation_plan_dict)}")

        except Exception as e:
            print(f"Error during insight adaptation analysis/planning: {e}. Using fallback plan.")
            adaptation_plan_dict = None  # Fallback

    if not adaptation_plan_dict:  # Fallback plan if analysis failed or no data
        fallback_plan = ContentAdaptationPlan(
            next_topic_name=topic_name,
            next_level=level,
            focus=ContentAdaptationFocus.MAINTAIN_PACE,
            specific_instructions_for_insight_generation=f"Generate standard introductory insights for {topic_name} at level {level}.",
            number_of_insights_to_generate=6
        )
        adaptation_plan_dict = fallback_plan.model_dump(exclude_none=True)
        print(f"Using Fallback Content Adaptation Plan: {json.dumps(adaptation_plan_dict)}")

    insights_payload = {
        "domain_name": domain_name,
        "topic_name": adaptation_plan_dict.get("next_topic_name", topic_name),  # Use plan's target topic/level
        "level": adaptation_plan_dict.get("next_level", level),
        "user_id": user_id,
        "content_adaptation_plan": adaptation_plan_dict,
    }

    result = await _insights_agent.run(json.dumps(insights_payload))
    insights: List[InsightDetail] = result.output

    if not insights:  # Fallback if AI returns empty list
        print(
            f"Warning: Insights agent returned no insights for {topic_name} L{level}. Generating generic fallback insight.")
        fallback_insight = InsightDetail(
            title=f"Key Concepts of {topic_name}",
            explanation=f"Understanding {topic_name} is important. This section will cover fundamental aspects. Ensure to review related materials if needed."
        )
        insights = [fallback_insight]  # Return a list with one fallback

    enriched_insights = await asyncio.gather(
        *[add_questions(insight) for insight in insights]
    )
    return enriched_insights


async def generate_review_logic(
        topic_name: str,
        level: int,
        performance_data: Dict[str, Any],  # Java will construct this from its TopicPerformanceDataDTO
) -> ReviewResponse:

    if not _USE_REAL_LLM:
        await _maybe_sleep_for_mock()
        return _mock_review(topic_name, level)

    payload = {
        "topic_name": topic_name,
        "level": level,
        "performance_data": performance_data,
    }
    result = await _review_agent.run(json.dumps(payload))
    return result.output
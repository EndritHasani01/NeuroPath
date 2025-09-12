from __future__ import annotations

from enum import Enum
from typing import List, Dict, Any, Optional

from pydantic import BaseModel, Field, conint, constr
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    """Common base that renders snake_case fields in *camelCase* so that
    the JSON produced by FastAPI maps 1‑to‑1 to the Java / React DTOs.
    """
    model_config = {
        "alias_generator": to_camel,
        "populate_by_name": True,
        "extra": "forbid",  # no unexpected properties!
    }


# ---------- Enums ----------------------------------------------------------
class QuestionType(str, Enum):
    MULTIPLE_CHOICE = "MULTIPLE_CHOICE"
    TRUE_FALSE = "TRUE_FALSE"

class ContentAdaptationFocus(str, Enum):
    REINFORCE_WEAKNESSES = "REINFORCE_WEAKNESSES"
    EXPLORE_STRENGTHS = "EXPLORE_STRENGTHS"
    MAINTAIN_PACE = "MAINTAIN_PACE"
    ACCELERATE = "ACCELERATE"


# ---------- User Performance & Adaptation Plan Models ----------------------
class UserAnswerDetail(CamelModel):
    question_id: int = Field(..., description="Unique identifier for the question, corresponding to the QuestionEntity ID in the backend system.")
    question_text: str = Field(..., description="The full text of the question asked.")
    options: Optional[List[str]] = Field(None, description="A list of possible answer choices for multiple-choice questions. Will be null or empty for true/false questions if options are not presented as text.")
    selected_answer: str = Field(..., description="The answer chosen by the user.")
    correct_answer: str = Field(..., description="The correct answer to the question.")
    is_correct: bool = Field(..., description="Boolean flag indicating if the user's selected answer was correct.")
    time_taken_ms: Optional[int] = Field(None, description="Optional time in milliseconds the user spent on this question.")

class InsightPerformanceData(CamelModel):
    insight_id: int = Field(..., description="Unique identifier for the insight, corresponding to the InsightEntity ID in the backend system.")
    insight_title: str = Field(..., description="A concise, self-explanatory title for the insight. Reading the title alone should give a clear understanding of the insight's core message.")
    # insight_explanation: Optional[str] = None # Can be large, maybe omit for performance data
    questions_answered: List[UserAnswerDetail] = Field(default_factory=list, description="List of details for each question answered by the user related to this insight.")
    times_shown: Optional[int] = Field(default=0, description="Number of times this insight has been presented to the user.")

class TopicPerformanceData(CamelModel): # Main "User Data" payload from Java
    user_id: Optional[int] = Field(None, description="Optional unique identifier for the user.")
    domain_name: Optional[str] = Field(None, description="Optional name of the learning domain this performance data pertains to.")
    topic_name: Optional[str] = Field(None, description="Optional name of the specific topic this performance data pertains to.")
    current_level: Optional[int] = Field(None, description="Optional current difficulty or progression level of the user within the topic.")
    assessment_answers: Optional[List[AssessmentAnswer]] = Field(default_factory=list, description="List of answers provided by the user during an initial assessment at the very beginning. Used for initial adaptation.")
    insights_performance: Optional[List[InsightPerformanceData]] = Field(default_factory=list, description="List of performance data for specific insights within a topic. Used for topic-level adaptation. Importance is higher.")

class UserProficiencyProfile(CamelModel): # Output of UserDataAnalyzerAgent
    user_id: Optional[int] = Field(None, description="Optional unique identifier for the user whose proficiency is being profiled.")
    analyzed_context: Optional[str] = Field(None, description="e.g., 'InitialAssessment for Domain X' or 'Topic Y, Level Z'")
    overall_understanding_level: Optional[constr(min_length=3, max_length=50)] = Field(
        None, description="e.g., Beginner, Intermediate, Advanced, Needs Reinforcement"
    )
    strengths: List[str] = Field(default_factory=list, description="Specific concepts or skills user grasps well.")
    weaknesses: List[str] = Field(default_factory=list, description="Specific concepts or areas needing improvement.")
    key_observations: Optional[str] = Field(None, description="Brief summary of patterns or important points.")

class ContentAdaptationPlan(CamelModel): # Output of ContentAdaptationPlannerAgent
    next_topic_name: Optional[str] = Field(None, description="The name of the topic recommended for the user to engage with next. Often the current topic, but could be different for branching.")
    next_level: Optional[int] = Field(None, description="The recommended difficulty level for the next set of insights within the `next_topic_name`.")
    focus: ContentAdaptationFocus = Field(ContentAdaptationFocus.MAINTAIN_PACE, description="The primary pedagogical focus for the next set of content (e.g., reinforce weaknesses, explore strengths).")
    specific_instructions_for_insight_generation: Optional[str] = Field(
        None, description="Customized guidance highlighting effective ways to tailor insights based on user's identified strengths and weaknesses, such as recommending analogies for abstract concepts, applying real-world scenarios, or reinforcing foundational knowledge."
    )
    number_of_insights_to_generate: conint(ge=6, le=10) = 6 # Default to 6 as per problem (6 insights per level)


# ---------- Request DTOs (from Java to Python) -------------------------------------
class AssessmentAnswer(CamelModel): # This is what Java should send for assessment answers
    question_id: conint(gt=0) = Field(..., description="Unique identifier for the assessment question.")
    question_text: constr(min_length=3, max_length=500) = Field(..., description="The full text of the assessment question.")
    options: List[constr(min_length=1, max_length=120)] = Field(..., description="List of answer choices provided for the question. For TRUE/FALSE questions, this might be ['True', 'False'] or could be empty if not using explicit option text.")
    selected_answer: constr(min_length=1, max_length=120) = Field(..., description="The answer selected by the user for this assessment question.")
    # Correctness is not part of initial assessment, it's for learning path generation

class LearningPathRequest(CamelModel): # For /api/ai/generate-learning-path
    domain_name: str = Field(..., description="The name of the learning domain for which the learning path is requested.")
    user_id: Optional[int] = Field(None, description="Optional unique identifier for the user, used for personalized path generation if available.")
    user_topic_performance_data: Optional[TopicPerformanceData] = Field(None, description="Optional performance data of the user, typically containing assessment answers, to tailor the initial learning path.")

class InsightsRequest(CamelModel): # For /api/ai/generate-insights
    domain_name: str  = Field(..., description="The name of the learning domain to which the topic belongs.")
    topic_name: str = Field(..., description="The name of the specific topic for which insights are requested.")
    level: int = Field(..., description="The difficulty level within the topic for which insights are requested.")
    user_id: Optional[int] = Field(None, description="Optional unique identifier for the user, for personalized insight generation.")
    user_topic_performance_data: Optional[TopicPerformanceData] = Field(None, description="Optional performance data of the user for the current topic, used to adapt the generated insights.")

class SimpleInsightInfo(CamelModel): # Existing, used by /get-next-insight
    insight_id: int = Field(..., description="Unique identifier for the insight.")
    title: constr(min_length=3, max_length=120) = Field(..., description="The concise title of the insight.")
    relevance_score: Optional[float] = Field(default=0.0, ge=0, le=1, description="A score (0.0 to 1.0) indicating the calculated relevance of this insight for the user at this time.")
    times_shown: Optional[int] = Field(default=0, ge=0, description="The number of times this specific insight has already been shown to the user.")

class NextInsightRequest(CamelModel): # Existing
    user_id: Optional[int] = Field(None, description="Optional unique identifier for the user requesting the next insight.")
    topic_progress_id: Optional[int] = Field(None, description="Optional identifier for the user's current progress within a topic, aiding context.")
    uncompleted_insights: List[SimpleInsightInfo] = Field(..., min_items=1, description="A list of insights that are available but not yet completed by the user, including their IDs, titles, relevance scores, and times shown.")

class ReviewGenerationRequest(CamelModel): # Existing
    user_id: Optional[int] = Field(None, description="Optional unique identifier of the user for whom the review is being generated.")
    topic_progress_id: Optional[int] = Field(None, description="Optional identifier for the user's current progress within a topic, providing context for the review.")
    performance_data: Dict[str, Any] = Field(..., description="A dictionary containing structured performance data for the topic, which will be used to generate a personalized review.", min_length=1)

# ---------- Response DTOs (Python to Java) ---------------------------------------
class LearningPathResponse(CamelModel): # Existing
    domain_name: str = Field(..., description="The name of the learning domain for which this learning path is defined.")
    topics: List[str] = Field(..., min_items=10, max_items=20, description="An ordered list of topic names that constitute the learning path, typically 10-20 topics, progressing in difficulty.")

class QuestionDetail(CamelModel): # Existing
    question_type: QuestionType = Field(..., description="The type of question, e.g., MULTIPLE_CHOICE or TRUE_FALSE.")
    question_text: constr(min_length=5, max_length=500) = Field(..., description="The full text of the question being asked.")
    options: List[str] = Field(..., description="A list of possible answer strings for the question. For TRUE/FALSE, this might be ['True', 'False'] or specific statements.")
    correct_answer: str = Field(..., description="The string representing the correct answer from the `options` list.")
    answer_feedbacks: Dict[str, str] = Field(default_factory=dict, description="A dictionary where keys are the answer options (or 'True'/'False') and values are specific feedback strings for choosing that option. This can provide tailored explanations for both correct and incorrect choices.")

class InsightDetail(CamelModel): # Existing
    title: constr(min_length=3, max_length=100) = Field(..., description="A concise, self-explanatory title for the insight (around 5-10 words). Reading the title alone should give a clear understanding of the insight's core message.")
    explanation: constr(min_length=100, max_length=5_000) = Field(..., description="A detailed explanation of the insight (typically 3-5 sentences, aiming for a ~1-minute read time), elaborating on the title and providing context, examples, or further information. This should be between 100 and 5,000 characters.")
    ai_metadata: Optional[Dict[str, Any]] = Field(default_factory=dict, description="Optional dictionary for storing any AI-specific metadata related to the generation or properties of this insight, such as model version, generation parameters, or internal tags.")
    questions: List[QuestionDetail] = Field(default_factory=list, description="A list of questions related to the insight, designed to assess understanding. Each question can be either multiple-choice or true/false, with only one correct answer.")

class InsightsGroup(CamelModel): # Existing (though /generate-insights now returns List[InsightDetail] directly)
    domain: str = Field(..., description="The name of the learning domain to which these insights belong.")
    topic: str = Field(..., description="The name of the specific topic covered by these insights.")
    insights: List[InsightDetail] = Field(..., min_items=1, description="A list of detailed insights for the specified domain and topic.")

class NextInsightResponse(CamelModel): # Existing
    insight_id: int = Field(..., description="The unique identifier of the next insight selected to be shown to the user.")

class ReviewResponse(CamelModel): # Existing
    summary: str = Field(..., description="A textual summary (2-3 paragraphs) recapping the user's progress and highlighting key areas of focus for review.")
    strengths: List[str] = Field(default_factory=list, description="A list of specific concepts or skills identified as strengths based on the user's performance.")
    weaknesses: List[str] = Field(default_factory=list, description="A list of specific concepts or areas identified as needing improvement or further review based on the user's performance.")
    # revision_questions: List[QuestionDetail] # This was commented out, keep as is or decide based on full req.
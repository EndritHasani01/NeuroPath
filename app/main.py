from typing import List, Dict, Any # Ensure Dict, Any are imported

from fastapi import FastAPI, HTTPException, Body
from fastapi.exceptions import RequestValidationError
from fastapi.responses    import JSONResponse
from app.core.config import settings
from app.models import ( # Import new and existing models from app.models
    LearningPathResponse,
    # InsightsGroup, # Not directly returned by /generate-insights anymore
    NextInsightRequest,
    NextInsightResponse,
    ReviewGenerationRequest,
    ReviewResponse, InsightDetail,
    LearningPathRequest, # New request DTO
    InsightsRequest      # New request DTO
)
from app.services import indexing_service, llm_service

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="AI microservice powering the Adaptive Learning Platform.",
)

# Catch & log validation errors so we see exactly which field is wrong
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    # Log the raw JSON + the validation errors
    print("⛔️ Invalid /api/ai/generate-insights request:", await request.body())
    print(exc.errors())
    return JSONResponse(status_code=422, content={"detail": exc.errors()})
@app.get("/", tags=["General"], summary="Health‑check")
async def health_check() -> dict[str, str]:
    return {"status": "ok"}

@app.post(
    "/api/ai/generate-learning-path",
    response_model=LearningPathResponse,
    tags=["Learning Path"],
    summary="Generate learning path (adaptable with assessment data)",
)
async def generate_learning_path(request: LearningPathRequest) -> LearningPathResponse: # Use new request DTO
    """Creates a 10‑20 step curriculum for the selected domain.
    If assessment_answers are provided via user_topic_performance_data, the path may be adapted."""
    try:
        return await llm_service.generate_learning_path_logic(
            domain_name=request.domain_name,
            user_id=request.user_id,
            user_topic_performance_data=request.user_topic_performance_data
        )
    except Exception as exc:
        print(f"Exception in generate_learning_path: {exc}") # More logging
        raise HTTPException(
            status_code=500, detail=f"Failed to generate learning path: {str(exc)}"
        ) from exc

@app.post(
    "/api/ai/generate-insights",
    response_model=List[InsightDetail], # Directly returns list of insights
    tags=["Insights"],
    summary="Generate insight batch (adaptable with user performance data)",
)
async def generate_insights(request: InsightsRequest = Body(..., embed=False)) -> List[InsightDetail]: # Use new request DTO
    """Generates micro‑learning insights for the given topic/level,
    adapted based on user_topic_performance_data if provided."""
    try:
        return await llm_service.generate_insights_logic(
            domain_name=request.domain_name,
            topic_name=request.topic_name,
            level=request.level,
            user_id=request.user_id,
            user_topic_performance_data=request.user_topic_performance_data,
        )
    except Exception as exc:
        print(f"Exception in generate_insights: {exc}") # More logging
        raise HTTPException(
            status_code=500, detail=f"Failed to generate insights: {str(exc)}"
        ) from exc

# Endpoints for get_next_insight and generate_review remain structurally the same
@app.post(
    "/api/ai/get-next-insight",
    response_model=NextInsightResponse,
    tags=["Insights"],
    summary="Pick next insight",
)
async def get_next_insight(request: NextInsightRequest) -> NextInsightResponse:
    if not request.uncompleted_insights:
        raise HTTPException(status_code=400, detail="No uncompleted insights provided.")
    try:
        return indexing_service.choose_next_insight_logic( # Assuming indexing_service is simpler and not LLM-based for now
            uncompleted_insights=request.uncompleted_insights,
            user_id=request.user_id,
        )
    except ValueError as val_err:
        raise HTTPException(status_code=400, detail=str(val_err)) from val_err
    except Exception as exc:
        raise HTTPException(
            status_code=500, detail=f"Failed to choose next insight: {str(exc)}"
        ) from exc

@app.post(
    "/api/ai/generate-review",
    response_model=ReviewResponse,
    tags=["Review"],
    summary="Generate spaced‑repetition review",
)
async def generate_review(request: ReviewGenerationRequest) -> ReviewResponse:
    try:
        # performance_data comes as Dict[str, Any], it's up to Java to structure it meaningfully.
        topic_name = request.performance_data.get("topicName", "Unknown Topic")
        level = request.performance_data.get("level", 1)

        return await llm_service.generate_review_logic(
            topic_name=str(topic_name),
            level=int(level),
            performance_data=request.performance_data, # Pass the whole dict
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Failed to generate review: {str(exc)}") from exc
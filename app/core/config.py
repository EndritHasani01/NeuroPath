import os
from dotenv import load_dotenv
from pydantic import BaseModel

load_dotenv()

class Settings(BaseModel):
    APP_NAME: str = "Adaptive Learning AI Service"
    APP_VERSION: str = "0.0.1"
    GROQ_API_KEY: str = os.getenv("GROQ_API_KEY", "...")
    # For LlamaIndex, if using a specific model for embeddings locally or from an API
    # EMBEDDING_MODEL_NAME: str = "BAAI/bge-small-en-v1.5" # Example for local HuggingFace model
    # LLAMA_INDEX_LLM_MODEL: str = "mixtral-8x7b-32768" # Groq model also for LlamaIndex

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

settings = Settings()

# Ensure GROQ_API_KEY is set
if settings.GROQ_API_KEY == "your_groq_api_key_here_if_not_in_env" or not settings.GROQ_API_KEY:
    print("WARNING: GROQ_API_KEY is not set. Please set it in your environment or .env file.")
    # For prototype, we might allow it to proceed with mocked responses if needed,
    # but real functionality will fail.
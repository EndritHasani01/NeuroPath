import os
from dotenv import load_dotenv
from pydantic import BaseModel

load_dotenv()

class Settings(BaseModel):
    APP_NAME: str = "Adaptive Learning AI Service"
    APP_VERSION: str = "0.0.1"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

settings = Settings()

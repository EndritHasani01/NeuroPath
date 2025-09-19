# ---------- Build stage ----------------------------------------------------
FROM python:3.12-slim AS builder
WORKDIR /app

# 1. Install system deps that some wheels need (uvicorn's uvloop, etc.)
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential curl && \
    rm -rf /var/lib/apt/lists/*

# 2. Install Python deps
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# 3. Copy the source
COPY . .

# ---------- Final runtime image -------------------------------------------
FROM python:3.12-slim
WORKDIR /app

# Copy only the virtual-env from builder
COPY --from=builder /usr/local/lib/python3.12 /usr/local/lib/python3.12
COPY --from=builder /usr/local/bin /usr/local/bin
COPY --from=builder /app /app

# Render sets PORT, so honour it
ENV PORT=8000 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

EXPOSE $PORT


CMD uvicorn app.main:app --host 0.0.0.0 --port $PORT --workers 1
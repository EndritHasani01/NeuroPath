# InsightPath AI – Frontend

This is the frontend for **InsightPath AI**, a learning platform that delivers personalized AI-generated insights and questions. Built with **React 18**, **Vite**, and **Material-UI v5**, it interfaces with a Spring Boot backend and a FastAPI microservice.

## Features

* User authentication via JWT (login/register)
* Domain and topic selection
* AI-generated insights, questions, and review summaries
* Responsive UI using Material-UI components
* Protected routes and context-based state management

## Project Structure

```
src/
├─ pages/             # Screens (login, dashboard, etc.)
├─ components/        # Reusable UI (InsightView, QuestionSection, etc.)
├─ contexts/          # Auth and learning state providers
├─ services/          # API client and auth helpers
└─ theme.js           # MUI custom theme
```

## Tech Stack

* **React 18**, **Vite**
* **Material-UI v5**
* **React Router v6**
* **Axios** with JWT interceptors
* **Context API + Reducer** for state

## Setup

```bash
# Install dependencies
npm install

# Start dev server
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

## Authentication

* Tokens stored in `localStorage`
* Axios auto-injects `Authorization` header
* Unauthenticated users redirected to `/login`

## Deployment

Builds to static files served via Nginx or Caddy:

```bash
npm run build
```

## Key Files

* `src/contexts/LearningContext.jsx`: global state machine
* `src/components/InsightView.jsx`: renders insight and questions
* `src/services/api.js`: Axios setup and interceptors


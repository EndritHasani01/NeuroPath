# InsightPath AI Frontend

This project contains the React application used by **InsightPath AI**. It communicates with a separate REST API to provide personalized learning paths, insights, and review questions.

## Stack

* **React 19** with [Vite](https://vitejs.dev/) for development and bundling
* **Material-UI 7** for UI components and styling
* **React Router 7** for routing
* **Axios** with interceptors for API access
  
## Features

* User authentication via JWT (login/register)
* Domain and topic selection
* AI-generated insights, questions, and review summaries
* Responsive UI using Material-UI components
* Protected routes and context-based state management

## Project layout

```
src/
├─ components/        # Reusable UI elements and layout pieces
│  ├─ common/         # Helpers such as PrivateRoute
│  └─ layout/         # Shared layout like the Header
├─ pages/             # Route screens (login, domain overview, learning, etc.)
├─ contexts/          # Global state providers
├─ services/          # Axios client and auth helpers
├─ assets/            # Static assets
├─ theme.js           # Custom Material-UI theme
└─ main.jsx           # Application bootstrap
```

## Development

1. Install dependencies:

   ```bash
   npm install
   ```
2. Start the development server (ensure `VITE_API_BASE_URL` points to the backend API):

   ```bash
   VITE_API_BASE_URL=http://localhost:8080 npm run dev
   ```
3. Run the linter:

   ```bash
   npm run lint
   ```
4. Build optimized assets:

   ```bash
   npm run build
   ```

   Output will be in the `dist/` directory.

## Routing

Routes are defined in `src/App.jsx`. Public pages are `"/login"` and `"/register"`; all others use `PrivateRoute` to require a valid token.

```
/                 → DomainSelectorPage
/assessment       → AssessmentPage
/domain/:id       → DomainHomePage
/learn            → LearningDashboardPage
/review           → ReviewView
```

`PrivateRoute` (in `src/components/common/PrivateRoute.jsx`) checks the token from `AuthContext` and redirects unauthenticated users to `/login`.

## State management

`LearningContext` (in `src/contexts/LearningContext.jsx`) holds the application state such as available domains, assessment answers, current insight, and review data. It exposes helper functions to fetch domains, submit answers, and advance the learning flow.

## API client

All network requests go through `src/services/api.js`. The Axios instance automatically attaches the `Authorization` header and redirects to the login page on a `401` response. Key endpoints include:

* `GET /learning/domains/status` – list available domains and progress
* `GET /learning/domains/{id}/assessment-questions`
* `POST /learning/domains/start` – begin a domain and get the learning path
* `GET /learning/domains/{id}/next-insight`
* `POST /learning/insights/submit-answer`
* `GET /learning/domains/{id}/progress`
* `GET /learning/domains/{id}/review`
* `POST /learning/domains/{id}/complete-review?satisfactoryPerformance=`
* `GET /learning/domains/{id}/overview`
* `POST /learning/domains/{id}/select-topic/{topicIdx}`
* `GET /users/me` – fetch profile information

Authentication helpers live in `src/services/auth.js` and wrap the `/auth/login` and `/auth/register` endpoints.

## Theming

The Material-UI theme defined in `src/theme.js` customises the colour palette, typography, and component defaults used across the app.

---

After cloning this repository and configuring the backend URL, the app can be started locally. Upon sign-up or login, users pick a domain, take an assessment, and then begin receiving generated insights with question sections. Progress is stored server-side and fetched through the endpoints above, allowing the UI to adapt as learners advance.

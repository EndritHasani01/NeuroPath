import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Container, Box, Typography, Alert } from '@mui/material';
import { useLearningContext } from './contexts/LearningContext';

import PrivateRoute from './components/common/PrivateRoute';
import LoginPage from './pages/LoginPage.jsx';
import DomainSelectorPage from './pages/DomainSelectorPage';
import AssessmentPage from './pages/AssessmentPage';
import LearningDashboardPage from './pages/LearningDashboardPage';
import DomainHomePage from './components/DomainHomePage';
import ReviewView from './components/ReviewView';
import RegisterPage from "./pages/RegisterPage.jsx";
import Header from "./components/layout/Header.jsx";

function App() {
    const { state } = useLearningContext();
    const isLoggedIn = !!localStorage.getItem('token');

    return (
        <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
            {isLoggedIn && <Header />}

            {state.error && (
                <Alert severity="error" sx={{ my: 2 }}>
                    {state.error}
                </Alert>
            )}

            <Routes>
                <Route path="/login"     element={<LoginPage />} />
                <Route path="/register"  element={<RegisterPage />} />

                {/* everything under here is protected */}
                <Route element={<PrivateRoute />}>
                    <Route path="/"          element={<DomainSelectorPage />} />
                    <Route path="assessment" element={<AssessmentPage />} />
                    <Route path="domain/:id" element={<DomainHomePage />} />
                    <Route path="learn"      element={<LearningDashboardPage />} />
                    <Route path="review"     element={<ReviewView />} />
                    <Route path="*"          element={<Navigate to="/" replace />} />
                </Route>
            </Routes>
        </Container>
    );
}

export default App;

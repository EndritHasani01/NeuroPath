import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme';
import { BrowserRouter } from 'react-router-dom';
import { LearningProvider } from './contexts/LearningContext.jsx';
import {AuthProvider} from "./contexts/AuthContext.jsx"; // We'll create this

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <AuthProvider>
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <BrowserRouter>
                <LearningProvider> {/* Wrap App with LearningProvider */}
                    <App />
                </LearningProvider>
            </BrowserRouter>
        </ThemeProvider>
        </AuthProvider>
    </React.StrictMode>
);
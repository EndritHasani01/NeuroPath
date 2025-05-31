import React, { useEffect } from 'react';
import { useLearningContext } from '../contexts/LearningContext.jsx';
import { Box, Typography, Paper, LinearProgress } from '@mui/material';
import InsightView from '../components/InsightView.jsx';

function LearningDashboardPage() {
    const { state, fetchNextInsight, fetchTopicProgress } = useLearningContext();

    useEffect(() => {
        if (state.selectedDomain && state.learningPath && !state.topicProgress) {
            fetchTopicProgress(state.selectedDomain.id);
        }
    }, [state.selectedDomain, state.learningPath, state.topicProgress, fetchTopicProgress]);


    useEffect(() => {
        if (
            state.learningPath &&
            !state.currentInsight &&
            !state.topicProgress?.reviewAvailable &&
            !state.isLoading) {
            if(state.topicProgress &&
                state.topicProgress.completedInsightsCount < state.topicProgress.totalInsightsInLevel) {
                fetchNextInsight();
            }
        }
    }, [state.learningPath, state.currentInsight, state.topicProgress, state.reviewData, state.isLoading, fetchNextInsight]);



    if (!state.learningPath) {
        return <Typography>Loading learning path...</Typography>;
    }

    const { currentTopic, currentLevel, topicProgress } = state;

    return (
        <Paper elevation={3} sx={{ p: 3 }}>
            <Typography variant="h4" gutterBottom component="h2">
                Learning: {currentTopic || state.learningPath.topics[0]} - Level {currentLevel}
            </Typography>

            {topicProgress && (
                <Box sx={{ mb: 3 }}>
                    <Typography variant="body1">
                        Overall Topic Progress: {topicProgress.completedInsightsCount} / {topicProgress.totalInsightsInLevel} insights completed for this level.
                    </Typography>
                    <LinearProgress
                        variant="determinate"
                        value={(topicProgress.completedInsightsCount / (topicProgress.totalInsightsInLevel || 1)) * 100}
                        sx={{ height: 10, borderRadius: 5, mt: 1 }}
                    />
                    <Typography variant="caption" display="block" gutterBottom sx={{mt: 0.5}}>
                        Total insights available for this topic: {topicProgress.totalGeneratedInsightsForTopic}
                    </Typography>
                </Box>
            )}

            {state.currentInsight && !state.reviewData && (
                <InsightView />
            )}

        </Paper>
    );
}

export default LearningDashboardPage;
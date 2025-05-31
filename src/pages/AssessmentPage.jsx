import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useLearningContext } from '../contexts/LearningContext.jsx';
import { Box, Typography, Button, RadioGroup, FormControlLabel, Radio, FormControl, FormLabel, Paper, CircularProgress } from '@mui/material';

function AssessmentPage() {
    const { state, updateAssessmentAnswer, submitAssessment } = useLearningContext();
    const navigate = useNavigate();

    if (!state.selectedDomain) {
        navigate('/'); // Should not happen if routing is correct
        return null;
    }

    const handleAnswerChange = (questionId, answer) => {
        updateAssessmentAnswer(questionId, answer);
    };

    const handleSubmit = async () => {
        await submitAssessment();
        navigate(`/domain/${state.selectedDomain.id}`);
    };

    if (state.isLoading && state.assessmentQuestions.length === 0) {
        return <Box sx={{display: 'flex', justifyContent: 'center'}}><CircularProgress /></Box>;
    }
    if (state.assessmentQuestions.length === 0 && !state.isLoading) {
        return <Typography>No assessment questions found for this domain.</Typography>
    }

    return (
        <Paper elevation={3} sx={{ p: 3 }}>
            <Typography variant="h5" gutterBottom>
                Assessment for: {state.selectedDomain.name}
            </Typography>
            <Typography variant="body1" gutterBottom sx={{mb: 2}}>
                Please answer these questions to help us tailor your learning path.
            </Typography>
            {state.assessmentQuestions.map((q) => (
                <FormControl component="fieldset" key={q.id} sx={{ mb: 3, width: '100%' }}>
                    <FormLabel component="legend">{q.questionText}</FormLabel>
                    <RadioGroup
                        aria-label={q.questionText}
                        name={`question-${q.id}`}
                        value={state.assessmentAnswers[q.id] || ''}
                        onChange={(e) => handleAnswerChange(q.id, e.target.value)}
                    >
                        {q.options.map((option, index) => (
                            <FormControlLabel key={index} value={option} control={<Radio />} label={option} />
                        ))}
                    </RadioGroup>
                </FormControl>
            ))}
            <Button
                variant="contained"
                color="primary"
                onClick={handleSubmit}
                disabled={state.isLoading || Object.keys(state.assessmentAnswers).length < state.assessmentQuestions.length}
            >
                {state.isLoading ? <CircularProgress size={24} /> : "Submit Assessment & Get Learning Path"}
            </Button>
        </Paper>
    );
}

export default AssessmentPage;
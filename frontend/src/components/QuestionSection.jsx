import React, { useState, useEffect } from 'react';
import { Box, Alert, Button, CircularProgress } from '@mui/material';
import QuestionCard from './QuestionCard.jsx';
import { useLearningContext } from '../contexts/LearningContext.jsx';

function QuestionSection({ questions, onFinish, forReview = false, finishLabel = 'Finish' }) {
    const { state, submitQuestionAnswer, handleNextQuestionOrInsight, nextReviewQuestion } = useLearningContext();
    const { currentQuestionIndex, feedback, isLoading } = state;

    const [hasSubmitted, setHasSubmitted] = useState(false);

    const currentQuestion = questions[currentQuestionIndex];
    const isLast = currentQuestionIndex === questions.length - 1 && currentQuestion != null;
    const isAnswered =
        feedback?.questionId === currentQuestion.id && hasSubmitted;

    // Clear “submitted” flag whenever we move to a different question
    useEffect(() => {
        setHasSubmitted(false);
    }, [currentQuestionIndex]);

    const handleSubmit = (selectedAnswer, timeTakenMs) => {
        setHasSubmitted(true);
        submitQuestionAnswer(
            currentQuestion.id,
            selectedAnswer,
            timeTakenMs,
            forReview
        );
    };

    // Wrap the context helper so we can optionally call onFinish
    const handleAdvance = () => {
        if (isLast) {
            onFinish();
            return;
        }
        if (forReview) {
            nextReviewQuestion();
        } else {
            handleNextQuestionOrInsight();
        }

    };

    return (
        <Box>
            <QuestionCard
                question={currentQuestion}
                onSubmit={handleSubmit}
                showSubmit={!hasSubmitted}
            />

            {feedback && feedback.questionId === currentQuestion.id && (
                <Alert
                    severity={feedback.correct ? 'success' : 'error'}
                    sx={{ mt: 2 }}
                >
                    {feedback.feedback}
                </Alert>
            )}

            {isAnswered && (
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                    <Button
                        variant="contained"
                        onClick={handleAdvance}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <CircularProgress size={24} />
                        ) : (
                            isLast ? finishLabel : 'Next Question'
                        )}
                    </Button>
                </Box>
            )}
        </Box>
    );
}

export default QuestionSection;

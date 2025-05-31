import React, { useState } from 'react';
import { Box, RadioGroup, FormControlLabel, Radio, FormControl, FormLabel, Button } from '@mui/material';

function QuestionCard({ question, onSubmit, showSubmit = true }) {
    const [selectedAnswer, setSelectedAnswer] = useState('');
    const [startTime] = useState(Date.now());

    // For true/false questions without provided options, default to ['True','False']
    const options = Array.isArray(question.options) && question.options.length > 0
        ? question.options
        : ['True', 'False'];

    const handleAnswerChange = (event) => {
        setSelectedAnswer(event.target.value);
    };

    const handleSubmit = () => {
        if (!selectedAnswer) return;
        const timeTakenMs = Date.now() - startTime;
        onSubmit(selectedAnswer, timeTakenMs);
    };

    return (
        <Box>
            <FormControl component="fieldset" fullWidth>
                <FormLabel component="legend">{question.questionText}</FormLabel>
                <RadioGroup value={selectedAnswer} onChange={handleAnswerChange}>
                    {options.map((option) => (
                        <FormControlLabel key={option} value={option} control={<Radio />} label={option} />
                    ))}
                </RadioGroup>
            </FormControl>
            {showSubmit && (
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                    <Button
                        variant="contained"
                        onClick={handleSubmit}
                        disabled={!selectedAnswer}
                    >
                        Submit Answer
                    </Button>
                </Box>
            )}
        </Box>
    );
}

export default QuestionCard;
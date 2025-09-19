import React from 'react';
import { useLearningContext } from '../contexts/LearningContext.jsx';
import { Paper, Typography, List, ListItem, ListItemText, Divider } from '@mui/material';
import QuestionSection from './QuestionSection.jsx';
import {useNavigate} from "react-router-dom";

function ReviewView() {
    const { state, completeReview } = useLearningContext();
    const { reviewData } = state;
    const navigate = useNavigate();

    if (!reviewData) {
        return <Typography sx={{ mt: 2 }}>Loading review data...</Typography>;
    }

    const handleFinishReview = async () => {
        await completeReview(true);
        navigate(`/domain/${state.selectedDomain.id}`);   // NEW – preserve id
    };
    return (
        <Paper sx={{ p: 2 }}>
            <Typography variant="h6">Review Summary</Typography>
            <Typography sx={{ mt: 1 }}>{reviewData.summary}</Typography>

            <Typography variant="subtitle1" sx={{ mt: 2 }}>Strengths:</Typography>
            <List>
                {reviewData.strengths.map((s, i) => (
                    <ListItem key={i} disablePadding>
                        <ListItemText primary={s} />
                    </ListItem>
                ))}
            </List>

            <Typography variant="subtitle1" sx={{ mt: 2 }}>Weaknesses:</Typography>
            <List>
                {reviewData.weaknesses.map((w, i) => (
                    <ListItem key={i} disablePadding>
                        <ListItemText primary={w} />
                    </ListItem>
                ))}
            </List>

            <Divider sx={{ my: 2 }} />
            <QuestionSection
                questions={reviewData.revisionQuestions}
                forReview={true}
                onFinish={handleFinishReview}
                finishLabel="Complete Review"
            />
        </Paper>
    );
}

export default ReviewView;

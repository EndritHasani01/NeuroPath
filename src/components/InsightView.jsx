import React from 'react';
import { useLearningContext } from '../contexts/LearningContext.jsx';
import { Card, CardContent, Typography, Divider } from '@mui/material';
import QuestionSection from './QuestionSection.jsx';
import {useNavigate} from "react-router-dom";

function InsightView() {
    const { state, dispatch } = useLearningContext();
    const { currentInsight } = state;
    const navigate = useNavigate();

    if (!currentInsight) {
        return <Typography sx={{ mt: 2 }}>Loading next insight or all insights completed for now...</Typography>;
    }
    const handleFinishInsight = async () => {
        dispatch({ type: 'INSIGHT_COMPLETED' });
        navigate(`/domain/${state.selectedDomain.id}`);
    };

    return (
        <Card>
            <CardContent>
                <Typography variant="h5">{currentInsight.title}</Typography>
                <Typography variant="body1" sx={{ mt: 1 }}>{currentInsight.explanation}</Typography>
                <Divider sx={{ my: 2 }} />
                <QuestionSection
                    questions={currentInsight.questions}
                    forReview={false}
                    onFinish={handleFinishInsight}
                    finishLabel="Finish Insight"
                />
            </CardContent>
        </Card>
    );
}

export default InsightView;

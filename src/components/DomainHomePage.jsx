import React, { useEffect } from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import { useLearningContext } from '../contexts/LearningContext.jsx';
import {Box, Card, CardContent, Typography, Button, LinearProgress, Stack, CircularProgress} from '@mui/material';

function TopicCard({ topic, idx, onLearn, onReview }){
    const pct = Math.min(100, (topic.completedInsights/ topic.requiredInsights)*100);
    return (
        <Card sx={{ mb:2, opacity: topic.unlocked?1:0.4 }}>
            <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Box>
                        <Typography variant="h6">{idx+1}. {topic.topicName}</Typography>
                        <Typography variant="body2">Level {topic.level}</Typography>
                        <LinearProgress variant="determinate" value={pct} sx={{ mt:1, width:200 }} />
                    </Box>
                    <Stack direction="row" spacing={1}>
                        <Button variant="contained" disabled={topic.reviewAvailable || (!topic.current && !topic.unlocked)}
                                onClick={()=>onLearn(idx)}>
                            Learn next insight
                        </Button>
                        <Button variant="outlined" disabled={!topic.reviewAvailable}
                                onClick={()=>onReview(idx)}>
                            Review
                        </Button>
                    </Stack>
                </Stack>
            </CardContent>
        </Card>
    );
}

export default function DomainHomePage(){
    const { domainId: paramDomainId } = useParams();
    const nav = useNavigate();
    const { state, fetchOverview, pickTopic, fetchNextInsight, fetchReview } = useLearningContext();
    const domainId = Number(paramDomainId ?? state.selectedDomain?.id);

    useEffect(()=>{
        if (domainId) fetchOverview(domainId);
    }, [domainId, fetchOverview]);

    if (state.error) return (
        <Typography sx={{ p: 4 }} color="error">{state.error}</Typography>
    );

    if (state.isLoading || !state.overview) return (
        <Box sx={{ p: 4 }}><CircularProgress /></Box>
    );
    console.log(state.overview);
    const handleLearn = async (idx) => {
        if(idx !== state.currentTopicIndex) {
            await pickTopic(domainId, idx);
            await fetchOverview(domainId);
        }
        await fetchNextInsight(domainId);
        nav('/learn');
    };

    const handleReview = async (idx) => {
        if(idx !== state.currentTopicIndex) {
            await pickTopic(domainId, idx);
            await fetchOverview(domainId);
        }
        await fetchReview(domainId);
        nav('/review');
    };

    return (
        <Box sx={{ p:3 }}>
            <Typography variant="h4" gutterBottom>{state.overview.domainName}</Typography>
            {(state.overview.topics ?? []).map((t, i)=>(
                <TopicCard key={i} idx={i} topic={t} onLearn={handleLearn} onReview={handleReview} />
            ))}
        </Box>
    );
}
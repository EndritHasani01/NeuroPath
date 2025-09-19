import React, {createContext, useContext, useReducer, useCallback} from 'react';
import * as api from '../services/api';

const LearningContext = createContext();

const initialState = {
    domains: [],
    overview: null,
    selectedDomain: null, // { id, name, description }
    assessmentQuestions: [],
    assessmentAnswers: {}, // { questionId: answer }
    learningPath: null, // { domainName, topics: [] }
    currentTopicIndex: 0,
    currentTopic: null, // From learningPath.topics[currentTopicIndex]
    currentLevel: 1, // Default level for a new topic
    currentInsight: null, // { id, title, explanation, questions: [] }
    currentQuestionIndex: 0,
    userAnswersForInsight: {}, // { questionId: userAnswer }
    feedback: null, // { questionId, selectedAnswer, correct, correctAnswer, feedback }
    topicProgress: null, // { topicName, level, completedInsightsCount, totalInsightsInLevel, reviewAvailable }
    reviewData: null, // { summary, strengths, weaknesses, revisionQuestions }
    isLoading: false,
    error: null,
    insightsCompletedThisLevel: 0,
    insightsRequiredForReview: 6, // Default, should match backend
    profileVersion: 0,
};

function learningReducer(state, action) {
    switch (action.type) {
        case 'SET_LOADING':
            return { ...state, isLoading: action.payload, error: null };
        case 'SET_ERROR':
            return { ...state, isLoading: false, error: action.payload };
        case 'SET_DOMAINS':
            return { ...state, domains: action.payload, isLoading: false };
        case 'SELECT_DOMAIN':
            return {
                ...state,
                selectedDomain: action.payload,
                assessmentQuestions: [],
                assessmentAnswers: {},
                learningPath: null,
                currentTopic: null,
                currentInsight: null,
                topicProgress: null,
                reviewData: null,
                insightsCompletedThisLevel: 0,
                currentLevel: 1,
            };
        case 'SET_ASSESSMENT_QUESTIONS':
            return { ...state, assessmentQuestions: action.payload, isLoading: false };
        case 'UPDATE_ASSESSMENT_ANSWER':
            return {
                ...state,
                assessmentAnswers: {
                    ...state.assessmentAnswers,
                    [action.payload.questionId]: action.payload.answer,
                },
            };
        case 'SET_LEARNING_PATH':
            { const path = action.payload;
            const initialTopic = path && path.topics && path.topics.length > 0 ? path.topics[0] : null;
            return {
                ...state,
                learningPath: path,
                currentTopicIndex: 0,
                currentTopic: initialTopic,
                currentLevel: 1,
                isLoading: false,
                insightsCompletedThisLevel: 0,
            }; }
        case 'SET_CURRENT_INSIGHT':
            return {
                ...state,
                currentInsight: action.payload,
                currentQuestionIndex: 0,
                userAnswersForInsight: {},
                feedback: null,
                isLoading: false,
            };
        case 'CLEAR_CURRENT_INSIGHT':
            return { ...state, currentInsight: null, feedback: null };
        case 'SET_FEEDBACK':
            return { ...state, feedback: action.payload, isLoading: false };
        case 'UPDATE_USER_ANSWER':
            return {
                ...state,
                userAnswersForInsight: {
                    ...state.userAnswersForInsight,
                    [action.payload.questionId]: action.payload.answer,
                },
            };
        case 'INCREMENT_QUESTION_INDEX':
            return { ...state, currentQuestionIndex: state.currentQuestionIndex + 1, feedback: null };
        case 'INSIGHT_COMPLETED':
            return {
                ...state,
                currentInsight: null,
                profileVersion: state.profileVersion + 1
            };
        case 'SET_TOPIC_PROGRESS':
            return {
                ...state,
                topicProgress: action.payload,
                insightsRequiredForReview: action.payload?.totalInsightsInLevel || state.insightsRequiredForReview,
                isLoading: false,
            };
        case 'SET_REVIEW_DATA':
            return {
                ...state,
                reviewData: action.payload,
                currentQuestionIndex: 0,
                feedback: null,
                isLoading: false,
            };
        case 'ADVANCE_LEVEL':
            return {
                ...state,
                currentLevel: state.currentLevel + 1,
                currentInsight: null,
                reviewData: null,
                topicProgress: null,
                insightsCompletedThisLevel: 0,
            };
        case 'ADVANCE_TOPIC':
            { const nextTopicIndex = state.currentTopicIndex + 1;
            if (state.learningPath && nextTopicIndex < state.learningPath.topics.length) {
                return {
                    ...state,
                    currentTopicIndex: nextTopicIndex,
                    currentTopic: state.learningPath.topics[nextTopicIndex],
                    currentLevel: 1,
                    currentInsight: null,
                    reviewData: null,
                    topicProgress: null,
                    insightsCompletedThisLevel: 0,
                };
            }
            return { ...state, isLoading: false }; }
        case 'SET_OVERVIEW':
            return {
                ...state,
                overview: action.payload,
                topicProgress: null,
                reviewData: null,
                isLoading: false };
        case 'SET_CURRENT_TOPIC_IDX':
            { const nextTopic = state.learningPath?.topics[action.payload] ?? null;
            return {
                ...state,
                currentTopicIndex: action.payload,
                currentTopic: nextTopic,
                currentLevel: 1,
                insightsCompletedThisLevel: 0,
                currentInsight: null,
            }; }
        case 'RESET_ALL':
            return { ...initialState };
        case 'PROFILE_TICK':
            return { ...state, profileVersion: state.profileVersion + 1 };
        default:
            return state;
    }
}
export const INSIGHTS_PER_LEVEL = 6;

export const LearningProvider = ({ children }) => {
    const [state, dispatch] = useReducer(learningReducer, initialState);

    const fetchDomains = useCallback(async () => {
        dispatch({ type: 'SET_LOADING', payload: true });
        try {
            const response = await api.getDomainsWithStatus();
            dispatch({ type: 'SET_DOMAINS', payload: response.data });
        } catch (error) {
            dispatch({ type: 'SET_ERROR', payload: error.response?.data?.message || error.message });
        }
    }, []);

    const logout = () => {
        localStorage.removeItem('token');
        sessionStorage.removeItem('token');

        dispatch({ type: 'RESET_ALL' });
    };

    const fetchOverview = useCallback(async (domainId)=>{
        dispatch({type:'SET_LOADING', payload:true});
        try{
            const response  = await api.getDomainOverview(domainId);
            const overviewData = response.data;

            dispatch({type:'SET_OVERVIEW', payload:overviewData});

            const topics = overviewData.topics.map(t => t.topicName);

            dispatch({
                type: 'SET_LEARNING_PATH',
                payload: {
                    domainName: overviewData.domainName,
                    topics,
                },
            });

            const currentIdx = overviewData.topics.findIndex(t => t.current);
            if (currentIdx >= 0) {
                dispatch({ type: 'SET_CURRENT_TOPIC_IDX', payload: currentIdx });
            }
        } catch (error) {
            dispatch({
                type: 'SET_ERROR',
                payload: error.response?.data?.message || error.message,
            });
        }
    },[]);

    const pickTopic = useCallback(async (domainId, idx)=>{
        await api.selectTopic(domainId, idx);
        dispatch({type:'SET_CURRENT_TOPIC_IDX', payload:idx});
        await fetchOverview(domainId);
    },[fetchOverview]);

    const selectDomain = useCallback(async (domain) => {
        dispatch({ type: 'SELECT_DOMAIN', payload: domain });
        dispatch({ type: 'SET_LOADING', payload: true });
        try {
            const response = await api.getAssessmentQuestions(domain.id);
            dispatch({ type: 'SET_ASSESSMENT_QUESTIONS', payload: response.data });
        } catch (error) {
            dispatch({ type: 'SET_ERROR', payload: error.response?.data?.message || error.message });
        }
    }, []);

    const updateAssessmentAnswer = useCallback((questionId, answer) => {
        dispatch({ type: 'UPDATE_ASSESSMENT_ANSWER', payload: { questionId, answer } });
    }, []);

    const submitAssessment = useCallback(async () => {
        if (!state.selectedDomain) return;
        dispatch({ type: 'SET_LOADING', payload: true });
        const submission = {
            domainId: state.selectedDomain.id,
            answers: state.assessmentAnswers,
        };
        try {
            const response = await api.startDomainAndGetLearningPath(submission);
            dispatch({ type: 'SET_LEARNING_PATH', payload: response.data });
            dispatch({ type: 'PROFILE_TICK' });
            if (response.data && response.data.topics && response.data.topics.length > 0) {
                await fetchTopicProgress(state.selectedDomain.id);
            }
        } catch (error) {
            dispatch({ type: 'SET_ERROR', payload: error.response?.data?.message || error.message });
        }
    }, [state.selectedDomain, state.assessmentAnswers]);

    const fetchNextInsight = useCallback(async () => {
        if (!state.selectedDomain) return;
        dispatch({ type: 'SET_LOADING', payload: true });
        try {
            const response = await api.getNextInsight(state.selectedDomain.id);
            if (response.status === 204 || !response.data) {
                dispatch({ type: 'CLEAR_CURRENT_INSIGHT' });
                await fetchTopicProgress(state.selectedDomain.id);
            } else {
                dispatch({ type: 'SET_CURRENT_INSIGHT', payload: response.data });
            }
        } catch (error) {
            dispatch({ type: 'SET_ERROR', payload: error.response?.data?.message || error.message });
        }
    }, [state.selectedDomain]);

    const submitQuestionAnswer = useCallback(async (questionId, selectedAnswer, timeTakenMs = 0, forReview = false) => {
        dispatch({ type: 'SET_LOADING', payload: true });
        try {
            const response = await api.submitAnswer({ questionId, selectedAnswer, timeTakenMs, forReview });
            dispatch({ type: 'SET_FEEDBACK', payload: response.data });
            if (state.selectedDomain) {
                await fetchTopicProgress(state.selectedDomain.id);
            }
        } catch (error) {
            dispatch({ type: 'SET_ERROR', payload: error.response?.data?.message || error.message });
        }
    }, [state.selectedDomain]);

    const nextReviewQuestion = useCallback(() => {
        if (!state.reviewData) return;               // guard: only in review mode

        const questions = state.reviewData.revisionQuestions || [];
        if (state.currentQuestionIndex < questions.length - 1) {
            dispatch({ type: 'INCREMENT_QUESTION_INDEX' });
        }
    }, [state.reviewData, state.currentQuestionIndex]);

    const handleNextQuestionOrInsight = useCallback(() => {
        if (!state.currentInsight) return;
        dispatch({ type: 'SET_LOADING', payload: true });
        const { questions } = state.currentInsight;
        if (state.currentQuestionIndex < questions.length - 1) {
            dispatch({ type: 'INCREMENT_QUESTION_INDEX' });
        } else {
            dispatch({ type: 'INSIGHT_COMPLETED' });
        }
        dispatch({ type: 'SET_LOADING', payload: false });
    }, [
        state.currentInsight,
        state.currentQuestionIndex,
    ]);

    const fetchTopicProgress = useCallback(async (domainId) => {
        if (!domainId && state.selectedDomain) domainId = state.selectedDomain.id;
        if (!domainId) return;

        dispatch({ type: 'SET_LOADING', payload: true });
        try {
            const response = await api.getTopicProgress(domainId);
            dispatch({ type: 'SET_TOPIC_PROGRESS', payload: response.data });
        } catch (error) {
            dispatch({ type: 'SET_ERROR', payload: error.response?.data?.message || error.message });
        }
    }, [state.selectedDomain]);

    const fetchReview = useCallback(async () => {
        if (!state.selectedDomain) return;
        dispatch({ type: 'SET_LOADING', payload: true });
        try {
            const response = await api.getReview(state.selectedDomain.id);
            dispatch({ type: 'SET_REVIEW_DATA', payload: response.data });
        } catch (error) {
            dispatch({ type: 'SET_ERROR', payload: error.response?.data?.message || error.message });
        }
    }, [state.selectedDomain]);

    const completeReview = useCallback(async (satisfactoryPerformance) => {
        if (!state.selectedDomain) return;
        dispatch({ type: 'SET_LOADING', payload: true });
        try {
            await api.completeReviewAndAdvance(state.selectedDomain.id, satisfactoryPerformance);
            if (satisfactoryPerformance) {
                dispatch({ type: 'ADVANCE_LEVEL' });
            }
            await fetchTopicProgress(state.selectedDomain.id);
            await fetchOverview(state.selectedDomain.id);
            dispatch({ type: 'SET_REVIEW_DATA', payload: null });
        } catch (error) {
            dispatch({ type: 'SET_ERROR', payload: error.response?.data?.message || error.message });
        }
    }, [state.selectedDomain, fetchOverview]);




    return (
        <LearningContext.Provider
            value={{
                state,
                dispatch,
                fetchDomains,
                selectDomain,
                updateAssessmentAnswer,
                submitAssessment,
                fetchNextInsight,
                submitQuestionAnswer,
                handleNextQuestionOrInsight,
                nextReviewQuestion,
                fetchTopicProgress,
                fetchReview,
                completeReview,
                fetchOverview,
                pickTopic,
                logout
            }}
        >
            {children}
        </LearningContext.Provider>
    );
};

export const useLearningContext = () => useContext(LearningContext);
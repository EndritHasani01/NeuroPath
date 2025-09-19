import axios from 'axios';

const rawBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api';
const baseURL = rawBaseUrl.endsWith('/') && rawBaseUrl !== '/' ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

const apiClient = axios.create({
    baseURL,
    headers: {
        'Content-Type': 'application/json',
    },
});

apiClient.interceptors.request.use(cfg=>{
    const t = localStorage.getItem('token');
    if(t) cfg.headers.Authorization = `Bearer ${t}`;
    return cfg;
});
apiClient.interceptors.response.use(
    res=>res,
    err=> { if(err.response?.status===401){
        localStorage.removeItem("token");
        window.location='/login';
    }
        return Promise.reject(err);
    }
);




// Learning Service Calls
export const getDomains = () => apiClient.get('/learning/domains');
export const getDomainsWithStatus = () => apiClient.get('/learning/domains/status');
export const getAssessmentQuestions = (domainId) => apiClient.get(`/learning/domains/${domainId}/assessment-questions`);
export const startDomainAndGetLearningPath = (submission) => apiClient.post('/learning/domains/start', submission);
export const getNextInsight = (domainId) => apiClient.get(`/learning/domains/${domainId}/next-insight`);
export const submitAnswer = (answerSubmission) => apiClient.post('/learning/insights/submit-answer', answerSubmission);
export const getTopicProgress = (domainId) => apiClient.get(`/learning/domains/${domainId}/progress`);
export const getReview = (domainId) => apiClient.get(`/learning/domains/${domainId}/review`);
export const completeReviewAndAdvance = (domainId, satisfactoryPerformance) =>
    apiClient.post(`/learning/domains/${domainId}/complete-review?satisfactoryPerformance=${satisfactoryPerformance}`);
export const getDomainOverview = (domainId) =>
    apiClient.get(`/learning/domains/${domainId}/overview`);
export const selectTopic = (domainId, topicIdx) =>
    apiClient.post(`/learning/domains/${domainId}/select-topic/${topicIdx}`);
export const getProfile = () => apiClient.get('/auth/me');


export default apiClient;

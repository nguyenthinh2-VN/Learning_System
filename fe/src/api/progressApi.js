import api from './auth';

const progressApi = {
    /**
     * Get user's progress data including overview, heatmap, and ongoing courses.
     * Requires authentication.
     */
    getProgress: () => {
        return api.get('/progress');
    },
};

export default progressApi;

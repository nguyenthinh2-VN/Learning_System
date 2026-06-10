import api from './auth';

export const createSessionApi = () => {
  return api.post('/chatbot/sessions');
};

export const getMessagesApi = (sessionId) => {
  return api.get(`/chatbot/sessions/${sessionId}/messages`);
};

export const sendMessageApi = (sessionId, content) => {
  return api.post(`/chatbot/sessions/${sessionId}/messages`, {
    content,
  });
};

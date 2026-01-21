import axios from "axios";

const api = axios.create({
    baseURL: "/slib/ai",
    headers: {
        "Content-Type": "application/json",
    },
});

// Add Authorization header if token exists
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('librarian_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    console.log("[AI API][Request]", config.method?.toUpperCase(), config.baseURL + config.url);
    return config;
});

api.interceptors.response.use(
    (res) => {
        console.log("[AI API][Response]", res.config?.url, res.status);
        return res;
    },
    (err) => {
        console.error("[AI API][Error]", err.config?.url, err.response?.status, err.response?.data);
        return Promise.reject(err);
    }
);

/* ========================== AI CONFIG ========================= */

/**
 * GET AI CONFIG
 */
export const getAIConfig = () => api.get("/admin/config");

/**
 * SAVE AI CONFIG
 */
export const saveAIConfig = (payload) => api.post("/admin/config", payload);

/**
 * TEST GEMINI API CONNECTION
 */
export const testAPIConnection = () => api.post("/admin/test-api");

/* ========================== KNOWLEDGE BASE ========================= */

/**
 * GET ALL KNOWLEDGE
 */
export const getKnowledge = () => api.get("/admin/knowledge");

/**
 * CREATE KNOWLEDGE
 */
export const createKnowledge = (payload) => api.post("/admin/knowledge", payload);

/**
 * UPDATE KNOWLEDGE
 */
export const updateKnowledge = (id, payload) => api.put(`/admin/knowledge/${id}`, payload);

/**
 * DELETE KNOWLEDGE
 */
export const deleteKnowledge = (id) => api.delete(`/admin/knowledge/${id}`);

/* ========================== PROMPT TEMPLATES ========================= */

/**
 * GET ALL PROMPTS
 */
export const getPrompts = () => api.get("/admin/prompts");

/**
 * CREATE PROMPT
 */
export const createPrompt = (payload) => api.post("/admin/prompts", payload);

/**
 * UPDATE PROMPT
 */
export const updatePrompt = (id, payload) => api.put(`/admin/prompts/${id}`, payload);

/**
 * DELETE PROMPT
 */
export const deletePrompt = (id) => api.delete(`/admin/prompts/${id}`);

/* ========================== CHAT (Admin) ========================= */

/**
 * GET ESCALATED SESSIONS (need librarian intervention)
 */
export const getEscalatedSessions = () => api.get("/admin/escalated");

/**
 * GET SESSION DETAIL
 */
export const getSessionDetail = (sessionId) => api.get(`/admin/session/${sessionId}`);

/**
 * LIBRARIAN REPLY
 */
export const librarianReply = (sessionId, content) =>
    api.post(`/admin/reply/${sessionId}`, { content });

/**
 * RESOLVE ESCALATED SESSION
 */
export const resolveSession = (sessionId) => api.put(`/admin/resolve/${sessionId}`);

/* ========================== CHAT (Test) ========================= */

/**
 * SEND TEST MESSAGE (for admin testing)
 * Note: This requires authentication token
 */
export const sendTestMessage = (message, sessionId = null) =>
    api.post("/chat/message", { message, sessionId });

export default api;

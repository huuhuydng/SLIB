import axios from "axios";

/**
 * Python AI Service API Client
 * Connects to the FastAPI AI service on port 8001
 */
const pythonAiApi = axios.create({
    baseURL: "http://localhost:8001/api/ai",
    headers: {
        "Content-Type": "application/json",
    },
});

// Request interceptor
pythonAiApi.interceptors.request.use((config) => {
    console.log("[Python AI API][Request]", config.method?.toUpperCase(), config.baseURL + config.url);
    return config;
});

// Response interceptor
pythonAiApi.interceptors.response.use(
    (res) => {
        console.log("[Python AI API][Response]", res.config?.url, res.status);
        return res;
    },
    (err) => {
        console.error("[Python AI API][Error]", err.config?.url, err.response?.status, err.response?.data);
        return Promise.reject(err);
    }
);

/* ========================== AI CONFIG ========================= */

/**
 * GET AI CONFIG
 */
export const getAIConfig = () => pythonAiApi.get("/config");

/**
 * TEST GEMINI API CONNECTION
 */
export const testAPIConnection = () => pythonAiApi.post("/test-connection");

/* ========================== CHAT ========================= */

/**
 * SIMPLE CHAT
 * @param {string} message - User message
 * @param {string} sessionId - Optional session ID
 */
export const sendChat = (message, sessionId = null) =>
    pythonAiApi.post("/chat", { message, session_id: sessionId });

/**
 * GENERATE WITH CONFIG OVERRIDE
 * @param {string} userMessage 
 * @param {Array} chatHistory 
 * @param {Object} config - Optional config override
 */
export const generateResponse = (userMessage, chatHistory = [], config = null) =>
    pythonAiApi.post("/generate", {
        user_message: userMessage,
        chat_history: chatHistory,
        config
    });

/* ========================== KNOWLEDGE BASE ========================= */

/**
 * GET ALL KNOWLEDGE
 */
export const getKnowledge = () => pythonAiApi.get("/knowledge");

/**
 * ADD KNOWLEDGE
 * @param {string} title 
 * @param {string} content 
 * @param {string} type - INFO, RULES, GUIDE
 */
export const addKnowledge = (title, content, type = "INFO") =>
    pythonAiApi.post(`/knowledge?title=${encodeURIComponent(title)}&content=${encodeURIComponent(content)}&knowledge_type=${type}`);

/* ========================== HEALTH CHECK ========================= */

/**
 * HEALTH CHECK
 */
export const healthCheck = () => axios.get("http://localhost:8001/health");

/* ========================== PROMPTS ========================= */

/**
 * GET ALL PROMPTS
 */
export const getPrompts = () => pythonAiApi.get("/prompts");

/**
 * CREATE PROMPT
 */
export const createPrompt = (data) =>
    pythonAiApi.post(`/prompts?name=${encodeURIComponent(data.name)}&prompt=${encodeURIComponent(data.prompt)}&context=${data.context || 'GENERAL'}`);

/**
 * UPDATE PROMPT (Stub - not implemented in Python service)
 */
export const updatePrompt = (id, data) => Promise.resolve({ data: { ...data, id } });

/**
 * DELETE PROMPT (Stub)
 */
export const deletePrompt = (id) => Promise.resolve({ data: { success: true } });

/* ========================== KNOWLEDGE CRUD (Extended) ========================= */

/**
 * UPDATE KNOWLEDGE (Stub)
 */
export const updateKnowledge = (id, data) => Promise.resolve({ data: { ...data, id } });

/**
 * DELETE KNOWLEDGE (Stub)
 */
export const deleteKnowledge = (id) => Promise.resolve({ data: { success: true } });

/* ========================== CONFIG (Stub for save) ========================= */

/**
 * SAVE AI CONFIG
 * Note: Python service reads config from ENV, this is a stub
 */
export const saveAIConfig = (payload) => {
    console.log("[Python AI API] Config save requested (config is managed via ENV):", payload);
    return Promise.resolve({
        data: {
            success: true,
            message: "Config lưu thành công (trong session). Để thay đổi vĩnh viễn, cập nhật file .env",
            config: payload
        }
    });
};

/* ========================== TEST CHAT ========================= */

/**
 * SEND TEST MESSAGE (for admin testing)
 */
export const sendTestMessage = (message, sessionId = null) =>
    pythonAiApi.post("/chat", { message, session_id: sessionId }).then(res => ({
        data: {
            success: true,
            reply: res.data.reply,
            sessionId: res.data.session_id,
            needsLibrarian: res.data.needs_review,
            confidence: res.data.confidence_score
        }
    }));

export default {
    getAIConfig,
    saveAIConfig,
    testAPIConnection,
    sendChat,
    generateResponse,
    getKnowledge,
    addKnowledge,
    updateKnowledge,
    deleteKnowledge,
    getPrompts,
    createPrompt,
    updatePrompt,
    deletePrompt,
    sendTestMessage,
    healthCheck
};

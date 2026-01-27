import axios from "axios";

/**
 * Python AI Service API Client (RAG Mode)
 * Connects to the FastAPI AI service on port 8001
 * 
 * Endpoints:
 * - /api/ai/* - Legacy endpoints
 * - /api/v1/chat/* - RAG Chat
 * - /api/v1/ingest/* - Document Ingestion
 */
const pythonAiApi = axios.create({
    baseURL: "http://localhost:8001",
    headers: {
        "Content-Type": "application/json",
    },
});

// File upload API (multipart/form-data)
const uploadApi = axios.create({
    baseURL: "http://localhost:8001",
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

/* ========================== HEALTH CHECK ========================= */

/**
 * HEALTH CHECK
 */
export const healthCheck = () => axios.get("http://localhost:8001/health");

/* ========================== AI CONFIG ========================= */

/**
 * GET AI CONFIG (from Python service)
 */
export const getAIConfig = () => pythonAiApi.get("/api/ai/config");

/**
 * TEST API CONNECTION
 */
export const testAPIConnection = () => pythonAiApi.post("/api/ai/test-connection");

/**
 * REFRESH CONFIG
 */
export const refreshConfig = () => pythonAiApi.post("/api/ai/refresh");

/* ========================== RAG CHAT ========================= */

/**
 * RAG CHAT QUERY - Main chat endpoint with vector search
 * @param {string} message - User message
 * @param {string} sessionId - Optional session ID
 * @param {boolean} includeSources - Whether to include source references
 */
export const ragQuery = (message, sessionId = null, includeSources = true) =>
    pythonAiApi.post("/api/v1/chat/query", {
        message,
        session_id: sessionId,
        include_sources: includeSources
    });

/**
 * SIMPLE CHAT (Legacy compatible)
 */
export const sendChat = (message, sessionId = null) =>
    pythonAiApi.post("/api/ai/chat", { message, session_id: sessionId });

/**
 * GENERATE RESPONSE
 */
export const generateResponse = (userMessage, chatHistory = [], config = null) =>
    pythonAiApi.post("/api/ai/generate", {
        user_message: userMessage,
        chat_history: chatHistory,
        config
    });

/**
 * TEST RAG SERVICE
 */
export const testRAGService = () => pythonAiApi.get("/api/v1/chat/test");

/**
 * CLEAR CHAT SESSION
 */
export const clearChatSession = (sessionId) =>
    pythonAiApi.delete(`/api/v1/chat/session/${sessionId}`);

/* ========================== DOCUMENT INGESTION ========================= */

/**
 * UPLOAD DOCUMENT (PDF/DOCX)
 * @param {File} file - The file to upload
 * @param {string} category - Category for the document
 * @param {string} source - Optional custom source name
 */
export const uploadDocument = (file, category = "document", source = null) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("category", category);
    if (source) {
        formData.append("source", source);
    }
    return uploadApi.post("/api/v1/ingest/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" }
    });
};

/**
 * INGEST RAW TEXT
 * @param {Object} data - { content, source, category, metadata }
 */
export const ingestText = (data) =>
    pythonAiApi.post("/api/v1/ingest/text", data);

/**
 * GET KNOWLEDGE BASE STATS
 */
export const getKnowledgeStats = () => pythonAiApi.get("/api/v1/ingest/stats");

/**
 * DELETE SOURCE
 * @param {string} source - Source identifier to delete
 */
export const deleteSource = (source) =>
    pythonAiApi.delete(`/api/v1/ingest/source/${encodeURIComponent(source)}`);

/* ========================== LEGACY KNOWLEDGE ========================= */

/**
 * GET ALL KNOWLEDGE (Legacy)
 */
export const getKnowledge = () => pythonAiApi.get("/api/ai/knowledge");

/**
 * ADD KNOWLEDGE (Legacy)
 */
export const addKnowledge = (title, content, type = "INFO") =>
    pythonAiApi.post(`/api/ai/knowledge?title=${encodeURIComponent(title)}&content=${encodeURIComponent(content)}&knowledge_type=${type}`);

/* ========================== PROMPTS ========================= */

/**
 * GET ALL PROMPTS
 */
export const getPrompts = () => pythonAiApi.get("/api/ai/prompts");

/**
 * CREATE PROMPT
 */
export const createPrompt = (data) =>
    pythonAiApi.post(`/api/ai/prompts?name=${encodeURIComponent(data.name)}&prompt=${encodeURIComponent(data.prompt)}&context=${data.context || 'GENERAL'}`);

/* ========================== ANALYTICS ========================= */

/**
 * GET PEAK HOURS
 */
export const getPeakHours = (areaId = null) =>
    pythonAiApi.get("/api/ai/analytics/peak-hours", { params: { area_id: areaId } });

/**
 * GET TIME SLOT RECOMMENDATIONS
 */
export const getRecommendedSlots = (durationHours = 2) =>
    pythonAiApi.get("/api/ai/analytics/recommend-slots", { params: { duration_hours: durationHours } });

/**
 * GET USAGE STATISTICS
 */
export const getUsageStatistics = (period = "week", areaId = null) =>
    pythonAiApi.get("/api/ai/analytics/statistics", { params: { period, area_id: areaId } });

/* ========================== TEST CHAT ========================= */

/**
 * SEND TEST MESSAGE (for admin testing)
 * Uses RAG query with full response data
 */
export const sendTestMessage = (message, sessionId = null) =>
    ragQuery(message, sessionId, true).then(res => ({
        data: {
            success: res.data.success,
            reply: res.data.reply,
            sessionId: res.data.session_id,
            needsLibrarian: res.data.action === "ESCALATE_TO_LIBRARIAN",
            confidence: res.data.similarity_score,
            action: res.data.action,
            sources: res.data.sources || []
        }
    }));

/**
 * SAVE AI CONFIG (stub - config managed via ENV)
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

export default {
    // Health
    healthCheck,
    // Config
    getAIConfig,
    saveAIConfig,
    testAPIConnection,
    refreshConfig,
    // RAG Chat
    ragQuery,
    sendChat,
    generateResponse,
    testRAGService,
    clearChatSession,
    // Ingestion
    uploadDocument,
    ingestText,
    getKnowledgeStats,
    deleteSource,
    // Legacy Knowledge
    getKnowledge,
    addKnowledge,
    // Prompts
    getPrompts,
    createPrompt,
    // Analytics
    getPeakHours,
    getRecommendedSlots,
    getUsageStatistics,
    // Test
    sendTestMessage
};

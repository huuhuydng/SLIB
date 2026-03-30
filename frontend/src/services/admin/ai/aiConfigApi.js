import axios from "axios";
import { API_BASE_URL } from '../../../config/apiConfig';

/**
 * Java Backend API Client for AI Configuration
 * Connects to Spring Boot backend on port 8080 for persistent storage
 * 
 * Use this for: Config CRUD, Knowledge Base CRUD, Prompt Templates CRUD
 * Use pythonAiApi.js for: Chat, Test Connection, Health Check
 */
const aiConfigApi = axios.create({
    baseURL: `${API_BASE_URL}/slib/ai/admin`,
    headers: {
        "Content-Type": "application/json",
    },
});

// Request interceptor - add auth token
aiConfigApi.interceptors.request.use((config) => {
    const token = sessionStorage.getItem('librarian_token') || localStorage.getItem('librarian_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Response interceptor
aiConfigApi.interceptors.response.use(
    (res) => res,
    (err) => {
        console.error("[AI Config API][Error]", err.config?.url, err.response?.status, err.response?.data);
        return Promise.reject(err);
    }
);

/* ========================== AI CONFIG ========================= */

/**
 * GET AI CONFIG
 */
export const getAIConfig = () => aiConfigApi.get("/config");

/**
 * SAVE AI CONFIG
 * @param {Object} config - AI configuration object
 */
export const saveAIConfig = (config) => aiConfigApi.post("/config", config);

/**
 * RESET AI CONFIG TO DEFAULTS
 */
export const resetAIConfig = () => aiConfigApi.post("/config/reset");

/* ========================== KNOWLEDGE BASE ========================= */

/**
 * GET ALL KNOWLEDGE
 */
export const getKnowledge = () => aiConfigApi.get("/knowledge");

/**
 * GET SINGLE KNOWLEDGE BY ID
 */
export const getKnowledgeById = (id) => aiConfigApi.get(`/knowledge/${id}`);

/**
 * CREATE NEW KNOWLEDGE
 * @param {Object} knowledge - { title, content, type, isActive }
 */
export const createKnowledge = (knowledge) => aiConfigApi.post("/knowledge", knowledge);

/**
 * UPDATE KNOWLEDGE
 * @param {number} id - Knowledge ID
 * @param {Object} knowledge - Updated knowledge data
 */
export const updateKnowledge = (id, knowledge) => aiConfigApi.put(`/knowledge/${id}`, knowledge);

/**
 * DELETE KNOWLEDGE
 * @param {number} id - Knowledge ID to delete
 */
export const deleteKnowledge = (id) => aiConfigApi.delete(`/knowledge/${id}`);

/* ========================== PROMPT TEMPLATES ========================= */

/**
 * GET ALL PROMPTS
 */
export const getPrompts = () => aiConfigApi.get("/prompts");

/**
 * GET SINGLE PROMPT BY ID
 */
export const getPromptById = (id) => aiConfigApi.get(`/prompts/${id}`);

/**
 * CREATE NEW PROMPT
 * @param {Object} prompt - { name, prompt, context, isActive }
 */
export const createPrompt = (prompt) => aiConfigApi.post("/prompts", prompt);

/**
 * UPDATE PROMPT
 * @param {number} id - Prompt ID
 * @param {Object} prompt - Updated prompt data
 */
export const updatePrompt = (id, prompt) => aiConfigApi.put(`/prompts/${id}`, prompt);

/**
 * DELETE PROMPT
 * @param {number} id - Prompt ID to delete
 */
export const deletePrompt = (id) => aiConfigApi.delete(`/prompts/${id}`);

export default {
    // Config
    getAIConfig,
    saveAIConfig,
    resetAIConfig,
    // Knowledge
    getKnowledge,
    getKnowledgeById,
    createKnowledge,
    updateKnowledge,
    deleteKnowledge,
    // Prompts
    getPrompts,
    getPromptById,
    createPrompt,
    updatePrompt,
    deletePrompt,
};

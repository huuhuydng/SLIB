import axios from "axios";

const BASE_URL = "http://localhost:8080/slib/ai/admin";

const api = axios.create({
    baseURL: BASE_URL,
    headers: { "Content-Type": "application/json" }
});

// Request/Response interceptors
api.interceptors.request.use(config => {
    console.log("[Materials API]", config.method?.toUpperCase(), config.url);
    return config;
});

// ==================== MATERIALS ====================

export const getMaterials = () => api.get("/materials");
export const getMaterialById = (id) => api.get(`/materials/${id}`);
export const createMaterial = (data) => api.post("/materials", data);
export const updateMaterial = (id, data) => api.put(`/materials/${id}`, data);
export const deleteMaterial = (id) => api.delete(`/materials/${id}`);

// Material Items
export const getMaterialItems = (materialId) => api.get(`/materials/${materialId}/items`);

export const addTextItem = (materialId, data) =>
    api.post(`/materials/${materialId}/items/text`, data);

export const addFileItem = (materialId, file, name) => {
    const formData = new FormData();
    formData.append("file", file);
    if (name) formData.append("name", name);
    return axios.post(`${BASE_URL}/materials/${materialId}/items/file`, formData, {
        headers: { "Content-Type": "multipart/form-data" }
    });
};

export const deleteItem = (materialId, itemId) =>
    api.delete(`/materials/${materialId}/items/${itemId}`);

// ==================== KNOWLEDGE STORES ====================

export const getKnowledgeStores = () => api.get("/knowledge-stores");
export const getKnowledgeStoreById = (id) => api.get(`/knowledge-stores/${id}`);
export const createKnowledgeStore = (data) => api.post("/knowledge-stores", data);
export const updateKnowledgeStore = (id, data) => api.put(`/knowledge-stores/${id}`, data);
export const deleteKnowledgeStore = (id) => api.delete(`/knowledge-stores/${id}`);
export const syncKnowledgeStore = (id) => api.post(`/knowledge-stores/${id}/sync`);

export default {
    // Materials
    getMaterials,
    getMaterialById,
    createMaterial,
    updateMaterial,
    deleteMaterial,
    getMaterialItems,
    addTextItem,
    addFileItem,
    deleteItem,
    // Knowledge Stores
    getKnowledgeStores,
    getKnowledgeStoreById,
    createKnowledgeStore,
    updateKnowledgeStore,
    deleteKnowledgeStore,
    syncKnowledgeStore
};

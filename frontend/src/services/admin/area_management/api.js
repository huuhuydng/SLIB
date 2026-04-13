import axios from "axios";
import { API_BASE_URL } from "../../../config/apiConfig";
import { getStaffAuthToken } from "../../shared/staffAuth";

const api = axios.create({
  baseURL: `${API_BASE_URL}/slib`,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add Authorization header if token exists
api.interceptors.request.use((config) => {
  const isKioskRoute =
    typeof window !== "undefined" && window.location.pathname.startsWith("/kiosk");
  const token = isKioskRoute
    ? (
        localStorage.getItem("kiosk_device_token") ||
        sessionStorage.getItem("kiosk_device_token") ||
        getStaffAuthToken()
      )
    : getStaffAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    return Promise.reject(err);
  }
);

/* ========================== AREA ========================= */

/**
 * GET ALL AREAS
 */
export const getAreas = () =>
  api.get("/areas");

/**
 * GET AREA BY ID
 */
export const getAreaById = (areaId) =>
  api.get(`/areas/${areaId}`);

/**
 * CREATE AREA
 */
export const createArea = (payload) =>
  api.post("/areas", payload);

/**
 * UPDATE AREA
 */
export const updateArea = (areaId, payload) =>
  api.put(`/areas/${areaId}`, payload);

/**
 * UPDATE AREA POSITION
 * Backend: PUT /slib/areas/{id}/position
 */
export const updateAreaPosition = (areaId, x, y) =>
  api.put(`/areas/${areaId}/position`, {
    positionX: x,
    positionY: y,
  });

/**
 * UPDATE AREA DIMENSIONS
 * Backend: PUT /slib/areas/{id}/dimensions
 */
export const updateAreaDimensions = (areaId, width, height) =>
  api.put(`/areas/${areaId}/dimensions`, {
    width,
    height,
  });

/**
 * UPDATE AREA POSITION AND DIMENSIONS
 * Backend: PUT /slib/areas/{id}/position-and-dimensions
 */
export const updateAreaPositionAndDimensions = (areaId, payload) =>
  api.put(`/areas/${areaId}/position-and-dimensions`, payload);

/**
 * UPDATE AREA LOCKED STATUS
 * Backend: PUT /slib/areas/{id}/locked
 */
export const updateAreaLocked = (areaId, payload) =>
  api.put(`/areas/${areaId}/locked`, payload);

/**
 * UPDATE AREA IS_ACTIVE STATUS
 * Backend: PUT /slib/areas/{id}/active
 */
export const updateAreaIsActive = (areaId, payload) =>
  api.put(`/areas/${areaId}/active`, payload);

/**
 * DELETE AREA
 */
export const deleteArea = (areaId) =>
  api.delete(`/areas/${areaId}`);

/* ========================== ZONE ========================= */

/**
 * GET ZONES BY AREA
 */
export const getZonesByArea = (areaId) =>
  api.get(`/zones`, { params: { areaId } });

export const getZones = () =>
  api.get(`/zones`);

/**
 * CREATE ZONE
 */
export const createZone = (payload) =>
  api.post("/zones", payload);

/**
 * UPDATE ZONE
 */
export const updateZone = (zoneId, payload) =>
  api.put(`/zones/${zoneId}`, payload);

/**
 * UPDATE ZONE POSITION
 * Backend: PUT /slib/zones/{id}/position
 */
export const updateZonePosition = (zoneId, x, y) =>
  api.put(`/zones/${zoneId}/position`, {
    positionX: x,
    positionY: y,
  });

/**
 * UPDATE ZONE DIMENSIONS
 * Backend: PUT /slib/zones/{id}/dimensions
 */
export const updateZoneDimensions = (zoneId, width, height) =>
  api.put(`/zones/${zoneId}/dimensions`, {
    width,
    height,
  });

/**
 * UPDATE ZONE POSITION AND DIMENSIONS
 * Backend: PUT /slib/zones/{id}/position-and-dimensions
 */
export const updateZonePositionAndDimensions = (zoneId, payload) =>
  api.put(`/zones/${zoneId}/position-and-dimensions`, payload);

/**
 * DELETE ZONE
 */
export const deleteZone = (zoneId) =>
  api.delete(`/zones/${zoneId}`);

/* ========================== SEAT ========================= */

/**
 * GET SEATS BY ZONE
 * Backend: GET /slib/seats?zoneId={zoneId}
 */
export const getSeats = (zoneId) =>
  api.get(`/seats`, { params: { zoneId } });

/**
 * CREATE SEAT
 */
export const createSeat = (payload) =>
  api.post("/seats", payload);

/**
 * UPDATE SEAT
 * Backend: PUT /slib/seats/{id}
 */
export const updateSeat = (seatId, payload) =>
  api.put(`/seats/${seatId}`, payload);

/**
 * UPDATE SEAT POSITION
 * Backend: PUT /slib/seats/{id}/position
 */
export const updateSeatPosition = (seatId, x, y) =>
  api.put(`/seats/${seatId}/position`, {
    positionX: x,
    positionY: y,
  });

/**
 * UPDATE SEAT DIMENSIONS
 * Backend: PUT /slib/seats/{id}/dimensions
 */
export const updateSeatDimensions = (seatId, width, height) =>
  api.put(`/seats/${seatId}/dimensions`, {
    width,
    height,
  });

/**
 * UPDATE SEAT POSITION AND DIMENSIONS
 * Backend: PUT /slib/seats/{id}/position-and-dimensions
 */
export const updateSeatPositionAndDimensions = (seatId, payload) =>
  api.put(`/seats/${seatId}/position-and-dimensions`, payload);

/**
 * DELETE SEAT
 */
export const deleteSeat = (seatId) =>
  api.delete(`/seats/${seatId}`);

/**
 * UPDATE SEAT NFC UID (UID Mapping Strategy)
 * Backend: PUT /slib/seats/{seatId}/nfc-uid
 * @param {number} seatId - Seat ID to update
 * @param {string} nfcTagUid - NFC tag UID in uppercase HEX format (e.g., "04A23C91")
 */
export const updateSeatNfcUid = (seatId, nfcTagUid) =>
  api.put(`/seats/${seatId}/nfc-uid`, { nfcTagUid });

/* ========================== LAYOUT ADMIN ========================= */

export const getLayoutDraft = () =>
  api.get("/layout-admin/draft");

export const getLayoutHistory = (limit = 20) =>
  api.get("/layout-admin/history", { params: { limit } });

export const validateLayoutSnapshot = (payload) =>
  api.post("/layout-admin/validate", payload);

export const saveLayoutDraft = (payload) =>
  api.post("/layout-admin/draft", payload);

export const discardLayoutDraft = () =>
  api.delete("/layout-admin/draft");

export const getLayoutSchedule = () =>
  api.get("/layout-admin/schedule");

export const scheduleLayoutSnapshot = (payload) =>
  api.post("/layout-admin/schedule", payload);

export const cancelLayoutSchedule = (scheduleId) =>
  api.delete(`/layout-admin/schedule/${scheduleId}`);

export const publishLayoutSnapshot = (payload) =>
  api.post("/layout-admin/publish", payload);

/**
 * CLEAR SEAT NFC UID
 * Backend: DELETE /slib/seats/{seatId}/nfc-uid
 */
export const clearSeatNfcUid = (seatId) =>
  api.delete(`/seats/${seatId}/nfc-uid`);

/**
 * GET SEAT BY NFC UID
 * Backend: GET /slib/seats/by-nfc-uid/{nfcTagUid}
 * @param {string} nfcTagUid - NFC tag UID in uppercase HEX format
 */
export const getSeatByNfcUid = (nfcTagUid) =>
  api.get(`/seats/by-nfc-uid/${nfcTagUid}`);

/* ========================== FACTORY ========================= */

/**
 * GET FACTORIES BY AREA
 * Backend: GET /slib/area_factories/area/{areaId}
 */
export const getAreaFactoriesByArea = (areaId) =>
  api.get(`/area_factories/area/${areaId}`);

export const getAreaFactories = () =>
  api.get(`/area_factories`);

/**
 * CREATE FACTORY IN AREA
 * Backend: POST /slib/area_factories/area/{areaId}
 */
export const createAreaFactoryInArea = (areaId, payload) =>
  api.post(`/area_factories/area/${areaId}`, payload);

/**
 * UPDATE FACTORY
 * Backend: PUT /slib/area_factories/{id}
 */
export const updateAreaFactory = (factoryId, payload) =>
  api.put(`/area_factories/${factoryId}`, payload);

/**
 * DRAG FACTORY (move position)
 * Backend: PATCH /slib/area_factories/{id}/drag?x=...&y=...
 * NOTE: Convert x, y to integers (backend expects Int)
 */
export const dragAreaFactory = (factoryId, x, y) =>
  api.patch(`/area_factories/${factoryId}/drag`, null, { params: { x: Math.round(x), y: Math.round(y) } });

/**
 * RESIZE FACTORY
 * Backend: PATCH /slib/area_factories/{id}/resize?width=...&height=...
 * NOTE: Convert width, height to integers (backend expects Int)
 */
export const resizeAreaFactory = (factoryId, width, height) =>
  api.patch(`/area_factories/${factoryId}/resize`, null, { params: { width: Math.round(width), height: Math.round(height) } });

/**
 * DELETE FACTORY
 * Backend: DELETE /slib/area_factories/{id}
 */
export const deleteAreaFactory = (factoryId) =>
  api.delete(`/area_factories/${factoryId}`);

/* ========================== AMENITY ========================= */

/**
 * GET AMENITIES BY ZONE
 * Backend: GET /slib/zone_amenities?zoneId=...
 */
export const getAmenitiesByZone = (zoneId) =>
  api.get(`/zone_amenities`, { params: { zoneId } });

/**
 * CREATE AMENITY
 * Backend: POST /slib/zone_amenities
 */
export const createAmenity = (payload) =>
  api.post(`/zone_amenities`, payload);

/**
 * DELETE AMENITY
 * Backend: DELETE /slib/zone_amenities/{id}
 */
export const deleteAmenity = (amenityId) =>
  api.delete(`/zone_amenities/${amenityId}`);

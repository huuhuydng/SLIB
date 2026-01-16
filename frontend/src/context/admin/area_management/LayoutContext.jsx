import { createContext, useContext, useReducer } from "react";

/**
 * =========================
 * CONSTANTS
 * =========================
 */

export const DEFAULT_AMENITIES = [
  { id: "wifi", name: "WiFi", icon: "📶" },
  { id: "power", name: "Power Outlet", icon: "🔌" },
  { id: "lamp", name: "Desk Lamp", icon: "💡" },
  { id: "quiet", name: "Quiet Area", icon: "🤫" },
  { id: "discussion", name: "Discussion Allowed", icon: "💬" },
];

export const FIXED_ZONE_PRESETS = [
  { id: "bookshelf", name: "Bookshelf", icon: "📚", color: "#8B4513" },
  { id: "divider", name: "Divider / Wall", icon: "🧱", color: "#9CA3AF" },
  { id: "entrance", name: "Entrance", icon: "🚪", color: "#CD853F" },
];

/**
 * =========================
 * INITIAL STATE
 * =========================
 */

const initialState = {
  // ===== DATA FROM BE =====
  areas: [],
  zones: [],
  seats: [],
  factories: [],

  // ===== UI STATE =====
  selectedAreaId: null,
  selectedZoneId: null,
  selectedItem: null, // { type: 'area' | 'zone' | 'seat', id }
  isCanvasFullscreen: false,

  canvas: {
    panX: 0,
    panY: 0,
    zoom: 1,
  },

  availableAmenities: DEFAULT_AMENITIES,
  fixedZonePresets: FIXED_ZONE_PRESETS,
};

/**
 * =========================
 * ACTION TYPES
 * =========================
 */

export const ACTIONS = {
  // ===== AREA =====
  SET_AREAS: "SET_AREAS",
  ADD_AREA: "ADD_AREA",
  UPDATE_AREA: "UPDATE_AREA",
  DELETE_AREA: "DELETE_AREA",
  SELECT_AREA: "SELECT_AREA",

  // ===== ZONE =====
  SET_ZONES: "SET_ZONES",
  MERGE_ZONES: "MERGE_ZONES",
  ADD_ZONE: "ADD_ZONE",
  UPDATE_ZONE: "UPDATE_ZONE",
  DELETE_ZONE: "DELETE_ZONE",

  // ===== SEAT =====
  SET_SEATS: "SET_SEATS",
  MERGE_SEATS: "MERGE_SEATS",
  ADD_SEAT: "ADD_SEAT",
  UPDATE_SEAT: "UPDATE_SEAT",
  DELETE_SEAT: "DELETE_SEAT",

  // ===== FACTORY =====
  SET_FACTORIES: "SET_FACTORIES",
  MERGE_FACTORIES: "MERGE_FACTORIES",
  ADD_FACTORY: "ADD_FACTORY",
  UPDATE_FACTORY: "UPDATE_FACTORY",
  DELETE_FACTORY: "DELETE_FACTORY",

  // ===== SELECTION =====
  SELECT_ITEM: "SELECT_ITEM",

  // ===== CANVAS =====
  SET_PAN: "SET_PAN",
  SET_ZOOM: "SET_ZOOM",
  TOGGLE_CANVAS_FULLSCREEN: "TOGGLE_CANVAS_FULLSCREEN",
};

/**
 * =========================
 * REDUCER
 * =========================
 */

function layoutReducer(state, action) {
  switch (action.type) {
    // ===== AREA =====
    case ACTIONS.SET_AREAS:
      return {
        ...state,
        areas: action.payload,
        selectedAreaId: action.payload[0]?.areaId || null,
      };

    case ACTIONS.ADD_AREA:
      return {
        ...state,
        areas: [...state.areas, action.payload],
        selectedAreaId: action.payload.areaId,
        selectedItem: { type: "area", id: action.payload.areaId },
      };

    case ACTIONS.UPDATE_AREA:
      return {
        ...state,
        areas: state.areas.map((a) =>
          a.areaId === action.payload.areaId ? action.payload : a
        ),
      };

    case ACTIONS.DELETE_AREA:
      return {
        ...state,
        areas: state.areas.filter((a) => a.areaId !== action.payload),
        zones: state.zones.filter((z) => z.areaId !== action.payload),
        seats: state.seats.filter((s) =>
          state.zones.find((z) => z.zoneId === s.zoneId)?.areaId !== action.payload
        ),
        selectedAreaId:
          state.selectedAreaId === action.payload ? null : state.selectedAreaId,
        selectedItem: null,
      };

    case ACTIONS.SELECT_AREA:
      console.log("SELECT_AREA reducer - payload:", action.payload);
      return {
        ...state,
        selectedAreaId: action.payload,
        selectedItem: { type: "area", id: action.payload },
      };

    // ===== ZONE =====
    case ACTIONS.SET_ZONES:
      return {
        ...state,
        zones: action.payload,
      };

    case ACTIONS.MERGE_ZONES:
      // Merge zones for a specific area
      const { areaId, zones: newZones } = action.payload;
      const otherZones = state.zones.filter(z => z.areaId !== areaId);
      return {
        ...state,
        zones: [...otherZones, ...newZones],
      };

    case ACTIONS.ADD_ZONE:
      return {
        ...state,
        zones: [...state.zones, action.payload],
        selectedItem: { type: "zone", id: action.payload.zoneId },
      };

    case ACTIONS.UPDATE_ZONE:
      return {
        ...state,
        zones: state.zones.map((z) =>
          z.zoneId === action.payload.zoneId ? action.payload : z
        ),
      };

    case ACTIONS.DELETE_ZONE:
      return {
        ...state,
        zones: state.zones.filter((z) => z.zoneId !== action.payload),
        seats: state.seats.filter((s) => s.zoneId !== action.payload),
        selectedItem: null,
      };

    // ===== SEAT =====
    case ACTIONS.SET_SEATS:
      return {
        ...state,
        // Sort by seatId to maintain consistent order
        seats: (action.payload || []).sort((a, b) => a.seatId - b.seatId),
      };

    case ACTIONS.MERGE_SEATS:
      // Merge seats for a specific zone without overwriting other zones' seats
      const { zoneId, seats: newSeats } = action.payload || {};
      const otherSeats = state.seats.filter((s) => s.zoneId !== zoneId);
      const mergedSeats = [...otherSeats, ...(newSeats || [])];
      
      // Sort by seatId to maintain consistent order
      return {
        ...state,
        seats: mergedSeats.sort((a, b) => a.seatId - b.seatId),
      };

    case ACTIONS.ADD_SEAT:
      return {
        ...state,
        seats: [...state.seats, action.payload],
      };

    case ACTIONS.UPDATE_SEAT:
      {
        const before = state.seats.find((s) => s.seatId === action.payload.seatId);
        console.log("[Reducer] UPDATE_SEAT", {
          seatId: action.payload.seatId,
          prevIsActive: before?.isActive,
          nextIsActive: action.payload.isActive,
        });
      }
      return {
        ...state,
        seats: state.seats.map((s) =>
          s.seatId === action.payload.seatId ? action.payload : s
        ),
      };

    case ACTIONS.DELETE_SEAT:
      return {
        ...state,
        seats: state.seats.filter((s) => s.seatId !== action.payload),
      };

    // ===== FACTORY =====
    case ACTIONS.SET_FACTORIES:
      return {
        ...state,
        factories: action.payload || [],
      };

    case ACTIONS.MERGE_FACTORIES:
      // Merge factories for a specific area
      const { areaId: factoryAreaId, factories: newFactories } = action.payload;
      console.log(`📦 MERGE_FACTORIES - Area: ${factoryAreaId}, New factories:`, newFactories);
      const otherFactories = state.factories.filter(f => f.areaId !== factoryAreaId);
      const merged = [...otherFactories, ...newFactories];
      console.log(`📦 After merge:`, merged);
      return {
        ...state,
        factories: merged,
      };

    case ACTIONS.ADD_FACTORY:
      console.log(`➕ ADD_FACTORY:`, action.payload);
      return {
        ...state,
        factories: [...state.factories, action.payload],
      };

    case ACTIONS.UPDATE_FACTORY:
      return {
        ...state,
        factories: state.factories.map((f) =>
          f.factoryId === action.payload.factoryId ? action.payload : f
        ),
      };

    case ACTIONS.DELETE_FACTORY:
      return {
        ...state,
        factories: state.factories.filter((f) => f.factoryId !== action.payload),
      };

    // ===== SELECTION =====
    case ACTIONS.SELECT_ITEM:
      return {
        ...state,
        selectedItem: action.payload,
        // ✅ Nếu select area, cập nhật selectedAreaId
        selectedAreaId: action.payload?.type === 'area' 
          ? action.payload.id 
          : state.selectedAreaId,
        // ✅ Nếu select zone, cập nhật selectedZoneId, ngược lại reset về null
        selectedZoneId: action.payload?.type === 'zone'
          ? action.payload.id
          : null,
      };

    // ===== CANVAS =====
    case ACTIONS.SET_PAN:
      return {
        ...state,
        canvas: {
          ...state.canvas,
          panX: action.payload.x,
          panY: action.payload.y,
        },
      };

    case ACTIONS.SET_ZOOM:
      return {
        ...state,
        canvas: { ...state.canvas, zoom: action.payload },
      };

    case ACTIONS.TOGGLE_CANVAS_FULLSCREEN:
      return {
        ...state,
        isCanvasFullscreen: !state.isCanvasFullscreen,
      };

    default:
      return state;
  }
}

/**
 * =========================
 * CONTEXT
 * =========================
 */

const LayoutContext = createContext(null);

/**
 * =========================
 * PROVIDER
 * =========================
 */

export function LayoutProvider({ children }) {
  const [state, dispatch] = useReducer(layoutReducer, initialState);

  return (
    <LayoutContext.Provider
      value={{
        state,
        dispatch,
        actions: ACTIONS,
      }}
    >
      {children}
    </LayoutContext.Provider>
  );
}

/**
 * =========================
 * HOOK
 * =========================
 */

export function useLayout() {
  const context = useContext(LayoutContext);
  if (!context) {
    throw new Error("useLayout must be used within LayoutProvider");
  }
  return context;
}

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
  selectedItem: null, // { type: 'area' | 'zone' | 'seat' | 'factory', id }
  selectedItems: [],  // Array of { type, id } for multi-select (Ctrl+A)
  isCanvasFullscreen: false,
  isPreviewMode: false, // Preview mode: view-only, no editing
  hasUnsavedChanges: false,
  isSaving: false,

  // ===== PENDING CHANGES (for batch save) =====
  pendingChanges: {
    newZones: [],        // Zones created locally (not yet in DB)
    newFactories: [],    // Factories created locally (not yet in DB)
    newSeats: [],        // Seats created locally (not yet in DB)
    deletedZones: [],    // Zone IDs to delete
    deletedFactories: [], // Factory IDs to delete
    deletedSeats: [],    // Seat IDs to delete
    updatedSeats: [],    // Seat objects with updates
  },

  // ===== UNDO/REDO HISTORY =====
  history: [],           // Stack of previous states for undo
  redoStack: [],         // Stack for redo

  // ===== CLIPBOARD =====
  clipboard: null,       // { type: 'zone' | 'factory', data: {...} }

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
  REPLACE_SEAT_BY_TEMP_ID: "REPLACE_SEAT_BY_TEMP_ID",
  REPLACE_ZONE_BY_TEMP_ID: "REPLACE_ZONE_BY_TEMP_ID",
  REPLACE_FACTORY_BY_TEMP_ID: "REPLACE_FACTORY_BY_TEMP_ID",

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
  SET_PREVIEW_MODE: "SET_PREVIEW_MODE",

  // ===== SAVE STATE =====
  SET_UNSAVED_CHANGES: "SET_UNSAVED_CHANGES",
  SET_SAVING: "SET_SAVING",
  MARK_SAVED: "MARK_SAVED",

  // ===== PENDING CHANGES =====
  ADD_PENDING_SEAT_DELETE: "ADD_PENDING_SEAT_DELETE",
  ADD_PENDING_SEAT_UPDATE: "ADD_PENDING_SEAT_UPDATE",
  ADD_PENDING_ZONE: "ADD_PENDING_ZONE",
  ADD_PENDING_ZONE_DELETE: "ADD_PENDING_ZONE_DELETE",
  ADD_PENDING_FACTORY: "ADD_PENDING_FACTORY",
  ADD_PENDING_FACTORY_DELETE: "ADD_PENDING_FACTORY_DELETE",
  CLEAR_PENDING_CHANGES: "CLEAR_PENDING_CHANGES",

  // ===== UNDO/REDO =====
  UNDO: "UNDO",
  REDO: "REDO",
  PUSH_HISTORY: "PUSH_HISTORY",

  // ===== CLIPBOARD =====
  COPY: "COPY",
  PASTE: "PASTE",

  // ===== MOVEMENT =====
  MOVE_SELECTED: "MOVE_SELECTED",
  MOVE_ALL_SELECTED: "MOVE_ALL_SELECTED",

  // ===== DESELECT =====
  DESELECT: "DESELECT",

  // ===== SELECT ALL / MULTI-SELECT =====
  SELECT_ALL: "SELECT_ALL",
  TOGGLE_SELECT: "TOGGLE_SELECT",
  DELETE_ALL_SELECTED: "DELETE_ALL_SELECTED",
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
      // Preserve pending new zones for this area (not yet saved to DB)
      const pendingNewZonesForArea = state.pendingChanges.newZones.filter(z => z.areaId === areaId);
      // Filter out zones that are marked for deletion
      const filteredNewZones = newZones.filter(z =>
        !state.pendingChanges.deletedZones.includes(z.zoneId)
      );
      return {
        ...state,
        zones: [...otherZones, ...filteredNewZones, ...pendingNewZonesForArea],
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
        // Also remove from pendingChanges.newZones if it was a pending zone (not yet saved)
        pendingChanges: {
          ...state.pendingChanges,
          newZones: state.pendingChanges.newZones.filter(z => z.zoneId !== action.payload),
          // Remove any pending seats that belong to this zone
          newSeats: state.pendingChanges.newSeats.filter(s => s.zoneId !== action.payload),
        },
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
      // Preserve pending new seats for this zone (not yet saved to DB)
      const pendingNewSeatsForZone = state.pendingChanges.newSeats.filter(s => s.zoneId === zoneId);
      // Filter out seats that are marked for deletion
      const filteredNewSeats = (newSeats || []).filter(s =>
        !state.pendingChanges.deletedSeats.includes(s.seatId)
      );
      const mergedSeats = [...otherSeats, ...filteredNewSeats, ...pendingNewSeatsForZone];

      // Sort by seatId to maintain consistent order
      return {
        ...state,
        seats: mergedSeats.sort((a, b) => a.seatId - b.seatId),
      };

    case ACTIONS.ADD_SEAT:
      const newSeat = { ...action.payload, isPending: true };
      return {
        ...state,
        seats: [...state.seats, newSeat],
        pendingChanges: {
          ...state.pendingChanges,
          newSeats: [...state.pendingChanges.newSeats, newSeat],
        },
        hasUnsavedChanges: true,
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

    // Replace temp seat with real seat after API creation (matches by tempId)
    case ACTIONS.REPLACE_SEAT_BY_TEMP_ID:
      const { tempId: seatTempId, realSeat } = action.payload;
      return {
        ...state,
        seats: state.seats.map((s) =>
          s.seatId === seatTempId ? { ...realSeat, isPending: false } : s
        ),
      };

    case ACTIONS.DELETE_SEAT:
      return {
        ...state,
        seats: state.seats.filter((s) => s.seatId !== action.payload),
        // Also remove from pendingChanges.newSeats if it was a pending seat (not yet saved)
        pendingChanges: {
          ...state.pendingChanges,
          newSeats: state.pendingChanges.newSeats.filter(s => s.seatId !== action.payload),
        },
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
      console.log(`MERGE_FACTORIES - Area: ${factoryAreaId}, New factories:`, newFactories);
      const otherFactories = state.factories.filter(f => f.areaId !== factoryAreaId);
      // Preserve pending new factories for this area (not yet saved to DB)
      const pendingNewFactoriesForArea = state.pendingChanges.newFactories.filter(f => f.areaId === factoryAreaId);
      // Filter out factories that are marked for deletion
      const filteredNewFactories = newFactories.filter(f =>
        !state.pendingChanges.deletedFactories.includes(f.factoryId)
      );
      const merged = [...otherFactories, ...filteredNewFactories, ...pendingNewFactoriesForArea];
      console.log(`After merge:`, merged);
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
        // Also remove from pendingChanges.newFactories if it was a pending factory (not yet saved)
        pendingChanges: {
          ...state.pendingChanges,
          newFactories: state.pendingChanges.newFactories.filter(f => f.factoryId !== action.payload),
        },
      };

    // Replace temp zone with real zone after API creation (matches by tempId)
    case ACTIONS.REPLACE_ZONE_BY_TEMP_ID:
      const { tempId: zoneTempId, realZone } = action.payload;
      return {
        ...state,
        zones: state.zones.map((z) =>
          z.zoneId === zoneTempId ? { ...realZone, isPending: false } : z
        ),
      };

    // Replace temp factory with real factory after API creation (matches by tempId)
    case ACTIONS.REPLACE_FACTORY_BY_TEMP_ID:
      const { tempId: factoryTempId, realFactory } = action.payload;
      return {
        ...state,
        factories: state.factories.map((f) =>
          f.factoryId === factoryTempId ? { ...realFactory, isPending: false } : f
        ),
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

    case ACTIONS.SET_PREVIEW_MODE:
      // When entering preview mode, deselect all items
      if (action.payload === true) {
        return {
          ...state,
          isPreviewMode: true,
          selectedItem: null,
          selectedItems: [],
          selectedZoneId: null,
        };
      }
      return {
        ...state,
        isPreviewMode: action.payload,
      };

    // ===== SAVE STATE =====
    case ACTIONS.SET_UNSAVED_CHANGES:
      return {
        ...state,
        hasUnsavedChanges: action.payload,
      };

    case ACTIONS.SET_SAVING:
      return {
        ...state,
        isSaving: action.payload,
      };

    case ACTIONS.MARK_SAVED:
      return {
        ...state,
        hasUnsavedChanges: false,
        isSaving: false,
        pendingChanges: {
          newZones: [],
          newFactories: [],
          newSeats: [],        // FIX: Was missing - must reset newSeats too!
          deletedZones: [],
          deletedFactories: [],
          deletedSeats: [],
          updatedSeats: [],
        },
        history: [], // Clear history after save
      };

    // ===== PENDING CHANGES =====
    case ACTIONS.ADD_PENDING_SEAT_DELETE:
      return {
        ...state,
        hasUnsavedChanges: true,
        pendingChanges: {
          ...state.pendingChanges,
          deletedSeats: [...state.pendingChanges.deletedSeats, action.payload],
        },
      };

    case ACTIONS.ADD_PENDING_SEAT_UPDATE:
      // Replace if seat already in pendingUpdates, otherwise add
      const existingIndex = state.pendingChanges.updatedSeats.findIndex(
        s => s.seatId === action.payload.seatId
      );
      const newUpdatedSeats = existingIndex >= 0
        ? state.pendingChanges.updatedSeats.map((s, i) =>
          i === existingIndex ? action.payload : s
        )
        : [...state.pendingChanges.updatedSeats, action.payload];
      return {
        ...state,
        hasUnsavedChanges: true,
        pendingChanges: {
          ...state.pendingChanges,
          updatedSeats: newUpdatedSeats,
        },
      };

    case ACTIONS.ADD_PENDING_ZONE:
      return {
        ...state,
        hasUnsavedChanges: true,
        zones: [...state.zones, action.payload],
        pendingChanges: {
          ...state.pendingChanges,
          newZones: [...state.pendingChanges.newZones, action.payload],
        },
        selectedItem: { type: "zone", id: action.payload.zoneId },
      };

    case ACTIONS.ADD_PENDING_FACTORY:
      return {
        ...state,
        hasUnsavedChanges: true,
        factories: [...state.factories, action.payload],
        pendingChanges: {
          ...state.pendingChanges,
          newFactories: [...state.pendingChanges.newFactories, action.payload],
        },
      };

    case ACTIONS.ADD_PENDING_ZONE_DELETE:
      return {
        ...state,
        hasUnsavedChanges: true,
        pendingChanges: {
          ...state.pendingChanges,
          deletedZones: [...state.pendingChanges.deletedZones, action.payload],
        },
      };

    case ACTIONS.ADD_PENDING_FACTORY_DELETE:
      return {
        ...state,
        hasUnsavedChanges: true,
        pendingChanges: {
          ...state.pendingChanges,
          deletedFactories: [...state.pendingChanges.deletedFactories, action.payload],
        },
      };

    case ACTIONS.CLEAR_PENDING_CHANGES:
      return {
        ...state,
        pendingChanges: {
          newZones: [],
          newFactories: [],
          newSeats: [],
          deletedZones: [],
          deletedFactories: [],
          deletedSeats: [],
          updatedSeats: [],
        },
      };

    // ===== UNDO/REDO =====
    case ACTIONS.PUSH_HISTORY:
      // Save current state (excluding history/redo stacks) to history
      const { history: _h, redoStack: _r, ...stateForHistory } = state;
      return {
        ...state,
        history: [...state.history.slice(-19), stateForHistory],
        redoStack: [], // Clear redo stack when new action is performed
      };

    case ACTIONS.UNDO:
      if (state.history.length === 0) return state;
      const prevState = state.history[state.history.length - 1];
      const { history: _h2, redoStack: _r2, ...currentForRedo } = state;
      return {
        ...prevState,
        history: state.history.slice(0, -1),
        redoStack: [...state.redoStack, currentForRedo],
        hasUnsavedChanges: true,
      };

    case ACTIONS.REDO:
      if (state.redoStack.length === 0) return state;
      const nextState = state.redoStack[state.redoStack.length - 1];
      const { history: _h3, redoStack: _r3, ...currentForHistory } = state;
      return {
        ...nextState,
        history: [...state.history, currentForHistory],
        redoStack: state.redoStack.slice(0, -1),
        hasUnsavedChanges: true,
      };

    // ===== CLIPBOARD =====
    case ACTIONS.COPY:
      const copySel = state.selectedItem;
      if (!copySel) return state;

      let copiedData = null;
      if (copySel.type === 'zone') {
        const zone = state.zones.find(z => z.zoneId === copySel.id);
        if (zone) copiedData = { type: 'zone', data: { ...zone } };
      } else if (copySel.type === 'factory') {
        const factory = state.factories.find(f => f.factoryId === copySel.id);
        if (factory) copiedData = { type: 'factory', data: { ...factory } };
      }
      return { ...state, clipboard: copiedData };

    case ACTIONS.PASTE:
      if (!state.clipboard || !state.selectedAreaId) return state;
      const { clipboard } = state;
      const tempId = -Date.now();

      if (clipboard.type === 'zone') {
        const newZone = {
          ...clipboard.data,
          zoneId: tempId,
          zoneName: clipboard.data.zoneName,
          positionX: (clipboard.data.positionX || 0) + 20,
          positionY: (clipboard.data.positionY || 0) + 20,
          areaId: state.selectedAreaId,
          isPending: true,
        };
        return {
          ...state,
          zones: [...state.zones, newZone],
          pendingChanges: {
            ...state.pendingChanges,
            newZones: [...state.pendingChanges.newZones, newZone],
          },
          selectedItem: { type: 'zone', id: tempId },
          hasUnsavedChanges: true,
        };
      } else if (clipboard.type === 'factory') {
        const newFactory = {
          ...clipboard.data,
          factoryId: tempId,
          factoryName: clipboard.data.factoryName,
          positionX: (clipboard.data.positionX || 0) + 20,
          positionY: (clipboard.data.positionY || 0) + 20,
          areaId: state.selectedAreaId,
          isPending: true,
        };
        return {
          ...state,
          factories: [...state.factories, newFactory],
          pendingChanges: {
            ...state.pendingChanges,
            newFactories: [...state.pendingChanges.newFactories, newFactory],
          },
          selectedItem: { type: 'factory', id: tempId },
          hasUnsavedChanges: true,
        };
      }
      return state;

    // ===== MOVEMENT =====
    case ACTIONS.MOVE_SELECTED:
      const { dx, dy } = action.payload;
      const sel = state.selectedItem;
      if (!sel) return state;

      if (sel.type === 'zone') {
        return {
          ...state,
          zones: state.zones.map(z =>
            z.zoneId === sel.id
              ? { ...z, positionX: (z.positionX || 0) + dx, positionY: (z.positionY || 0) + dy }
              : z
          ),
          hasUnsavedChanges: true,
        };
      } else if (sel.type === 'factory') {
        return {
          ...state,
          factories: state.factories.map(f =>
            f.factoryId === sel.id
              ? { ...f, positionX: (f.positionX || 0) + dx, positionY: (f.positionY || 0) + dy }
              : f
          ),
          hasUnsavedChanges: true,
        };
      }
      return state;

    // ===== DESELECT =====
    case ACTIONS.DESELECT:
      return {
        ...state,
        selectedItem: null,
        selectedItems: [],
        selectedZoneId: null,
      };

    // ===== SELECT ALL =====
    case ACTIONS.SELECT_ALL:
      const selectAllAreaId = action.payload?.areaId || state.selectedAreaId;
      if (!selectAllAreaId) return state;

      const allZones = state.zones
        .filter(z => z.areaId === selectAllAreaId)
        .map(z => ({ type: 'zone', id: z.zoneId }));
      const allFactories = state.factories
        .filter(f => f.areaId === selectAllAreaId)
        .map(f => ({ type: 'factory', id: f.factoryId }));
      const allItems = [...allZones, ...allFactories];

      return {
        ...state,
        selectedItems: allItems,
        selectedItem: allItems.length > 0 ? allItems[0] : null,
      };

    // ===== TOGGLE SELECT (Ctrl+Click) =====
    case ACTIONS.TOGGLE_SELECT:
      const toggleItem = action.payload; // { type: 'zone' | 'factory', id }
      const currentItems = state.selectedItems || [];
      const toggleIdx = currentItems.findIndex(
        item => item.type === toggleItem.type && item.id === toggleItem.id
      );

      let newSelectedItems;
      if (toggleIdx >= 0) {
        // Remove from selection
        newSelectedItems = currentItems.filter((_, i) => i !== toggleIdx);
      } else {
        // Add to selection
        newSelectedItems = [...currentItems, toggleItem];
      }

      return {
        ...state,
        selectedItems: newSelectedItems,
        selectedItem: newSelectedItems.length > 0 ? newSelectedItems[0] : null,
      };

    // ===== MOVE ALL SELECTED =====
    case ACTIONS.MOVE_ALL_SELECTED:
      const { dx: moveDx, dy: moveDy } = action.payload;
      const itemsToMove = state.selectedItems || [];
      if (itemsToMove.length === 0) return state;

      const zoneIdsToMove = itemsToMove.filter(i => i.type === 'zone').map(i => i.id);
      const factoryIdsToMove = itemsToMove.filter(i => i.type === 'factory').map(i => i.id);

      return {
        ...state,
        zones: state.zones.map(z =>
          zoneIdsToMove.includes(z.zoneId)
            ? { ...z, positionX: (z.positionX || 0) + moveDx, positionY: (z.positionY || 0) + moveDy }
            : z
        ),
        factories: state.factories.map(f =>
          factoryIdsToMove.includes(f.factoryId)
            ? { ...f, positionX: (f.positionX || 0) + moveDx, positionY: (f.positionY || 0) + moveDy }
            : f
        ),
        hasUnsavedChanges: true,
      };

    // ===== DELETE ALL SELECTED =====
    case ACTIONS.DELETE_ALL_SELECTED:
      const itemsToDelete = state.selectedItems || [];
      if (itemsToDelete.length === 0) return state;

      const zoneIdsToDelete = itemsToDelete.filter(i => i.type === 'zone').map(i => i.id);
      const factoryIdsToDelete = itemsToDelete.filter(i => i.type === 'factory').map(i => i.id);

      // Separate pending (not yet saved) vs existing (already in DB)
      // Pending items have negative IDs (tempId = -Date.now())
      const existingZoneIdsToDelete = zoneIdsToDelete.filter(id => id > 0);
      const existingFactoryIdsToDelete = factoryIdsToDelete.filter(id => id > 0);
      const pendingZoneIdsToDelete = zoneIdsToDelete.filter(id => id < 0);
      const pendingFactoryIdsToDelete = factoryIdsToDelete.filter(id => id < 0);

      return {
        ...state,
        zones: state.zones.filter(z => !zoneIdsToDelete.includes(z.zoneId)),
        factories: state.factories.filter(f => !factoryIdsToDelete.includes(f.factoryId)),
        pendingChanges: {
          ...state.pendingChanges,
          // Add existing IDs to delete list for API call
          deletedZones: [...state.pendingChanges.deletedZones, ...existingZoneIdsToDelete],
          deletedFactories: [...state.pendingChanges.deletedFactories, ...existingFactoryIdsToDelete],
          // Remove pending items from newZones/newFactories (they were never saved)
          newZones: state.pendingChanges.newZones.filter(z => !pendingZoneIdsToDelete.includes(z.zoneId)),
          newFactories: state.pendingChanges.newFactories.filter(f => !pendingFactoryIdsToDelete.includes(f.factoryId)),
        },
        selectedItems: [],
        selectedItem: null,
        hasUnsavedChanges: true,
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

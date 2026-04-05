import { useRef, useState, useCallback, useEffect, useLayoutEffect } from "react";
import { useToast } from "../../common/ToastProvider";
import { useLayout } from "../../../context/admin/area_management/LayoutContext";
import { useUnsavedChanges } from "../../../hooks/useUnsavedChanges";
import Area from "./Area";
import {
  getLayoutDraft,
  getLayoutHistory,
  publishLayoutSnapshot,
  saveLayoutDraft,
  validateLayoutSnapshot,
} from "../../../services/admin/area_management/api";
import { clearAllPositionCache } from "../../../utils/positionCache";
import "../../../styles/admin/canvas.css";

function CanvasBoard() {
  const toast = useToast();
  const { state, dispatch, actions } = useLayout();
  const { areas, zones, seats, factories, canvas, selectedAreaId, selectedItem, selectedItems, hasUnsavedChanges, isSaving, isPreviewMode } = state;

  // Warn user about unsaved changes when leaving
  useUnsavedChanges(hasUnsavedChanges);

  const containerRef = useRef(null);
  const boardRef = useRef(null);
  const handleSaveRef = useRef(null); // Ref to store handleSave for keyboard shortcuts

  const [isPanning, setIsPanning] = useState(false);
  const [startPan, setStartPan] = useState({ x: 0, y: 0 });
  const [panMode, setPanMode] = useState(false);
  const [didAutoFit, setDidAutoFit] = useState(false);
  const [spacePressed, setSpacePressed] = useState(false);
  const [draftMeta, setDraftMeta] = useState(null);
  const [historyItems, setHistoryItems] = useState([]);
  const [showHistoryPanel, setShowHistoryPanel] = useState(false);
  const [validationConflicts, setValidationConflicts] = useState([]);
  const [showConflictPanel, setShowConflictPanel] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);

  const formatDraftMetaTime = (value) => {
    if (!value) return "";
    try {
      return new Date(value).toLocaleTimeString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return "";
    }
  };

  const hydrateLayoutSnapshot = useCallback((snapshot) => {
    const areasRaw = Array.isArray(snapshot?.areas) ? snapshot.areas : [];
    const zonesRaw = Array.isArray(snapshot?.zones) ? snapshot.zones : [];
    const seatsRaw = Array.isArray(snapshot?.seats) ? snapshot.seats : [];
    const factoriesRaw = Array.isArray(snapshot?.factories) ? snapshot.factories : [];

    const areasNormalized = areasRaw.map((a) => ({
      areaId: a.area_id ?? a.areaId,
      areaName: a.area_name ?? a.areaName,
      positionX: a.position_x ?? a.positionX ?? 0,
      positionY: a.position_y ?? a.positionY ?? 0,
      width: a.width ?? 300,
      height: a.height ?? 250,
      locked: a.locked ?? a.is_locked ?? false,
      isActive: a.is_active ?? a.isActive ?? true,
    }));
    const zonesNormalized = zonesRaw.map((z) => ({
      zoneId: z.zone_id ?? z.zoneId,
      zoneName: z.zone_name ?? z.zoneName,
      zoneDes: z.zone_des ?? z.zoneDes ?? "",
      areaId: z.area_id ?? z.areaId,
      positionX: z.position_x ?? z.positionX ?? 0,
      positionY: z.position_y ?? z.positionY ?? 0,
      width: z.width ?? 120,
      height: z.height ?? 100,
      color: z.color ?? "#9CA3AF",
      isLocked: z.is_locked ?? z.isLocked ?? false,
      amenities: Array.isArray(z.amenities) ? z.amenities.map((a) => ({
        amenityId: a.amenity_id ?? a.amenityId,
        zoneId: a.zone_id ?? a.zoneId ?? (z.zone_id ?? z.zoneId),
        amenityName: a.amenity_name ?? a.amenityName ?? "",
      })) : [],
    }));
    const seatsNormalized = seatsRaw.map((s) => ({
      seatId: s.seat_id ?? s.seatId,
      zoneId: s.zone_id ?? s.zoneId,
      seatCode: s.seat_code ?? s.seatCode,
      width: s.width ?? 30,
      height: s.height ?? 30,
      rowNumber: s.row_number ?? s.rowNumber,
      columnNumber: s.column_number ?? s.columnNumber,
      positionX: s.position_x ?? s.positionX ?? 0,
      positionY: s.position_y ?? s.positionY ?? 0,
      isActive: (s.is_active ?? s.isActive) ?? true,
      nfcTagUid: s.nfc_tag_uid ?? s.nfcTagUid ?? "",
      seatStatus: s.seat_status ?? s.seatStatus ?? "AVAILABLE",
    }));
    const factoriesNormalized = factoriesRaw.map((f) => ({
      factoryId: f.factory_id ?? f.factoryId,
      factoryName: f.factory_name ?? f.factoryName,
      positionX: f.position_x ?? f.positionX ?? 0,
      positionY: f.position_y ?? f.positionY ?? 0,
      width: f.width ?? 120,
      height: f.height ?? 80,
      color: f.color ?? "#9CA3AF",
      areaId: f.area_id ?? f.areaId,
      isLocked: f.is_locked ?? f.isLocked ?? false,
    }));

    dispatch({ type: actions.SET_ZONES, payload: zonesNormalized });
    dispatch({ type: actions.SET_SEATS, payload: seatsNormalized });
    dispatch({ type: actions.SET_FACTORIES, payload: factoriesNormalized });
    dispatch({ type: actions.SET_LAYOUT_HYDRATED, payload: true });
    dispatch({ type: actions.SET_AREAS, payload: areasNormalized });
    dispatch({
      type: actions.SELECT_AREA,
      payload: areasNormalized[0]?.areaId ?? null,
    });
  }, [dispatch, actions]);

  const buildLayoutSnapshot = useCallback(() => ({
    areas: areas.map((area) => ({
      areaId: area.areaId,
      areaName: area.areaName,
      width: Math.round(area.width ?? 300),
      height: Math.round(area.height ?? 250),
      positionX: Math.round(area.positionX ?? 0),
      positionY: Math.round(area.positionY ?? 0),
      isActive: area.isActive ?? true,
      locked: area.locked ?? false,
    })),
    zones: zones.map((zone) => ({
      zoneId: zone.zoneId,
      zoneName: zone.zoneName,
      zoneDes: zone.zoneDes ?? "",
      areaId: zone.areaId,
      positionX: Math.round(zone.positionX ?? 0),
      positionY: Math.round(zone.positionY ?? 0),
      width: Math.round(zone.width ?? 120),
      height: Math.round(zone.height ?? 100),
      isLocked: zone.isLocked ?? false,
      amenities: Array.isArray(zone.amenities)
        ? zone.amenities.map((amenity) => ({
            amenityId: amenity.amenityId,
            zoneId: zone.zoneId,
            amenityName: amenity.amenityName,
          }))
        : [],
    })),
    seats: seats.map((seat) => ({
      seatId: seat.seatId,
      zoneId: seat.zoneId,
      seatCode: seat.seatCode,
      rowNumber: seat.rowNumber,
      columnNumber: seat.columnNumber,
      isActive: seat.isActive ?? true,
      nfcTagUid: seat.nfcTagUid ?? null,
    })),
    factories: factories.map((factory) => ({
      factoryId: factory.factoryId,
      factoryName: factory.factoryName,
      areaId: factory.areaId,
      positionX: Math.round(factory.positionX ?? 0),
      positionY: Math.round(factory.positionY ?? 0),
      width: Math.round(factory.width ?? 120),
      height: Math.round(factory.height ?? 80),
      isLocked: factory.isLocked ?? false,
    })),
  }), [areas, zones, seats, factories]);

  const loadHistoryFeed = useCallback(async () => {
    try {
      const res = await getLayoutHistory(20);
      setHistoryItems(Array.isArray(res?.data) ? res.data : []);
    } catch (error) {
      console.error("Không thể tải lịch sử sơ đồ", error);
    }
  }, []);

  // ===== FIGMA-STYLE LOADING =====
  const [isLoadingAreas, setIsLoadingAreas] = useState(true);
  /* =============================
     LOAD AREA FROM BACKEND
  ============================== */
  useEffect(() => {
    setIsLoadingAreas(true);
    setDidAutoFit(false);
    dispatch({ type: actions.SET_ZOOM, payload: 1 });
    dispatch({ type: actions.SET_PAN, payload: { x: 0, y: 0 } });

    (async () => {
      try {
        const [draftRes] = await Promise.all([
          getLayoutDraft(),
          loadHistoryFeed(),
        ]);

        setDraftMeta(draftRes?.data ?? null);
        hydrateLayoutSnapshot(draftRes?.data?.snapshot ?? {});
        dispatch({ type: actions.MARK_SAVED });
      } catch (err) {
        console.error("Load areas failed", err);
        dispatch({ type: actions.SET_AREAS, payload: [] });
        dispatch({ type: actions.SET_ZONES, payload: [] });
        dispatch({ type: actions.SET_SEATS, payload: [] });
        dispatch({ type: actions.SET_FACTORIES, payload: [] });
        dispatch({ type: actions.SET_LAYOUT_HYDRATED, payload: false });
      } finally {
        setIsLoadingAreas(false);
      }
    })();
  }, [dispatch, actions, hydrateLayoutSnapshot, loadHistoryFeed]);

  /* =============================
     ZOOM
  ============================== */
  const handleZoomIn = () =>
    dispatch({ type: actions.SET_ZOOM, payload: Math.min(canvas.zoom + 0.1, 3) });

  const handleZoomOut = () =>
    dispatch({ type: actions.SET_ZOOM, payload: Math.max(canvas.zoom - 0.1, 0.1) });

  const handleWheel = useCallback((e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.05 : 0.05;
    const newZoom = Math.min(Math.max(canvas.zoom + delta, 0.1), 3);
    dispatch({ type: actions.SET_ZOOM, payload: newZoom });
  }, [canvas.zoom, dispatch, actions]);

  /* =============================
     PAN
  ============================== */
  const handleMouseDown = (e) => {
    if (e.button !== 0) return;
    // In preview mode, allow panning by default without needing panMode or space
    if (!isPreviewMode && !panMode && !spacePressed) return;

    setIsPanning(true);
    setStartPan({
      x: e.clientX - canvas.panX,
      y: e.clientY - canvas.panY,
    });
  };

  const handleMouseMove = useCallback(
    (e) => {
      if (!isPanning) return;
      dispatch({
        type: actions.SET_PAN,
        payload: {
          x: e.clientX - startPan.x,
          y: e.clientY - startPan.y,
        },
      });
    },
    [isPanning, startPan, dispatch, actions]
  );

  const handleMouseUp = () => setIsPanning(false);

  useEffect(() => {
    if (!isPanning) return;
    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, [isPanning, handleMouseMove]);

  /* =============================
     KEYBOARD SHORTCUTS (Figma-like)
  ============================== */
  useEffect(() => {
    const handleKeyDown = (e) => {
      const activeTag = document.activeElement.tagName;
      const isTyping = activeTag === "INPUT" || activeTag === "TEXTAREA";

      // Space for panning (not when typing)
      if (e.code === "Space" && !e.repeat && !isTyping) {
        e.preventDefault();
        setSpacePressed(true);
        return;
      }

      // Skip shortcuts when typing in inputs
      if (isTyping) return;

      // Skip editing shortcuts when in preview mode (allow panning and zoom)
      const isMac = navigator.platform.toUpperCase().includes('MAC');
      const cmdKey = isMac ? e.metaKey : e.ctrlKey;

      // In preview mode, only allow zoom shortcuts
      if (isPreviewMode) {
        return;
      }

      // Ctrl+Z / Cmd+Z - Undo
      if (cmdKey && e.key === 'z' && !e.shiftKey) {
        e.preventDefault();
        dispatch({ type: actions.UNDO });
        return;
      }

      // Ctrl+Y / Cmd+Shift+Z - Redo
      if ((cmdKey && e.key === 'y') || (cmdKey && e.key === 'z' && e.shiftKey)) {
        e.preventDefault();
        dispatch({ type: actions.REDO });
        return;
      }

      // Ctrl+A / Cmd+A - Select All zones and factories in current area
      if (cmdKey && e.key === 'a') {
        e.preventDefault();
        if (selectedAreaId) {
          dispatch({ type: actions.SELECT_ALL, payload: { areaId: selectedAreaId } });
        }
        return;
      }

      // Ctrl+S / Cmd+S - Save
      if (cmdKey && e.key === 's') {
        e.preventDefault();
        if (hasUnsavedChanges && !isSaving && handleSaveRef.current) {
          handleSaveRef.current();
        }
        return;
      }

      // Ctrl+C / Cmd+C - Copy
      if (cmdKey && e.key === 'c') {
        e.preventDefault();
        dispatch({ type: actions.COPY });
        return;
      }

      // Ctrl+V / Cmd+V - Paste
      if (cmdKey && e.key === 'v') {
        e.preventDefault();
        dispatch({ type: actions.PUSH_HISTORY });
        dispatch({ type: actions.PASTE });
        return;
      }

      // Ctrl+D / Cmd+D - Duplicate (Copy + Paste)
      if (cmdKey && e.key === 'd') {
        e.preventDefault();
        dispatch({ type: actions.PUSH_HISTORY });
        dispatch({ type: actions.COPY });
        setTimeout(() => dispatch({ type: actions.PASTE }), 10);
        return;
      }

      // Delete / Backspace - Delete all selected items
      if (e.key === 'Delete' || e.key === 'Backspace') {
        e.preventDefault();
        const hasMultiSelection = (selectedItems || []).length > 0;
        const hasSingleSelection = selectedItem && (selectedItem.type === 'zone' || selectedItem.type === 'factory');

        if (hasMultiSelection) {
          dispatch({ type: actions.PUSH_HISTORY });
          dispatch({ type: actions.DELETE_ALL_SELECTED });
        } else if (hasSingleSelection) {
          // Add single item to selectedItems and delete
          dispatch({ type: actions.PUSH_HISTORY });
          dispatch({ type: actions.TOGGLE_SELECT, payload: selectedItem });
          dispatch({ type: actions.DELETE_ALL_SELECTED });
        }
        return;
      }

      // Escape - Deselect
      if (e.key === 'Escape') {
        e.preventDefault();
        dispatch({ type: actions.DESELECT });
        return;
      }

      // Arrow keys - Move all selected items
      if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
        e.preventDefault();
        const step = e.shiftKey ? 10 : 1;
        let dx = 0, dy = 0;

        switch (e.key) {
          case 'ArrowUp': dy = -step; break;
          case 'ArrowDown': dy = step; break;
          case 'ArrowLeft': dx = -step; break;
          case 'ArrowRight': dx = step; break;
        }

        dispatch({ type: actions.PUSH_HISTORY });
        // Use MOVE_ALL_SELECTED if multi-select, otherwise MOVE_SELECTED
        if ((selectedItems || []).length > 0) {
          dispatch({ type: actions.MOVE_ALL_SELECTED, payload: { dx, dy } });
        } else {
          dispatch({ type: actions.MOVE_SELECTED, payload: { dx, dy } });
        }
        return;
      }
    };

    const handleKeyUp = (e) => {
      const activeTag = document.activeElement.tagName;
      if (e.code === "Space" && activeTag !== "TEXTAREA") {
        e.preventDefault();
        setSpacePressed(false);
        setIsPanning(false);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
    };
  }, [dispatch, actions, hasUnsavedChanges, isSaving, zones, selectedAreaId, selectedItem, selectedItems, isPreviewMode]);

  /* =============================
     FIT TO VIEW
  ============================== */
  const isCanvasReady = !isLoadingAreas;

  const handleFitToView = () => {
    if (!areas.length || !boardRef.current) return;

    const boardRect = boardRef.current.getBoundingClientRect();

    let minX = Infinity,
      minY = Infinity,
      maxX = -Infinity,
      maxY = -Infinity;

    areas.forEach((a) => {
      minX = Math.min(minX, a.positionX || 0);
      minY = Math.min(minY, a.positionY || 0);
      maxX = Math.max(maxX, (a.positionX || 0) + (a.width || 300));
      maxY = Math.max(maxY, (a.positionY || 0) + (a.height || 250));
    });

    const contentW = maxX - minX + 160;
    const contentH = maxY - minY + 160;

    const scale = Math.min(
      boardRect.width / contentW,
      boardRect.height / contentH,
      0.9
    );

    const centerX = (minX + maxX) / 2;
    const centerY = (minY + maxY) / 2;

    dispatch({ type: actions.SET_ZOOM, payload: scale });
    dispatch({
      type: actions.SET_PAN,
      payload: {
        x: boardRect.width / 2 - centerX * scale,
        y: boardRect.height / 2 - centerY * scale,
      },
    });
  };

  // Auto fit once when areas are loaded/changed.
  useLayoutEffect(() => {
    if (!isLoadingAreas && areas && areas.length === 0) return;

    if (didAutoFit || isLoadingAreas || !areas?.length || !boardRef.current) return;

    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        handleFitToView();
        setDidAutoFit(true);

        // Fit once more after the first paint to avoid stale measurements.
        requestAnimationFrame(() => {
          handleFitToView();
        });
      });
    });
  }, [areas, didAutoFit, isLoadingAreas]);

  useEffect(() => {
    if (!didAutoFit) return;
    const onResize = () => handleFitToView();
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, [didAutoFit, areas]);

  // Calculate statistics
  const totalSeats = seats.length;
  const activeSeats = seats.filter(s => s.isActive !== false).length;
  const totalZones = zones.length;
  const extractValidationPayload = (error) =>
    error?.response?.data?.validation || error?.response?.data;

  const runValidation = useCallback(async (snapshot) => {
    try {
      const res = await validateLayoutSnapshot(snapshot);
      return res.data;
    } catch (error) {
      const validationPayload = extractValidationPayload(error);
      if (validationPayload?.conflicts) {
        return validationPayload;
      }
      throw error;
    }
  }, []);

  const handleSaveDraft = useCallback(async () => {
    if (!hasUnsavedChanges || isSaving) return;

    dispatch({ type: actions.SET_SAVING, payload: true });
    try {
      const snapshot = buildLayoutSnapshot();
      const validation = await runValidation(snapshot);
      if (!validation?.valid) {
        setValidationConflicts(validation?.conflicts || []);
        setShowConflictPanel(true);
        dispatch({ type: actions.SET_SAVING, payload: false });
        toast.error(`Sơ đồ còn ${validation.conflicts.length} xung đột, chưa thể lưu nháp`);
        return;
      }

      const res = await saveLayoutDraft(snapshot);
      setDraftMeta(res?.data ?? null);
      await loadHistoryFeed();
      dispatch({ type: actions.MARK_SAVED });
      toast.success("Đã lưu nháp sơ đồ thư viện");
    } catch (error) {
      console.error("Save draft failed:", error);
      dispatch({ type: actions.SET_SAVING, payload: false });
      const validation = extractValidationPayload(error);
      if (validation?.conflicts) {
        setValidationConflicts(validation.conflicts);
        setShowConflictPanel(true);
      }
      toast.error(error?.response?.data?.message || "Không thể kiểm tra và lưu nháp sơ đồ");
    }
  }, [actions, buildLayoutSnapshot, dispatch, hasUnsavedChanges, isSaving, loadHistoryFeed, runValidation, toast]);

  const handlePublish = useCallback(async () => {
    if (isPublishing || isSaving) return;

    setIsPublishing(true);
    try {
      const snapshot = buildLayoutSnapshot();
      const validation = await runValidation(snapshot);
      if (!validation?.valid) {
        setValidationConflicts(validation?.conflicts || []);
        setShowConflictPanel(true);
        setIsPublishing(false);
        toast.error(`Sơ đồ còn ${validation.conflicts.length} xung đột, chưa thể xuất bản`);
        return;
      }

      const res = await publishLayoutSnapshot(snapshot);
      hydrateLayoutSnapshot(res?.data?.snapshot ?? {});
      clearAllPositionCache();
      dispatch({ type: actions.MARK_SAVED });
      setDraftMeta({
        hasDraft: false,
        basedOnPublishedVersion: res?.data?.publishedVersion,
        updatedByName: res?.data?.publishedByName,
        updatedAt: new Date().toISOString(),
        snapshot: res?.data?.snapshot,
      });
      await loadHistoryFeed();
      toast.success(`Đã xuất bản sơ đồ thư viện, phiên bản ${res?.data?.publishedVersion}`);
    } catch (error) {
      console.error("Publish layout failed:", error);
      const validation = extractValidationPayload(error);
      if (validation?.conflicts) {
        setValidationConflicts(validation.conflicts);
        setShowConflictPanel(true);
      }
      toast.error(error?.response?.data?.message || "Không thể kiểm tra và xuất bản sơ đồ");
    } finally {
      setIsPublishing(false);
    }
  }, [actions, buildLayoutSnapshot, dispatch, hydrateLayoutSnapshot, isPublishing, isSaving, loadHistoryFeed, runValidation, toast]);

  handleSaveRef.current = handleSaveDraft;

  return (
    <main className="canvas-container" ref={containerRef}>
      {/* Header Toolbar */}
      <div className="canvas-header" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '12px',
        padding: '12px 20px',
        background: 'linear-gradient(135deg, #ffffff 0%, #F8FAFC 100%)',
        borderBottom: '1px solid #E2E8F0',
        boxShadow: '0 2px 8px rgba(0,0,0,0.04)'
      }}>
        {/* Left: View Mode Toggle */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          background: '#F1F5F9',
          borderRadius: '10px',
          padding: '4px'
        }}>
          <button
            onClick={() => dispatch({ type: actions.SET_PREVIEW_MODE, payload: false })}
            style={{
              padding: '8px 16px',
              borderRadius: '8px',
              border: 'none',
              background: !isPreviewMode
                ? 'linear-gradient(135deg, #FF751F 0%, #E85A00 100%)'
                : 'transparent',
              color: !isPreviewMode ? 'white' : '#64748B',
              cursor: 'pointer',
              fontSize: '13px',
              fontWeight: '600',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            Chỉnh sửa
          </button>
          <button
            onClick={() => dispatch({ type: actions.SET_PREVIEW_MODE, payload: true })}
            style={{
              padding: '8px 16px',
              borderRadius: '8px',
              border: 'none',
              background: isPreviewMode
                ? 'linear-gradient(135deg, #22C55E 0%, #16A34A 100%)'
                : 'transparent',
              color: isPreviewMode ? 'white' : '#64748B',
              cursor: 'pointer',
              fontSize: '13px',
              fontWeight: '600',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            Xem trước
          </button>
        </div>

        {/* Center: Statistics */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          fontSize: '13px',
          color: '#475569'
        }}>
          <span><strong>{areas.length}</strong> Phòng</span>
          <span style={{ color: '#CBD5E1' }}>|</span>
          <span><strong>{totalZones}</strong> Khu vực</span>
          <span style={{ color: '#CBD5E1' }}>|</span>
          <span><strong>{activeSeats}/{totalSeats}</strong> ghế hoạt động</span>
        </div>

        {/* Right: Tools */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {draftMeta && (
            <div style={{
              padding: '8px 10px',
              borderRadius: '12px',
              background: draftMeta.hasDraft ? '#FFF7ED' : '#F8FAFC',
              border: `1px solid ${draftMeta.hasDraft ? '#FDBA74' : '#E2E8F0'}`,
              color: '#475569',
              fontSize: '11px',
              lineHeight: 1.25,
              minWidth: '152px',
              maxWidth: '200px',
            }}>
              <div style={{
                fontWeight: 700,
                color: '#1F2937',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                marginBottom: '2px',
              }}>
                {draftMeta.hasDraft ? 'Đang mở nháp' : 'Đang xem bản xuất bản'}
              </div>
              <div style={{
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}>
                {draftMeta.updatedByName
                  ? `${draftMeta.updatedByName}${formatDraftMetaTime(draftMeta.updatedAt) ? ` · ${formatDraftMetaTime(draftMeta.updatedAt)}` : ''}`
                  : 'Chưa có nháp'}
              </div>
            </div>
          )}

          {/* Pan Mode - only show in edit mode */}
          {!isPreviewMode && (
            <button
              onClick={() => setPanMode(!panMode)}
              aria-label={panMode ? "Tắt chế độ kéo canvas" : "Bật chế độ kéo canvas"}
              title={panMode ? "Tắt chế độ kéo" : "Bật chế độ kéo (Space)"}
              style={{
                width: '40px',
                height: '40px',
                borderRadius: '10px',
                border: panMode ? '2px solid #3B82F6' : '2px solid #E2E8F0',
                background: panMode
                  ? 'linear-gradient(135deg, #DBEAFE 0%, #BFDBFE 100%)'
                  : 'white',
                cursor: 'pointer',
                fontSize: '16px',
                transition: 'all 0.2s',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: panMode ? '#1E40AF' : '#64748B'
              }}
            >
              ☰
            </button>
          )}

          {/* Zoom Controls */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
            backgroundColor: '#F1F5F9',
            borderRadius: '10px',
            padding: '4px'
          }}>
            <button
              onClick={handleZoomOut}
              aria-label="Thu nhỏ sơ đồ"
              title="Thu nhỏ"
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                border: 'none',
                background: 'white',
                cursor: 'pointer',
                fontSize: '18px',
                fontWeight: '700',
                color: '#64748B',
                transition: 'all 0.2s',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
              }}
            >
              −
            </button>

            <span style={{
              minWidth: '60px',
              textAlign: 'center',
              fontSize: '13px',
              fontWeight: '700',
              color: '#1F2937'
            }}>
              {Math.round(canvas.zoom * 100)}%
            </span>

            <button
              onClick={handleZoomIn}
              aria-label="Phóng to sơ đồ"
              title="Phóng to"
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                border: 'none',
                background: 'white',
                cursor: 'pointer',
                fontSize: '18px',
                fontWeight: '700',
                color: '#64748B',
                transition: 'all 0.2s',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
              }}
            >
              +
            </button>
          </div>

          {/* Fit to View */}
          <button
            onClick={handleFitToView}
            aria-label="Căn sơ đồ vừa màn hình"
            title="Vừa với màn hình"
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '10px',
              border: '2px solid #E2E8F0',
              background: 'white',
              cursor: 'pointer',
              fontSize: '16px',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#64748B'
            }}
          >
            ⬜
          </button>

          {/* Fullscreen Toggle */}
          <button
            onClick={() => dispatch({ type: actions.TOGGLE_CANVAS_FULLSCREEN })}
            aria-label={state.isCanvasFullscreen ? "Thoát toàn màn hình canvas" : "Mở toàn màn hình canvas"}
            title={state.isCanvasFullscreen ? "Thoát toàn màn hình" : "Toàn màn hình"}
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '10px',
              border: state.isCanvasFullscreen ? '2px solid #EF4444' : '2px solid #E2E8F0',
              background: state.isCanvasFullscreen
                ? 'linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%)'
                : 'white',
              cursor: 'pointer',
              fontSize: '16px',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: state.isCanvasFullscreen ? '#DC2626' : '#64748B'
            }}
          >
            {state.isCanvasFullscreen ? '✕' : '⛶'}
          </button>

          <div style={{ position: 'relative' }}>
            <button
              onClick={() => setShowHistoryPanel((prev) => !prev)}
              title="Xem lịch sử thay đổi sơ đồ"
              style={{
                padding: '8px 14px',
                borderRadius: '10px',
                border: showHistoryPanel ? '2px solid #F97316' : '2px solid #E2E8F0',
                background: showHistoryPanel ? '#FFF7ED' : 'white',
                color: '#334155',
                cursor: 'pointer',
                fontSize: '13px',
                fontWeight: '600',
              }}
            >
              Lịch sử
            </button>

            {showHistoryPanel && (
              <div style={{
                position: 'absolute',
                top: 'calc(100% + 12px)',
                right: 0,
                width: '420px',
                maxWidth: 'min(420px, calc(100vw - 180px))',
                maxHeight: '60vh',
                overflow: 'auto',
                zIndex: 180,
                background: 'white',
                border: '1px solid #E2E8F0',
                borderRadius: '18px',
                boxShadow: '0 20px 60px rgba(15, 23, 42, 0.16)',
                padding: '18px',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <div>
                    <div style={{ fontSize: '16px', fontWeight: 800, color: '#0F172A' }}>Lịch sử thay đổi sơ đồ</div>
                    <div style={{ fontSize: '13px', color: '#64748B' }}>Theo dõi ai sửa gì và thời điểm xuất bản</div>
                  </div>
                  <button onClick={() => setShowHistoryPanel(false)} style={{ border: 'none', background: 'transparent', cursor: 'pointer', color: '#64748B', fontWeight: 700 }}>
                    Đóng
                  </button>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {historyItems.length === 0 && (
                    <div style={{ padding: '14px', borderRadius: '12px', background: '#F8FAFC', color: '#475569' }}>
                      Chưa có lịch sử thay đổi sơ đồ.
                    </div>
                  )}
                  {historyItems.map((item) => (
                    <div key={item.historyId} style={{ padding: '12px 14px', borderRadius: '12px', background: '#F8FAFC', border: '1px solid #E2E8F0' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', marginBottom: '4px' }}>
                        <div style={{ fontSize: '13px', fontWeight: 800, color: '#0F172A' }}>
                          {item.actionType === 'PUBLISH' ? 'Xuất bản sơ đồ' : 'Lưu nháp sơ đồ'}
                        </div>
                        <div style={{ fontSize: '12px', color: '#64748B' }}>
                          {item.createdAt ? new Date(item.createdAt).toLocaleString('vi-VN') : ''}
                        </div>
                      </div>
                      <div style={{ fontSize: '13px', color: '#475569', marginBottom: '4px' }}>{item.summary}</div>
                      <div style={{ fontSize: '12px', color: '#64748B' }}>
                        {item.createdByName ? `Người thao tác: ${item.createdByName}` : 'Không rõ người thao tác'}
                        {item.publishedVersion ? ` · Phiên bản ${item.publishedVersion}` : ''}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Save Button */}
          <button
            onClick={handleSaveDraft}
            disabled={!hasUnsavedChanges || isSaving}
            title={hasUnsavedChanges ? "Lưu nháp thay đổi" : "Không có thay đổi cần lưu"}
            style={{
              padding: '8px 16px',
              borderRadius: '10px',
              border: hasUnsavedChanges ? '2px solid #3B82F6' : '2px solid #E2E8F0',
              background: hasUnsavedChanges
                ? 'linear-gradient(135deg, #3B82F6 0%, #2563EB 100%)'
                : '#F1F5F9',
              color: hasUnsavedChanges ? 'white' : '#94A3B8',
              cursor: hasUnsavedChanges && !isSaving ? 'pointer' : 'not-allowed',
              fontSize: '13px',
              fontWeight: '600',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              opacity: isSaving ? 0.7 : 1,
            }}
          >
            {isSaving ? 'Đang lưu nháp...' : hasUnsavedChanges ? 'Lưu nháp' : 'Nháp đã lưu'}
          </button>

          <button
            onClick={handlePublish}
            disabled={isPublishing || isSaving}
            title="Xuất bản sơ đồ hiện tại ra môi trường live"
            style={{
              padding: '8px 16px',
              borderRadius: '10px',
              border: '2px solid #22C55E',
              background: isPublishing
                ? '#DCFCE7'
                : 'linear-gradient(135deg, #22C55E 0%, #16A34A 100%)',
              color: isPublishing ? '#166534' : 'white',
              cursor: isPublishing || isSaving ? 'not-allowed' : 'pointer',
              fontSize: '13px',
              fontWeight: '600',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              opacity: isPublishing ? 0.8 : 1,
            }}
          >
            {isPublishing ? 'Đang xuất bản...' : 'Xuất bản'}
          </button>
        </div>
      </div>

      {showConflictPanel && (
        <div style={{
          position: 'absolute',
          top: '86px',
          right: '24px',
          width: '420px',
          maxHeight: '60vh',
          overflow: 'auto',
          zIndex: 150,
          background: 'white',
          border: '1px solid #E2E8F0',
          borderRadius: '18px',
          boxShadow: '0 20px 60px rgba(15, 23, 42, 0.16)',
          padding: '18px',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <div>
              <div style={{ fontSize: '16px', fontWeight: 800, color: '#0F172A' }}>Kiểm tra xung đột sơ đồ</div>
              <div style={{ fontSize: '13px', color: '#64748B' }}>
                {validationConflicts.length === 0 ? 'Không phát hiện xung đột' : `Đang có ${validationConflicts.length} xung đột cần xử lý`}
              </div>
            </div>
            <button onClick={() => setShowConflictPanel(false)} style={{ border: 'none', background: 'transparent', cursor: 'pointer', color: '#64748B', fontWeight: 700 }}>
              Đóng
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {validationConflicts.length === 0 && (
              <div style={{ padding: '14px', borderRadius: '12px', background: '#F0FDF4', color: '#166534', fontWeight: 600 }}>
                Sơ đồ hiện tại không có xung đột. Anh có thể lưu nháp hoặc xuất bản.
              </div>
            )}
            {validationConflicts.map((conflict, index) => (
              <div
                key={`${conflict.code}-${conflict.entityKey}-${index}`}
                style={{
                  padding: '12px 14px',
                  borderRadius: '12px',
                  background: conflict.severity === 'warning' ? '#FFFBEB' : '#FEF2F2',
                  border: `1px solid ${conflict.severity === 'warning' ? '#FCD34D' : '#FCA5A5'}`,
                }}
              >
                <div style={{ fontSize: '13px', fontWeight: 800, color: '#0F172A', marginBottom: '4px' }}>{conflict.title}</div>
                <div style={{ fontSize: '13px', color: '#475569' }}>{conflict.message}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Canvas Board */}
      <div
        ref={boardRef}
        className={`canvas-board ${panMode || spacePressed ? 'pan-mode' : ''} ${isPreviewMode ? 'preview-mode' : ''}`}
        onMouseDown={handleMouseDown}
        onWheel={handleWheel}
        style={{
          background: isPreviewMode
            ? 'linear-gradient(135deg, #F0FDF4 0%, #ECFDF5 50%, #D1FAE5 100%)'
            : undefined,
          position: 'relative',
        }}
      >
        {/* Loading Overlay - Figma-style */}
        {!isCanvasReady && (
          <div style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'linear-gradient(135deg, #F8FAFC 0%, #E2E8F0 100%)',
            zIndex: 1000,
          }}>
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '16px',
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                border: '4px solid #E2E8F0',
                borderTopColor: '#3B82F6',
                borderRadius: '50%',
                animation: 'spin 1s linear infinite',
              }} />
              <div style={{
                fontSize: '14px',
                fontWeight: '600',
                color: '#64748B',
              }}>
                Đang tải sơ đồ thư viện...
              </div>
            </div>
          </div>
        )}

        {/* Canvas Content - Hidden until ready */}
        <div style={{
          opacity: isCanvasReady ? 1 : 0,
          transition: 'opacity 0.3s ease-in-out',
          width: '100%',
          height: '100%',
        }}>
          {/* Preview Mode Banner */}
          {isPreviewMode && (
            <div style={{
              position: 'absolute',
              top: '20px',
              left: '50%',
              transform: 'translateX(-50%)',
              background: 'linear-gradient(135deg, #22C55E 0%, #16A34A 100%)',
              color: 'white',
              padding: '10px 24px',
              borderRadius: '30px',
              fontSize: '14px',
              fontWeight: '600',
              boxShadow: '0 4px 14px rgba(34, 197, 94, 0.3)',
              zIndex: 100,
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              Chế độ xem trước - Sinh viên sẽ nhìn thấy như này
            </div>
          )}

          <div
            className="canvas-grid"
            style={{
              transform: `translate(${canvas.panX}px, ${canvas.panY}px) scale(${canvas.zoom})`,
              transformOrigin: "0 0",
              position: 'relative',
            }}
          >
            {areas.map((area) => (
              <Area
                key={area.areaId}
                area={area}
              />
            ))}

            {areas.length === 0 && (
              <div style={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                textAlign: 'center',
                padding: '60px',
                background: 'white',
                borderRadius: '20px',
                boxShadow: '0 10px 40px rgba(0,0,0,0.1)',
                border: '2px dashed #E2E8F0'
              }}>
                <div style={{ fontSize: '60px', marginBottom: '20px' }}></div>
                <h3 style={{
                  margin: '0 0 10px 0',
                  color: '#1F2937',
                  fontSize: '20px',
                  fontWeight: '700'
                }}>
                  Chưa có phòng thư viện
                </h3>
                <p style={{
                  margin: 0,
                  color: '#64748B',
                  fontSize: '14px'
                }}>
                  Nhấn "Thêm Phòng" ở sidebar để bắt đầu thiết kế
                </p>
              </div>
            )}
          </div>
        </div>
      </div>{/* End Canvas Content wrapper */}
    </main>
  );
}

export default CanvasBoard;

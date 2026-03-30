import { useRef, useState, useCallback, useEffect } from "react";
import { useToast } from "../../common/ToastProvider";
import { useLayout } from "../../../context/admin/area_management/LayoutContext";
import { useUnsavedChanges } from "../../../hooks/useUnsavedChanges";
import Area from "./Area";
import { getAreas, updateAreaFactory } from "../../../services/admin/area_management/api";
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

  // ===== FIGMA-STYLE LOADING =====
  const [isLoadingAreas, setIsLoadingAreas] = useState(true);
  const [isCanvasReady, setIsCanvasReady] = useState(false);

  /* =============================
     LOAD AREA FROM BACKEND
  ============================== */
  useEffect(() => {
    setIsLoadingAreas(true);
    setIsCanvasReady(false);
    setDidAutoFit(false);

    (async () => {
      try {
        const res = await getAreas();
        const raw = Array.isArray(res?.data) ? res.data : [];
        const areasNormalized = raw.map((a) => ({
          areaId: a.area_id ?? a.areaId,
          areaName: a.area_name ?? a.areaName,
          positionX: a.position_x ?? a.positionX ?? 0,
          positionY: a.position_y ?? a.positionY ?? 0,
          width: a.width ?? 300,
          height: a.height ?? 250,
          locked: a.locked ?? a.is_locked ?? false,
          isActive: a.is_active ?? a.isActive ?? true,
        }));

        if (areasNormalized.length > 0) {
          dispatch({
            type: actions.SET_AREAS,
            payload: areasNormalized,
          });
          dispatch({
            type: actions.SELECT_AREA,
            payload: areasNormalized[0].areaId,
          });
        } else {
          dispatch({ type: actions.SET_AREAS, payload: [] });
        }
      } catch (err) {
        console.error("Load areas failed", err);
        dispatch({ type: actions.SET_AREAS, payload: [] });
      } finally {
        setIsLoadingAreas(false);
      }
    })();
  }, []);

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

    const contentW = maxX - minX + 100;
    const contentH = maxY - minY + 100;

    const scale = Math.min(
      boardRect.width / contentW,
      boardRect.height / contentH,
      1
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

  // Auto fit once when areas are loaded/changed, then mark canvas as ready
  useEffect(() => {
    if (!didAutoFit && !isLoadingAreas && areas && areas.length > 0 && boardRef.current) {
      // Use requestAnimationFrame to ensure DOM is ready
      requestAnimationFrame(() => {
        handleFitToView();
        setDidAutoFit(true);

        // Wait a bit more for all Area components to load their zones
        // then mark canvas as ready
        setTimeout(() => {
          setIsCanvasReady(true);
        }, 300);
      });
    } else if (!isLoadingAreas && areas && areas.length === 0) {
      // No areas - still mark as ready
      setIsCanvasReady(true);
    }
  }, [areas, didAutoFit, isLoadingAreas]);

  // Calculate statistics
  const totalSeats = seats.length;
  const activeSeats = seats.filter(s => s.isActive !== false).length;
  const totalZones = zones.length;

  // Handle Save - batch save all changes to API
  const handleSave = async () => {
    if (!hasUnsavedChanges || isSaving) return;

    dispatch({ type: actions.SET_SAVING, payload: true });

    try {
      const { pendingChanges } = state;
      const { createZone, createAreaFactoryInArea, updateZonePositionAndDimensions, updateAreaFactory, deleteSeat, updateSeat, createSeat } = await import('../../../services/admin/area_management/api');

      // 1. Create new zones FIRST (pending with negative IDs)
      // Zones must be created before seats that belong to them
      const zoneIdMapping = new Map(); // tempId -> realId
      const newZonePromises = (pendingChanges?.newZones || []).map(async (zone) => {
        try {
          // Get current zone data from UI state (user may have changed name/position after creating)
          const currentZone = zones.find(z => z.zoneId === zone.zoneId);
          const finalZoneName = currentZone?.zoneName ?? zone.zoneName;
          const finalColor = currentZone?.color ?? zone.color;

          // Create zone with initial values
          const res = await createZone({
            zoneName: finalZoneName,
            areaId: zone.areaId,
            positionX: 0,  // Default, will update with actual position below
            positionY: 0,
            width: 120,
            height: 100,
            color: finalColor,
          });
          const realId = res.data?.zone_id ?? res.data?.zoneId;

          // Store mapping for seats that reference this zone
          zoneIdMapping.set(zone.zoneId, realId);

          // Get current position/size from UI state
          const finalPositionX = currentZone?.positionX ?? zone.positionX ?? 0;
          const finalPositionY = currentZone?.positionY ?? zone.positionY ?? 0;
          const finalWidth = currentZone?.width ?? zone.width ?? 120;
          const finalHeight = currentZone?.height ?? zone.height ?? 100;

          // Update position/size in BE with user's actual values
          await updateZonePositionAndDimensions(realId, {
            positionX: Math.round(finalPositionX),
            positionY: Math.round(finalPositionY),
            width: Math.round(finalWidth),
            height: Math.round(finalHeight),
          });

          // Replace temp zone with real zone (using temp ID to find it)
          dispatch({
            type: actions.REPLACE_ZONE_BY_TEMP_ID,
            payload: {
              tempId: zone.zoneId,
              realZone: {
                ...zone,
                zoneId: realId,
                positionX: finalPositionX,
                positionY: finalPositionY,
                width: finalWidth,
                height: finalHeight,
                isPending: false,
              },
            },
          });
          return { tempId: zone.zoneId, realId };
        } catch (e) {
          console.error(`Failed to create zone ${zone.zoneName}:`, e);
          return null;
        }
      });

      // 2. Create new factories (pending with negative IDs) - can run in parallel with zones
      const newFactoryPromises = (pendingChanges?.newFactories || []).map(async (factory) => {
        try {
          // Get current factory data from UI state (user may have changed name/position after creating)
          const currentFactory = factories.find(f => f.factoryId === factory.factoryId);
          const finalFactoryName = currentFactory?.factoryName ?? factory.factoryName;
          const finalColor = currentFactory?.color ?? factory.color;

          // Create factory with initial values
          const res = await createAreaFactoryInArea(factory.areaId, {
            factoryName: finalFactoryName,
            positionX: 0,  // Default, will update with actual position below
            positionY: 0,
            width: 120,
            height: 80,
            color: finalColor,
          });
          const realId = res.data?.factory_id ?? res.data?.factoryId;

          // Get current position/size from UI state
          const finalPositionX = currentFactory?.positionX ?? factory.positionX ?? 0;
          const finalPositionY = currentFactory?.positionY ?? factory.positionY ?? 0;
          const finalWidth = currentFactory?.width ?? factory.width ?? 120;
          const finalHeight = currentFactory?.height ?? factory.height ?? 80;

          // Update position/size in BE with user's actual values
          await updateAreaFactory(realId, {
            factoryId: realId,
            factoryName: finalFactoryName,
            positionX: Math.round(finalPositionX),
            positionY: Math.round(finalPositionY),
            width: Math.round(finalWidth),
            height: Math.round(finalHeight),
            color: finalColor,
            areaId: factory.areaId,
          });

          // Replace temp factory with real factory (using temp ID to find it)
          dispatch({
            type: actions.REPLACE_FACTORY_BY_TEMP_ID,
            payload: {
              tempId: factory.factoryId,
              realFactory: {
                ...factory,
                factoryId: realId,
                positionX: finalPositionX,
                positionY: finalPositionY,
                width: finalWidth,
                height: finalHeight,
                isPending: false,
              },
            },
          });
          return { tempId: factory.factoryId, realId };
        } catch (e) {
          console.error(`Failed to create factory ${factory.factoryName}:`, e);
          return null;
        }
      });

      // Wait for zones and factories to be created first
      await Promise.all([...newZonePromises, ...newFactoryPromises]);

      // 3. Create new seats AFTER zones (so we can map pending zoneIds to real IDs)
      const newSeatPromises = (pendingChanges?.newSeats || []).map(async (seat) => {
        try {
          // Map zoneId: if seat belongs to a pending zone, use the real ID
          const realZoneId = seat.zoneId < 0 ? zoneIdMapping.get(seat.zoneId) : seat.zoneId;

          if (!realZoneId) {
            console.error(`Cannot create seat ${seat.seatCode}: zoneId ${seat.zoneId} not found in mapping`);
            return null;
          }

          const res = await createSeat({
            seatCode: seat.seatCode,
            zoneId: realZoneId,  // Use mapped real zoneId
            rowNumber: seat.rowNumber,
            columnNumber: seat.columnNumber,
            seatStatus: seat.seatStatus || 'AVAILABLE',
          });
          // Build real seat from API response
          const realSeat = {
            seatId: res.data?.seat_id ?? res.data?.seatId,
            seatCode: res.data?.seat_code ?? res.data?.seatCode ?? seat.seatCode,
            zoneId: res.data?.zone_id ?? res.data?.zoneId ?? realZoneId,
            rowNumber: res.data?.row_number ?? res.data?.rowNumber ?? seat.rowNumber,
            columnNumber: res.data?.column_number ?? res.data?.columnNumber ?? seat.columnNumber,
            seatStatus: res.data?.seat_status ?? res.data?.seatStatus ?? seat.seatStatus ?? 'AVAILABLE',
          };
          // Replace temp seat with real seat
          dispatch({
            type: actions.REPLACE_SEAT_BY_TEMP_ID,
            payload: { tempId: seat.seatId, realSeat },
          });
          return { tempId: seat.seatId, realId: realSeat.seatId };
        } catch (e) {
          console.error(`Failed to create seat ${seat.seatCode}:`, e);
          return null;
        }
      });

      // Wait for seats to be created
      await Promise.all(newSeatPromises);

      // 3. Update existing zones (those with positive IDs)
      const { updateZone } = await import('../../../services/admin/area_management/api');
      const existingZones = zones.filter(z => z.zoneId > 0 && !z.isPending);
      const updateZonePromises = existingZones.map(zone =>
        updateZone(zone.zoneId, {
          zoneName: zone.zoneName,
          positionX: Math.round(zone.positionX || 0),
          positionY: Math.round(zone.positionY || 0),
          width: Math.round(zone.width || 250),
          height: Math.round(zone.height || 200),
          color: zone.color,
        }).catch(e => console.error(`Failed to update zone ${zone.zoneId}:`, e))
      );

      // 4. Update existing factories (those with positive IDs)
      const existingFactories = factories.filter(f => f.factoryId > 0 && !f.isPending);
      const updateFactoryPromises = existingFactories.map(factory =>
        updateAreaFactory(factory.factoryId, {
          factoryId: factory.factoryId,
          factoryName: factory.factoryName,
          positionX: factory.positionX,
          positionY: factory.positionY,
          width: factory.width,
          height: factory.height,
          color: factory.color,
          areaId: factory.areaId,
        }).catch(e => console.error(`Failed to update factory ${factory.factoryId}:`, e))
      );

      // 5. Delete pending deleted seats
      console.log('[SAVE] Pending deleted seats:', pendingChanges?.deletedSeats);
      const deletePromises = (pendingChanges?.deletedSeats || []).map(seatId =>
        deleteSeat(seatId).catch(e => console.error(`Failed to delete seat ${seatId}:`, e))
      );

      // 6. Delete zones marked for deletion
      const { deleteZone, deleteAreaFactory } = await import('../../../services/admin/area_management/api');
      console.log('[SAVE] Pending deleted zones:', pendingChanges?.deletedZones);
      const deleteZonePromises = (pendingChanges?.deletedZones || []).map(zoneId =>
        deleteZone(zoneId).catch(e => console.error(`Failed to delete zone ${zoneId}:`, e))
      );

      // 7. Delete factories marked for deletion
      const deleteFactoryPromises = (pendingChanges?.deletedFactories || []).map(factoryId =>
        deleteAreaFactory(factoryId).catch(e => console.error(`Failed to delete factory ${factoryId}:`, e))
      );

      // 6. Update pending updated seats
      const updateSeatPromises = (pendingChanges?.updatedSeats || []).map(seat =>
        updateSeat(seat.seatId, {
          seatId: seat.seatId,
          seatCode: seat.seatCode,
          columnNumber: seat.columnNumber,
          positionX: seat.positionX,
          rowNumber: seat.rowNumber,
        }).catch(e => console.error(`Failed to update seat ${seat.seatId}:`, e))
      );

      await Promise.all([
        ...updateZonePromises,
        ...updateFactoryPromises,
        ...deletePromises,
        ...deleteZonePromises,
        ...deleteFactoryPromises,
        ...updateSeatPromises,
      ]);

      // Clear position cache after successful save
      clearAllPositionCache();
      dispatch({ type: actions.MARK_SAVED });
      console.log('All changes saved successfully');
    } catch (error) {
      console.error('Failed to save:', error);
      toast.error('Lưu thất bại: ' + (error.response?.data?.message || error.message));
      dispatch({ type: actions.SET_SAVING, payload: false });
    }
  };

  // Store handleSave in ref for keyboard shortcut access
  handleSaveRef.current = handleSave;

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
          <span><strong>{activeSeats}/{totalSeats}</strong> Ghế</span>
        </div>

        {/* Right: Tools */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {/* Pan Mode - only show in edit mode */}
          {!isPreviewMode && (
            <button
              onClick={() => setPanMode(!panMode)}
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

          {/* Save Button */}
          <button
            onClick={handleSave}
            disabled={!hasUnsavedChanges || isSaving}
            title={hasUnsavedChanges ? "Lưu thay đổi" : "Không có thay đổi"}
            style={{
              padding: '8px 16px',
              borderRadius: '10px',
              border: hasUnsavedChanges ? '2px solid #22C55E' : '2px solid #E2E8F0',
              background: hasUnsavedChanges
                ? 'linear-gradient(135deg, #22C55E 0%, #16A34A 100%)'
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
            {isSaving ? 'Đang lưu...' : hasUnsavedChanges ? 'Lưu' : 'Đã lưu'}
          </button>
        </div>
      </div>

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

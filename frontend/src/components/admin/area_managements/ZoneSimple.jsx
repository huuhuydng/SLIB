import { useEffect, useRef, useState } from 'react';
import { useLayout } from '../../../context/admin/area_management/LayoutContext';
import Seat from './Seat';
import { getSeats, updateZonePosition, updateZoneDimensions, updateZonePositionAndDimensions } from '../../../services/admin/area_management/api';
import { calculateDynamicSeatLayout, calculateMinZoneDimensions } from '../../../utils/admin/seatLayout';
import { Rnd } from 'react-rnd';

function ZoneSimple({ zone, area }) {
  const { state, dispatch, actions } = useLayout();
  const { selectedItem, selectedItems, seats, canvas, isPreviewMode } = state;
  const zoneSeats = (seats || []).filter((s) => String(s.zoneId) === String(zone.zoneId));

  // Check if this zone is selected (either single or multi-select)
  const isSelected = (selectedItem?.type === 'zone' && selectedItem?.id === zone.zoneId) ||
    (selectedItems || []).some(item => item.type === 'zone' && item.id === zone.zoneId);

  // Delay rendering until after initial render cycle to ensure position and zoom are stable
  // react-rnd has issues reading controlled position correctly when scale changes on mount
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    // Wait for canvas zoom to stabilize after handleFitToView
    const timer = setTimeout(() => {
      setIsReady(true);
    }, 350);
    return () => clearTimeout(timer);
  }, []);



  // Load seats for this zone
  useEffect(() => {
    if (!zone?.zoneId) return;
    (async () => {
      try {
        const res = await getSeats(zone.zoneId);
        const seatsWithDefaults = (res.data || []).map(seat => ({
          ...seat,
          positionY: seat.positionY !== null ? seat.positionY : 0,
        }));
        dispatch({
          type: actions?.MERGE_SEATS || "MERGE_SEATS",
          payload: {
            zoneId: zone?.zoneId,
            seats: seatsWithDefaults,
          },
        });
      } catch (e) {
        console.error('Failed to load seats for zone', zone.zoneId, e);
      }
    })();
  }, [zone?.zoneId, dispatch, actions]);

  const saveTimerRef = useRef(null);
  const dragStartPos = useRef({ x: 0, y: 0 }); // Track start position for multi-select drag
  const resizeStartSize = useRef({ width: 0, height: 0, x: 0, y: 0 }); // Track original size for resize
  const [collidingWith, setCollidingWith] = useState(null);
  const [resetKey, setResetKey] = useState(0);

  // Helper: Check if two rectangles overlap (with small padding to allow touching)
  const isColliding = (rect1, rect2, padding = 0) => {
    return (
      rect1.left < rect2.right - padding &&
      rect1.right + padding > rect2.left &&
      rect1.top < rect2.bottom - padding &&
      rect1.bottom + padding > rect2.top
    );
  };

  // Helper: Check if zone collides with any other zone and return collision info
  const getCollisionInfo = (zoneId, newX, newY, newWidth, newHeight) => {
    const newRect = {
      left: newX,
      right: newX + newWidth,
      top: newY,
      bottom: newY + newHeight,
    };

    // Check against factories in same area
    for (const factory of (state.factories || [])) {
      if (String(factory.areaId) !== String(area?.areaId)) continue;

      const factoryRect = {
        left: factory.positionX || 0,
        right: (factory.positionX || 0) + (factory.width || 160),
        top: factory.positionY || 0,
        bottom: (factory.positionY || 0) + (factory.height || 120),
      };

      if (isColliding(newRect, factoryRect, -2)) {
        console.warn(`Chồng lấp: "${zone.zoneName}" chồng lấp với Factory "${factory.factoryName}"`);
        return { hasCollision: true, collidingZone: { ...factory, isFactory: true } };
      }
    }

    // Check against other zones in same area
    for (const otherZone of (state.zones || [])) {
      if (otherZone.zoneId === zoneId) continue;
      if (String(otherZone.areaId) !== String(area?.areaId)) continue;

      const otherRect = {
        left: otherZone.positionX || 0,
        right: (otherZone.positionX || 0) + (otherZone.width || 120),
        top: otherZone.positionY || 0,
        bottom: (otherZone.positionY || 0) + (otherZone.height || 100),
      };

      if (isColliding(newRect, otherRect, -2)) {
        console.warn(`Chồng lấp: "${zone.zoneName}" chồng lấp với "${otherZone.zoneName}"`);
        return { hasCollision: true, collidingZone: otherZone };
      }
    }
    return { hasCollision: false, collidingZone: null };
  };

  const handleZoneClick = (e) => {
    e.stopPropagation();
    const isMac = navigator.platform.toUpperCase().includes('MAC');
    const isMultiSelectKey = isMac ? e.metaKey : e.ctrlKey;

    if (isMultiSelectKey) {
      // Ctrl+Click: Toggle this zone in/out of selection
      dispatch({
        type: actions.TOGGLE_SELECT,
        payload: { type: 'zone', id: zone.zoneId },
      });
    } else if (isSelected) {
      // Clicking on already selected item - keep selection, just update selectedItem
      dispatch({
        type: actions.SELECT_ITEM,
        payload: { type: 'zone', id: zone.zoneId },
      });
    } else {
      // Normal click on non-selected item: Select only this zone, clear others
      dispatch({ type: actions.DESELECT });
      dispatch({
        type: actions.SELECT_ITEM,
        payload: { type: 'zone', id: zone.zoneId },
      });
    }
  };

  // Push history when starting drag/resize so user can undo
  const handleDragStart = (e, d) => {
    dispatch({ type: actions.PUSH_HISTORY });
    // Save start position for multi-select drag delta calculation
    dragStartPos.current = { x: zone.positionX || 0, y: zone.positionY || 0 };
  };

  const handleResizeStart = () => {
    dispatch({ type: actions.PUSH_HISTORY });
    // Save original size to compare in resizeStop
    resizeStartSize.current = {
      width: zone.width || 120,
      height: zone.height || 100,
      x: zone.positionX || 0,
      y: zone.positionY || 0,
    };
  };

  const handleDrag = (e, d) => {
    // Check for collision before allowing drag
    const { hasCollision, collidingZone } = getCollisionInfo(zone.zoneId, d.x, d.y, zone.width || 120, zone.height || 100);

    if (hasCollision) {
      console.warn(`Chồng lấp: "${zone.zoneName}" chồng lấp với "${collidingZone.zoneName}"`);
      setCollidingWith(collidingZone);
      return; // Block the drag
    }

    setCollidingWith(null);

    // Check if this zone is part of multi-select
    const isMultiSelected = (selectedItems || []).length > 1 &&
      (selectedItems || []).some(item => item.type === 'zone' && item.id === zone.zoneId);

    if (isMultiSelected) {
      // Calculate delta from last known position
      const dx = d.x - (zone.positionX || 0);
      const dy = d.y - (zone.positionY || 0);

      if (dx !== 0 || dy !== 0) {
        // Move all selected items by delta (realtime)
        dispatch({ type: actions.MOVE_ALL_SELECTED, payload: { dx, dy } });
      }
    } else {
      // Update only this zone
      dispatch({
        type: actions.UPDATE_ZONE,
        payload: {
          ...zone,
          positionX: d.x,
          positionY: d.y,
        },
      });
    }

    // Mark as having unsaved changes
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
  };

  const handleDragStop = (e, d) => {
    // Check if position actually changed (user dragged, not just clicked)
    const startX = dragStartPos.current.x;
    const startY = dragStartPos.current.y;
    const hasPositionChanged = Math.abs(d.x - startX) > 1 || Math.abs(d.y - startY) > 1;

    // If no actual movement, do nothing (prevents cache pollution on click)
    if (!hasPositionChanged) {
      return;
    }

    // Final collision check
    const { hasCollision, collidingZone } = getCollisionInfo(zone.zoneId, d.x, d.y, zone.width || 120, zone.height || 100);

    if (hasCollision) {
      console.warn(`Vị trí bị từ chối: "${zone.zoneName}" chồng lấp`);
      setCollidingWith(collidingZone);
      setResetKey(prev => prev + 1);
      // Clear warning after reset animation
      setTimeout(() => setCollidingWith(null), 300);
      return;
    }

    setCollidingWith(null);

    // Check if this zone is part of multi-select
    const isMultiSelected = (selectedItems || []).length > 1 &&
      (selectedItems || []).some(item => item.type === 'zone' && item.id === zone.zoneId);

    // For single selection, update the zone position 
    // (for multi-select, realtime drag already updated all positions)
    if (!isMultiSelected) {
      dispatch({
        type: actions.UPDATE_ZONE,
        payload: {
          ...zone,
          positionX: d.x,
          positionY: d.y,
        },
      });
    }

    // Cache position for instant restore on reload (only when position changed)
    // REMOVED: cacheZonePosition - don't cache unsaved changes to localStorage
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
  };

  // Handle resize in real-time (updates zone dimensions while dragging)
  const handleResize = (e, direction, ref, delta, position) => {
    const newWidth = parseInt(ref.style.width);
    const newHeight = parseInt(ref.style.height);

    // Update zone dimensions immediately for real-time seat repositioning
    dispatch({
      type: actions.UPDATE_ZONE,
      payload: {
        ...zone,
        positionX: position.x,
        positionY: position.y,
        width: newWidth,
        height: newHeight,
      },
    });
  };

  const handleResizeStop = (e, direction, ref, delta, position) => {
    const newWidth = parseInt(ref.style.width);
    const newHeight = parseInt(ref.style.height);

    // Check if size or position actually changed (compare with ORIGINAL size, not current state)
    const originalSize = resizeStartSize.current;
    const hasSizeChanged = Math.abs(newWidth - originalSize.width) > 1 ||
      Math.abs(newHeight - originalSize.height) > 1;
    const hasPositionChanged = Math.abs(position.x - originalSize.x) > 1 ||
      Math.abs(position.y - originalSize.y) > 1;

    // If no actual change, do nothing
    if (!hasSizeChanged && !hasPositionChanged) {
      return;
    }

    // Check for collision with new size/position
    const { hasCollision, collidingZone } = getCollisionInfo(zone.zoneId, position.x, position.y, newWidth, newHeight);

    if (hasCollision) {
      console.warn(`Chồng lấp: "${zone.zoneName}"`);
      setCollidingWith(collidingZone);
      setResetKey(prev => prev + 1);
      // Clear warning after reset animation
      setTimeout(() => setCollidingWith(null), 300);
      return;
    }

    setCollidingWith(null);

    // Update local state only (no API call)
    dispatch({
      type: actions.UPDATE_ZONE,
      payload: {
        ...zone,
        positionX: position.x,
        positionY: position.y,
        width: newWidth,
        height: newHeight,
      },
    });
    // REMOVED: cacheZonePosition - don't cache unsaved changes to localStorage
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
  };

  // Don't render until we're ready (position data is stable)
  if (!isReady) {
    return null;
  }

  return (
    <Rnd
      key={`zone-${zone.zoneId}-${resetKey}`}
      scale={canvas?.zoom || 1}
      position={{
        x: zone.positionX || 0,
        y: zone.positionY || 0,
      }}
      size={{
        width: zone.width || 120,
        height: zone.height || 100,
      }}
      onDragStart={(zone.isLocked || isPreviewMode || area?.locked) ? undefined : handleDragStart}
      onDrag={(zone.isLocked || isPreviewMode || area?.locked) ? undefined : handleDrag}
      onDragStop={(zone.isLocked || isPreviewMode || area?.locked) ? undefined : handleDragStop}
      onResizeStart={(zone.isLocked || isPreviewMode || area?.locked) ? undefined : handleResizeStart}
      onResize={(zone.isLocked || isPreviewMode || area?.locked) ? undefined : handleResize}
      onResizeStop={(zone.isLocked || isPreviewMode || area?.locked) ? undefined : handleResizeStop}
      disableDragging={!!zone.isLocked || isPreviewMode || !!area?.locked}
      enableResizing={!zone.isLocked && !isPreviewMode && !area?.locked}
      bounds="parent"
      minWidth={calculateMinZoneDimensions(zoneSeats).minWidth}
      minHeight={calculateMinZoneDimensions(zoneSeats).minHeight}
      style={{
        zIndex: isSelected ? 100 : (zone.isPending ? 50 : 1),
        cursor: isPreviewMode ? 'default' : ((zone.isLocked || area?.locked) ? 'not-allowed' : 'move'),
      }}
    >
      <div
        className={`zone-card ${isSelected ? 'selected' : ''}`}
        style={{
          width: '100%',
          height: '100%',
          border: collidingWith
            ? '3px solid #dc2626'
            : isSelected
              ? '3px solid #1976d2'
              : zone.isLocked
                ? '2px solid #fca5a5'
                : '1px solid #9CA3AF',
          borderRadius: '8px',
          padding: '8px',
          boxSizing: 'border-box',
          cursor: 'pointer',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: collidingWith
            ? '0 0 15px rgba(220, 38, 38, 0.6), inset 0 0 10px rgba(220, 38, 38, 0.1)'
            : zone.isLocked
              ? '0 0 10px rgba(220, 38, 38, 0.2)'
              : 'none',
          transition: 'all 0.2s ease',
          backgroundColor: collidingWith ? '#fee8e8' : '#E5E7EB',
        }}
        onClick={handleZoneClick}
      >
        {/* Collision Warning */}
        {collidingWith && (
          <div style={{
            position: 'absolute',
            top: '4px',
            right: '4px',
            backgroundColor: '#dc2626',
            color: 'white',
            padding: '4px 8px',
            borderRadius: '4px',
            fontSize: '11px',
            fontWeight: '600',
            zIndex: 10,
            maxWidth: '90px',
            textAlign: 'center',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}>
            ⚠️ Chồng lấp
          </div>
        )}

        <div style={{
          fontSize: '12px',
          fontWeight: '600',
          marginBottom: '4px',
          color: collidingWith ? '#991b1b' : '#06361a',
        }}>
          {zone.zoneName || 'Unnamed Zone'}
        </div>



        <div style={{
          flex: 1,
          overflow: 'hidden',
          position: 'relative',
          minHeight: '100px',
        }}>
          {zoneSeats.map((seat) => {
            const layout = calculateDynamicSeatLayout(seat, zone.width || 120, zone.height || 100, zoneSeats);

            return (
              <div
                key={seat.seatId}
                style={{
                  position: 'absolute',
                  left: layout.positionX,
                  top: layout.positionY,
                  width: layout.width,
                  height: layout.height,
                  transition: 'left 0.15s ease-out, top 0.15s ease-out',
                }}
              >
                <Seat seat={seat} zone={zone} />
              </div>
            );
          })}
        </div>

        <div style={{
          fontSize: '10px',
          color: collidingWith ? '#991b1b' : '#666',
          marginTop: '4px',
        }}>
          {zoneSeats.length} seats
        </div>
      </div>
    </Rnd>
  );
}

export default ZoneSimple;

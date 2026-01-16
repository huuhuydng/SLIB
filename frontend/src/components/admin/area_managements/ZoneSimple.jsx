import { useEffect, useRef, useState } from 'react';
import { useLayout } from '../../../context/admin/area_management/LayoutContext';
import Seat from './Seat';
import { getSeats, updateZonePosition, updateZoneDimensions, updateZonePositionAndDimensions } from '../../../services/admin/area_management/api';
import { calculateSeatLayout, calculateMinZoneDimensions } from '../../../utils/admin/seatLayout';
import { Rnd } from 'react-rnd';

function ZoneSimple({ zone, area }) {
  const { state, dispatch, actions } = useLayout();
  const { selectedItem, seats, canvas } = state;
  const zoneSeats = (seats || []).filter((s) => String(s.zoneId) === String(zone.zoneId));
  
  console.log(`🔍 ZoneSimple ${zone.zoneId} - zoneSeats count:`, zoneSeats.length, zoneSeats.map(s => ({ id: s.seatId, code: s.seatCode, row: s.rowNumber, col: s.columnNumber })));

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

  const isSelected = selectedItem?.type === 'zone' && selectedItem?.id === zone.zoneId;

  const saveTimerRef = useRef(null);
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
        console.warn(`❌ Chồng lấp: "${zone.zoneName}" chồng lấp với Factory "${factory.factoryName}"`);
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
        console.warn(`❌ Chồng lấp: "${zone.zoneName}" chồng lấp với "${otherZone.zoneName}"`);
        return { hasCollision: true, collidingZone: otherZone };
      }
    }
    return { hasCollision: false, collidingZone: null };
  };

  const handleZoneClick = (e) => {
    e.stopPropagation();
    dispatch({
      type: actions.SELECT_ITEM,
      payload: { type: 'zone', id: zone.zoneId },
    });
  };

  const handleDrag = (e, d) => {
    // Check for collision before allowing drag
    const { hasCollision, collidingZone } = getCollisionInfo(zone.zoneId, d.x, d.y, zone.width || 120, zone.height || 100);
    
    if (hasCollision) {
      console.warn(`❌ Chồng lấp: "${zone.zoneName}" chồng lấp với "${collidingZone.zoneName}"`);
      setCollidingWith(collidingZone);
      return; // Block the drag
    }

    setCollidingWith(null);

    // Update UI immediately
    dispatch({
      type: actions.UPDATE_ZONE,
      payload: {
        ...zone,
        positionX: d.x,
        positionY: d.y,
      },
    });

    // Debounce API call
    if (saveTimerRef.current) {
      clearTimeout(saveTimerRef.current);
    }
    saveTimerRef.current = setTimeout(async () => {
      try {
        await updateZonePosition(zone.zoneId, d.x, d.y);
      } catch (e) {
        console.error('Failed to update zone position', e);
      }
    }, 300);
  };

  const handleDragStop = async (e, d) => {
    // Final collision check before saving
    const { hasCollision, collidingZone } = getCollisionInfo(zone.zoneId, d.x, d.y, zone.width || 120, zone.height || 100);
    
    if (hasCollision) {
      console.warn(`❌ Vị trí bị từ chối: "${zone.zoneName}" chồng lấp với "${collidingZone.zoneName}"`);
      setCollidingWith(collidingZone);
      // Force reset to original position by changing key
      setResetKey(prev => prev + 1);
      return;
    }

    setCollidingWith(null);

    // Clear debounce timer
    if (saveTimerRef.current) {
      clearTimeout(saveTimerRef.current);
    }
    // Save immediately on stop
    try {
      await updateZonePosition(zone.zoneId, d.x, d.y);
      dispatch({
        type: actions.UPDATE_ZONE,
        payload: {
          ...zone,
          positionX: d.x,
          positionY: d.y,
        },
      });
    } catch (e) {
      console.error('Failed to update zone position', e);
    }
  };

  const handleResizeStop = async (e, direction, ref, delta, position) => {
    const newWidth = parseInt(ref.style.width);
    const newHeight = parseInt(ref.style.height);
    const positionChanged = position.x !== zone.positionX || position.y !== zone.positionY;
    const dimensionsChanged = newWidth !== zone.width || newHeight !== zone.height;

    // Check for collision with new size/position
    const { hasCollision, collidingZone } = getCollisionInfo(zone.zoneId, position.x, position.y, newWidth, newHeight);
    
    if (hasCollision) {
      console.warn(`❌ Chồng lấp: "${zone.zoneName}" chồng lấp với "${collidingZone.zoneName}"`);
      setCollidingWith(collidingZone);
      setResetKey(prev => prev + 1); // Reset to original position
      return; // Block the resize
    }

    setCollidingWith(null);

    try {
      // If both position and dimensions changed, update both
      if (positionChanged && dimensionsChanged) {
        await updateZonePositionAndDimensions(zone.zoneId, {
          positionX: position.x,
          positionY: position.y,
          width: newWidth,
          height: newHeight,
        });
      }
      // If only dimensions changed, update dimensions only
      else if (dimensionsChanged) {
        await updateZoneDimensions(zone.zoneId, newWidth, newHeight);
      }
      // If only position changed (shouldn't happen in resize, but handle it)
      else if (positionChanged) {
        await updateZonePosition(zone.zoneId, position.x, position.y);
      }

      // Update local state
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
    } catch (e) {
      console.error('Failed to update zone size/position', e);
    }
  };

  return (
    <Rnd
      key={`zone-${zone.zoneId}-${resetKey}`}
      scale={canvas?.zoom || 1}
      default={{
        x: zone.positionX || 0,
        y: zone.positionY || 0,
        width: zone.width || 120,
        height: zone.height || 100,
      }}
      onDrag={zone.isLocked ? undefined : handleDrag}
      onDragStop={zone.isLocked ? undefined : handleDragStop}
      onResizeStop={zone.isLocked ? undefined : handleResizeStop}
      disableDragging={!!zone.isLocked}
      bounds="parent"
      minWidth={calculateMinZoneDimensions(zoneSeats).minWidth}
      minHeight={calculateMinZoneDimensions(zoneSeats).minHeight}
      style={{
        zIndex: isSelected ? 5 : 1,
        cursor: zone.isLocked ? 'not-allowed' : 'move',
      }}
    >
      <div
        className={`zone-card ${isSelected ? 'selected' : ''}`}
        style={{
          width: '100%',
          height: '100%',
          backgroundColor: zone.color || '#d1f7d8',
          border: collidingWith
            ? '3px solid #dc2626'
            : isSelected 
            ? '3px solid #1976d2' 
            : zone.isLocked 
            ? '2px solid #fca5a5' 
            : '1px solid #34a853',
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
          backgroundColor: collidingWith ? '#fee8e8' : zone.color || '#d1f7d8',
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

      {/* Collision Info */}
      {collidingWith && (
        <div style={{
          fontSize: '10px',
          color: '#991b1b',
          padding: '4px',
          backgroundColor: '#fecaca',
          borderRadius: '4px',
          marginBottom: '4px',
          fontWeight: '500',
        }}>
          🔴 Chồng lấp với: {collidingWith.zoneName}
        </div>
      )}
      
      <div style={{
        flex: 1,
        overflow: 'hidden',
        position: 'relative',
        minHeight: '100px',
      }}>
        {zoneSeats.map((seat) => {
          const layout = calculateSeatLayout(seat);
          console.log(`📐 [ZoneSimple] Seat ${seat.seatId}: layout =`, layout);
          
          return (
            <div
              key={seat.seatId}
              style={{
                position: 'absolute',
                left: layout.positionX,
                top: layout.positionY - 40, // Trừ headerHeight vì container không phải full zone
                width: layout.width,
                height: layout.height,
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

import { useRef, useState } from 'react';
import { useLayout } from '../../../context/admin/area_management/LayoutContext';
import { updateAreaFactory, dragAreaFactory, resizeAreaFactory, deleteAreaFactory } from '../../../services/admin/area_management/api';
import { Rnd } from 'react-rnd';

function Factory({ factory }) {
  const { state, dispatch, actions } = useLayout();
  const { selectedItem, factories, zones, canvas, isPreviewMode } = state;

  const isSelected = selectedItem?.type === 'factory' && selectedItem?.id === factory.factoryId;
  const saveTimerRef = useRef(null);
  const [collidingWith, setCollidingWith] = useState(null);
  const [resetKey, setResetKey] = useState(0);

  // Helper: Check if two rectangles overlap
  const isColliding = (rect1, rect2, padding = 0) => {
    return (
      rect1.left < rect2.right - padding &&
      rect1.right + padding > rect2.left &&
      rect1.top < rect2.bottom - padding &&
      rect1.bottom + padding > rect2.top
    );
  };

  // Helper: Check if factory collides with any other factory in same area
  const getCollisionInfo = (factoryId, newX, newY, newWidth, newHeight) => {
    const newRect = {
      left: newX,
      right: newX + newWidth,
      top: newY,
      bottom: newY + newHeight,
    };

    // Check against other factories
    for (const otherFactory of (factories || [])) {
      if (otherFactory.factoryId === factoryId) continue;
      if (String(otherFactory.areaId) !== String(factory?.areaId)) continue;

      const otherRect = {
        left: otherFactory.positionX || 0,
        right: (otherFactory.positionX || 0) + (otherFactory.width || 160),
        top: otherFactory.positionY || 0,
        bottom: (otherFactory.positionY || 0) + (otherFactory.height || 120),
      };

      if (isColliding(newRect, otherRect, -2)) {
        return { hasCollision: true, collidingFactory: otherFactory };
      }
    }

    // Check against zones in same area
    for (const zone of (zones || [])) {
      if (String(zone.areaId) !== String(factory?.areaId)) continue;

      const zoneRect = {
        left: zone.positionX || 0,
        right: (zone.positionX || 0) + (zone.width || 200),
        top: zone.positionY || 0,
        bottom: (zone.positionY || 0) + (zone.height || 150),
      };

      if (isColliding(newRect, zoneRect, -2)) {
        return { hasCollision: true, collidingFactory: { factoryName: `Zone: ${zone.zoneName}`, isZone: true } };
      }
    }

    return { hasCollision: false, collidingFactory: null };
  };

  const handleSelectFactory = (e) => {
    if (e) e.stopPropagation();
    dispatch({
      type: actions.SELECT_ITEM,
      payload: {
        type: 'factory',
        id: factory.factoryId,
      },
    });
  };

  const handleDrag = (e, d) => {
    // Check for collision
    const { hasCollision, collidingFactory } = getCollisionInfo(
      factory.factoryId,
      d.x,
      d.y,
      factory.width || 160,
      factory.height || 120
    );

    if (hasCollision) {
      console.warn(`❌ Chồng lấp: "${factory.factoryName}" chồng lấp với factory khác`);
      setCollidingWith(collidingFactory);
      return;
    }

    setCollidingWith(null);

    // Update UI immediately
    dispatch({
      type: actions.UPDATE_FACTORY,
      payload: {
        ...factory,
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
        await dragAreaFactory(factory.factoryId, d.x, d.y);
      } catch (e) {
        console.error('Failed to drag factory', e);
      }
    }, 300);
  };

  const handleDragStop = async (e, d) => {
    console.log('🔵 handleDragStop called:', {
      factoryId: factory.factoryId,
      oldPosition: { x: factory.positionX, y: factory.positionY },
      newPosition: { x: d.x, y: d.y }
    });

    // Final collision check
    const { hasCollision, collidingFactory } = getCollisionInfo(
      factory.factoryId,
      d.x,
      d.y,
      factory.width || 160,
      factory.height || 120
    );

    if (hasCollision) {
      console.warn(`❌ Vị trí bị từ chối: "${factory.factoryName}" chồng lấp`);
      setCollidingWith(collidingFactory);
      setResetKey(prev => prev + 1); // Reset to original position
      return;
    }

    setCollidingWith(null);

    // Clear timer
    if (saveTimerRef.current) {
      clearTimeout(saveTimerRef.current);
    }

    // Save immediately on stop
    try {
      console.log('📡 Calling dragAreaFactory API...');
      const response = await dragAreaFactory(factory.factoryId, d.x, d.y);
      console.log('✅ dragAreaFactory response:', response.data);

      dispatch({
        type: actions.UPDATE_FACTORY,
        payload: {
          ...factory,
          positionX: d.x,
          positionY: d.y,
        },
      });
      console.log('✅ State updated with new position');
    } catch (e) {
      console.error('❌ Failed to save factory position:', e);
      console.error('Error details:', e.response?.data);
    }
  };

  const handleResizeStop = async (e, direction, ref, delta, position) => {
    const newWidth = parseInt(ref.style.width);
    const newHeight = parseInt(ref.style.height);
    const positionChanged = position.x !== factory.positionX || position.y !== factory.positionY;
    const dimensionsChanged = newWidth !== factory.width || newHeight !== factory.height;

    console.log('🟣 handleResizeStop called:', {
      factoryId: factory.factoryId,
      oldPosition: { x: factory.positionX, y: factory.positionY },
      newPosition: { x: position.x, y: position.y },
      oldSize: { width: factory.width, height: factory.height },
      newSize: { width: newWidth, height: newHeight },
      positionChanged,
      dimensionsChanged
    });

    // Check for collision
    const { hasCollision, collidingFactory } = getCollisionInfo(
      factory.factoryId,
      position.x,
      position.y,
      newWidth,
      newHeight
    );

    if (hasCollision) {
      console.warn(`❌ Chồng lấp: resize bị từ chối`);
      setCollidingWith(collidingFactory);
      setResetKey(prev => prev + 1); // Reset to original position
      return;
    }

    setCollidingWith(null);

    try {
      // Handle position change separately
      if (positionChanged) {
        console.log('📡 Calling dragAreaFactory for position change...');
        const posRes = await dragAreaFactory(factory.factoryId, position.x, position.y);
        console.log('✅ dragAreaFactory response:', posRes.data);
      }

      // Handle dimension change separately
      if (dimensionsChanged) {
        console.log('📡 Calling resizeAreaFactory for dimension change...');
        const sizeRes = await resizeAreaFactory(factory.factoryId, newWidth, newHeight);
        console.log('✅ resizeAreaFactory response:', sizeRes.data);
      }

      // Update local state only if something changed
      if (positionChanged || dimensionsChanged) {
        dispatch({
          type: actions.UPDATE_FACTORY,
          payload: {
            ...factory,
            positionX: position.x,
            positionY: position.y,
            width: newWidth,
            height: newHeight,
          },
        });
        console.log('✅ State updated with new position/size');
      } else {
        console.log('ℹ️ No changes detected, skipping update');
      }
    } catch (e) {
      console.error('❌ Failed to update factory:', e);
      console.error('Error details:', e.response?.data);
    }
  };

  const handleDeleteFactory = async () => {
    if (confirm(`Delete factory "${factory.factoryName}"?`)) {
      try {
        // Only call API if factory has real ID (positive) - pending factories have negative tempId
        if (factory.factoryId > 0) {
          await deleteAreaFactory(factory.factoryId);
        }
        dispatch({
          type: actions.DELETE_FACTORY,
          payload: factory.factoryId,
        });
      } catch (e) {
        console.error('Failed to delete factory', e);
      }
    }
  };

  return (
    <>
      {collidingWith && (
        <div
          style={{
            position: 'absolute',
            top: factory.positionY - 30,
            left: factory.positionX,
            backgroundColor: '#dc2626',
            color: 'white',
            padding: '4px 8px',
            borderRadius: '3px',
            fontSize: '11px',
            whiteSpace: 'nowrap',
            zIndex: 200,
            pointerEvents: 'none',
          }}
        >
          Chồng lấp
        </div>
      )}

      <Rnd
        key={`factory-${factory.factoryId}-${resetKey}`}
        scale={canvas?.zoom || 1}
        default={{
          x: factory.positionX || 0,
          y: factory.positionY || 0,
          width: factory.width || 160,
          height: factory.height || 120,
        }}
        minWidth={80}
        minHeight={60}
        bounds="parent"
        disableDragging={isPreviewMode}
        enableResizing={isPreviewMode ? false : undefined}
        onDrag={isPreviewMode ? undefined : handleDrag}
        onDragStop={isPreviewMode ? undefined : handleDragStop}
        onResizeStop={isPreviewMode ? undefined : handleResizeStop}
        dragHandleClassName="factory-header"
        style={{
          position: 'absolute',
          boxSizing: 'border-box',
          zIndex: isSelected ? 100 : 10,
          pointerEvents: isPreviewMode ? 'none' : 'auto',
        }}
      >
        <div
          style={{
            width: '100%',
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: '#9CA3AF',  // Fixed gray for obstacles
            border: isSelected ? '3px solid #0066CC' : '2px solid #999',
            borderRadius: '6px',
            cursor: isPreviewMode ? 'default' : 'move',
            position: 'relative',
            overflow: 'hidden',
            pointerEvents: isPreviewMode ? 'none' : 'auto',
          }}
        >
          <div
            className="factory-header"
            style={{
              padding: '6px 8px',
              backgroundColor: 'rgba(0, 0, 0, 0.15)',
              borderBottom: '1px solid rgba(0, 0, 0, 0.2)',
              cursor: 'grab',
              fontSize: '12px',
              fontWeight: '600',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              userSelect: 'none',
            }}
            onClick={isPreviewMode ? undefined : handleSelectFactory}
          >
            <span title={factory.factoryName}>{factory.factoryName}</span>
          </div>
          <div
            onClick={isPreviewMode ? undefined : handleSelectFactory}
            style={{
              flex: 1,
              padding: '4px 8px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '11px',
              color: '#333',
              fontWeight: '500',
              cursor: 'pointer',
            }}
          >
          </div>
        </div>
      </Rnd>
    </>
  );
}

export default Factory;

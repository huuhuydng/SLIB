import { useEffect, useRef, useState } from 'react';
import { useLayout } from "../../../context/admin/area_management/LayoutContext";
import ZoneSimple from './ZoneSimple';
import Shape from './Shape';
import { getZonesByArea, getAreaFactoriesByArea } from '../../../services/admin/area_management/api';
import { Rnd } from 'react-rnd';
function Area({ area }) {
  const { state, dispatch, actions } = useLayout();
  const { zones, factories, selectedItem, canvas, isPreviewMode, isLayoutHydrated } = state;
  // Start with loading=true to prevent rendering before API data arrives
  const [loadingZones, setLoadingZones] = useState(true);
  const [loadingFactories, setLoadingFactories] = useState(true);

  // Load zones for this area
  useEffect(() => {
    if (!area?.areaId) {
      setLoadingZones(false);
      return;
    }

    if (area.areaId < 0) {
      setLoadingZones(false);
      return;
    }

    if (isLayoutHydrated) {
      setLoadingZones(false);
      return;
    }
    setLoadingZones(true);
    (async () => {
      try {
        const res = await getZonesByArea(area.areaId);
        const raw = Array.isArray(res?.data) ? res.data : [];
        const zonesNormalized = raw.map((z) => ({
          zoneId: z.zone_id ?? z.zoneId,
          zoneName: z.zone_name ?? z.zoneName,
          zoneDes: z.zone_des ?? z.zoneDes ?? '',
          areaId: z.area_id ?? z.areaId ?? area.areaId,
          positionX: z.position_x ?? z.positionX ?? 0,
          positionY: z.position_y ?? z.positionY ?? 0,
          width: z.width ?? 120,
          height: z.height ?? 100,
          color: z.color ?? '#d1f7d8',
          isLocked: z.is_locked ?? z.isLocked ?? false,
        }));
        // Use data directly from database (no localStorage cache override)
        dispatch({
          type: actions.MERGE_ZONES,
          payload: {
            areaId: area.areaId,
            zones: zonesNormalized,
          },
        });
      } catch (e) {
        console.error('Failed to load zones for area', area.areaId, e);
      } finally {
        setLoadingZones(false);
      }
    })();
  }, [area?.areaId, isLayoutHydrated]);

  // Load factories for this area
  useEffect(() => {
    if (!area?.areaId) {
      setLoadingFactories(false);
      return;
    }

    if (area.areaId < 0) {
      setLoadingFactories(false);
      return;
    }

    if (isLayoutHydrated) {
      setLoadingFactories(false);
      return;
    }
    setLoadingFactories(true);
    (async () => {
      try {
        const res = await getAreaFactoriesByArea(area.areaId);
        // Convert snake_case response to camelCase
        const convertedFactories = (res.data || []).map(f => {
          return {
            factoryId: f.factory_id ?? f.factoryId,
            factoryName: f.factory_name ?? f.factoryName,
            positionX: f.position_x ?? f.positionX ?? 0,
            positionY: f.position_y ?? f.positionY ?? 0,
            width: f.width ?? 120,
            height: f.height ?? 80,
            color: f.color ?? "#9CA3AF",
            areaId: f.area_id ?? f.areaId,
            isLocked: f.is_locked ?? f.isLocked ?? false,
          };
        });
        // Use data directly from database (no localStorage cache override)
        dispatch({
          type: actions.MERGE_FACTORIES,
          payload: {
            areaId: area.areaId,
            factories: convertedFactories,
          },
        });
      } catch (e) {
        console.error('Failed to load factories for area', area.areaId, e);
      } finally {
        setLoadingFactories(false);
      }
    })();
  }, [area?.areaId, isLayoutHydrated]);

  // Filter zones for this area
  const areaZones = zones.filter((z) => z.areaId === area.areaId);

  // Filter factories for this area
  const areaFactories = factories.filter((f) => f.areaId === area.areaId);
  const isSelected = selectedItem?.type === 'area' && selectedItem?.id === area.areaId;

  const [resizeError, setResizeError] = useState(null);
  const [collidingWith, setCollidingWith] = useState(null);
  const [resetKey, setResetKey] = useState(0);
  const headerRef = useRef(null);
  const [headerHeight, setHeaderHeight] = useState(48);

  // Đo chiều cao header thực tế để tính vùng chứa zones chính xác
  useEffect(() => {
    if (headerRef.current) {
      const h = headerRef.current.offsetHeight;
      if (typeof h === 'number' && h > 0) {
        setHeaderHeight(h);
      }
    }
  }, [area?.areaId, area?.width, area?.height]);

  // Helper: Check if two rectangles overlap
  const isColliding = (rect1, rect2, padding = 0) => {
    return (
      rect1.left < rect2.right - padding &&
      rect1.right + padding > rect2.left &&
      rect1.top < rect2.bottom - padding &&
      rect1.bottom + padding > rect2.top
    );
  };

  // Helper: Check if area collides with any other area and return collision info
  const getAreaCollisionInfo = (areaId, newX, newY, newWidth, newHeight) => {
    const newRect = {
      left: newX,
      right: newX + newWidth,
      top: newY,
      bottom: newY + newHeight,
    };

    for (const otherArea of (state.areas || [])) {
      if (otherArea.areaId === areaId) continue;

      const otherRect = {
        left: otherArea.positionX || 0,
        right: (otherArea.positionX || 0) + (otherArea.width || 300),
        top: otherArea.positionY || 0,
        bottom: (otherArea.positionY || 0) + (otherArea.height || 250),
      };

      if (isColliding(newRect, otherRect, -2)) {
        return { hasCollision: true, collidingArea: otherArea };
      }
    }
    return { hasCollision: false, collidingArea: null };
  };

  // Validate: Không để area resize làm cắt mất zone bên trong
  const validateAreaResize = (newX, newY, newWidth, newHeight) => {
    // Nội dung zones nằm dưới header, nên chiều cao khả dụng phải trừ header
    const contentWidth = newWidth; // zones dùng toạ độ tương đối theo room-content (0 -> contentWidth)
    const contentHeight = Math.max(0, newHeight - (headerHeight || 48));

    for (const zone of areaZones) {
      const zoneLeft = zone.positionX || 0;
      const zoneTop = zone.positionY || 0;
      const zoneRight = zoneLeft + (zone.width || 120);
      const zoneBottom = zoneTop + (zone.height || 100);

      // Zone phải nằm hoàn toàn trong vùng nội dung (không cắt mép)
      if (
        zoneLeft < 0 ||
        zoneTop < 0 ||
        zoneRight > contentWidth ||
        zoneBottom > contentHeight
      ) {
        return {
          isValid: false,
          message: `Không thể resize: Zone "${zone.zoneName}" sẽ bị cắt tỏa`,
          offendingZone: zone,
        };
      }
    }

    return { isValid: true, message: null, offendingZone: null };
  };

  const handleDrag = (e, d) => {
    // Check for collision with other areas
    const { hasCollision, collidingArea } = getAreaCollisionInfo(area.areaId, d.x, d.y, area.width || 300, area.height || 250);

    if (hasCollision) {
      console.warn(`Chồng lấp: "${area.areaName}" chồng lấp với "${collidingArea.areaName}"`);
      setCollidingWith(collidingArea);
      return; // Block the drag
    }

    setCollidingWith(null);

    // Update UI immediately
    dispatch({
      type: actions.UPDATE_AREA,
      payload: {
        ...area,
        positionX: d.x,
        positionY: d.y,
      },
    });

    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
  };

  const handleDragStop = async (e, d) => {
    // Final collision check before saving
    const { hasCollision, collidingArea } = getAreaCollisionInfo(area.areaId, d.x, d.y, area.width || 300, area.height || 250);

    if (hasCollision) {
      console.warn(`Vị trí bị từ chối: "${area.areaName}" chồng lấp với "${collidingArea.areaName}"`);
      setCollidingWith(collidingArea);
      setResetKey(prev => prev + 1);
      return;
    }

    setCollidingWith(null);

    dispatch({
      type: actions.UPDATE_AREA,
      payload: {
        ...area,
        positionX: d.x,
        positionY: d.y,
      },
    });
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
  };

  const handleResizeStop = async (e, direction, ref, delta, position) => {
    const newWidth = parseInt(ref.style.width);
    const newHeight = parseInt(ref.style.height);
    const positionChanged = position.x !== area.positionX || position.y !== area.positionY;
    const dimensionsChanged = newWidth !== area.width || newHeight !== area.height;

    // Validate area resize doesn't cut off zones
    const validation = validateAreaResize(position.x, position.y, newWidth, newHeight);
    if (!validation.isValid) {
      console.warn(validation.message);
      setResizeError(validation);
      return; // Block the resize
    }

    // Check for collision with other areas
    const { hasCollision, collidingArea } = getAreaCollisionInfo(area.areaId, position.x, position.y, newWidth, newHeight);
    if (hasCollision) {
      console.warn(`Chồng lấp: "${area.areaName}" chồng lấp với "${collidingArea.areaName}"`);
      setCollidingWith(collidingArea);
      setResetKey(prev => prev + 1); // Reset to original position
      return; // Block the resize
    }

    setResizeError(null);
    setCollidingWith(null);

    if (positionChanged || dimensionsChanged) {
      dispatch({
        type: actions.UPDATE_AREA,
        payload: {
          ...area,
          positionX: position.x,
          positionY: position.y,
          width: newWidth,
          height: newHeight,
        },
      });
      dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    }
  };

  return (
    <Rnd
      key={`area-${area.areaId}-${resetKey}`}
      scale={canvas?.zoom || 1}
      default={{
        x: area.positionX || 0,
        y: area.positionY || 0,
        width: area.width || 300,
        height: area.height || 250,
      }}
      onDrag={isPreviewMode ? undefined : handleDrag}
      onDragStop={isPreviewMode ? undefined : handleDragStop}
      onResizeStop={isPreviewMode ? undefined : handleResizeStop}
      dragHandleClassName="room-header"
      cancel=".zone-card, .zone-card *"
      minWidth={200}
      minHeight={150}
      disableDragging={isPreviewMode || area.locked === true || area.isActive === false}
      enableResizing={isPreviewMode ? false : (area.locked === true || area.isActive === false ? false : true)}
      resizeHandleClasses={{
        bottom: 'resize-handle-bottom',
        right: 'resize-handle-right',
        bottomRight: 'resize-handle-bottom-right',
      }}
      style={{
        zIndex: isSelected ? 10 : 1,
      }}
    >
      <div
        className={`room-wrapper ${isSelected ? 'active' : ''}`}
        style={{
          width: '100%',
          height: '100%',
          border: collidingWith
            ? '3px solid #dc2626'
            : resizeError
              ? '3px solid #dc2626'
              : 'none',
          boxShadow: collidingWith
            ? '0 0 15px rgba(220, 38, 38, 0.6), inset 0 0 10px rgba(220, 38, 38, 0.1)'
            : resizeError
              ? '0 0 15px rgba(220, 38, 38, 0.4)'
              : 'none',
          transition: 'all 0.2s ease',
          borderRadius: '4px',
          position: 'relative',
          backgroundColor: collidingWith ? 'rgba(254, 226, 226, 0.3)' : 'transparent',
          opacity: area.isActive === false ? 0.5 : 1,
        }}
        onClick={(e) => {
          e.stopPropagation();
          // ✅ Dispatch cả SELECT_ITEM và SELECT_AREA để đồng bộ
          dispatch({
            type: actions.SELECT_ITEM,
            payload: { type: 'area', id: area.areaId },
          });
          dispatch({
            type: actions.SELECT_AREA,
            payload: area.areaId,
          });
        }}
      >


        {/* Collision Warning */}
        {collidingWith && (
          <div style={{
            position: 'absolute',
            top: '10px',
            left: '50%',
            transform: 'translateX(-50%)',
            backgroundColor: '#dc2626',
            color: 'white',
            padding: '8px 12px',
            borderRadius: '4px',
            fontSize: '12px',
            fontWeight: '600',
            zIndex: 20,
            whiteSpace: 'nowrap',
            boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
          }}>
            Chồng lấp với: {collidingWith.areaName}
          </div>
        )}

        {/* Resize Error Warning */}
        {resizeError && !collidingWith && (
          <div style={{
            position: 'absolute',
            top: '10px',
            left: '50%',
            transform: 'translateX(-50%)',
            backgroundColor: '#dc2626',
            color: 'white',
            padding: '8px 12px',
            borderRadius: '4px',
            fontSize: '12px',
            fontWeight: '600',
            zIndex: 20,
            whiteSpace: 'nowrap',
            boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
          }}>
            {resizeError.message}
          </div>
        )}

        <div
          className={`room ${isSelected ? 'selected' : ''}`}
          style={{ width: '100%', height: '100%', position: 'relative' }}
        >
          <div className="room-header" ref={headerRef}>
            <div className="room-title">
              {area.areaName || 'Unnamed Area'}
            </div>
            <div className="room-dimensions">
              {area.width} × {area.height}
            </div>
          </div>

          <div
            className="room-content"
            onClick={(e) => {
              // Deselect all when clicking on empty area
              if (e.target === e.currentTarget) {
                dispatch({ type: actions.DESELECT });
              }
            }}
          >
            {(loadingZones || loadingFactories) ? (
              <div className="room-empty">
                <p>Đang tải...</p>
              </div>
            ) : areaZones.length === 0 && areaFactories.length === 0 ? (
              <div className="room-empty">
                <p>Không có khu vực hoặc vật cản</p>
                <small>Nhấn "Thêm phòng" hoặc "Thêm vật cản"</small>
              </div>
            ) : (
              <div
                style={{ position: 'relative', width: '100%', height: '100%' }}
                onClick={(e) => {
                  // Deselect all when clicking on empty area inside content
                  if (e.target === e.currentTarget) {
                    dispatch({ type: actions.DESELECT });
                  }
                }}
              >
                {areaZones.map((zone) => (
                  <ZoneSimple
                    key={zone.zoneId}
                    zone={zone}
                    area={area}
                  />
                ))}
                {areaFactories.map((factory) => (
                  <Shape
                    key={factory.factoryId}
                    factory={factory}
                    area={area}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Rnd>
  );
}

export default Area;

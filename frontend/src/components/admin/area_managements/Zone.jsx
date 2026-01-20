import Draggable from "react-draggable";
import { ResizableBox } from "react-resizable";
import { useRef, useState, useEffect } from "react";
import { useLayout } from "../../context/admin/area_management/LayoutContext";
import { updateZonePosition, updateZoneDimensions, getSeats } from "../services/api";
import Seat from "./Seat";
import { calculateSeatLayout, calculateMinZoneDimensions } from "../utils/seatLayout";
import "react-resizable/css/styles.css";
import "../styles/Zone.css";

function Zone({ zone, areaBounds }) {
  const { state, dispatch, actions } = useLayout();
  const { selectedItem, seats, availableAmenities } = state;

  const nodeRef = useRef(null);

  const [liveSize, setLiveSize] = useState({
    width: zone.width,
    height: zone.height,
  });

  /* ================= COLLISION ================= */

  const isColliding = (rect1, rect2, padding = 0) => {
    return (
      rect1.left < rect2.right - padding &&
      rect1.right + padding > rect2.left &&
      rect1.top < rect2.bottom - padding &&
      rect1.bottom + padding > rect2.top
    );
  };

  const getCollisionInfo = (zoneId, newX, newY, newWidth, newHeight) => {
    const newRect = {
      left: newX,
      right: newX + newWidth,
      top: newY,
      bottom: newY + newHeight,
    };

    // Check against factories in same area
    const factoriesInArea = (state.factories || []).filter(f => String(f.areaId) === String(zone?.areaId));
    for (const factory of factoriesInArea) {
      const factoryRect = {
        left: factory.positionX || 0,
        right: (factory.positionX || 0) + (factory.width || 160),
        top: factory.positionY || 0,
        bottom: (factory.positionY || 0) + (factory.height || 120),
      };

      if (isColliding(newRect, factoryRect, -2)) {
        console.warn(`Zone "${zone.zoneName}" chồng lấp với Factory "${factory.factoryName}"`);
        return { hasCollision: true, collidingWith: `Factory: ${factory.factoryName}` };
      }
    }

    // Check against other zones in same area
    const otherZones = (state.zones || []).filter(z => z.zoneId !== zoneId && String(z.areaId) === String(zone?.areaId));
    for (const otherZone of otherZones) {
      const otherRect = {
        left: otherZone.positionX || 0,
        right: (otherZone.positionX || 0) + (otherZone.width || 200),
        top: otherZone.positionY || 0,
        bottom: (otherZone.positionY || 0) + (otherZone.height || 150),
      };

      if (isColliding(newRect, otherRect, -2)) {
        console.warn(`Zone "${zone.zoneName}" chồng lấp với Zone "${otherZone.zoneName}"`);
        return { hasCollision: true, collidingWith: `Zone: ${otherZone.zoneName}` };
      }
    }

    return { hasCollision: false, collidingWith: null };
  };

  const [collidingWith, setCollidingWith] = useState(null);
  const [isResizeInvalid, setIsResizeInvalid] = useState(false);
  const headerRef = useRef(null);

  /* ================= LOAD SEATS ================= */
  useEffect(() => {
    if (!zone?.zoneId) return;
    (async () => {
      try {
        const res = await getSeats(zone.zoneId);
        console.log("Loaded seats for zone", zone.zoneId, ":", res.data);
        dispatch({
          type: actions.SET_SEATS,
          payload: res.data || [],
        });
      } catch (e) {
        console.error("Failed to load seats for zone", zone.zoneId, e);
      }
    })();
  }, [zone?.zoneId, dispatch, actions]);

  /* ================= AUTO-RESIZE IF TOO SMALL ================= */
  useEffect(() => {
    if (zoneSeats.length === 0) return;

    const minDims = calculateMinZoneDimensions(zoneSeats);

    // If zone is smaller than required, auto-resize it
    if (zone.width < minDims.minWidth || zone.height < minDims.minHeight) {
      const newWidth = Math.max(zone.width, minDims.minWidth);
      const newHeight = Math.max(zone.height, minDims.minHeight);

      console.log(`Zone ${zone.zoneId} auto-resized: ${zone.width}×${zone.height} → ${newWidth}×${newHeight}`);

      updateZoneDimensions(zone.zoneId, newWidth, newHeight).then(() => {
        dispatch({
          type: actions.UPDATE_ZONE,
          payload: {
            ...zone,
            width: newWidth,
            height: newHeight,
          },
        });
        setLiveSize({ width: newWidth, height: newHeight });
      }).catch(e => console.error('Failed to auto-resize zone', e));
    }
  }, [zoneSeats.length, zone, dispatch, actions]);

  /* ================= DERIVED ================= */

  const zoneSeats = seats.filter(s => s.zoneId === zone.zoneId);

  console.log(`Zone ${zone.zoneId} rendered - seats: ${zoneSeats.length}`);

  const isSelected =
    selectedItem?.type === "zone" &&
    selectedItem?.id === zone.zoneId;

  const zoneAmenities = availableAmenities.filter(a =>
    zone.amenities?.includes(a.id)
  );

  /* ================= SELECT ================= */

  const handleClick = (e) => {
    e.stopPropagation();
    dispatch({
      type: actions.SELECT_ITEM,
      payload: { type: "zone", id: zone.zoneId },
    });
  };

  /* ================= DRAG ================= */

  const handleDragStop = async (e, data) => {
    if (zone.isLocked) return;

    const maxX = areaBounds.width - zone.width;
    const maxY = areaBounds.height - zone.height;

    const x = Math.max(0, Math.min(data.x, maxX));
    const y = Math.max(0, Math.min(data.y, maxY));

    // Check collision
    const { hasCollision, collidingWith } = getCollisionInfo(zone.zoneId, x, y, zone.width, zone.height);
    if (hasCollision) {
      console.warn(`Kéo bị chặn: ${collidingWith}`);
      setCollidingWith(collidingWith);
      return;
    }

    setCollidingWith(null);

    // update backend
    try {
      await updateZonePosition(zone.zoneId, x, y);
      console.log(`Zone position saved: (${x}, ${y})`);
    } catch (e) {
      console.error('Failed to save zone position', e);
      return;
    }

    // sync context
    dispatch({
      type: actions.UPDATE_ZONE,
      payload: {
        ...zone,
        positionX: x,
        positionY: y,
      },
    });
  };

  /* ================= RESIZE ================= */

  const validateZoneResize = (newWidth, newHeight) => {
    if (!zoneSeats || zoneSeats.length === 0) {
      return true;
    }

    // Get minimum dimensions required to fit all seats
    const minDimensions = calculateMinZoneDimensions(zoneSeats);

    // Check if new dimensions can fit all seats
    if (newWidth < minDimensions.minWidth) {
      console.warn(`Zone Resize BLOCKED: newWidth(${newWidth}) < minWidth(${minDimensions.minWidth})`);
      return false;
    }

    if (newHeight < minDimensions.minHeight) {
      console.warn(`Zone Resize BLOCKED: newHeight(${newHeight}) < minHeight(${minDimensions.minHeight})`);
      return false;
    }

    return true;
  };

  const handleResize = (_, { size }) => {
    if (zone.isLocked) {
      return;
    }

    // Check collision with new size
    const { hasCollision, collidingWith } = getCollisionInfo(zone.zoneId, zone.positionX, zone.positionY, size.width, size.height);
    if (hasCollision) {
      console.warn(`Resize BLOCKED by collision: ${collidingWith}`);
      setCollidingWith(collidingWith);
      setIsResizeInvalid(true);
      return;
    }

    setCollidingWith(null);

    const isValid = validateZoneResize(size.width, size.height);
    setIsResizeInvalid(!isValid);

    if (isValid) {
      setLiveSize(size);
    }
  };

  const handleResizeStop = async (_, { size }) => {
    if (zone.isLocked) return;

    // Check collision
    const { hasCollision, collidingWith } = getCollisionInfo(zone.zoneId, zone.positionX, zone.positionY, size.width, size.height);
    if (hasCollision) {
      console.warn(`Resize REJECTED by collision: ${collidingWith}`);
      setCollidingWith(collidingWith);
      setIsResizeInvalid(false);
      setLiveSize({ width: zone.width, height: zone.height });
      return;
    }

    setCollidingWith(null);

    const isValid = validateZoneResize(size.width, size.height);

    if (!isValid) {
      setIsResizeInvalid(false);
      setLiveSize({ width: zone.width, height: zone.height });
      return;
    }

    try {
      await updateZoneDimensions(zone.zoneId, size.width, size.height);
      console.log(`Zone ${zone.zoneId} saved: ${size.width}×${size.height}`);
    } catch (e) {
      console.error('Failed to save zone dimensions', e);
      setLiveSize({ width: zone.width, height: zone.height });
      return;
    }

    dispatch({
      type: actions.UPDATE_ZONE,
      payload: {
        ...zone,
        width: size.width,
        height: size.height,
      },
    });

    setLiveSize(size);
    setIsResizeInvalid(false);
  };

  /* ================= SYNC PROP ================= */

  useEffect(() => {
    setLiveSize({
      width: zone.width,
      height: zone.height,
    });
  }, [zone.width, zone.height]);

  /* ================= GRID ================= */

  const getGridStyle = () => {
    if (zone.direction === "vertical") {
      return { gridTemplateColumns: "repeat(auto-fill, 48px)" };
    }
    return {
      gridTemplateColumns: "repeat(auto-fill, 48px)",
      gridAutoFlow: "column",
      gridTemplateRows: "repeat(auto-fill, 48px)",
    };
  };

  /* ================= FIXED ICON ================= */

  const getFixedZoneIcon = () => {
    // No icons - clean UI
    return '';
  };

  /* ================= RENDER ================= */

  return (
    <Draggable
      nodeRef={nodeRef}
      position={{
        x: zone.positionX,
        y: zone.positionY,
      }}
      onStop={handleDragStop}
      bounds="parent"
      handle=".zone-header"
      disabled={zone.isLocked}
    >
      <div ref={nodeRef} style={{ position: "absolute" }}>
        <ResizableBox
          width={liveSize.width}
          height={liveSize.height}
          minConstraints={(() => {
            const dims = calculateMinZoneDimensions(zoneSeats);
            console.log(`Zone ${zone.zoneId} - minConstraints set to: [${dims.minWidth}, ${dims.minHeight}]`);
            return [dims.minWidth, dims.minHeight];
          })()}
          maxConstraints={[
            areaBounds.width - zone.positionX,
            areaBounds.height - zone.positionY,
          ]}
          onResize={handleResize}
          onResizeStop={handleResizeStop}
          resizeHandles={zone.isLocked ? [] : ["se"]}
        >
          <div
            className={`zone ${isSelected ? "selected" : ""} ${zone.isFixed ? "fixed" : ""
              } ${zone.isLocked ? "locked" : ""} ${isResizeInvalid ? "resize-invalid" : ""}`}
            onClick={handleClick}
            style={{
              width: "100%",
              height: "100%",
              backgroundColor: "#E5E7EB",  // Light gray for all zones
              borderColor: "#9CA3AF",
            }}
          >
            {/* ===== HEADER TABBAR ===== */}
            <div className="zone-header" ref={headerRef}>
              <div className="zone-header-left">
                <span className="zone-name">{zone.zoneName}</span>
              </div>

              <div className="zone-header-right">
                {zone.isFixed ? (
                  <span className="zone-badge fixed">
                    {getFixedZoneIcon()} {zone.fixedType}
                  </span>
                ) : null}
              </div>
            </div>

            {/* ===== CONTENT ===== */}
            <div className="zone-content">
              {zone.isFixed ? (
                <div className="zone-fixed-display">
                  <span className="fixed-icon">{getFixedZoneIcon()}</span>
                  <span>{zone.zoneName}</span>
                </div>
              ) : (
                <>
                  <div className="seats-container" style={{ position: "relative", width: "100%", height: "100%", overflow: "hidden" }}>
                    {zoneSeats && zoneSeats.length > 0 ? (
                      <>
                        {console.log(`Zone ${zone.zoneId} rendering ${zoneSeats.length} seats`)}
                        {(() => {
                          const minDims = calculateMinZoneDimensions(zoneSeats);
                          const headerHeight = 40;
                          const padding = 8;
                          const minContentHeight = minDims.minHeight - headerHeight - padding;

                          console.log(`Zone ${zone.zoneId} - minDims: ${minDims.minWidth}×${minDims.minHeight}, liveSize: ${liveSize.width}×${liveSize.height}`);

                          return (
                            <div style={{
                              position: "relative",
                              minWidth: minDims.minWidth,
                              minHeight: minContentHeight,
                              padding: `${padding}px`,
                              backgroundColor: 'rgba(0,0,0,0.02)',
                              border: '1px dashed #ccc',
                            }}>
                              {zoneSeats.map((seat, idx) => {
                                const layout = calculateSeatLayout(seat);
                                const finalLeft = layout.positionX + padding;
                                const finalTop = layout.positionY - headerHeight + padding;

                                if (idx === 0) {
                                  console.log(`   Seat[0] ${seat.seatId} (${seat.seatCode}): row=${seat.rowNumber}, col=${seat.columnNumber}`);
                                  console.log(`       layout.positionX=${layout.positionX}, layout.positionY=${layout.positionY}`);
                                  console.log(`       finalLeft=${finalLeft}, finalTop=${finalTop}`);
                                }

                                return (
                                  <div
                                    key={seat.seatId}
                                    style={{
                                      position: "absolute",
                                      left: finalLeft,
                                      top: finalTop,
                                      width: layout.width,
                                      height: layout.height,
                                    }}
                                  >
                                    <Seat seat={seat} />
                                  </div>
                                );
                              })}
                            </div>
                          );
                        })()}
                      </>
                    ) : (
                      <div style={{ padding: '20px', color: '#999' }}>No seats</div>
                    )}
                  </div>

                  {zoneSeats.length === 0 && (
                    <div className="zone-empty">
                      <small>No seats</small>
                    </div>
                  )}
                </>
              )}
            </div>

            {/* ===== FOOTER ===== */}
            <div className="zone-footer">
              <span>
                x:{Math.round(zone.positionX)} y:{Math.round(zone.positionY)}
              </span>
              <span>
                {Math.round(liveSize.width)}×{Math.round(liveSize.height)}
              </span>
              {!zone.isFixed && <span>{zoneSeats.length} seats</span>}
              <span className="zone-direction">
                {zone.direction === "vertical" ? "vertical" : "horizontal"}
              </span>
            </div>
          </div>
        </ResizableBox>
      </div>
    </Draggable>
  );
}

export default Zone;

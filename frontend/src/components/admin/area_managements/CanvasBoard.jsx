import { useRef, useState, useCallback, useEffect } from "react";
import { useLayout } from "../../../context/admin/area_management/LayoutContext"
import Area from "./Area";
import { getAreas } from "../../../services/admin/area_management/api";
import "../../../styles/admin/canvas.css";

function CanvasBoard() {
  const { state, dispatch, actions } = useLayout();
  const { areas, canvas, selectedAreaId } = state;

  const containerRef = useRef(null);
  const boardRef = useRef(null);

  const [isPanning, setIsPanning] = useState(false);
  const [startPan, setStartPan] = useState({ x: 0, y: 0 });
  const [panMode, setPanMode] = useState(false);
  const [didAutoFit, setDidAutoFit] = useState(false);
  const [spacePressed, setSpacePressed] = useState(false);

  /* =============================
     LOAD AREA FROM BACKEND
  ============================== */
  useEffect(() => {
    (async () => {
      try {
        const res = await getAreas();
        const raw = Array.isArray(res?.data) ? res.data : [];
        // Normalize snake_case -> camelCase to match UI expectations
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
          // Auto-select first area
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
    if (!panMode && !spacePressed) return;

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
     SPACE KEY FOR PAN
  ============================== */
  useEffect(() => {
    const handleKeyDown = (e) => {
      const activeTag = document.activeElement.tagName;
      if (e.code === "Space" && !e.repeat && activeTag !== "INPUT" && activeTag !== "TEXTAREA") {
        e.preventDefault();
        setSpacePressed(true);
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
  }, []);

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

  // Auto fit once when areas are loaded/changed
  useEffect(() => {
    if (!didAutoFit && areas && areas.length > 0) {
      handleFitToView();
      setDidAutoFit(true);
    }
  }, [areas, didAutoFit]);

  return (
    <main className="canvas-container" ref={containerRef}>
      <div className="canvas-header" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '8px',
        padding: '9px 20px',
        background: 'linear-gradient(135deg, #ffffff 0%, #f9fafb 100%)',
        borderBottom: '1px solid #e5e7eb',
        boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
      }}>
        {/* Pan Control - Left */}
        <button 
          onClick={() => setPanMode(!panMode)}
          title={panMode ? "Pan Mode: On (Click to turn off)" : "Pan Mode: Off (Click to turn on)"}
          style={{
            width: '36px',
            height: '36px',
            borderRadius: '6px',
            border: panMode ? '2px solid #3b82f6' : '1px solid #d1d5db',
            background: panMode ? '#dbeafe' : 'white',
            cursor: 'pointer',
            fontSize: '16px',
            transition: 'all 0.2s',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: panMode ? '#1e40af' : '#666'
          }}
          onMouseEnter={(e) => {
            if (!panMode) {
              e.target.style.background = '#f3f4f6';
              e.target.style.borderColor = '#9ca3af';
            }
          }}
          onMouseLeave={(e) => {
            if (!panMode) {
              e.target.style.background = 'white';
              e.target.style.borderColor = '#d1d5db';
            }
          }}
        >
          ✋
        </button>

        {/* Spacer - Push zoom to center */}
        <div style={{ flex: 1 }} />

        {/* Zoom Controls - Center */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', backgroundColor: '#f3f4f6', borderRadius: '8px', padding: '4px' }}>
          <button 
            onClick={handleZoomOut}
            title="Zoom Out"
            style={{
              width: '32px',
              height: '32px',
              borderRadius: '6px',
              border: '1px solid #d1d5db',
              background: 'white',
              cursor: 'pointer',
              fontSize: '16px',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
            onMouseEnter={(e) => {
              e.target.style.background = '#eff6ff';
              e.target.style.borderColor = '#3b82f6';
            }}
            onMouseLeave={(e) => {
              e.target.style.background = 'white';
              e.target.style.borderColor = '#d1d5db';
            }}
          >
            −
          </button>
          
          <span style={{
            minWidth: '50px',
            textAlign: 'center',
            fontSize: '12px',
            fontWeight: '600',
            color: '#374151'
          }}>
            {Math.round(canvas.zoom * 100)}%
          </span>
          
          <button 
            onClick={handleZoomIn}
            title="Zoom In"
            style={{
              width: '32px',
              height: '32px',
              borderRadius: '6px',
              border: '1px solid #d1d5db',
              background: 'white',
              cursor: 'pointer',
              fontSize: '16px',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
            onMouseEnter={(e) => {
              e.target.style.background = '#eff6ff';
              e.target.style.borderColor = '#3b82f6';
            }}
            onMouseLeave={(e) => {
              e.target.style.background = 'white';
              e.target.style.borderColor = '#d1d5db';
            }}
          >
            +
          </button>
        </div>

        {/* Spacer - Push fit & fullscreen to right */}
        <div style={{ flex: 1 }} />

        {/* Fit to View & Fullscreen - Right */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
          
          <button 
            onClick={handleFitToView}
            title="Fit all areas to view"
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '6px',
              border: '1px solid #d1d5db',
              background: 'white',
              cursor: 'pointer',
              fontSize: '16px',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#666'
            }}
            onMouseEnter={(e) => {
              e.target.style.background = '#f0fdf4';
              e.target.style.borderColor = '#22c55e';
              e.target.style.color = '#15803d';
            }}
            onMouseLeave={(e) => {
              e.target.style.background = 'white';
              e.target.style.borderColor = '#d1d5db';
              e.target.style.color = '#666';
            }}
          >
            ⬜
          </button>

          {/* Fullscreen Toggle */}
          <button 
            onClick={() => dispatch({ type: actions.TOGGLE_CANVAS_FULLSCREEN })}
            title={state.isCanvasFullscreen ? "Exit Fullscreen" : "Fullscreen"}
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '6px',
              border: state.isCanvasFullscreen ? '2px solid #dc2626' : '1px solid #d1d5db',
              background: state.isCanvasFullscreen ? '#fee2e2' : 'white',
              cursor: 'pointer',
              fontSize: '16px',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: state.isCanvasFullscreen ? '#dc2626' : '#666'
            }}
            onMouseEnter={(e) => {
              if (!state.isCanvasFullscreen) {
                e.target.style.background = '#fef3c7';
                e.target.style.borderColor = '#f59e0b';
                e.target.style.color = '#d97706';
              }
            }}
            onMouseLeave={(e) => {
              if (!state.isCanvasFullscreen) {
                e.target.style.background = 'white';
                e.target.style.borderColor = '#d1d5db';
                e.target.style.color = '#666';
              }
            }}
          >
            {state.isCanvasFullscreen ? '❌' : '⛶'}
          </button>
        </div>
      </div>

      <div
        ref={boardRef}
        className={`canvas-board ${panMode || spacePressed ? 'pan-mode' : ''}`}
        onMouseDown={handleMouseDown}
        onWheel={handleWheel}
      >
        <div
          className="canvas-grid"
          style={{
            transform: `translate(${canvas.panX}px, ${canvas.panY}px) scale(${canvas.zoom})`,
            transformOrigin: "0 0",
            position: 'relative',
          }}
        >
          {areas.map((area) => (
            <Area key={area.areaId} area={area} />
          ))}

          {areas.length === 0 && (
            <div className="canvas-empty">
            </div>
          )}
        </div>
      </div>
    </main>
  );
}

export default CanvasBoard;

import { useEffect, useState } from "react";
import { useLayout } from "../../../context/admin/area_management/LayoutContext";
import {
  createArea,
  createZone,
  createSeat,
  getZonesByArea,
  getSeats,
  createAreaFactoryInArea,
} from "../../../services/admin/area_management/api";
import { generateNewSeatData } from "../../../utils/admin/seatLayout";
import "../../../styles/admin/sidebar_area.css";

// Constants for seat creation
const DEFAULT_SEAT_SIZE = 30;
const DEFAULT_SEAT_SPACING = 35;

function Sidebar() {
  const { state, dispatch, actions } = useLayout();
  const {
    areas,
    zones,
    seats,
    selectedItem,
    selectedAreaId,
    selectedZoneId,
  } = state;

  const [seatCount, setSeatCount] = useState(1);
  const [rowNumber, setRowNumber] = useState(1);
  const [rowLetter, setRowLetter] = useState('A'); // Thêm state cho chữ cái
  const [showRowModal, setShowRowModal] = useState(false);

  /* ================= FILTER ================= */
  const currentAreaZones = zones.filter((z) => z.areaId === selectedAreaId);
  
  // ✅ Lấy zone từ selectedZoneId hoặc selectedItem
  const selectedZone = selectedZoneId
    ? zones.find((z) => z.zoneId === selectedZoneId)
    : selectedItem?.type === "zone"
    ? zones.find((z) => z.zoneId === selectedItem.id)
    : null;

  /* ================= HANDLERS ================= */

  const handleAddArea = async () => {
    // Compute a smart position: right next to current row/selection
    const DEFAULT = { x: 50, y: 50 };
    const NEW_SIZE = { width: 600, height: 400 };
    let positionX = DEFAULT.x;
    let positionY = DEFAULT.y;

    if (areas && areas.length > 0) {
      const GAP = 40;
      const anchor = selectedAreaId
        ? areas.find(a => a.areaId === selectedAreaId)
        : null;

      if (anchor) {
        // Find the rightmost area on the same row (within 100px vertically)
        const rowPeers = areas.filter(a => Math.abs((a.positionY || 0) - (anchor.positionY || 0)) < 100);
        const rightMost = rowPeers.reduce((acc, a) => {
          const right = (a.positionX || 0) + (a.width || NEW_SIZE.width);
          return right > acc.maxRight ? { maxRight: right, area: a } : acc;
        }, { maxRight: (anchor.positionX || 0) + (anchor.width || NEW_SIZE.width), area: anchor });

        positionX = rightMost.maxRight + GAP;
        positionY = anchor.positionY || DEFAULT.y;
      } else {
        // No selection; place after the overall rightmost area
        const rightMost = areas.reduce((acc, a) => {
          const right = (a.positionX || 0) + (a.width || NEW_SIZE.width);
          return right > acc ? right : acc;
        }, 0);
        const topOfRightMost = areas.reduce((acc, a) => {
          const right = (a.positionX || 0) + (a.width || NEW_SIZE.width);
          return right === rightMost ? (a.positionY || DEFAULT.y) : acc;
        }, DEFAULT.y);
        positionX = rightMost + GAP;
        positionY = topOfRightMost;
      }
    }

    const res = await createArea({
      areaName: "New Area",
      width: NEW_SIZE.width,
      height: NEW_SIZE.height,
      positionX,
      positionY,
    });

    dispatch({ type: actions.ADD_AREA, payload: res.data });
    // Also select new area so it centers in view (optional)
    dispatch({ type: actions.SELECT_AREA, payload: res.data.areaId });
  };

  const handleAddZone = async () => {
    if (!selectedAreaId) {
      alert("Vui lòng chọn khu vực trước");
      return;
    }

    try {
      const payload = {
        zoneName: "New Zone",
        areaId: selectedAreaId,
        hasPowerOutlet: false,
        width: 200,
        height: 150,
        positionX: 20,
        positionY: 20,
      };
      
      console.log("Creating zone with payload:", payload);
      const res = await createZone(payload);
      console.log("Zone created (raw):", res.data);

      // Normalize snake_case -> camelCase so zones render inside the selected area immediately
      const zoneNormalized = {
        zoneId: res.data?.zone_id ?? res.data?.zoneId,
        zoneName: res.data?.zone_name ?? res.data?.zoneName ?? "New Zone",
        areaId: res.data?.area_id ?? res.data?.areaId ?? selectedAreaId,
        positionX: res.data?.position_x ?? res.data?.positionX ?? 20,
        positionY: res.data?.position_y ?? res.data?.positionY ?? 20,
        width: res.data?.width ?? 200,
        height: res.data?.height ?? 150,
        color: res.data?.color ?? "#d1f7d8",
        isLocked: res.data?.is_locked ?? res.data?.isLocked ?? false,
        isFixed: res.data?.is_fixed ?? res.data?.isFixed ?? false,
        fixedType: res.data?.fixed_type ?? res.data?.fixedType,
        amenities: res.data?.amenities ?? [],
      };

      console.log("Zone normalized:", zoneNormalized);
      dispatch({ type: actions.ADD_ZONE, payload: zoneNormalized });
    } catch (e) {
      console.error("Failed to create zone:", e);
      console.error("Error response:", e.response);
      console.error("Status:", e.response?.status);
      console.error("Data:", e.response?.data);
      alert("Tạo phòng thất bại: " + (e.response?.data?.message || e.message));
    }
  };

  const handleAddFactory = async () => {
    if (!selectedAreaId) {
      alert("Vui lòng chọn khu vực trước");
      return;
    }

    try {
      const payload = {
        factoryName: "New Factory",
        width: 160,
        height: 120,
        positionX: 20,
        positionY: 20,
        areaId: selectedAreaId,
        isActive: true,
      };

      console.log("🏭 Creating factory with payload:", payload);
      console.log("📍 Area ID:", selectedAreaId);
      const res = await createAreaFactoryInArea(selectedAreaId, payload);
      console.log("✅ Factory created:", res.data);
      // Convert snake_case response to camelCase
      const factoryData = {
        factoryId: res.data.factory_id || res.data.factoryId,
        factoryName: res.data.factory_name || res.data.factoryName,
        positionX: res.data.position_x || res.data.positionX,
        positionY: res.data.position_y || res.data.positionY,
        width: res.data.width,
        height: res.data.height,
        color: res.data.color || "#90EE90",
        areaId: res.data.area_id || res.data.areaId || selectedAreaId,
      };
      console.log("🏭 Factory data to dispatch:", factoryData);
      dispatch({ type: actions.ADD_FACTORY, payload: factoryData });
    } catch (e) {
      console.error("❌ Failed to create factory:", e);
      console.error("Error response:", e.response);
      console.error("Status:", e.response?.status);
      console.error("Data:", e.response?.data);
      alert("Tạo xưởng thất bại: " + (e.response?.data?.message || e.message));
    }
  };

  const handleAddSeat = async () => {
    if (!selectedZone) {
      alert("Vui lòng chọn phòng trước");
      return;
    }

    setShowRowModal(true);
  };

  const confirmAddSeat = async () => {
    // Convert chữ cái thành số (A->1, B->2, ...)
    const rowNum = rowLetter.toUpperCase().charCodeAt(0) - 64;
    
    if (!selectedZone || !rowLetter || rowNum < 1 || rowNum > 26) {
      alert("Vui lòng chọn hàng hợp lệ (A-Z)");
      return;
    }

    try {
      // Lấy tất cả ghế trong zone
      const seatsInZone = seats.filter(s => s.zoneId === selectedZone.zoneId);
      
      // Tính toán data tự động (fixed size)
      const seatData = generateNewSeatData({
        zoneId: selectedZone.zoneId,
        rowNumber: rowNum,
        seatsInZone,
        seatHeight: 44,
      });

      console.log("🪑 CreateSeat with auto-calculated data:", seatData);
      const res = await createSeat(seatData);

      console.log("✅ CreateSeat Response:", res.data);
      
      // Force override rowNumber và columnNumber vì backend có thể không trả về
      const completeSeatData = {
        ...res.data,
        rowNumber: seatData.rowNumber,
        columnNumber: seatData.columnNumber,
        seatCode: seatData.seatCode,
      };
      
      console.log("🔧 Complete seat data to dispatch:", completeSeatData);
      
      dispatch({ type: actions.ADD_SEAT, payload: completeSeatData });
      setShowRowModal(false);
    } catch (e) {
      console.error("❌ Error adding seat:", e);
      console.error("📋 Request failed - Status:", e.response?.status);
      console.error("📋 Server error message:", e.response?.data);
      alert("Tạo ghế thất bại: " + (e.response?.data?.message || e.message));
    }
  };

  const handleAddMultipleSeats = async () => {
    if (!selectedZone) {
      alert("Vui lòng chọn phòng trước");
      return;
    }

    setShowRowModal(true);
  };

  const confirmAddMultipleSeats = async () => {
    // Convert chữ cái thành số (A->1, B->2, ...)
    const rowNum = rowLetter.toUpperCase().charCodeAt(0) - 64;
    
    if (!selectedZone || !rowLetter || rowNum < 1 || rowNum > 26) {
      alert("Vui lòng chọn hàng hợp lệ (A-Z)");
      return;
    }

    try {
      // Lấy tất cả ghế hiện tại trong zone
      let seatsInZone = seats.filter(s => s.zoneId === selectedZone.zoneId);
      
      for (let i = 0; i < seatCount; i++) {
        // Tính toán data tự động cho mỗi ghế (fixed size)
        const seatData = generateNewSeatData({
          zoneId: selectedZone.zoneId,
          rowNumber: rowNum,
          seatsInZone,
          seatHeight: 44,
        });
        
        console.log(`🪑 Creating seat ${i + 1}/${seatCount}:`, seatData);
        const res = await createSeat(seatData);
        console.log(`✅ Seat ${i + 1} Response:`, res.data);
        
        // Force override rowNumber và columnNumber vì backend có thể không trả về
        const completeSeatData = {
          ...res.data,
          rowNumber: seatData.rowNumber,
          columnNumber: seatData.columnNumber,
          seatCode: seatData.seatCode,
        };
        
        console.log(`🔧 Complete seat ${i + 1} data:`, completeSeatData);
        
        dispatch({ type: actions.ADD_SEAT, payload: completeSeatData });
        
        // Thêm ghế mới vào danh sách local để tính column tiếp theo đúng
        seatsInZone = [...seatsInZone, completeSeatData];
      }
      setShowRowModal(false);
    } catch (e) {
      console.error("❌ Error adding multiple seats:", e);
      console.error("📋 Request failed - Status:", e.response?.status);
      console.error("📋 Server error message:", e.response?.data);
      alert("Tạo nhiều ghế thất bại: " + (e.response?.data?.message || e.message));
    }
  };

  const handleSelectArea = (areaId) => {
    console.log("handleSelectArea called with areaId:", areaId);
    console.log("Current selectedAreaId before dispatch:", selectedAreaId);
    dispatch({ type: actions.SELECT_AREA, payload: areaId });
    console.log("Dispatch completed for SELECT_AREA");
  };

  const handleSelectZone = async (zoneId) => {
    console.log("handleSelectZone called with zoneId:", zoneId);
    dispatch({ type: actions.SELECT_ITEM, payload: { type: "zone", id: zoneId } });
    try {
      const res = await getSeats(zoneId);
      const raw = Array.isArray(res?.data) ? res.data : [];
      const seatsNormalized = raw.map((s) => ({
        seatId: s.seat_id ?? s.seatId,
        zoneId: s.zone_id ?? s.zoneId ?? zoneId,
        seatCode: s.seat_code ?? s.seatCode,
        width: s.width ?? 30,
        height: s.height ?? 30,
        rowNumber: s.row_number ?? s.rowNumber,
        columnNumber: s.column_number ?? s.columnNumber,
        positionX: s.position_x ?? s.positionX ?? 0,
        positionY: s.position_y ?? s.positionY ?? 0,
        isActive: (s.is_active ?? s.isActive) ?? (s.status === 'ACTIVE' ? true : s.status === 'INACTIVE' ? false : true),
      }));
      const debugList = seatsNormalized.map((s) => ({ seatId: s.seatId, isActive: s.isActive }));
      dispatch({ type: actions.SET_SEATS, payload: seatsNormalized });
    } catch (e) {
      console.error("Failed to load seats:", e);
      dispatch({ type: actions.SET_SEATS, payload: [] });
    }
  };

  const isAreaSelected = (id) =>
    selectedItem?.type === "area" ? selectedItem.id === id : selectedAreaId === id;

  const isZoneSelected = (id) =>
    selectedItem?.type === "zone" && selectedItem.id === id;

  /* ================= RENDER ================= */

  return (
    <aside className="sidebar_area" style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%'
    }}>
      <div style={{
        padding: '16px',
        background: 'linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%)',
        borderBottom: '2px solid #e5e7eb',
        flexShrink: 0
      }}>
        <h2 style={{
          margin: 0,
          fontSize: '16px',
          fontWeight: '700',
          color: '#1f2937',
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          Bố cục thư viện
        </h2>
      </div>

      {/* ===== ACTIONS ===== */}
      <div className="sidebar-actions" style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        padding: '16px 12px',
        borderBottom: '1px solid #e5e7eb'
      }}>
        <button 
          className="action-btn" 
          onClick={handleAddArea}
          style={{
            width: '100%',
            padding: '10px 12px',
            borderRadius: '8px',
            border: '2px solid #e5e7eb',
            background: 'linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%)',
            color: '#166534',
            cursor: 'pointer',
            fontSize: '13px',
            fontWeight: '600',
            transition: 'all 0.2s',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '6px'
          }}
          onMouseEnter={(e) => {
            e.target.style.borderColor = '#22c55e';
            e.target.style.boxShadow = '0 2px 6px rgba(34, 197, 94, 0.2)';
          }}
          onMouseLeave={(e) => {
            e.target.style.borderColor = '#e5e7eb';
            e.target.style.boxShadow = 'none';
          }}
        >
          Thêm khu vực
        </button>

        <button
          className="action-btn"
          onClick={handleAddZone}
          disabled={!selectedAreaId}
          style={{
            width: '100%',
            padding: '10px 12px',
            borderRadius: '8px',
            border: selectedAreaId ? '2px solid #e5e7eb' : '2px solid #f3e8e8',
            background: selectedAreaId ? 'linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%)' : '#f9fafb',
            color: selectedAreaId ? '#1e40af' : '#d1d5db',
            cursor: selectedAreaId ? 'pointer' : 'not-allowed',
            fontSize: '13px',
            fontWeight: '600',
            transition: 'all 0.2s',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '6px',
            opacity: selectedAreaId ? 1 : 0.6
          }}
          onMouseEnter={(e) => {
            if (selectedAreaId) {
              e.target.style.borderColor = '#3b82f6';
              e.target.style.boxShadow = '0 2px 6px rgba(59, 130, 246, 0.2)';
            }
          }}
          onMouseLeave={(e) => {
            if (selectedAreaId) {
              e.target.style.borderColor = '#e5e7eb';
              e.target.style.boxShadow = 'none';
            }
          }}
        >
          Thêm phòng
        </button>

        <button
          className="action-btn"
          onClick={handleAddFactory}
          disabled={!selectedAreaId}
          style={{
            width: '100%',
            padding: '10px 12px',
            borderRadius: '8px',
            border: selectedAreaId ? '2px solid #e5e7eb' : '2px solid #f3e8e8',
            background: selectedAreaId ? 'linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%)' : '#f9fafb',
            color: selectedAreaId ? '#334155' : '#d1d5db',
            cursor: selectedAreaId ? 'pointer' : 'not-allowed',
            fontSize: '13px',
            fontWeight: '600',
            transition: 'all 0.2s',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '6px',
            opacity: selectedAreaId ? 1 : 0.6
          }}
          onMouseEnter={(e) => {
            if (selectedAreaId) {
              e.target.style.borderColor = '#64748b';
              e.target.style.boxShadow = '0 2px 6px rgba(100, 116, 139, 0.2)';
            }
          }}
          onMouseLeave={(e) => {
            if (selectedAreaId) {
              e.target.style.borderColor = '#e5e7eb';
              e.target.style.boxShadow = 'none';
            }
          }}
        >
          Thêm vật cản
        </button>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            className="action-btn"
            onClick={handleAddSeat}
            disabled={!selectedZone}
            style={{
              flex: 1,
              padding: '10px 12px',
              borderRadius: '8px',
              border: selectedZone ? '2px solid #e5e7eb' : '2px solid #f3e8e8',
              background: selectedZone ? 'linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)' : '#f9fafb',
              color: selectedZone ? '#92400e' : '#d1d5db',
              cursor: selectedZone ? 'pointer' : 'not-allowed',
              fontSize: '13px',
              fontWeight: '600',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '4px',
              opacity: selectedZone ? 1 : 0.6
            }}
            onMouseEnter={(e) => {
              if (selectedZone) {
                e.target.style.borderColor = '#f59e0b';
                e.target.style.boxShadow = '0 2px 6px rgba(245, 158, 11, 0.2)';
              }
            }}
            onMouseLeave={(e) => {
              if (selectedZone) {
                e.target.style.borderColor = '#e5e7eb';
                e.target.style.boxShadow = 'none';
              }
            }}
          >
            Thêm ghế
          </button>

          <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
            <input
              type="number"
              min="1"
              max="20"
              value={seatCount}
              onChange={(e) =>
                setSeatCount(
                  Math.max(1, Math.min(20, Number(e.target.value) || 1))
                )
              }
              style={{
                width: '45px',
                padding: '6px 8px',
                borderRadius: '6px',
                border: '1px solid #d1d5db',
                fontSize: '12px',
                fontWeight: '600',
                textAlign: 'center'
              }}
            />
            <button
              className="action-btn"
              onClick={handleAddMultipleSeats}
              disabled={!selectedZone}
              style={{
                width: '45px',
                height: '36px',
                padding: '6px 8px',
                borderRadius: '6px',
                border: selectedZone ? '2px solid #e5e7eb' : '2px solid #f3e8e8',
                background: selectedZone ? 'linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%)' : '#f9fafb',
                color: selectedZone ? '#1e40af' : '#d1d5db',
                cursor: selectedZone ? 'pointer' : 'not-allowed',
                fontSize: '12px',
                fontWeight: '600',
                transition: 'all 0.2s',
                opacity: selectedZone ? 1 : 0.6
              }}
              onMouseEnter={(e) => {
                if (selectedZone) {
                  e.target.style.borderColor = '#3b82f6';
                  e.target.style.boxShadow = '0 2px 6px rgba(59, 130, 246, 0.2)';
                }
              }}
              onMouseLeave={(e) => {
                if (selectedZone) {
                  e.target.style.borderColor = '#e5e7eb';
                  e.target.style.boxShadow = 'none';
                }
              }}
            >
              Tạo 
            </button>
          </div>
        </div>
      </div>

      {/* ===== AREA LIST ===== */}
      <div className="sidebar-section" style={{
        padding: '16px 12px',
        borderBottom: '1px solid #e5e7eb'
      }}>
        <h3 style={{
          margin: '0 0 10px 0',
          fontSize: '12px',
          fontWeight: '700',
          textTransform: 'uppercase',
          color: '#6b7280',
          letterSpacing: '0.5px'
        }}>
          Khu vực
        </h3>
        <ul style={{ listStyle: 'none', margin: 0, padding: 0 }}>
          {areas.map((area) => (
            <li
              key={area.areaId}
              onClick={(e) => {
                console.log("Area click event fired:", area.areaId);
                handleSelectArea(area.areaId);
              }}
              style={{
                padding: '10px 12px',
                marginBottom: '6px',
                borderRadius: '6px',
                cursor: 'pointer',
                fontSize: '13px',
                fontWeight: '500',
                color: isAreaSelected(area.areaId) ? '#166534' : '#374151',
                background: isAreaSelected(area.areaId) ? '#dcfce7' : '#f9fafb',
                border: isAreaSelected(area.areaId) ? '2px solid #22c55e' : '1px solid #e5e7eb',
                transition: 'all 0.2s'
              }}
              onMouseEnter={(e) => {
                if (!isAreaSelected(area.areaId)) {
                  e.target.style.background = '#f3f4f6';
                  e.target.style.borderColor = '#d1d5db';
                }
              }}
              onMouseLeave={(e) => {
                if (!isAreaSelected(area.areaId)) {
                  e.target.style.background = '#f9fafb';
                  e.target.style.borderColor = '#e5e7eb';
                }
              }}
            >
              {area.areaName}
            </li>
          ))}
        </ul>
      </div>

      {/* ===== ZONE LIST ===== */}
      <div className="sidebar-section" style={{
        padding: '16px 12px',
        borderBottom: '1px solid #e5e7eb',
        flex: 1,
        overflow: 'auto'
      }}>
        <h3 style={{
          margin: '0 0 10px 0',
          fontSize: '12px',
          fontWeight: '700',
          textTransform: 'uppercase',
          color: '#6b7280',
          letterSpacing: '0.5px'
        }}>
          Phòng
        </h3>
        <ul style={{ 
          listStyle: 'none', 
          margin: 0, 
          padding: 0,
          maxHeight: '150px', // Hiển thị tối đa 3 zones
          overflowY: 'auto',
          overflowX: 'hidden'
        }}>
          {currentAreaZones.map((zone) => (
            <li
              key={zone.zoneId}
              onClick={() => handleSelectZone(zone.zoneId)}
              style={{
                padding: '10px 12px',
                marginBottom: '6px',
                borderRadius: '6px',
                cursor: 'pointer',
                fontSize: '13px',
                fontWeight: '500',
                color: isZoneSelected(zone.zoneId) ? '#1e40af' : '#374151',
                background: isZoneSelected(zone.zoneId) ? '#dbeafe' : '#f9fafb',
                border: isZoneSelected(zone.zoneId) ? '2px solid #3b82f6' : '1px solid #e5e7eb',
                transition: 'all 0.2s',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}
              onMouseEnter={(e) => {
                if (!isZoneSelected(zone.zoneId)) {
                  e.target.style.background = '#f3f4f6';
                  e.target.style.borderColor = '#d1d5db';
                }
              }}
              onMouseLeave={(e) => {
                if (!isZoneSelected(zone.zoneId)) {
                  e.target.style.background = '#f9fafb';
                  e.target.style.borderColor = '#e5e7eb';
                }
              }}
            >
              <span>{zone.zoneName}</span>
              <span style={{
                fontSize: '11px',
                fontWeight: '600',
                backgroundColor: isZoneSelected(zone.zoneId) ? 'rgba(30, 64, 175, 0.1)' : '#f0f0f0',
                color: isZoneSelected(zone.zoneId) ? '#1e40af' : '#6b7280',
                padding: '2px 6px',
                borderRadius: '4px',
                minWidth: '28px',
                textAlign: 'center'
              }}>
                {seats.filter((s) => s.zoneId === zone.zoneId).length}
              </span>
            </li>
          ))}
          {currentAreaZones.length === 0 && (
            <li style={{
              padding: '16px 12px',
              textAlign: 'center',
              color: '#999',
              fontSize: '12px',
              fontStyle: 'italic'
            }}>
              Chưa có phòng
            </li>
          )}
        </ul>
      </div>

      {/* ===== FOOTER ===== */}
      <div style={{
        padding: '12px',
        borderTop: '1px solid #e5e7eb',
        background: 'linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%)',
        display: 'grid',
        gridTemplateColumns: '1fr 1fr 1fr',
        gap: '8px'
      }}>
        <div style={{
          padding: '10px',
          borderRadius: '6px',
          background: 'white',
          border: '1px solid #e5e7eb',
          textAlign: 'center'
        }}>
          <div style={{
            fontSize: '18px',
            fontWeight: '700',
            color: '#166534',
            lineHeight: '1'
          }}>
            {areas.length}
          </div>
          <div style={{
            fontSize: '10px',
            color: '#6b7280',
            fontWeight: '600',
            marginTop: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px'
          }}>
            Khu vực
          </div>
        </div>

        <div style={{
          padding: '10px',
          borderRadius: '6px',
          background: 'white',
          border: '1px solid #e5e7eb',
          textAlign: 'center'
        }}>
          <div style={{
            fontSize: '18px',
            fontWeight: '700',
            color: '#1e40af',
            lineHeight: '1'
          }}>
            {zones.length}
          </div>
          <div style={{
            fontSize: '10px',
            color: '#6b7280',
            fontWeight: '600',
            marginTop: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px'
          }}>
            Phòng
          </div>
        </div>

        <div style={{
          padding: '10px',
          borderRadius: '6px',
          background: 'white',
          border: '1px solid #e5e7eb',
          textAlign: 'center'
        }}>
          <div style={{
            fontSize: '18px',
            fontWeight: '700',
            color: '#b45309',
            lineHeight: '1'
          }}>
            {seats.length}
          </div>
          <div style={{
            fontSize: '10px',
            color: '#6b7280',
            fontWeight: '600',
            marginTop: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px'
          }}>
            Ghế
          </div>
        </div>
      </div>

      {/* ===== ROW NUMBER MODAL ===== */}
      {showRowModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            borderRadius: '8px',
            padding: '24px',
            width: '90%',
            maxWidth: '400px',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
          }}>
            <h3 style={{ marginTop: 0, marginBottom: '16px', color: '#1f2937' }}>
              Chọn hàng ghế
            </h3>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontSize: '14px',
                fontWeight: '600',
                color: '#374151'
              }}>
                Hàng ghế (A, B, C, ...)
              </label>
              <input
                type="text"
                maxLength="1"
                value={rowLetter}
                onChange={(e) => {
                  const val = e.target.value.toUpperCase();
                  if (/^[A-Z]?$/.test(val)) {
                    setRowLetter(val);
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') confirmAddSeat();
                }}
                placeholder="A"
                style={{
                  width: '100%',
                  padding: '8px 12px',
                  border: '2px solid #e5e7eb',
                  borderRadius: '6px',
                  fontSize: '14px',
                  boxSizing: 'border-box',
                  textTransform: 'uppercase',
                  textAlign: 'center',
                  fontWeight: '600'
                }}
                autoFocus
              />
              <p style={{
                margin: '8px 0 0 0',
                fontSize: '12px',
                color: '#6b7280'
              }}>
                💡 Nhập chữ cái A-Z 
              </p>
            </div>

            {seatCount > 1 && (
              <div style={{
                marginBottom: '16px',
                padding: '12px',
                backgroundColor: '#f0f9ff',
                borderRadius: '6px',
                border: '1px solid #bfdbfe'
              }}>
                <p style={{
                  margin: 0,
                  fontSize: '13px',
                  color: '#1e40af',
                  fontWeight: '500'
                }}>
                  Sẽ thêm {seatCount} ghế vào hàng {rowLetter || 'A'}
                </p>
              </div>
            )}
            
            <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
              <button
                onClick={() => {
                  setShowRowModal(false);
                  setRowNumber(1);
                  setRowLetter('A');
                }}
                style={{
                  padding: '8px 16px',
                  border: '1px solid #ddd',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  backgroundColor: '#f3f4f6',
                  color: '#374151',
                  fontSize: '14px',
                  fontWeight: '600'
                }}
              >
                Hủy
              </button>
              <button
                onClick={seatCount > 1 ? confirmAddMultipleSeats : confirmAddSeat}
                style={{
                  padding: '8px 16px',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  background: 'linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%)',
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '600'
                }}
              >
                Thêm {seatCount > 1 ? seatCount : '1'} Ghế
              </button>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}

export default Sidebar;

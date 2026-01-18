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

// Zone Type Options - All use light gray, simplified
const ZONE_TYPES = [
  { id: 'silent', name: 'Khu Yên Tĩnh' },
  { id: 'discussion', name: 'Khu Thảo Luận' },
  { id: 'computer', name: 'Khu Máy Tính' },
  { id: 'self_study', name: 'Khu Tự Học' },
  { id: 'general', name: 'Khu Chung' },
];

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
  const [rowLetter, setRowLetter] = useState('A');
  const [showRowModal, setShowRowModal] = useState(false);
  const [showZoneTypeModal, setShowZoneTypeModal] = useState(false);
  const [selectedZoneType, setSelectedZoneType] = useState(ZONE_TYPES[0]);

  /* ================= FILTER ================= */
  const currentAreaZones = zones.filter((z) => z.areaId === selectedAreaId);

  const selectedZone = selectedZoneId
    ? zones.find((z) => z.zoneId === selectedZoneId)
    : selectedItem?.type === "zone"
      ? zones.find((z) => z.zoneId === selectedItem.id)
      : null;

  // Calculate statistics
  const totalSeats = seats.length;
  const seatsInCurrentArea = seats.filter(s =>
    currentAreaZones.some(z => z.zoneId === s.zoneId)
  ).length;

  /* ================= HANDLERS ================= */

  const handleAddArea = async () => {
    const DEFAULT = { x: 50, y: 50 };
    const NEW_SIZE = { width: 800, height: 600 };
    let positionX = DEFAULT.x;
    let positionY = DEFAULT.y;

    if (areas && areas.length > 0) {
      const GAP = 60;
      const anchor = selectedAreaId
        ? areas.find(a => a.areaId === selectedAreaId)
        : null;

      if (anchor) {
        const rowPeers = areas.filter(a => Math.abs((a.positionY || 0) - (anchor.positionY || 0)) < 100);
        const rightMost = rowPeers.reduce((acc, a) => {
          const right = (a.positionX || 0) + (a.width || NEW_SIZE.width);
          return right > acc.maxRight ? { maxRight: right, area: a } : acc;
        }, { maxRight: (anchor.positionX || 0) + (anchor.width || NEW_SIZE.width), area: anchor });

        positionX = rightMost.maxRight + GAP;
        positionY = anchor.positionY || DEFAULT.y;
      } else {
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

    const newAreaName = `Phòng thư viện ${areas.length + 1}`;
    const res = await createArea({
      areaName: newAreaName,
      width: NEW_SIZE.width,
      height: NEW_SIZE.height,
      positionX,
      positionY,
    });

    dispatch({ type: actions.ADD_AREA, payload: res.data });
    dispatch({ type: actions.SELECT_AREA, payload: res.data.areaId });
  };

  const handleAddZone = async () => {
    if (!selectedAreaId) {
      alert("Vui lòng chọn phòng thư viện trước");
      return;
    }
    setShowZoneTypeModal(true);
  };

  const confirmAddZone = async () => {
    try {
      // Calculate position based on existing zones
      const existingZones = zones.filter(z => z.areaId === selectedAreaId);
      let posX = 20;
      let posY = 20;

      if (existingZones.length > 0) {
        const lastZone = existingZones[existingZones.length - 1];
        posX = (lastZone.positionX || 0) + (lastZone.width || 200) + 20;
        if (posX > 500) {
          posX = 20;
          posY = (lastZone.positionY || 0) + (lastZone.height || 150) + 20;
        }
      }

      const payload = {
        zoneName: selectedZoneType.name,
        areaId: selectedAreaId,
        hasPowerOutlet: selectedZoneType.id === 'computer',
        width: 250,
        height: 200,
        positionX: posX,
        positionY: posY,
        color: selectedZoneType.color,
      };

      const res = await createZone(payload);

      const zoneNormalized = {
        zoneId: res.data?.zone_id ?? res.data?.zoneId,
        zoneName: res.data?.zone_name ?? res.data?.zoneName ?? selectedZoneType.name,
        areaId: res.data?.area_id ?? res.data?.areaId ?? selectedAreaId,
        positionX: res.data?.position_x ?? res.data?.positionX ?? posX,
        positionY: res.data?.position_y ?? res.data?.positionY ?? posY,
        width: res.data?.width ?? 250,
        height: res.data?.height ?? 200,
        color: res.data?.color ?? selectedZoneType.color,
        isLocked: res.data?.is_locked ?? res.data?.isLocked ?? false,
        isFixed: res.data?.is_fixed ?? res.data?.isFixed ?? false,
        fixedType: res.data?.fixed_type ?? res.data?.fixedType,
        amenities: res.data?.amenities ?? [],
      };

      dispatch({ type: actions.ADD_ZONE, payload: zoneNormalized });
      setShowZoneTypeModal(false);
    } catch (e) {
      console.error("Failed to create zone:", e);
      alert("Tạo khu vực thất bại: " + (e.response?.data?.message || e.message));
    }
  };

  const handleAddFactory = async () => {
    if (!selectedAreaId) {
      alert("Vui lòng chọn phòng thư viện trước");
      return;
    }

    try {
      const payload = {
        factoryName: "Vật cản mới",
        width: 120,
        height: 80,
        positionX: 20,
        positionY: 20,
        areaId: selectedAreaId,
        isActive: true,
      };

      const res = await createAreaFactoryInArea(selectedAreaId, payload);
      const factoryData = {
        factoryId: res.data.factory_id || res.data.factoryId,
        factoryName: res.data.factory_name || res.data.factoryName,
        positionX: res.data.position_x || res.data.positionX,
        positionY: res.data.position_y || res.data.positionY,
        width: res.data.width,
        height: res.data.height,
        color: res.data.color || "#9CA3AF",
        areaId: res.data.area_id || res.data.areaId || selectedAreaId,
      };
      dispatch({ type: actions.ADD_FACTORY, payload: factoryData });
    } catch (e) {
      console.error("Failed to create factory:", e);
      alert("Tạo vật cản thất bại: " + (e.response?.data?.message || e.message));
    }
  };

  const handleAddSeat = async () => {
    if (!selectedZone) {
      alert("Vui lòng chọn khu vực ghế trước");
      return;
    }
    setShowRowModal(true);
  };

  const confirmAddSeat = async () => {
    const rowNum = rowLetter.toUpperCase().charCodeAt(0) - 64;

    if (!selectedZone || !rowLetter || rowNum < 1 || rowNum > 26) {
      alert("Vui lòng chọn hàng hợp lệ (A-Z)");
      return;
    }

    try {
      const seatsInZone = seats.filter(s => s.zoneId === selectedZone.zoneId);

      const seatData = generateNewSeatData({
        zoneId: selectedZone.zoneId,
        rowNumber: rowNum,
        seatsInZone,
        seatHeight: 44,
      });

      const res = await createSeat(seatData);

      const completeSeatData = {
        ...res.data,
        rowNumber: seatData.rowNumber,
        columnNumber: seatData.columnNumber,
        seatCode: seatData.seatCode,
      };

      dispatch({ type: actions.ADD_SEAT, payload: completeSeatData });
      setShowRowModal(false);
    } catch (e) {
      console.error("Error adding seat:", e);
      alert("Tạo ghế thất bại: " + (e.response?.data?.message || e.message));
    }
  };

  // OPTIMISTIC UPDATE: Add seats to UI immediately, API in parallel
  const confirmAddMultipleSeats = () => {
    const rowNum = rowLetter.toUpperCase().charCodeAt(0) - 64;

    if (!selectedZone || !rowLetter || rowNum < 1 || rowNum > 26) {
      alert("Vui lòng chọn hàng hợp lệ (A-Z)");
      return;
    }

    let seatsInZone = seats.filter(s => s.zoneId === selectedZone.zoneId);
    const newSeats = [];

    // Generate all seats first
    for (let i = 0; i < seatCount; i++) {
      const seatData = generateNewSeatData({
        zoneId: selectedZone.zoneId,
        rowNumber: rowNum,
        seatsInZone: [...seatsInZone, ...newSeats],
        seatHeight: 44,
      });

      // Generate temporary ID for optimistic update
      const tempId = `temp-${Date.now()}-${i}`;
      const tempSeat = {
        ...seatData,
        seatId: tempId,
        rowNumber: seatData.rowNumber,
        columnNumber: seatData.columnNumber,
        seatCode: seatData.seatCode,
      };
      newSeats.push(tempSeat);
    }

    // Add all seats to UI immediately (optimistic)
    newSeats.forEach(seat => {
      dispatch({ type: actions.ADD_SEAT, payload: seat });
    });
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    setShowRowModal(false);

    // Call API in background for each seat (non-blocking)
    newSeats.forEach(tempSeat => {
      createSeat(tempSeat).then(res => {
        // Replace temp seat with real seat from API
        dispatch({ type: actions.UPDATE_SEAT, payload: { ...res.data, tempId: tempSeat.seatId } });
      }).catch(e => {
        console.error("Failed to create seat:", e);
      });
    });
  };

  const handleSelectArea = (areaId) => {
    dispatch({ type: actions.SELECT_AREA, payload: areaId });
  };

  const handleSelectZone = async (zoneId) => {
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
        isActive: (s.is_active ?? s.isActive) ?? true,
      }));
      // Use MERGE_SEATS instead of SET_SEATS to preserve other zones' seats
      dispatch({ type: actions.MERGE_SEATS, payload: { zoneId, seats: seatsNormalized } });
    } catch (e) {
      console.error("Failed to load seats:", e);
      // Don't clear all seats on error
    }
  };

  const isAreaSelected = (id) =>
    selectedItem?.type === "area" ? selectedItem.id === id : selectedAreaId === id;

  const isZoneSelected = (id) =>
    selectedItem?.type === "zone" && selectedItem.id === id;

  const getZoneTypeIcon = (zoneName) => {
    const type = ZONE_TYPES.find(t => zoneName?.includes(t.name.split(' ')[1]));
    return type?.icon || '📍';
  };

  /* ================= RENDER ================= */

  return (
    <aside className="sidebar_area" style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      background: 'linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%)',
      borderRight: '1px solid #E2E8F0'
    }}>
      {/* Header */}
      <div style={{
        padding: '20px 16px',
        background: 'linear-gradient(135deg, #FF751F 0%, #E85A00 100%)',
        color: 'white',
        flexShrink: 0
      }}>
        <h2 style={{
          margin: 0,
          fontSize: '18px',
          fontWeight: '700',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}>
          Sơ đồ thư viện
        </h2>
        <p style={{
          margin: '6px 0 0 0',
          fontSize: '12px',
          opacity: 0.9
        }}>
          Thiết kế và quản lý bố cục
        </p>
      </div>

      {/* Quick Actions */}
      <div style={{
        padding: '16px',
        borderBottom: '1px solid #E2E8F0'
      }}>
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '10px'
        }}>
          <button
            onClick={handleAddArea}
            style={{
              padding: '10px 12px',
              borderRadius: '8px',
              border: '1px solid #E2E8F0',
              background: 'white',
              color: '#374151',
              cursor: 'pointer',
              fontSize: '12px',
              fontWeight: '600',
              transition: 'all 0.2s'
            }}
          >
            + Phòng
          </button>

          <button
            onClick={handleAddZone}
            disabled={!selectedAreaId}
            style={{
              padding: '10px 12px',
              borderRadius: '8px',
              border: '1px solid #E2E8F0',
              background: selectedAreaId ? 'white' : '#F9FAFB',
              color: selectedAreaId ? '#374151' : '#9CA3AF',
              cursor: selectedAreaId ? 'pointer' : 'not-allowed',
              fontSize: '12px',
              fontWeight: '600',
              transition: 'all 0.2s',
              opacity: selectedAreaId ? 1 : 0.6
            }}
          >
            + Khu vực
          </button>

          <button
            onClick={handleAddFactory}
            disabled={!selectedAreaId}
            style={{
              padding: '10px 12px',
              borderRadius: '8px',
              border: '1px solid #E2E8F0',
              background: selectedAreaId ? 'white' : '#F9FAFB',
              color: selectedAreaId ? '#374151' : '#9CA3AF',
              cursor: selectedAreaId ? 'pointer' : 'not-allowed',
              fontSize: '12px',
              fontWeight: '600',
              transition: 'all 0.2s',
              opacity: selectedAreaId ? 1 : 0.6
            }}
          >
            + Hình
          </button>

          <button
            onClick={handleAddSeat}
            disabled={!selectedZone}
            style={{
              padding: '10px 12px',
              borderRadius: '8px',
              border: '1px solid #E2E8F0',
              background: selectedZone ? 'white' : '#F9FAFB',
              color: selectedZone ? '#374151' : '#9CA3AF',
              cursor: selectedZone ? 'pointer' : 'not-allowed',
              fontSize: '12px',
              fontWeight: '600',
              transition: 'all 0.2s',
              opacity: selectedZone ? 1 : 0.6
            }}
          >
            + Ghế
          </button>
        </div>

        {/* Bulk Add Seats */}
        {selectedZone && (
          <div style={{
            marginTop: '12px',
            padding: '12px',
            background: '#F0F9FF',
            borderRadius: '10px',
            border: '1px solid #BAE6FD'
          }}>
            <div style={{
              fontSize: '11px',
              fontWeight: '600',
              color: '#0369A1',
              marginBottom: '8px'
            }}>
              Thêm nhanh nhiều ghế
            </div>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <input
                type="number"
                min="1"
                max="20"
                value={seatCount}
                onChange={(e) => setSeatCount(Math.max(1, Math.min(20, Number(e.target.value) || 1)))}
                style={{
                  width: '50px',
                  padding: '8px',
                  borderRadius: '8px',
                  border: '2px solid #BAE6FD',
                  fontSize: '13px',
                  fontWeight: '600',
                  textAlign: 'center'
                }}
              />
              <span style={{ fontSize: '12px', color: '#0369A1' }}>ghế</span>
              <button
                onClick={() => setShowRowModal(true)}
                style={{
                  flex: 1,
                  padding: '8px 12px',
                  borderRadius: '8px',
                  border: 'none',
                  background: 'linear-gradient(135deg, #0EA5E9 0%, #0284C7 100%)',
                  color: 'white',
                  fontSize: '12px',
                  fontWeight: '600',
                  cursor: 'pointer'
                }}
              >
                Tạo {seatCount} ghế
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Library Rooms List */}
      <div style={{
        padding: '16px',
        borderBottom: '1px solid #E2E8F0'
      }}>
        <h3 style={{
          margin: '0 0 12px 0',
          fontSize: '11px',
          fontWeight: '700',
          textTransform: 'uppercase',
          color: '#64748B',
          letterSpacing: '0.5px',
          display: 'flex',
          alignItems: 'center',
          gap: '6px'
        }}>
          Phòng thư viện
          <span style={{
            background: '#FF751F',
            color: 'white',
            padding: '2px 8px',
            borderRadius: '10px',
            fontSize: '10px'
          }}>{areas.length}</span>
        </h3>
        <ul style={{ listStyle: 'none', margin: 0, padding: 0 }}>
          {areas.map((area) => (
            <li
              key={area.areaId}
              onClick={() => handleSelectArea(area.areaId)}
              style={{
                padding: '12px 14px',
                marginBottom: '8px',
                borderRadius: '10px',
                cursor: 'pointer',
                fontSize: '13px',
                fontWeight: '600',
                color: isAreaSelected(area.areaId) ? '#C2410C' : '#374151',
                background: isAreaSelected(area.areaId)
                  ? 'linear-gradient(135deg, #FFF7F2 0%, #FFEDD5 100%)'
                  : '#F8FAFC',
                border: isAreaSelected(area.areaId)
                  ? '2px solid #FF751F'
                  : '2px solid transparent',
                transition: 'all 0.2s',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}
            >
              <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '16px' }}></span>
                {area.areaName}
              </span>
              <span style={{
                fontSize: '10px',
                padding: '4px 8px',
                borderRadius: '6px',
                background: isAreaSelected(area.areaId) ? '#FF751F' : '#E2E8F0',
                color: isAreaSelected(area.areaId) ? 'white' : '#64748B',
                fontWeight: '700'
              }}>
                {zones.filter(z => z.areaId === area.areaId).length} khu
              </span>
            </li>
          ))}
          {areas.length === 0 && (
            <li style={{
              padding: '20px',
              textAlign: 'center',
              color: '#9CA3AF',
              fontSize: '13px',
              background: '#F8FAFC',
              borderRadius: '10px',
              border: '2px dashed #E2E8F0'
            }}>
              Chưa có phòng thư viện nào
            </li>
          )}
        </ul>
      </div>

      {/* Zones List */}
      <div style={{
        padding: '16px',
        flex: 1,
        overflow: 'auto'
      }}>
        <h3 style={{
          margin: '0 0 12px 0',
          fontSize: '11px',
          fontWeight: '700',
          textTransform: 'uppercase',
          color: '#64748B',
          letterSpacing: '0.5px',
          display: 'flex',
          alignItems: 'center',
          gap: '6px'
        }}>
          Khu vực ghế
          <span style={{
            background: '#22C55E',
            color: 'white',
            padding: '2px 8px',
            borderRadius: '10px',
            fontSize: '10px'
          }}>{currentAreaZones.length}</span>
        </h3>
        <ul style={{
          listStyle: 'none',
          margin: 0,
          padding: 0,
          maxHeight: '200px',
          overflowY: 'auto'
        }}>
          {currentAreaZones.map((zone) => {
            const zoneSeats = seats.filter(s => s.zoneId === zone.zoneId);
            return (
              <li
                key={zone.zoneId}
                onClick={() => handleSelectZone(zone.zoneId)}
                style={{
                  padding: '12px 14px',
                  marginBottom: '8px',
                  borderRadius: '10px',
                  cursor: 'pointer',
                  fontSize: '13px',
                  fontWeight: '600',
                  color: isZoneSelected(zone.zoneId) ? '#C2410C' : '#374151',
                  background: isZoneSelected(zone.zoneId)
                    ? 'linear-gradient(135deg, #FFF7F2 0%, #FFEDD5 100%)'
                    : '#F8FAFC',
                  border: isZoneSelected(zone.zoneId)
                    ? '2px solid #FF751F'
                    : '2px solid transparent',
                  transition: 'all 0.2s',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}
              >
                <span style={{ fontWeight: '600' }}>{zone.zoneName}</span>
                <span style={{
                  fontSize: '11px',
                  padding: '4px 10px',
                  borderRadius: '6px',
                  background: isZoneSelected(zone.zoneId) ? '#FF751F' : '#E2E8F0',
                  color: isZoneSelected(zone.zoneId) ? 'white' : '#64748B',
                  fontWeight: '700'
                }}>
                  {zoneSeats.length} ghế
                </span>
              </li>
            );
          })}
          {currentAreaZones.length === 0 && selectedAreaId && (
            <li style={{
              padding: '20px',
              textAlign: 'center',
              color: '#9CA3AF',
              fontSize: '13px',
              background: '#F8FAFC',
              borderRadius: '10px',
              border: '2px dashed #E2E8F0'
            }}>
              Chưa có khu vực nào trong phòng này
            </li>
          )}
          {!selectedAreaId && (
            <li style={{
              padding: '20px',
              textAlign: 'center',
              color: '#9CA3AF',
              fontSize: '13px',
              background: '#FEF3C7',
              borderRadius: '10px',
              border: '2px dashed #FCD34D'
            }}>
              ⚠️ Chọn phòng thư viện để xem khu vực
            </li>
          )}
        </ul>
      </div>

      {/* Statistics Footer */}
      <div style={{
        padding: '16px',
        borderTop: '1px solid #E2E8F0',
        background: 'linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%)'
      }}>
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr 1fr',
          gap: '10px'
        }}>
          <div style={{
            padding: '12px 8px',
            borderRadius: '10px',
            background: 'white',
            border: '1px solid #E2E8F0',
            textAlign: 'center'
          }}>
            <div style={{
              fontSize: '20px',
              fontWeight: '700',
              color: '#C2410C',
              lineHeight: '1'
            }}>
              {areas.length}
            </div>
            <div style={{
              fontSize: '9px',
              color: '#64748B',
              fontWeight: '600',
              marginTop: '4px',
              textTransform: 'uppercase'
            }}>
              Phòng
            </div>
          </div>

          <div style={{
            padding: '12px 8px',
            borderRadius: '10px',
            background: 'white',
            border: '1px solid #E2E8F0',
            textAlign: 'center'
          }}>
            <div style={{
              fontSize: '20px',
              fontWeight: '700',
              color: '#166534',
              lineHeight: '1'
            }}>
              {zones.length}
            </div>
            <div style={{
              fontSize: '9px',
              color: '#64748B',
              fontWeight: '600',
              marginTop: '4px',
              textTransform: 'uppercase'
            }}>
              Khu vực
            </div>
          </div>

          <div style={{
            padding: '12px 8px',
            borderRadius: '10px',
            background: 'white',
            border: '1px solid #E2E8F0',
            textAlign: 'center'
          }}>
            <div style={{
              fontSize: '20px',
              fontWeight: '700',
              color: '#1E40AF',
              lineHeight: '1'
            }}>
              {totalSeats}
            </div>
            <div style={{
              fontSize: '9px',
              color: '#64748B',
              fontWeight: '600',
              marginTop: '4px',
              textTransform: 'uppercase'
            }}>
              Tổng ghế
            </div>
          </div>
        </div>
      </div>

      {/* Row Selection Modal */}
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
            borderRadius: '16px',
            padding: '28px',
            width: '90%',
            maxWidth: '400px',
            boxShadow: '0 20px 60px rgba(0, 0, 0, 0.2)'
          }}>
            <h3 style={{
              marginTop: 0,
              marginBottom: '20px',
              color: '#1F2937',
              fontSize: '18px',
              fontWeight: '700',
              display: 'flex',
              alignItems: 'center',
              gap: '10px'
            }}>
              Thêm ghế vào khu vực
            </h3>

            <div style={{ marginBottom: '20px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontSize: '14px',
                fontWeight: '600',
                color: '#374151'
              }}>
                Hàng ghế (A-Z)
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
                  if (e.key === 'Enter') {
                    seatCount > 1 ? confirmAddMultipleSeats() : confirmAddSeat();
                  }
                }}
                placeholder="A"
                style={{
                  width: '100%',
                  padding: '14px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '24px',
                  fontWeight: '700',
                  textAlign: 'center',
                  textTransform: 'uppercase'
                }}
                autoFocus
              />
              <p style={{
                margin: '10px 0 0 0',
                fontSize: '13px',
                color: '#64748B'
              }}>
                💡 Nhập A cho hàng A, B cho hàng B, v.v.
              </p>
            </div>

            {seatCount > 1 && (
              <div style={{
                marginBottom: '20px',
                padding: '14px',
                backgroundColor: '#F0F9FF',
                borderRadius: '12px',
                border: '1px solid #BAE6FD'
              }}>
                <p style={{
                  margin: 0,
                  fontSize: '14px',
                  color: '#0369A1',
                  fontWeight: '600'
                }}>
                  Sẽ tạo {seatCount} ghế: {rowLetter}1, {rowLetter}2, ... {rowLetter}{seatCount}
                </p>
              </div>
            )}

            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                onClick={() => {
                  setShowRowModal(false);
                  setRowLetter('A');
                }}
                style={{
                  flex: 1,
                  padding: '14px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  backgroundColor: '#F8FAFC',
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
                  flex: 1,
                  padding: '14px',
                  border: 'none',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  background: 'linear-gradient(135deg, #FF751F 0%, #E85A00 100%)',
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '600',
                  boxShadow: '0 4px 14px rgba(255, 117, 31, 0.3)'
                }}
              >
                Tạo {seatCount > 1 ? seatCount : ''} Ghế
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Zone Type Selection Modal */}
      {showZoneTypeModal && (
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
            borderRadius: '16px',
            padding: '28px',
            width: '90%',
            maxWidth: '450px',
            boxShadow: '0 20px 60px rgba(0, 0, 0, 0.2)'
          }}>
            <h3 style={{
              marginTop: 0,
              marginBottom: '20px',
              color: '#1F2937',
              fontSize: '18px',
              fontWeight: '700',
              display: 'flex',
              alignItems: 'center',
              gap: '10px'
            }}>
              Chọn loại khu vực
            </h3>

            <div style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: '12px',
              marginBottom: '24px'
            }}>
              {ZONE_TYPES.map((type) => (
                <button
                  key={type.id}
                  onClick={() => setSelectedZoneType(type)}
                  style={{
                    padding: '16px 12px',
                    borderRadius: '12px',
                    border: selectedZoneType.id === type.id
                      ? `3px solid ${type.color}`
                      : '2px solid #E2E8F0',
                    background: selectedZoneType.id === type.id
                      ? `${type.color}15`
                      : 'white',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: '8px'
                  }}
                >
                  <span style={{ fontSize: '28px' }}>{type.icon}</span>
                  <span style={{
                    fontSize: '12px',
                    fontWeight: '600',
                    color: selectedZoneType.id === type.id ? type.color : '#374151'
                  }}>
                    {type.name}
                  </span>
                </button>
              ))}
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                onClick={() => setShowZoneTypeModal(false)}
                style={{
                  flex: 1,
                  padding: '14px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  backgroundColor: '#F8FAFC',
                  color: '#374151',
                  fontSize: '14px',
                  fontWeight: '600'
                }}
              >
                Hủy
              </button>
              <button
                onClick={confirmAddZone}
                style={{
                  flex: 1,
                  padding: '14px',
                  border: 'none',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  background: `linear-gradient(135deg, ${selectedZoneType.color} 0%, ${selectedZoneType.color}DD 100%)`,
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '600',
                  boxShadow: `0 4px 14px ${selectedZoneType.color}40`
                }}
              >
                Tạo {selectedZoneType.name}
              </button>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}

export default Sidebar;

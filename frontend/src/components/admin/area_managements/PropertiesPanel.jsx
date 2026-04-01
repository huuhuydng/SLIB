import { useState, useEffect } from "react";
import { useToast } from "../../common/ToastProvider";
import { useLayout } from "../../../context/admin/area_management/LayoutContext";
import {
  updateArea,
  updateAreaLocked,
  updateAreaIsActive,
  updateZone,
  updateSeat,
  updateAreaFactory,
  deleteArea,
  deleteZone,
  deleteSeat,
  deleteAreaFactory,
  getAmenitiesByZone,
  deleteAmenity,
  createAmenity,
  updateSeatNfcUid,
  clearSeatNfcUid,
} from "../../../services/admin/area_management/api";
import nfcManagementService from "../../../services/admin/nfcManagementService";
import "../../../styles/admin/properties.css";

// Predefined amenities for quick add
const QUICK_AMENITIES = [
  { name: 'Ổ cắm điện' },
  { name: 'Đèn bàn' },
  { name: 'WiFi mạnh' },
  { name: 'Gần cửa sổ' },
  { name: 'Máy lạnh' },
  { name: 'Yên tĩnh' },
];

function PropertiesPanel() {
  const toast = useToast();
  const { state, dispatch, actions } = useLayout();
  const { selectedItem, areas, zones, seats, factories } = state;

  // IME composition handling
  const [isComposing, setIsComposing] = useState(false);

  // Local state for Area
  const [localAreaName, setLocalAreaName] = useState("");

  // Local state for Zone
  const [localZoneDes, setLocalZoneDes] = useState("");
  const [localZoneName, setLocalZoneName] = useState("");

  // Local state for Seat
  const [localSeatCode, setLocalSeatCode] = useState("");
  const [localSeatIsActive, setLocalSeatIsActive] = useState(true);
  const [localNfcTagUid, setLocalNfcTagUid] = useState("");

  // NFC Scanning state
  const [nfcScanning, setNfcScanning] = useState(false);
  const [nfcError, setNfcError] = useState(null);
  const [nfcSaving, setNfcSaving] = useState(false);

  // Local state for Factory
  const [localFactoryName, setLocalFactoryName] = useState("");
  const [localFactoryColor, setLocalFactoryColor] = useState("#9CA3AF");

  // Amenities state
  const [amenities, setAmenities] = useState([]);

  // Add Amenity Modal state
  const [showAddModal, setShowAddModal] = useState(false);
  const [addAmenityName, setAddAmenityName] = useState("");

  // Delete Confirmation Modal state
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Delete Amenity Modal state
  const [deleteAmenityId, setDeleteAmenityId] = useState(null);

  /* ================= GET SELECTED DATA ================= */
  const selectedData = (() => {
    if (!selectedItem) return null;

    switch (selectedItem.type) {
      case "area":
        return areas.find((a) => a.areaId === selectedItem.id);
      case "zone":
        return zones.find((z) => z.zoneId === selectedItem.id);
      case "seat":
        return seats.find((s) => s.seatId === selectedItem.id);
      case "factory":
        return factories.find((f) => f.factoryId === selectedItem.id);
      default:
        return null;
    }
  })();

  // Sync local state when selected item changes
  useEffect(() => {
    if (selectedItem?.type === "area" && selectedData) {
      setLocalAreaName(selectedData.areaName || "");
    } else if (selectedItem?.type === "zone" && selectedData) {
      setLocalZoneDes(selectedData.zoneDes || "");
      setLocalZoneName(selectedData.zoneName || "");
      // Load amenities when zone is selected
      (async () => {
        try {
          const res = await getAmenitiesByZone(selectedData.zoneId);
          const raw = Array.isArray(res?.data) ? res.data : [];
          const normalized = raw.map((a) => ({
            amenityId: a.amenity_id ?? a.amenityId,
            amenityName: a.amenity_name ?? a.amenityName,
            zoneId: a.zone_id ?? a.zoneId ?? selectedData.zoneId,
          }));
          setAmenities(normalized);
        } catch (e) {
          console.error("Failed to load amenities:", e);
          setAmenities([]);
        }
      })();
    } else if (selectedItem?.type === "seat" && selectedData) {
      setLocalSeatCode(selectedData.seatCode || "");
      setLocalSeatIsActive(selectedData.isActive !== false);
      setLocalNfcTagUid(selectedData.nfcTagUid || "");
      setNfcError(null);
    } else if (selectedItem?.type === "factory" && selectedData) {
      setLocalFactoryName(selectedData.factoryName || "");
      setLocalFactoryColor(selectedData.color || "#9CA3AF");
    }
  }, [selectedItem?.id, selectedItem?.type, selectedData?.areaName, selectedData?.zoneDes, selectedData?.zoneName, selectedData?.seatCode, selectedData?.factoryName, selectedData?.color]);

  if (!selectedItem || !selectedData) {
    return (
      <aside className="properties-panel" style={{
        background: 'linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%)',
        borderLeft: '1px solid #E2E8F0',
        display: 'flex',
        flexDirection: 'column',
        height: '100%'
      }}>
        <div style={{
          padding: '20px 16px',
          background: 'linear-gradient(135deg, #F1F5F9 0%, #E2E8F0 100%)',
          borderBottom: '1px solid #E2E8F0'
        }}>
          <h3 style={{
            margin: 0,
            fontSize: '16px',
            fontWeight: '700',
            color: '#64748B'
          }}>Thuộc tính</h3>
        </div>
        <div style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '40px 20px',
          textAlign: 'center'
        }}>
          <div>
            <div style={{ fontSize: '48px', marginBottom: '16px', opacity: 0.5 }}>🖱️</div>
            <p style={{
              margin: 0,
              color: '#94A3B8',
              fontSize: '14px',
              fontWeight: '500'
            }}>
              Chọn phòng, khu vực, ghế hoặc vật cản để chỉnh sửa
            </p>
          </div>
        </div>
      </aside>
    );
  }

  /* ================= HANDLERS ================= */

  const buildSeatPayload = (seat, override = {}) => {
    const payload = {
      seatId: seat?.seatId,
      zoneId: seat?.zoneId,
      seatCode: seat?.seatCode,
      width: seat?.width,
      height: seat?.height,
      rowNumber: seat?.rowNumber,
      columnNumber: seat?.columnNumber,
      positionX: seat?.positionX,
      positionY: seat?.positionY,
      isActive: seat?.isActive,
      ...override,
    };

    Object.keys(payload).forEach((key) => {
      if (payload[key] === undefined || payload[key] === null) delete payload[key];
    });

    return payload;
  };

  // OPTIMISTIC UPDATE: Update UI immediately, API in background
  const handleAreaChange = (field, value) => {
    // Update UI immediately
    const updated = { ...selectedData, [field]: value };
    dispatch({ type: actions.UPDATE_AREA, payload: updated });
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });

    // Sanitize payload: clamp positions to >= 0 (backend rejects negative values)
    const apiPayload = {
      ...updated,
      positionX: Math.max(0, updated.positionX ?? 0),
      positionY: Math.max(0, updated.positionY ?? 0),
    };

    // API call in background (non-blocking)
    updateArea(selectedData.areaId, apiPayload).catch(e => {
      console.error("Failed to update area:", e);
    });
  };

  // Update UI immediately, wait for Save button to persist
  const handleZoneChange = (field, value) => {
    // Update UI immediately
    const updated = { ...selectedData, [field]: value };
    dispatch({ type: actions.UPDATE_ZONE, payload: updated });
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    // API call will be made when user clicks Save button
  };

  // Update UI immediately, wait for Save button to persist
  const handleSeatChange = (field, value) => {
    const payload = buildSeatPayload(selectedData, { [field]: value });

    // Update UI immediately
    dispatch({ type: actions.UPDATE_SEAT, payload: { ...selectedData, [field]: value } });
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    // API call will be made when user clicks Save button
  };

  // Update UI immediately, wait for Save button to persist
  const handleFactoryChange = (field, value) => {
    // Update UI immediately
    const updated = { ...selectedData, [field]: value };
    dispatch({ type: actions.UPDATE_FACTORY, payload: updated });
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    // API call will be made when user clicks Save button
  };

  /* ================= SAVE FUNCTIONS ================= */

  const saveAreaName = async () => {
    if (localAreaName !== selectedData.areaName) {
      await handleAreaChange("areaName", localAreaName);
    }
  };

  const saveZoneName = async () => {
    if (localZoneName !== selectedData.zoneName) {
      await handleZoneChange("zoneName", localZoneName);
    }
  };

  const saveZoneDes = async () => {
    if (localZoneDes !== selectedData.zoneDes) {
      await handleZoneChange("zoneDes", localZoneDes);
    }
  };

  const saveSeatCode = async () => {
    if (localSeatCode !== selectedData.seatCode) {
      await handleSeatChange("seatCode", localSeatCode);
    }
  };

  const saveFactoryName = async () => {
    if (localFactoryName !== selectedData.factoryName) {
      await handleFactoryChange("factoryName", localFactoryName);
    }
  };

  const handleAddAmenity = async (amenityName) => {
    const name = amenityName || addAmenityName.trim();
    if (!name) {
      toast.warning("Vui lòng nhập tên tiện ích");
      return;
    }

    try {
      const res = await createAmenity({
        amenityName: name,
        zoneId: selectedData.zoneId
      });

      const normalizedAmenity = {
        amenityId: res.data.amenity_id ?? res.data.amenityId,
        amenityName: res.data.amenity_name ?? res.data.amenityName ?? name,
        zoneId: res.data.zone_id ?? res.data.zoneId ?? selectedData.zoneId,
      };

      setAmenities((prev) => [...prev, normalizedAmenity]);
      setShowAddModal(false);
      setAddAmenityName("");
    } catch (e) {
      console.error("Failed to add amenity:", e);
      toast.error("Thêm tiện ích thất bại");
    }
  };

  const handleDeleteAmenity = async (amenityId) => {
    if (!amenityId) return;
    setDeleteAmenityId(amenityId);
  };

  const confirmDeleteAmenity = async () => {
    if (!deleteAmenityId) return;
    try {
      await deleteAmenity(deleteAmenityId);
      setAmenities((prev) => prev.filter((a) => a.amenityId !== deleteAmenityId));
      setDeleteAmenityId(null);
    } catch (e) {
      console.error("Failed to delete amenity:", e);
      toast.error("Xóa tiện ích thất bại");
      setDeleteAmenityId(null);
    }
  };

  const handleDelete = async () => {
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    setShowDeleteConfirm(false);

    try {
      switch (selectedItem.type) {
        case "area":
          // Get all zones and factories in this area
          const areaZones = zones.filter(z => z.areaId === selectedItem.id);
          const areaFactories = factories.filter(f => f.areaId === selectedItem.id);

          // Delete all seats and zones from UI and track for batch delete
          for (const zone of areaZones) {
            const zoneSeats = seats.filter(s => s.zoneId === zone.zoneId);
            for (const seat of zoneSeats) {
              dispatch({ type: actions.DELETE_SEAT, payload: seat.seatId });
              if (seat.seatId > 0) {
                dispatch({ type: actions.ADD_PENDING_SEAT_DELETE, payload: seat.seatId });
              }
            }
            dispatch({ type: actions.DELETE_ZONE, payload: zone.zoneId });
            if (zone.zoneId > 0) {
              dispatch({ type: actions.ADD_PENDING_ZONE_DELETE, payload: zone.zoneId });
            }
          }

          // Delete all factories from UI and track for batch delete
          for (const factory of areaFactories) {
            dispatch({ type: actions.DELETE_FACTORY, payload: factory.factoryId });
            if (factory.factoryId > 0) {
              dispatch({ type: actions.ADD_PENDING_FACTORY_DELETE, payload: factory.factoryId });
            }
          }

          // Delete area from UI - Areas are deleted immediately (they're the container)
          await deleteArea(selectedItem.id);
          dispatch({ type: actions.DELETE_AREA, payload: selectedItem.id });
          break;

        case "zone":
          // Get all seats in this zone
          const zoneSeats = seats.filter(s => s.zoneId === selectedItem.id);

          // Delete seats from UI and track for batch delete
          for (const seat of zoneSeats) {
            dispatch({ type: actions.DELETE_SEAT, payload: seat.seatId });
            // Only track real seats (positive IDs) for batch delete
            if (seat.seatId > 0) {
              dispatch({ type: actions.ADD_PENDING_SEAT_DELETE, payload: seat.seatId });
            }
          }

          // Delete zone from UI
          dispatch({ type: actions.DELETE_ZONE, payload: selectedItem.id });

          // Only track real zones (positive IDs) for batch delete
          if (selectedItem.id > 0) {
            dispatch({ type: actions.ADD_PENDING_ZONE_DELETE, payload: selectedItem.id });
          }

          // NO API calls here - will delete when user clicks Save button
          break;

        case "seat":
          const seatToDelete = seats.find(s => s.seatId === selectedItem.id);
          const zoneIdOfDeletedSeat = seatToDelete?.zoneId;
          const deletedSeatRowNumber = seatToDelete?.rowNumber;

          let remainingSeatsInSameRow = [];
          if (zoneIdOfDeletedSeat && deletedSeatRowNumber) {
            remainingSeatsInSameRow = seats
              .filter(s =>
                s.zoneId === zoneIdOfDeletedSeat &&
                s.seatId !== selectedItem.id &&
                s.rowNumber === deletedSeatRowNumber
              )
              .sort((a, b) => (a.columnNumber || 0) - (b.columnNumber || 0));
          }

          // 1. Delete seat from UI immediately
          dispatch({ type: actions.DELETE_SEAT, payload: selectedItem.id });

          // 2. Track deleted seat for batch save (only if has real ID)
          if (selectedItem.id > 0) {
            dispatch({ type: actions.ADD_PENDING_SEAT_DELETE, payload: selectedItem.id });
          }

          // 3. Renumber and update remaining seats in UI immediately
          remainingSeatsInSameRow.forEach((seat, i) => {
            const newColumnNumber = i + 1;
            const rowLetter = String.fromCharCode(64 + (seat.rowNumber || 1));
            const newSeatCode = `${rowLetter}${newColumnNumber}`;
            const newPositionX = (newColumnNumber - 1) * 48;

            const updatedSeat = {
              ...seat,
              seatCode: newSeatCode,
              columnNumber: newColumnNumber,
              positionX: newPositionX
            };

            // Update UI
            dispatch({ type: actions.UPDATE_SEAT, payload: updatedSeat });

            // Track for batch save
            dispatch({ type: actions.ADD_PENDING_SEAT_UPDATE, payload: updatedSeat });
          });

          // NO API calls here - will save when user clicks Save button
          break;

        case "factory":
          // Delete factory from UI
          dispatch({ type: actions.DELETE_FACTORY, payload: selectedItem.id });

          // Only track real factories (positive IDs) for batch delete
          if (selectedItem.id > 0) {
            dispatch({ type: actions.ADD_PENDING_FACTORY_DELETE, payload: selectedItem.id });
          }

          // NO API calls here - will delete when user clicks Save button
          break;
      }

      dispatch({ type: actions.SELECT_ITEM, payload: null });
    } catch (e) {
      console.error("Delete error:", e);
      toast.error(`Lỗi xóa: ${e.response?.data?.message || e.message}`);
    }
  };

  // Get type label and icon
  const getTypeInfo = () => {
    switch (selectedItem.type) {
      case 'area':
        return { label: 'PHÒNG THƯ VIỆN', icon: '', color: '#C2410C', bg: '#FFF7F2' };
      case 'zone':
        return { label: 'KHU VỰC GHẾ', icon: '', color: '#C2410C', bg: '#FFF7F2' };
      case 'seat':
        return { label: 'GHẾ NGỒI', icon: '', color: '#C2410C', bg: '#FFF7F2' };
      case 'factory':
        return { label: 'VẬT CẢN', icon: '', color: '#C2410C', bg: '#FFF7F2' };
      default:
        return { label: 'THUỘC TÍNH', icon: '', color: '#C2410C', bg: '#FFF7F2' };
    }
  };

  const typeInfo = getTypeInfo();

  /* ================= RENDER ================= */

  return (
    <aside className="properties-panel" style={{
      background: 'linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%)',
      borderLeft: '1px solid #E2E8F0',
      display: 'flex',
      flexDirection: 'column',
      height: '100%'
    }}>
      {/* Header */}
      <div style={{
        padding: '9px 16px',
        background: `linear-gradient(135deg, ${typeInfo.bg} 0%, ${typeInfo.bg}CC 100%)`,
        borderBottom: `2px solid ${typeInfo.color}33`
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          marginBottom: '8px'
        }}>
          <span style={{ fontSize: '24px' }}>{typeInfo.icon}</span>
          <div>
            <span style={{
              fontSize: '10px',
              fontWeight: '700',
              color: typeInfo.color,
              letterSpacing: '0.5px'
            }}>
              {typeInfo.label}
            </span>
            <h3 style={{
              margin: '4px 0 0 0',
              fontSize: '16px',
              fontWeight: '700',
              color: '#1F2937'
            }}>
              {selectedItem.type === 'area' && selectedData.areaName}
              {selectedItem.type === 'zone' && selectedData.zoneName}
              {selectedItem.type === 'seat' && `Ghế ${selectedData.seatCode}`}
              {selectedItem.type === 'factory' && selectedData.factoryName}
            </h3>
          </div>
        </div>
      </div>

      {/* Content */}
      <div style={{
        flex: 1,
        overflow: 'auto',
        padding: '16px',
        display: 'flex',
        flexDirection: 'column',
        gap: '16px'
      }}>
        {/* ============ AREA ============ */}
        {selectedItem.type === "area" && (
          <>
            {/* Area Name */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '8px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Tên phòng thư viện
              </label>
              <input
                value={localAreaName}
                onChange={(e) => setLocalAreaName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !isComposing) {
                    e.preventDefault();
                    saveAreaName();
                  }
                }}
                onCompositionStart={() => setIsComposing(true)}
                onCompositionEnd={() => setIsComposing(false)}
                onBlur={() => saveAreaName()}
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  borderRadius: '10px',
                  border: '2px solid #E2E8F0',
                  fontSize: '14px',
                  fontWeight: '600',
                  transition: 'all 0.2s',
                  outline: 'none'
                }}
              />
            </div>

            {/* Status Controls */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '12px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Trạng thái
              </label>
              <div style={{ display: 'flex', gap: '10px' }}>
                <button
                  onClick={() => {
                    // Optimistic update - UI first
                    const newLocked = !selectedData.locked;
                    dispatch({
                      type: actions.UPDATE_AREA,
                      payload: { ...selectedData, locked: newLocked }
                    });
                    // API in background
                    updateAreaLocked(selectedData.areaId, {
                      areaId: selectedData.areaId,
                      locked: newLocked
                    }).catch(e => {
                      console.error("Failed to update locked:", e);
                      // Revert on failure
                      dispatch({
                        type: actions.UPDATE_AREA,
                        payload: { ...selectedData, locked: !newLocked }
                      });
                    });
                  }}
                  style={{
                    flex: 1,
                    padding: '12px',
                    borderRadius: '10px',
                    border: selectedData.locked ? '2px solid #EF4444' : '2px solid #E2E8F0',
                    backgroundColor: selectedData.locked ? '#FEE2E2' : 'white',
                    cursor: 'pointer',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: selectedData.locked ? '#DC2626' : '#64748B',
                    transition: 'all 0.2s'
                  }}
                >
                  {selectedData.locked ? 'Đã khóa' : 'Mở khóa'}
                </button>

                <button
                  onClick={() => {
                    // Optimistic update - UI first
                    const newIsActive = !selectedData.isActive;
                    dispatch({
                      type: actions.UPDATE_AREA,
                      payload: { ...selectedData, isActive: newIsActive }
                    });
                    // API in background
                    updateAreaIsActive(selectedData.areaId, {
                      areaId: selectedData.areaId,
                      isActive: newIsActive
                    }).catch(e => {
                      console.error("Failed to update isActive:", e);
                      // Revert on failure
                      dispatch({
                        type: actions.UPDATE_AREA,
                        payload: { ...selectedData, isActive: !newIsActive }
                      });
                    });
                  }}
                  style={{
                    flex: 1,
                    padding: '12px',
                    borderRadius: '10px',
                    border: selectedData.isActive ? '2px solid #22C55E' : '2px solid #EF4444',
                    backgroundColor: selectedData.isActive ? '#ECFDF5' : '#FEE2E2',
                    cursor: 'pointer',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: selectedData.isActive ? '#166534' : '#DC2626',
                    transition: 'all 0.2s'
                  }}
                >
                  {selectedData.isActive ? 'Hoạt động' : 'Đóng cửa'}
                </button>
              </div>
            </div>

            {/* Statistics */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '12px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Thống kê
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div style={{
                  padding: '14px',
                  background: '#ECFDF5',
                  borderRadius: '10px',
                  textAlign: 'center'
                }}>
                  <div style={{ fontSize: '24px', fontWeight: '700', color: '#166534' }}>
                    {zones.filter(z => z.areaId === selectedItem.id).length}
                  </div>
                  <div style={{ fontSize: '11px', fontWeight: '600', color: '#22C55E' }}>Khu vực</div>
                </div>
                <div style={{
                  padding: '14px',
                  background: '#EFF6FF',
                  borderRadius: '10px',
                  textAlign: 'center'
                }}>
                  <div style={{ fontSize: '24px', fontWeight: '700', color: '#1E40AF' }}>
                    {seats.filter(s => zones.find(z => z.areaId === selectedItem.id && z.zoneId === s.zoneId)).length}
                  </div>
                  <div style={{ fontSize: '11px', fontWeight: '600', color: '#3B82F6' }}>Tổng ghế</div>
                </div>
              </div>
            </div>
          </>
        )}

        {/* ============ ZONE ============ */}
        {selectedItem.type === "zone" && (
          <>
            {/* Zone Name */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '8px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Tên khu vực
              </label>
              <input
                value={localZoneName}
                onChange={(e) => setLocalZoneName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !isComposing) {
                    e.preventDefault();
                    saveZoneName();
                  }
                }}
                onCompositionStart={() => setIsComposing(true)}
                onCompositionEnd={() => setIsComposing(false)}
                onBlur={() => saveZoneName()}
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  borderRadius: '10px',
                  border: '2px solid #E2E8F0',
                  fontSize: '14px',
                  fontWeight: '600'
                }}
              />
            </div>

            {/* Zone Description */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '8px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Mô tả / Quy định
              </label>
              <textarea
                value={localZoneDes}
                onChange={(e) => setLocalZoneDes(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey && !isComposing) {
                    e.preventDefault();
                    saveZoneDes();
                  }
                }}
                onCompositionStart={() => setIsComposing(true)}
                onCompositionEnd={() => setIsComposing(false)}
                onBlur={() => saveZoneDes()}
                placeholder="VD: Khu vực yên tĩnh, không nói chuyện..."
                rows="3"
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  borderRadius: '10px',
                  border: '2px solid #E2E8F0',
                  fontSize: '13px',
                  resize: 'vertical'
                }}
              />
            </div>

            {/* Lock & Color */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '12px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                CÀI ĐẶT
              </label>

              <button
                onClick={() => handleZoneChange("isLocked", !selectedData.isLocked)}
                style={{
                  width: '100%',
                  padding: '12px',
                  borderRadius: '10px',
                  border: selectedData.isLocked ? '2px solid #EF4444' : '2px solid #E2E8F0',
                  backgroundColor: selectedData.isLocked ? '#FEE2E2' : 'white',
                  cursor: 'pointer',
                  fontSize: '13px',
                  fontWeight: '600',
                  color: selectedData.isLocked ? '#DC2626' : '#64748B',
                  marginBottom: '12px'
                }}
              >
                {selectedData.isLocked ? 'Đã khóa vị trí' : 'Vị trí có thể di chuyển'}
              </button>

              {/* Color picker removed - zones now use light gray only */}
            </div>

            {/* Amenities */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '12px'
              }}>
                <label style={{
                  fontSize: '11px',
                  fontWeight: '700',
                  color: '#64748B',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px'
                }}>
                  TIỆN ÍCH KHU VỰC
                </label>
                <button
                  onClick={() => setShowAddModal(true)}
                  style={{
                    padding: '6px 12px',
                    borderRadius: '8px',
                    border: 'none',
                    background: 'linear-gradient(135deg, #22C55E 0%, #16A34A 100%)',
                    color: 'white',
                    fontSize: '11px',
                    fontWeight: '600',
                    cursor: 'pointer'
                  }}
                >
                  + Thêm
                </button>
              </div>

              {/* Quick Add Buttons */}
              <div style={{
                display: 'flex',
                gap: '6px',
                flexWrap: 'wrap',
                marginBottom: '12px'
              }}>
                {QUICK_AMENITIES.filter(qa => !amenities.some(a => a.amenityName === qa.name)).map(qa => (
                  <button
                    key={qa.name}
                    onClick={() => handleAddAmenity(qa.name)}
                    style={{
                      padding: '6px 10px',
                      borderRadius: '20px',
                      border: '1px solid #E2E8F0',
                      background: '#F8FAFC',
                      fontSize: '11px',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '4px'
                    }}
                  >
                    <span>{qa.icon}</span>
                    <span>{qa.name}</span>
                  </button>
                ))}
              </div>

              {/* Amenities List */}
              <div style={{
                maxHeight: '120px',
                overflowY: 'auto',
                border: '1px solid #E2E8F0',
                borderRadius: '10px'
              }}>
                {amenities.length > 0 ? (
                  amenities.map((amenity) => (
                    <div key={amenity.amenityId} style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '10px 12px',
                      borderBottom: '1px solid #E2E8F0'
                    }}>
                      <span style={{ fontSize: '13px', fontWeight: '500' }}>
                        {QUICK_AMENITIES.find(q => q.name === amenity.amenityName)?.icon || '✓'} {amenity.amenityName}
                      </span>
                      <button
                        onClick={() => handleDeleteAmenity(amenity.amenityId)}
                        style={{
                          background: 'none',
                          border: 'none',
                          cursor: 'pointer',
                          color: '#EF4444',
                          fontSize: '16px'
                        }}
                      >
                        ×
                      </button>
                    </div>
                  ))
                ) : (
                  <div style={{
                    padding: '20px',
                    textAlign: 'center',
                    color: '#94A3B8',
                    fontSize: '13px'
                  }}>
                    Chưa có tiện ích nào
                  </div>
                )}
              </div>
            </div>
          </>
        )}

        {/* ============ SEAT ============ */}
        {selectedItem.type === "seat" && (
          <>
            {/* Seat Code */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '8px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Mã ghế
              </label>
              <input
                value={localSeatCode}
                onChange={(e) => setLocalSeatCode(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !isComposing) {
                    e.preventDefault();
                    saveSeatCode();
                  }
                }}
                onCompositionStart={() => setIsComposing(true)}
                onCompositionEnd={() => setIsComposing(false)}
                onBlur={() => saveSeatCode()}
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  borderRadius: '10px',
                  border: '2px solid #E2E8F0',
                  fontSize: '18px',
                  fontWeight: '700',
                  textAlign: 'center'
                }}
              />
            </div>

            {/* Seat Status */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '12px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Trạng thái ghế
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <button
                  onClick={() => {
                    setLocalSeatIsActive(true);
                    handleSeatChange("isActive", true);
                  }}
                  style={{
                    padding: '14px',
                    borderRadius: '10px',
                    border: localSeatIsActive ? '2px solid #22C55E' : '2px solid #E2E8F0',
                    background: localSeatIsActive ? '#ECFDF5' : 'white',
                    cursor: 'pointer',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: localSeatIsActive ? '#166534' : '#64748B'
                  }}
                >
                  Hoạt động
                </button>
                <button
                  onClick={() => {
                    setLocalSeatIsActive(false);
                    handleSeatChange("isActive", false);
                  }}
                  style={{
                    padding: '14px',
                    borderRadius: '10px',
                    border: !localSeatIsActive ? '2px solid #EF4444' : '2px solid #E2E8F0',
                    background: !localSeatIsActive ? '#FEE2E2' : 'white',
                    cursor: 'pointer',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: !localSeatIsActive ? '#DC2626' : '#64748B'
                  }}
                >
                  Bảo trì
                </button>
              </div>
            </div>

            {/* Seat Info */}
            <div style={{
              backgroundColor: '#F8FAFC',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '12px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Thông tin
              </label>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#64748B', fontSize: '13px' }}>Hàng:</span>
                  <span style={{ fontWeight: '600', fontSize: '13px' }}>
                    {String.fromCharCode(64 + (selectedData.rowNumber || 1))}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#64748B', fontSize: '13px' }}>Cột:</span>
                  <span style={{ fontWeight: '600', fontSize: '13px' }}>{selectedData.columnNumber || 1}</span>
                </div>
              </div>
            </div>

            {/* NFC Tag UID Mapping */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '12px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Thẻ NFC
              </label>

              {/* NFC Status Display */}
              <div style={{
                backgroundColor: localNfcTagUid ? '#ECFDF5' : '#FEF3C7',
                borderRadius: '8px',
                padding: '12px',
                marginBottom: '12px',
                border: localNfcTagUid ? '1px solid #22C55E' : '1px solid #FCD34D',
                display: 'flex',
                alignItems: 'center',
                gap: '10px'
              }}>
                <div style={{
                  width: '10px',
                  height: '10px',
                  borderRadius: '50%',
                  backgroundColor: localNfcTagUid ? '#22C55E' : '#F59E0B'
                }} />
                <span style={{
                  fontWeight: '600',
                  fontSize: '13px',
                  color: localNfcTagUid ? '#166534' : '#92400E'
                }}>
                  Trạng thái: {localNfcTagUid ? 'Đã gán NFC' : 'Chưa gán NFC'}
                </span>
              </div>

              {/* Buttons based on NFC status */}
              {localNfcTagUid ? (
                // Already assigned - Show "Gán thẻ mới" and "Xóa"
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    onClick={async () => {
                      setNfcScanning(true);
                      setNfcError(null);
                      try {
                        const data = await nfcManagementService.scanNfcFromBridge();

                        if (data.success && data.uid) {
                          setNfcSaving(true);
                          try {
                            await updateSeatNfcUid(selectedData.seatId, data.uid);
                            setLocalNfcTagUid(data.uid);
                            dispatch({
                              type: actions.UPDATE_SEAT,
                              payload: { ...selectedData, nfcTagUid: data.uid }
                            });
                            dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
                          } catch (saveErr) {
                            setNfcError(saveErr.response?.data?.error || 'Lỗi khi lưu NFC UID');
                          } finally {
                            setNfcSaving(false);
                          }
                        } else {
                          setNfcError(data.error || 'Không đọc được thẻ NFC');
                        }
                      } catch (err) {
                        setNfcError(err.message || 'Lỗi quét NFC');
                      } finally {
                        setNfcScanning(false);
                      }
                    }}
                    disabled={nfcScanning || nfcSaving}
                    style={{
                      flex: 1,
                      padding: '12px',
                      borderRadius: '10px',
                      border: 'none',
                      background: nfcScanning
                        ? '#FFF7ED'
                        : 'linear-gradient(135deg, #FF751F 0%, #E85A00 100%)',
                      cursor: nfcScanning || nfcSaving ? 'wait' : 'pointer',
                      fontSize: '13px',
                      fontWeight: '600',
                      color: nfcScanning ? '#EA580C' : 'white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '6px'
                    }}
                  >
                    {nfcScanning ? 'Đang chờ thẻ...' : nfcSaving ? 'Đang lưu...' : 'Gán thẻ mới'}
                  </button>

                  <button
                    onClick={async () => {
                      if (!confirm('Bạn có chắc muốn xóa NFC khỏi ghế này?')) return;
                      setNfcSaving(true);
                      try {
                        await clearSeatNfcUid(selectedData.seatId);
                        setLocalNfcTagUid('');
                        dispatch({
                          type: actions.UPDATE_SEAT,
                          payload: { ...selectedData, nfcTagUid: null }
                        });
                        dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
                      } catch (err) {
                        setNfcError(err.response?.data?.error || 'Lỗi khi xóa NFC');
                      } finally {
                        setNfcSaving(false);
                      }
                    }}
                    disabled={nfcSaving}
                    style={{
                      padding: '12px 16px',
                      borderRadius: '10px',
                      border: '2px solid #EF4444',
                      background: 'white',
                      cursor: nfcSaving ? 'wait' : 'pointer',
                      fontSize: '13px',
                      fontWeight: '600',
                      color: '#EF4444'
                    }}
                  >
                    Xóa
                  </button>
                </div>
              ) : (
                // Not assigned - Show "Thêm thẻ NFC"
                <button
                  onClick={async () => {
                    setNfcScanning(true);
                    setNfcError(null);
                    try {
                      const data = await nfcManagementService.scanNfcFromBridge();

                      if (data.success && data.uid) {
                        setNfcSaving(true);
                        try {
                          await updateSeatNfcUid(selectedData.seatId, data.uid);
                          setLocalNfcTagUid(data.uid);
                          dispatch({
                            type: actions.UPDATE_SEAT,
                            payload: { ...selectedData, nfcTagUid: data.uid }
                          });
                          dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
                        } catch (saveErr) {
                          setNfcError(saveErr.response?.data?.error || 'Lỗi khi lưu NFC UID');
                        } finally {
                          setNfcSaving(false);
                        }
                      } else {
                        setNfcError(data.error || 'Không đọc được thẻ NFC');
                      }
                    } catch (err) {
                      setNfcError(err.message || 'Lỗi quét NFC');
                    } finally {
                      setNfcScanning(false);
                    }
                  }}
                  disabled={nfcScanning || nfcSaving}
                  style={{
                    width: '100%',
                    padding: '14px',
                    borderRadius: '10px',
                    border: 'none',
                    background: nfcScanning
                      ? '#FFF7ED'
                      : 'linear-gradient(135deg, #FF751F 0%, #E85A00 100%)',
                    cursor: nfcScanning || nfcSaving ? 'wait' : 'pointer',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: nfcScanning ? '#EA580C' : 'white',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px'
                  }}
                >
                  {nfcScanning ? 'Đang chờ thẻ...' : nfcSaving ? 'Đang lưu...' : 'Thêm thẻ NFC'}
                </button>
              )}

              {/* Error Display */}
              {nfcError && (
                <div style={{
                  marginTop: '12px',
                  padding: '10px 12px',
                  backgroundColor: '#FEE2E2',
                  borderRadius: '8px',
                  color: '#DC2626',
                  fontSize: '12px'
                }}>
                  {nfcError}
                </div>
              )}
            </div>
          </>
        )}

        {/* ============ FACTORY ============ */}
        {selectedItem.type === "factory" && (
          <>
            {/* Factory Name */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '8px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Tên vật cản
              </label>
              <input
                value={localFactoryName}
                onChange={(e) => setLocalFactoryName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !isComposing) {
                    e.preventDefault();
                    saveFactoryName();
                  }
                }}
                onCompositionStart={() => setIsComposing(true)}
                onCompositionEnd={() => setIsComposing(false)}
                onBlur={() => saveFactoryName()}
                placeholder="VD: Kệ sách, Bàn lớn..."
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  borderRadius: '10px',
                  border: '2px solid #E2E8F0',
                  fontSize: '14px',
                  fontWeight: '600'
                }}
              />
            </div>

            {/* Color picker removed - obstacles now use fixed gray (#9CA3AF) */}

            {/* Lock Movement Toggle */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '12px',
              padding: '16px',
              border: '1px solid #E2E8F0'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '700',
                color: '#64748B',
                marginBottom: '12px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                TRẠNG THÁI
              </label>
              <button
                onClick={() => {
                  const newLocked = !selectedData.isLocked;
                  // Optimistic update
                  dispatch({
                    type: actions.UPDATE_FACTORY,
                    payload: { ...selectedData, isLocked: newLocked }
                  });
                  // API in background
                  handleFactoryChange("isLocked", newLocked);
                }}
                style={{
                  width: '100%',
                  padding: '12px',
                  borderRadius: '10px',
                  border: selectedData.isLocked ? '2px solid #EF4444' : '2px solid #22C55E',
                  backgroundColor: selectedData.isLocked ? '#FEE2E2' : '#ECFDF5',
                  cursor: 'pointer',
                  fontSize: '13px',
                  fontWeight: '600',
                  color: selectedData.isLocked ? '#DC2626' : '#166534',
                  transition: 'all 0.2s'
                }}
              >
                {selectedData.isLocked ? 'Đã khóa di chuyển' : 'Cho phép di chuyển'}
              </button>
            </div>
          </>
        )}
      </div>

      {/* Footer - Delete Button */}
      <div style={{
        padding: '16px',
        borderTop: '1px solid #E2E8F0',
        background: '#F8FAFC'
      }}>
        <button
          onClick={handleDelete}
          style={{
            width: '100%',
            padding: '14px',
            borderRadius: '12px',
            border: '2px solid #FECACA',
            background: 'linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%)',
            color: '#DC2626',
            fontSize: '14px',
            fontWeight: '600',
            cursor: 'pointer',
            transition: 'all 0.2s',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px'
          }}
        >
          Xóa {selectedItem.type === 'area' ? 'phòng' : selectedItem.type === 'zone' ? 'khu vực' : selectedItem.type === 'seat' ? 'ghế' : 'vật cản'}
        </button>
      </div>

      {/* Add Amenity Modal */}
      {showAddModal && (
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
            <h3 style={{ marginTop: 0, marginBottom: '20px', color: '#1F2937' }}>
              ➕ Thêm tiện ích mới
            </h3>

            <input
              type="text"
              placeholder="Tên tiện ích..."
              value={addAmenityName}
              onChange={(e) => setAddAmenityName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleAddAmenity();
              }}
              style={{
                width: '100%',
                padding: '14px 16px',
                border: '2px solid #E2E8F0',
                borderRadius: '12px',
                fontSize: '14px',
                marginBottom: '20px'
              }}
              autoFocus
            />

            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                onClick={() => {
                  setShowAddModal(false);
                  setAddAmenityName("");
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
                onClick={() => handleAddAmenity()}
                style={{
                  flex: 1,
                  padding: '14px',
                  border: 'none',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  background: 'linear-gradient(135deg, #22C55E 0%, #16A34A 100%)',
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '600'
                }}
              >
                Thêm
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.6)',
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
            maxWidth: '420px',
            boxShadow: '0 20px 60px rgba(0, 0, 0, 0.2)',
            border: '2px solid #FECACA'
          }}>
            <div style={{ textAlign: 'center', marginBottom: '24px' }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
              <h3 style={{ margin: '0 0 8px 0', color: '#DC2626', fontSize: '18px', fontWeight: '700' }}>
                Xác nhận xóa?
              </h3>
              <p style={{ margin: '0', color: '#64748B', fontSize: '14px' }}>
                {selectedItem.type === 'area' && (() => {
                  const areaZones = zones.filter(z => z.areaId === selectedItem.id);
                  const areaSeats = seats.filter(s => areaZones.some(z => z.zoneId === s.zoneId));
                  return `Xóa phòng sẽ xóa ${areaZones.length} khu vực và ${areaSeats.length} ghế bên trong! Thay đổi chỉ được lưu khi bấm nút Lưu.`;
                })()}
                {selectedItem.type === 'zone' && (() => {
                  const zoneSeats = seats.filter(s => s.zoneId === selectedItem.id);
                  return `Xóa khu vực sẽ xóa ${zoneSeats.length} ghế bên trong! Thay đổi chỉ được lưu khi bấm nút Lưu.`;
                })()}
                {selectedItem.type === 'seat' && 'Bạn có chắc muốn xóa ghế này? Thay đổi chỉ được lưu khi bấm nút Lưu.'}
                {selectedItem.type === 'factory' && 'Bạn có chắc muốn xóa vật cản này? Thay đổi chỉ được lưu khi bấm nút Lưu.'}
              </p>
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                onClick={() => setShowDeleteConfirm(false)}
                style={{
                  flex: 1,
                  padding: '14px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  backgroundColor: 'white',
                  color: '#374151',
                  fontSize: '14px',
                  fontWeight: '600'
                }}
              >
                Hủy
              </button>
              <button
                onClick={confirmDelete}
                style={{
                  flex: 1,
                  padding: '14px',
                  border: 'none',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  background: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)',
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '600',
                  boxShadow: '0 4px 14px rgba(239, 68, 68, 0.3)'
                }}
              >
                Xóa
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Amenity Confirmation Modal */}
      {deleteAmenityId && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.6)',
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
            maxWidth: '380px',
            boxShadow: '0 20px 60px rgba(0, 0, 0, 0.2)'
          }}>
            <div style={{ textAlign: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: '0 0 8px 0', color: '#DC2626', fontSize: '18px', fontWeight: '700' }}>
                Xóa tiện ích?
              </h3>
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                onClick={() => setDeleteAmenityId(null)}
                style={{
                  flex: 1,
                  padding: '14px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  backgroundColor: 'white',
                  color: '#374151',
                  fontSize: '14px',
                  fontWeight: '600'
                }}
              >
                Hủy
              </button>
              <button
                onClick={confirmDeleteAmenity}
                style={{
                  flex: 1,
                  padding: '14px',
                  border: 'none',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  background: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)',
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '600'
                }}
              >
                Xóa
              </button>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}

export default PropertiesPanel;

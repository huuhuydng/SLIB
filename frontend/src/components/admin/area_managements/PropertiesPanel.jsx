import { useState, useEffect } from "react";
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
} from "../../../services/admin/area_management/api";
import "../../../styles/admin/properties.css";

function PropertiesPanel() {
  const { state, dispatch, actions } = useLayout();
  const { selectedItem, areas, zones, seats, factories } = state;
  
  console.log("🎨 PropertiesPanel - selectedItem:", selectedItem);
  console.log("🏭 PropertiesPanel - factories:", factories);
  
  // IME composition handling
  const [isComposing, setIsComposing] = useState(false);
  
  // Local state cho Area
  const [localAreaName, setLocalAreaName] = useState("");
  
  // Local state cho Zone
  const [localZoneDes, setLocalZoneDes] = useState("");
  const [localZoneName, setLocalZoneName] = useState("");

  // Local state cho Seat
  const [localSeatCode, setLocalSeatCode] = useState("");
  const [localSeatIsActive, setLocalSeatIsActive] = useState(true);

  // Local state cho Factory
  const [localFactoryName, setLocalFactoryName] = useState("");
  const [localFactoryColor, setLocalFactoryColor] = useState("#90EE90");

  // Amenities state
  const [amenities, setAmenities] = useState([]);

  // Add Amenity Modal state
  const [showAddModal, setShowAddModal] = useState(false);
  const [addAmenityName, setAddAmenityName] = useState("");

  // Delete Confirmation Modal state
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteItemType, setDeleteItemType] = useState(null);
  
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
        const seatData = seats.find((s) => s.seatId === selectedItem.id);
        return seatData;
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
          console.log("📋 Amenities loaded for zone", selectedData.zoneId, ":", res.data);
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
    } else if (selectedItem?.type === "factory" && selectedData) {
      setLocalFactoryName(selectedData.factoryName || "");
      setLocalFactoryColor(selectedData.color || "#90EE90");
    }
  }, [selectedItem?.id, selectedItem?.type, selectedData?.areaName, selectedData?.zoneDes, selectedData?.zoneName, selectedData?.seatCode, selectedData?.status, selectedData?.factoryName, selectedData?.color]);

  if (!selectedItem || !selectedData) {
    return (
      <aside className="properties-panel">
        <div className="panel-header">
          <h3>Thuộc tính</h3>
        </div>
        <div className="panel-content empty">
          <p>Chọn mục để chỉnh sửa</p>
        </div>
      </aside>
    );
  }

  /* ================= HANDLERS ================= */

  const buildSeatPayload = (seat, override = {}) => {
    const {
      seatId,
      zoneId,
      seatCode,
      width,
      height,
      rowNumber,
      columnNumber,
      positionX,
      positionY,
      isActive,
    } = seat || {};

    const payload = {
      seatId,
      zoneId,
      seatCode,
      width,
      height,
      rowNumber,
      columnNumber,
      positionX,
      positionY,
      isActive,
      ...override,
    };

    console.log("[buildSeatPayload] before cleanup:", payload);

    // Strip undefined and null so we don't send invalid fields to backend
    Object.keys(payload).forEach((key) => {
      if (payload[key] === undefined || payload[key] === null) delete payload[key];
    });

    console.log("[buildSeatPayload] after cleanup:", payload);

    return payload;
  };

  const handleAreaChange = async (field, value) => {
    const res = await updateArea(selectedData.areaId, {
      ...selectedData,
      [field]: value,
    });

    dispatch({
      type: actions.UPDATE_AREA,
      payload: res.data,
    });
  };

  const handleZoneChange = async (field, value) => {
    const res = await updateZone(selectedData.zoneId, {
      ...selectedData,
      [field]: value,
    });

    dispatch({
      type: actions.UPDATE_ZONE,
      payload: res.data,
    });
  };

  const handleSeatChange = async (field, value) => {
    const payload = buildSeatPayload(selectedData, { [field]: value });
    
    console.log("[PropertiesPanel] handleSeatChange", {
      seatId: selectedData?.seatId,
      field,
      value,
      selectedData,
      payload
    });

    try {
      const res = await updateSeat(
        selectedData.seatId,
        payload
      );
      console.log("[PropertiesPanel] updateSeat response:", res?.status, res?.data);

      dispatch({
        type: actions.UPDATE_SEAT,
        payload: res.data,
      });
    } catch (e) {
      console.error("[PropertiesPanel] updateSeat error:", e.response?.status, e.response?.data, e.message);
      throw e;
    }
  };

  const handleFactoryChange = async (field, value) => {
    const res = await updateAreaFactory(selectedData.factoryId, {
      ...selectedData,
      [field]: value,
    });

    // Convert response to camelCase if needed
    const factoryData = {
      factoryId: res.data.factory_id || res.data.factoryId,
      factoryName: res.data.factory_name || res.data.factoryName,
      positionX: res.data.position_x || res.data.positionX,
      positionY: res.data.position_y || res.data.positionY,
      width: res.data.width,
      height: res.data.height,
      color: res.data.color,
      areaId: res.data.area_id || res.data.areaId,
    };

    dispatch({
      type: actions.UPDATE_FACTORY,
      payload: factoryData,
    });
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
      alert("Xóa tiện ích thất bại");
      setDeleteAmenityId(null);
    }
  };

  /* ================= SAVE FUNCTIONS (Enter/Blur only) ================= */

  // Area Name
  const saveAreaName = async () => {
    if (localAreaName !== selectedData.areaName) {
      await handleAreaChange("areaName", localAreaName);
    }
  };

  // Zone Name
  const saveZoneName = async () => {
    if (localZoneName !== selectedData.zoneName) {
      await handleZoneChange("zoneName", localZoneName);
    }
  };

  // Zone Description
  const saveZoneDes = async () => {
    if (localZoneDes !== selectedData.zoneDes) {
      await handleZoneChange("zoneDes", localZoneDes);
    }
  };

  // Seat Code
  const saveSeatCode = async () => {
    if (localSeatCode !== selectedData.seatCode) {
      await handleSeatChange("seatCode", localSeatCode);
    }
  };

  // Seat isActive
  const saveSeatIsActive = async () => {
    if (localSeatIsActive !== selectedData.isActive) {
      await handleSeatChange("isActive", localSeatIsActive);
    }
  };

  // Factory Name
  const saveFactoryName = async () => {
    if (localFactoryName !== selectedData.factoryName) {
      await handleFactoryChange("factoryName", localFactoryName);
    }
  };

  const handleAddAmenity = async () => {
    if (!addAmenityName.trim()) {
      alert("Vui lòng nhập tên tiện ích");
      return;
    }
    
    try {
      const res = await createAmenity({
        amenityName: addAmenityName.trim(),
        zoneId: selectedData.zoneId
      });
      setAmenities((prev) => [...prev, res.data]);
      setShowAddModal(false);
      setAddAmenityName("");
    } catch (e) {
      console.error("Failed to add amenity:", e);
      alert("Thêm tiện ích thất bại");
    }
  };

  const handleDelete = async () => {
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    console.log("🗑️ [confirmDelete] START - selectedItem:", selectedItem);
    console.log("🗑️ [confirmDelete] Current seats state:", seats);
    setShowDeleteConfirm(false);

    try {
      switch (selectedItem.type) {
        case "area":
          console.log("🗑️ [Delete Area] START - areaId:", selectedItem.id);
          
          // Delete all zones and factories in this area first
          const areaZones = zones.filter(z => z.areaId === selectedItem.id);
          const areaFactories = factories.filter(f => f.areaId === selectedItem.id);
          
          console.log(`🗑️ [Delete Area] Found ${areaZones.length} zones and ${areaFactories.length} factories to delete first`);
          
          // Delete all zones (and their seats)
          for (const zone of areaZones) {
            const zoneSeats = seats.filter(s => s.zoneId === zone.zoneId);
            console.log(`🗑️ [Delete Area] Deleting zone ${zone.zoneId} with ${zoneSeats.length} seats`);
            
            // Delete seats first
            for (const seat of zoneSeats) {
              try {
                await deleteSeat(seat.seatId);
                dispatch({ type: actions.DELETE_SEAT, payload: seat.seatId });
              } catch (e) {
                console.error(`Failed to delete seat ${seat.seatId}:`, e);
              }
            }
            
            // Then delete zone
            try {
              await deleteZone(zone.zoneId);
              dispatch({ type: actions.DELETE_ZONE, payload: zone.zoneId });
            } catch (e) {
              console.error(`Failed to delete zone ${zone.zoneId}:`, e);
            }
          }
          
          // Delete all factories
          for (const factory of areaFactories) {
            try {
              await deleteAreaFactory(factory.factoryId);
              dispatch({ type: actions.DELETE_FACTORY, payload: factory.factoryId });
            } catch (e) {
              console.error(`Failed to delete factory ${factory.factoryId}:`, e);
            }
          }
          
          // Now delete the area
          await deleteArea(selectedItem.id);
          console.log("🗑️ [Delete Area] SUCCESS - dispatching DELETE_AREA");
          dispatch({ type: actions.DELETE_AREA, payload: selectedItem.id });
          break;
        case "zone":
          // Delete all seats in this zone first
          const zoneSeats = seats.filter(s => s.zoneId === selectedItem.id);
          for (const seat of zoneSeats) {
            try {
              await deleteSeat(seat.seatId);
              dispatch({ type: actions.DELETE_SEAT, payload: seat.seatId });
          } catch (e) {
            console.error(`Failed to delete seat ${seat.seatId}:`, e);
          }
        }
        
        // Then delete the zone
        await deleteZone(selectedItem.id);
        dispatch({ type: actions.DELETE_ZONE, payload: selectedItem.id });
        break;
      case "seat":
        try {
          // Get the zone of the seat being deleted
          const seatToDelete = seats.find(s => s.seatId === selectedItem.id);
          const zoneIdOfDeletedSeat = seatToDelete?.zoneId;

          console.log(`🗑️ [Delete Seat] START - seatId: ${selectedItem.id}`);
          console.log(`🗑️ [Delete Seat] seatToDelete:`, seatToDelete);
          console.log(`🗑️ [Delete Seat] zoneIdOfDeletedSeat: ${zoneIdOfDeletedSeat}`);

          // Get the row number of the deleted seat
          const deletedSeatRowNumber = seatToDelete?.rowNumber;
          
          // Calculate remaining seats in SAME ROW ONLY
          let remainingSeatsInSameRow = [];
          if (zoneIdOfDeletedSeat && deletedSeatRowNumber) {
            remainingSeatsInSameRow = seats
              .filter(s => 
                s.zoneId === zoneIdOfDeletedSeat && 
                s.seatId !== selectedItem.id &&
                s.rowNumber === deletedSeatRowNumber // Only same row
              )
              .sort((a, b) => {
                // Sort by columnNumber to maintain order
                return (a.columnNumber || 0) - (b.columnNumber || 0);
              });
            console.log(`🗑️ [Delete Seat] remainingSeatsInSameRow (row ${deletedSeatRowNumber}):`, remainingSeatsInSameRow.map(s => ({ seatId: s.seatId, seatCode: s.seatCode, col: s.columnNumber })));
          }

          // Delete the seat from backend
          console.log(`🗑️ [Delete Seat] Calling deleteSeat API for seat ${selectedItem.id}...`);
          const deleteRes = await deleteSeat(selectedItem.id);
          console.log(`🗑️ [Delete Seat] deleteSeat API response:`, deleteRes);

          // Delete from state
          console.log(`🗑️ [Delete Seat] Dispatching DELETE_SEAT action...`);
          dispatch({ type: actions.DELETE_SEAT, payload: selectedItem.id });
          console.log(`🗑️ [Delete Seat] DELETE_SEAT dispatched`);

          // Renumber remaining seats in SAME ROW ONLY
          if (zoneIdOfDeletedSeat && remainingSeatsInSameRow.length > 0) {
            console.log(`✏️ [Delete Seat] START RENUMBERING ${remainingSeatsInSameRow.length} seats in ROW ${deletedSeatRowNumber}...`);
            
            for (let i = 0; i < remainingSeatsInSameRow.length; i++) {
              const seat = remainingSeatsInSameRow[i];
              const newColumnNumber = i + 1; // Reset column to 1, 2, 3, ...
              
              // Generate new seatCode based on rowNumber + columnNumber
              // rowNumber 1=A, 2=B, 3=C, etc.
              const rowLetter = String.fromCharCode(64 + (seat.rowNumber || 1)); // A=65, B=66, etc.
              const newSeatCode = `${rowLetter}${newColumnNumber}`;
              
              console.log(`✏️ [Delete Seat] Renumbering seat ${i}/${remainingSeatsInSameRow.length}:`);
              console.log(`   - seatId: ${seat.seatId}`);
              console.log(`   - oldCode: ${seat.seatCode}`);
              console.log(`   - rowNumber: ${seat.rowNumber}`);
              console.log(`   - rowLetter: ${rowLetter}`);
              console.log(`   - newColumnNumber: ${newColumnNumber}`);
              console.log(`   - newSeatCode: ${newSeatCode}`);
              
              try {
                const payload = buildSeatPayload(seat, { seatCode: newSeatCode, columnNumber: newColumnNumber });
                console.log(`✏️ [Delete Seat] Updating seat ${seat.seatId} -> ${newSeatCode}`);
                
                const res = await updateSeat(seat.seatId, payload);
                
                // Force update with new seatCode in state (backend may not update seatCode)
                const updatedSeatData = {
                  ...res.data,
                  rowNumber: seat.rowNumber, // Keep same row
                  seatCode: newSeatCode,
                  columnNumber: newColumnNumber
                };
                
                dispatch({
                  type: actions.UPDATE_SEAT,
                  payload: updatedSeatData,
                });
                console.log(`✏️ [Delete Seat] ✅ Updated seat ${seat.seatId} -> ${newSeatCode}`);
              } catch (e) {
                console.error(`❌ [Delete Seat] Failed to renumber seat ${seat.seatId}:`, e);
              }
            }
            console.log(`✅ [Delete Seat] RENUMBERING COMPLETE for ${remainingSeatsInSameRow.length} seats in ROW ${deletedSeatRowNumber}`);
          } else {
            console.log(`🗑️ [Delete Seat] No renumbering needed - zone: ${zoneIdOfDeletedSeat}, row: ${deletedSeatRowNumber}, remaining: ${remainingSeatsInSameRow.length}`);
          }
          
          console.log(`🗑️ [Delete Seat] COMPLETE`);
        } catch (e) {
          console.error("❌ [Delete Seat] ERROR during seat deletion and renumbering:", e);
        }
        break;
      case "factory":
        console.log("🗑️ [Delete Factory] START - factoryId:", selectedItem.id);
        await deleteAreaFactory(selectedItem.id);
        console.log("🗑️ [Delete Factory] SUCCESS - dispatching DELETE_FACTORY");
        dispatch({ type: actions.DELETE_FACTORY, payload: selectedItem.id });
        break;
    }

    console.log("🗑️ [confirmDelete] COMPLETE - clearing selection");
    dispatch({ type: actions.SELECT_ITEM, payload: null });
    } catch (e) {
      console.error("❌ [confirmDelete] ERROR:", e.response?.data || e.message);
      alert(`Lỗi xóa: ${e.response?.data?.message || e.message}`);
    }
  };

  /* ================= RENDER ================= */

  return (
    <aside className="properties-panel">
      <div className="panel-header">
        <h3>Thuộc tính</h3>
        <span className={`type-badge ${selectedItem.type}`}>
          {selectedItem.type === 'area' ? 'KHU VỰC' : selectedItem.type === 'zone' ? 'PHÒNG' : selectedItem.type === 'seat' ? 'GHẾ' : 'VẬT CẢN'}
        </span>
      </div>

      <div className="panel-content">
        {/* ============ AREA ============ */}
        {selectedItem.type === "area" && (
          <>
            {/* Điều khiển Section */}
            <div style={{
              backgroundColor: '#f9fafb',
              borderRadius: '8px',
              padding: '16px',
              marginBottom: '20px',
              border: '1px solid #e5e7eb'
            }}>
              <h4 style={{
                margin: '0 0 12px 0',
                fontSize: '14px',
                fontWeight: '600',
                color: '#374151',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                 Điều khiển
              </h4>

              {/* Area Name */}
              <div className="form-group" style={{ marginBottom: '12px' }}>
                <label style={{ fontSize: '13px', color: '#6b7280' }}>Tên khu vực</label>
                <input
                  value={localAreaName}
                  onChange={(e) => {
                    setLocalAreaName(e.target.value);
                  }}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !isComposing) {
                      e.preventDefault();
                      saveAreaName();
                    }
                  }}
                  onCompositionStart={() => setIsComposing(true)}
                  onCompositionEnd={() => setIsComposing(false)}
                  onBlur={() => {
                    saveAreaName();
                  }}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '6px',
                    border: '1px solid #d1d5db',
                    fontSize: '14px'
                  }}
                />
              </div>

              {/* Status Toggles */}
              <div style={{ display: 'flex', gap: '8px' }}>
                {/* Locked Toggle */}
                <button
                  onClick={async () => {
                    try {
                      const res = await updateAreaLocked(selectedData.areaId, { 
                        areaId: selectedData.areaId,
                        locked: !selectedData.locked 
                      });
                      dispatch({ type: actions.UPDATE_AREA, payload: res.data });
                    } catch (e) {
                      console.error("Failed to update area locked status:", e);
                      console.error("Error details:", e.response?.data);
                      alert("Cập nhật trạng thái khóa thất bại");
                    }
                  }}
                  style={{
                    flex: 1,
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: selectedData.locked ? '2px solid #ef4444' : '2px solid #d1d5db',
                    backgroundColor: selectedData.locked ? '#fee2e2' : 'white',
                    cursor: 'pointer',
                    fontSize: '13px',
                    fontWeight: '500',
                    color: selectedData.locked ? '#dc2626' : '#666',
                    transition: 'all 0.2s'
                  }}
                >
                  {selectedData.locked ? '🔒 Đã khóa' : '🔓 Mở khóa'}
                </button>

                {/* isActive Toggle */}
                <button
                  onClick={async () => {
                    try {
                      const res = await updateAreaIsActive(selectedData.areaId, { 
                        areaId: selectedData.areaId,
                        isActive: !selectedData.isActive 
                      });
                      dispatch({ type: actions.UPDATE_AREA, payload: res.data });
                    } catch (e) {
                      console.error("Failed to update area active status:", e);
                      alert("Cập nhật trạng thái hoạt động thất bại");
                    }
                  }}
                  style={{
                    flex: 1,
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: selectedData.isActive ? '2px solid #d1d5db' : '2px solid #fecaca',
                    backgroundColor: selectedData.isActive ? 'white' : '#fee2e2',
                    cursor: 'pointer',
                    fontSize: '13px',
                    fontWeight: '500',
                    color: selectedData.isActive ? '#666' : '#dc2626',
                    transition: 'all 0.2s'
                  }}
                >
                  {selectedData.isActive ? '✓ Hoạt động' : '✗ Không hoạt động'}
                </button>
              </div>
            </div>

            {/* Statistics */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb',
              marginTop: 'auto'
            }}>
              <h4 style={{
                margin: '0 0 8px 0',
                fontSize: '11px',
                fontWeight: '600',
                color: '#6b7280',
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                textAlign: 'center'
              }}>
                📊 Thống kê
              </h4>
              
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '8px'
              }}>
                {/* Total Zones */}
                <div style={{
                  textAlign: 'center',
                  padding: '8px',
                  backgroundColor: '#f0f9ff',
                  borderRadius: '6px',
                  border: '1px solid #bfdbfe'
                }}>
                  <div style={{
                    fontSize: '9px',
                    color: '#1e40af',
                    fontWeight: '600',
                    marginBottom: '4px',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }}>
                    Phòng
                  </div>
                  <div style={{
                    fontSize: '20px',
                    fontWeight: '700',
                    color: '#1e3a8a',
                    lineHeight: '1'
                  }}>
                    {zones.filter(z => z.areaId === selectedItem.id).length}
                  </div>
                </div>

                {/* Total Seats */}
                <div style={{
                  textAlign: 'center',
                  padding: '8px',
                  backgroundColor: '#f0fdf4',
                  borderRadius: '6px',
                  border: '1px solid #bbf7d0'
                }}>
                  <div style={{
                    fontSize: '9px',
                    color: '#15803d',
                    fontWeight: '600',
                    marginBottom: '4px',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }}>
                    Tổng số ghế
                  </div>
                  <div style={{
                    fontSize: '20px',
                    fontWeight: '700',
                    color: '#166534',
                    lineHeight: '1'
                  }}>
                    {seats.filter(s => zones.find(z => z.areaId === selectedItem.id && z.zoneId === s.zoneId)).length}
                  </div>
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
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '600',
                color: '#6b7280',
                marginBottom: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Tên khu vực
              </label>
              <input
                value={localZoneName}
                onChange={(e) => {
                  setLocalZoneName(e.target.value);
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !isComposing) {
                    e.preventDefault();
                    saveZoneName();
                  }
                }}
                onCompositionStart={() => setIsComposing(true)}
                onCompositionEnd={() => setIsComposing(false)}
                onBlur={() => {
                  saveZoneName();
                }}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '2px solid #e5e7eb',
                  borderRadius: '6px',
                  fontSize: '14px',
                  fontWeight: '500',
                  transition: 'all 0.2s',
                  outline: 'none'
                }}
                onFocus={(e) => e.target.style.borderColor = '#0ea5e9'}
                onBlurCapture={(e) => e.target.style.borderColor = '#e5e7eb'}
              />
            </div>

            {/* Zone Description */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '600',
                color: '#6b7280',
                marginBottom: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Mô tả khu vực
              </label>
              <textarea
                value={localZoneDes}
                onChange={(e) => {
                  setLocalZoneDes(e.currentTarget.value);
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey && !isComposing) {
                    e.preventDefault();
                    saveZoneDes();
                  }
                }}
                onCompositionStart={() => setIsComposing(true)}
                onCompositionEnd={() => setIsComposing(false)}
                onBlur={() => {
                  saveZoneDes();
                }}
                placeholder="Nhập mô tả khu vực, nhấn Enter để lưu..."
                rows="3"
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '2px solid #e5e7eb',
                  borderRadius: '6px',
                  fontSize: '13px',
                  fontFamily: 'inherit',
                  resize: 'vertical',
                  transition: 'all 0.2s',
                  outline: 'none'
                }}
                onFocus={(e) => e.target.style.borderColor = '#0ea5e9'}
                onBlurCapture={(e) => e.target.style.borderColor = '#e5e7eb'}
              />
            </div>

            {/* Lock & Color Settings */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb'
            }}>
              <h4 style={{
                margin: '0 0 10px 0',
                fontSize: '11px',
                fontWeight: '600',
                color: '#6b7280',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                ⚙️ Cài đặt
              </h4>

              {/* Lock Toggle Button */}
              <button
                onClick={() => handleZoneChange("isLocked", !selectedData.isLocked)}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  border: selectedData.isLocked ? '2px solid #fecaca' : '2px solid #d1d5db',
                  backgroundColor: selectedData.isLocked ? '#fee2e2' : 'white',
                  cursor: 'pointer',
                  fontSize: '13px',
                  fontWeight: '500',
                  color: selectedData.isLocked ? '#dc2626' : '#666',
                  transition: 'all 0.2s',
                  marginBottom: '10px'
                }}
              >
                {selectedData.isLocked ? '🔒 Đã khóa' : '🔓 Mở khóa'}
              </button>

              {/* Color Picker */}
              <div>
                <label style={{
                  display: 'block',
                  fontSize: '11px',
                  fontWeight: '600',
                  color: '#6b7280',
                  marginBottom: '6px',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px'
                }}>
                  🎨 Màu khu vực
                </label>
                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                  <div style={{
                    position: 'relative',
                    width: '60px',
                    height: '40px',
                    borderRadius: '6px',
                    border: '2px solid #e5e7eb',
                    overflow: 'hidden',
                    cursor: 'pointer'
                  }}>
                    <input
                      type="color"
                      value={selectedData.color || '#d1f7d8'}
                      onChange={(e) => handleZoneChange("color", e.target.value)}
                      style={{ 
                        width: '100%',
                        height: '100%',
                        border: 'none',
                        cursor: 'pointer',
                        padding: 0
                      }}
                      title="Chọn màu"
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Amenities List */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb',
              marginTop: 'auto'
            }}>
              <div style={{ 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'space-between',
                marginBottom: '8px'
              }}>
                <h4 style={{
                  margin: '0',
                  fontSize: '11px',
                  fontWeight: '600',
                  color: '#6b7280',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px'
                }}>
                  📋 Danh sách tiện ích
                </h4>
                <button
                  onClick={() => setShowAddModal(true)}
                  style={{
                    border: 'none',
                    borderRadius: '6px',
                    cursor: 'pointer',
                    fontSize: '10px',
                    fontWeight: '600',
                    padding: '4px 10px',
                    color: 'white',
                    transition: 'all 0.2s'
                  }}
                  onMouseEnter={(e) => e.target.style.transform = 'scale(1.05)'}
                  onMouseLeave={(e) => e.target.style.transform = 'scale(1)'}
                  title="Thêm tiện ích"
                >
                  ➕ 
                </button>
              </div>
               <div 
                 className="amenities-scroll"
                 style={{
                   border: '2px solid #3b82f6',
                   borderRadius: '8px',
                   maxHeight: '150px',
                   overflowY: 'auto',
                   overflowX: 'hidden',
                   backgroundColor: '#eff6ff',
                   boxShadow: '0 2px 4px rgba(59, 130, 246, 0.1)',
                   padding: '4px'
                 }}
               >
                 <style>{`
                   .amenities-scroll::-webkit-scrollbar {
                     width: 8px;
                   }
                   .amenities-scroll::-webkit-scrollbar-track {
                     background: #dbeafe;
                     border-radius: 4px;
                   }
                   .amenities-scroll::-webkit-scrollbar-thumb {
                     background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
                     border-radius: 4px;
                     border: 2px solid #dbeafe;
                   }
                   .amenities-scroll::-webkit-scrollbar-thumb:hover {
                     background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
                   }
                 `}</style>
                {amenities && amenities.length > 0 ? (
                  amenities.map((amenity, index) => (
                    <div key={amenity.amenityId || index} style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '10px 12px',
                      borderBottom: index < amenities.length - 1 ? '1px solid #e5e7eb' : 'none',
                      fontSize: '13px',
                      color: '#333',
                      transition: 'background 0.2s'
                    }}
                    onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#e0f2fe'}
                    onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                    >
                      <span style={{ flex: 1, fontWeight: '500' }}>{amenity.amenityName}</span>
                      <button
                        onClick={() => handleDeleteAmenity(amenity.amenityId)}
                        style={{
                          background: 'none',
                          border: 'none',
                          cursor: 'pointer',
                          padding: '4px',
                          fontSize: '15px',
                          color: '#ef4444',
                          transition: 'transform 0.2s'
                        }}
                        onMouseEnter={(e) => e.target.style.transform = 'scale(1.2)'}
                        onMouseLeave={(e) => e.target.style.transform = 'scale(1)'}
                        title="Xóa tiện ích"
                      >
                        ×
                      </button>
                    </div>
                  ))
                ) : (
                  <div style={{
                    padding: '20px',
                    textAlign: 'center',
                    color: '#9ca3af',
                    fontSize: '13px'
                  }}>
                    Chưa có tiện ích
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
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '600',
                color: '#6b7280',
                marginBottom: '6px',
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
                  padding: '8px 10px',
                  border: '2px solid #e5e7eb',
                  borderRadius: '6px',
                  fontSize: '14px',
                  fontWeight: '500',
                  transition: 'all 0.2s',
                  outline: 'none'
                }}
                onFocus={(e) => e.target.style.borderColor = '#0ea5e9'}
                onBlurCapture={(e) => e.target.style.borderColor = '#e5e7eb'}
              />
            </div>

            {/* Seat Status */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '600',
                color: '#6b7280',
                marginBottom: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Trạng thái
              </label>
              <select
                value={localSeatIsActive}
                onChange={(e) => {
                  const newIsActive = e.target.value === "true";
                  setLocalSeatIsActive(newIsActive);
                  handleSeatChange("isActive", newIsActive);
                }}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '2px solid #e5e7eb',
                  borderRadius: '6px',
                  fontSize: '14px',
                  fontWeight: '500',
                  backgroundColor: 'white',
                  outline: 'none',
                  transition: 'all 0.2s'
                }}
                onFocus={(e) => e.target.style.borderColor = '#0ea5e9'}
                onBlur={(e) => e.target.style.borderColor = '#e5e7eb'}
              >
                <option value="true">Hoạt động</option>
                <option value="false">Bảo trì</option>
              </select>
            </div>
          </>
        )}

        {/* ============ FACTORY ============ */}
        {selectedItem.type === "factory" && (
          <>
            {/* Factory Name */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '600',
                color: '#6b7280',
                marginBottom: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Tên cơ sở vật chất 
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
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  border: '2px solid #e5e7eb',
                  borderRadius: '6px',
                  fontSize: '14px',
                  fontWeight: '500',
                  transition: 'all 0.2s',
                  outline: 'none'
                }}
                onFocus={(e) => e.target.style.borderColor = '#0ea5e9'}
              />
            </div>

            {/* Factory Color */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '600',
                color: '#6b7280',
                marginBottom: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Màu sắc
              </label>
              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                <input
                  type="color"
                  value={localFactoryColor}
                  onChange={(e) => {
                    setLocalFactoryColor(e.target.value);
                    handleFactoryChange("color", e.target.value);
                  }}
                  style={{
                    width: '50px',
                    height: '40px',
                    border: '2px solid #e5e7eb',
                    borderRadius: '6px',
                    cursor: 'pointer'
                  }}
                />
                <input
                  type="text"
                  value={localFactoryColor}
                  onChange={(e) => {
                    setLocalFactoryColor(e.target.value);
                  }}
                  onBlur={() => handleFactoryChange("color", localFactoryColor)}
                  style={{
                    flex: 1,
                    padding: '8px 10px',
                    border: '2px solid #e5e7eb',
                    borderRadius: '6px',
                    fontSize: '14px',
                    fontWeight: '500',
                    outline: 'none'
                  }}
                  onFocus={(e) => e.target.style.borderColor = '#0ea5e9'}
                />
              </div>
            </div>

            {/* Factory Dimensions */}
            <div style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #e5e7eb'
            }}>
              <label style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: '600',
                color: '#6b7280',
                marginBottom: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
              }}>
                Kích thước
              </label>
              <div style={{ display: 'flex', gap: '8px' }}>
                <div style={{ flex: 1 }}>
                  <small style={{ color: '#9ca3af', display: 'block', marginBottom: '4px' }}>Chiều rộng</small>
                  <input
                    type="number"
                    value={selectedData.width || 0}
                    disabled
                    style={{
                      width: '100%',
                      padding: '8px 10px',
                      border: '2px solid #e5e7eb',
                      borderRadius: '6px',
                      fontSize: '12px',
                      backgroundColor: '#f9fafb',
                      color: '#9ca3af'
                    }}
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <small style={{ color: '#9ca3af', display: 'block', marginBottom: '4px' }}>Chiều cao</small>
                  <input
                    type="number"
                    value={selectedData.height || 0}
                    disabled
                    style={{
                      width: '100%',
                      padding: '8px 10px',
                      border: '2px solid #e5e7eb',
                      borderRadius: '6px',
                      fontSize: '12px',
                      backgroundColor: '#f9fafb',
                      color: '#9ca3af'
                    }}
                  />
                </div>
              </div>
              <small style={{ color: '#9ca3af', marginTop: '8px', display: 'block' }}>
                (Kéo trên canvas để thay đổi kích thước)
              </small>
            </div>
          </>
        )}
      </div>

      <div className="panel-footer">
        <button className="delete-btn" onClick={handleDelete}>
          Xóa
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
            borderRadius: '8px',
            padding: '24px',
            width: '90%',
            maxWidth: '400px',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
          }}>
            <h3 style={{ marginTop: 0, marginBottom: '16px', color: '#1f2937' }}>
              Thêm tiện ích mới
            </h3>
            
            <input
              type="text"
              placeholder="Nhập tên tiện ích..."
              value={addAmenityName}
              onChange={(e) => setAddAmenityName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleAddAmenity();
              }}
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #ddd',
                borderRadius: '6px',
                fontSize: '14px',
                marginBottom: '16px',
                boxSizing: 'border-box'
              }}
              autoFocus
            />
            
            <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
              <button
                onClick={() => {
                  setShowAddModal(false);
                  setAddAmenityName("");
                }}
                style={{
                  padding: '8px 16px',
                  border: '1px solid #ddd',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  backgroundColor: '#f3f4f6',
                  color: '#374151',
                  fontSize: '14px'
                }}
              >
                Hủy
              </button>
              <button
                onClick={handleAddAmenity}
                style={{
                  padding: '8px 16px',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  backgroundColor: '#22c55e',
                  color: 'white',
                  fontSize: '14px'
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
            borderRadius: '12px',
            padding: '24px',
            width: '90%',
            maxWidth: '420px',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.2)',
            border: '2px solid #fca5a5'
          }}>
            <div style={{ textAlign: 'center', marginBottom: '20px' }}>
              <div style={{ fontSize: '48px', marginBottom: '12px' }}>⚠️</div>
              <h3 style={{ margin: '0 0 8px 0', color: '#dc2626', fontSize: '18px', fontWeight: '600' }}>
                Xác nhận xóa {selectedItem.type === 'area' ? 'Phòng' : selectedItem.type === 'zone' ? 'Khu vực' : selectedItem.type === 'seat' ? 'Ghế' : 'Vật cản'}
              </h3>
              <p style={{ margin: '0', color: '#6b7280', fontSize: '14px' }}>
                {selectedItem.type === 'area' && 'Xóa Phòng sẽ xóa tất cả Khu vực và Ghế bên trong!'}
                {selectedItem.type === 'zone' && 'Xóa Khu vực sẽ xóa tất cả Ghế bên trong!'}
                {selectedItem.type === 'seat' && 'Bạn có chắc chắn muốn xóa Ghế này?'}
                {selectedItem.type === 'factory' && 'Bạn có chắc chắn muốn xóa Vật cản này?'}
              </p>
            </div>
            
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                onClick={() => setShowDeleteConfirm(false)}
                style={{
                  padding: '10px 24px',
                  border: '2px solid #d1d5db',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  backgroundColor: 'white',
                  color: '#374151',
                  fontSize: '14px',
                  fontWeight: '600',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => e.target.style.backgroundColor = '#f3f4f6'}
                onMouseLeave={(e) => e.target.style.backgroundColor = 'white'}
              >
                 Hủy
              </button>
              <button
                onClick={confirmDelete}
                style={{
                  padding: '10px 24px',
                  border: 'none',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  background: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '600',
                  boxShadow: '0 2px 8px rgba(239, 68, 68, 0.3)',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => e.target.style.transform = 'scale(1.05)'}
                onMouseLeave={(e) => e.target.style.transform = 'scale(1)'}
              >
                 Xóa
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal for Area/Zone/Seat */}
      {showDeleteConfirm && selectedItem && (
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
          zIndex: 1001
        }}>
          <div style={{
            backgroundColor: 'white',
            borderRadius: '12px',
            padding: '24px',
            width: '90%',
            maxWidth: '420px',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.2)',
            border: '2px solid #fca5a5'
          }}>
            <div style={{ textAlign: 'center', marginBottom: '20px' }}>
              <div style={{ fontSize: '48px', marginBottom: '12px' }}>⚠️</div>
              <h3 style={{ margin: '0 0 8px 0', color: '#dc2626', fontSize: '18px', fontWeight: '600' }}>
                Xác nhận xóa {selectedItem.type === 'area' ? 'Phòng' : selectedItem.type === 'zone' ? 'Khu vực' : selectedItem.type === 'seat' ? 'Ghế' : 'Vật cản'}
              </h3>
              <p style={{ margin: '0', color: '#6b7280', fontSize: '14px' }}>
                {selectedItem.type === 'area' && 'Xóa Phòng sẽ xóa tất cả Khu vực và Ghế bên trong!'}
                {selectedItem.type === 'zone' && 'Xóa Khu vực sẽ xóa tất cả Ghế bên trong!'}
                {selectedItem.type === 'seat' && 'Bạn có chắc chắn muốn xóa Ghế này?'}
                {selectedItem.type === 'factory' && 'Bạn có chắc chắn muốn xóa Vật cản này?'}
              </p>
            </div>
            
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                onClick={() => setShowDeleteConfirm(false)}
                style={{
                  padding: '10px 24px',
                  border: '2px solid #d1d5db',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  backgroundColor: 'white',
                  color: '#374151',
                  fontSize: '14px',
                  fontWeight: '600',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => e.target.style.backgroundColor = '#f3f4f6'}
                onMouseLeave={(e) => e.target.style.backgroundColor = 'white'}
              >
                Hủy
              </button>
              <button
                onClick={confirmDelete}
                style={{
                  padding: '10px 24px',
                  border: 'none',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  background: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '600',
                  boxShadow: '0 2px 8px rgba(239, 68, 68, 0.3)',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => e.target.style.transform = 'scale(1.05)'}
                onMouseLeave={(e) => e.target.style.transform = 'scale(1)'}
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
            borderRadius: '12px',
            padding: '24px',
            width: '90%',
            maxWidth: '380px',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.2)',
            border: '2px solid #fca5a5'
          }}>
            <div style={{ textAlign: 'center', marginBottom: '20px' }}>
        <h3 style={{ margin: '0 0 8px 0', color: '#dc2626', fontSize: '18px', fontWeight: '600' }}>
                Xóa tiện ích
              </h3>
              <p style={{ margin: '0', color: '#6b7280', fontSize: '14px' }}>
                Bạn có chắc chắn muốn xóa tiện ích này?
              </p>
            </div>
            
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                onClick={() => setDeleteAmenityId(null)}
                style={{
                  padding: '10px 24px',
                  border: '2px solid #d1d5db',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  backgroundColor: 'white',
                  color: '#374151',
                  fontSize: '14px',
                  fontWeight: '600',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => e.target.style.backgroundColor = '#f3f4f6'}
                onMouseLeave={(e) => e.target.style.backgroundColor = 'white'}
              >
                 Hủy
              </button>
              <button
                onClick={confirmDeleteAmenity}
                style={{
                  padding: '10px 24px',
                  border: 'none',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  background: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '600',
                  boxShadow: '0 2px 8px rgba(239, 68, 68, 0.3)',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => e.target.style.transform = 'scale(1.05)'}
                onMouseLeave={(e) => e.target.style.transform = 'scale(1)'}
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

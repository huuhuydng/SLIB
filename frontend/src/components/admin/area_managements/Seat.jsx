import { useLayout } from "../../../context/admin/area_management/LayoutContext";
import { updateSeat } from "../../../services/admin/area_management/api";

function Seat({ seat }) {
  const { state, dispatch, actions } = useLayout();
  const { selectedItem, isPreviewMode } = state;

  const isSelected =
    selectedItem?.type === "seat" &&
    selectedItem?.id === seat.seatId;

  /* ================= SELECT ================= */

  const handleClick = (e) => {
    e.stopPropagation();
    dispatch({
      type: actions.SELECT_ITEM,
      payload: { type: "seat", id: seat.seatId },
    });
  };

  /* ================= TOGGLE STATUS ================= */

  const handleDoubleClick = async (e) => {
    e.stopPropagation();

    const newIsActive = seat.isActive !== true;
    console.log("[Seat] DoubleClick - toggling seat", {
      seatId: seat.seatId,
      currentIsActive: seat.isActive,
      newIsActive
    });

    const buildSeatPayload = (s, override = {}) => {
      const {
        seatId,
        zoneId,
        seatCode,
        positionX,
        positionY,
        width,
        height,
        isActive,
      } = s || {};

      const payload = {
        seatId,
        zoneId,
        seatCode,
        positionX,
        positionY,
        width,
        height,
        isActive,
        ...override,
      };

      Object.keys(payload).forEach((key) => {
        if (payload[key] === undefined || payload[key] === null) delete payload[key];
      });

      return payload;
    };

    // update backend
    try {
      const payload = buildSeatPayload(seat, { isActive: newIsActive });
      console.log("[Seat] updateSeat payload:", payload);
      const res = await updateSeat(seat.seatId, payload);
      console.log("[Seat] updateSeat response:", res?.status, res?.data);

      // sync context
      dispatch({
        type: actions.UPDATE_SEAT,
        payload: {
          ...seat,
          seatId: seat.seatId,
          isActive: newIsActive,
        },
      });
      console.log("[Seat] Dispatched UPDATE_SEAT for", seat.seatId, "-> isActive:", newIsActive);
    } catch (e) {
      console.error("[Seat] Failed to update seat status", e);
    }
  };

  /* ================= RENDER ================= */

  // Preview mode: màu xanh lá giống mobile (#4CAF50)
  // Edit mode: màu cam (#F97316) cho ghế hoạt động, xám (#9CA3AF) cho ghế bảo trì
  const getStatusColor = () => {
    if (isPreviewMode) {
      return seat.isActive !== false ? "#4CAF50" : "#9CA3AF";
    }
    return seat.isActive !== false ? "#F97316" : "#9CA3AF";
  };

  const statusColor = getStatusColor();

  return (
    <div
      className={`seat ${seat.isActive ? "active" : "inactive"} ${isSelected ? "selected" : ""}`}
      onClick={isPreviewMode ? undefined : handleClick}
      onDoubleClick={isPreviewMode ? undefined : handleDoubleClick}
      title={`${seat.seatCode} - ${seat.isActive ? "Hoạt động" : "Bảo trì"}`}
      style={{
        width: '100%',
        height: '100%',
        borderRadius: isPreviewMode ? '8px' : '4px',
        backgroundColor: statusColor,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        fontSize: isPreviewMode ? '11px' : '12px',
        fontWeight: 'bold',
        cursor: isPreviewMode ? 'default' : 'pointer',
        border: isPreviewMode
          ? '2px solid rgba(0,0,0,0.15)'
          : (isSelected ? '2px solid #1976d2' : '1px solid #ccc'),
        transition: 'all 0.2s',
        boxShadow: isPreviewMode ? '0 2px 4px rgba(0,0,0,0.1)' : 'none',
        pointerEvents: isPreviewMode ? 'none' : 'auto',
      }}
    >
      {seat.seatCode || 'S'}
    </div>
  );
}

export default Seat;
import { useLayout } from "../../../context/admin/area_management/LayoutContext";
import { updateSeat } from "../../../services/admin/area_management/api";

function Seat({ seat }) {
  const { state, dispatch, actions } = useLayout();
  const { selectedItem } = state;

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

  const statusColor = seat.isActive !== false ? "var(--color-seat-available)" : "var(--color-seat-reserved)"; // Green/available for active, Red for maintenance

  return (
    <div
      className={`seat ${seat.isActive ? "active" : "inactive"} ${isSelected ? "selected" : ""}`}
      onClick={handleClick}
      onDoubleClick={handleDoubleClick}
      title={`${seat.seatCode} - ${seat.isActive ? "Hoạt động" : "Bảo trì"}`}
      style={{
        width: '100%',
        height: '100%',
        borderRadius: '4px',
        backgroundColor: statusColor,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        fontSize: '12px',
        fontWeight: 'bold',
        cursor: 'pointer',
        border: isSelected ? '2px solid #1976d2' : '1px solid #ccc',
        transition: 'all 0.2s',
      }}
    >
      {seat.seatCode || 'S'}
    </div>
  );
}

export default Seat;
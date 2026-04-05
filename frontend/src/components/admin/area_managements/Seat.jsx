import { useLayout } from "../../../context/admin/area_management/LayoutContext";

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

    const payload = buildSeatPayload(seat, { isActive: newIsActive });
    dispatch({
      type: actions.UPDATE_SEAT,
      payload: {
        ...seat,
        seatId: seat.seatId,
        isActive: newIsActive,
      },
    });
    if (seat.seatId > 0) {
      dispatch({ type: actions.ADD_PENDING_SEAT_UPDATE, payload });
    }
    dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
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

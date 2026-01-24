// Seat component cho librarian - hiển thị màu theo trạng thái booking
function LibrarianSeat({ seat, onSeatClick }) {
  const handleClick = (e) => {
    e.stopPropagation();
    if (onSeatClick) {
      onSeatClick(seat);
    }
  };

  // Màu sắc theo trạng thái:
  // AVAILABLE (trống) = cam #ff9800
  // BOOKED (đã đặt) = xám #9ca3af
  // UNAVAILABLE (bị hạn chế) = đỏ #ef4444
  let statusColor = '#ff9800'; // AVAILABLE = cam
  let statusText = 'Trống';

  const status = (seat.seatStatus || 'AVAILABLE').toUpperCase();

  if (status === 'BOOKED') {
    statusColor = '#9ca3af'; // BOOKED = xám
    statusText = 'Đã đặt';
  } else if (status === 'UNAVAILABLE') {
    statusColor = '#ef4444'; // UNAVAILABLE = đỏ
    statusText = 'Bị hạn chế';
  }

  return (
    <div
      onClick={handleClick}
      title={`${seat.seatCode} - ${statusText}`}
      style={{
        width: '100%',
        height: '100%',
        borderRadius: '4px',
        backgroundColor: statusColor,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        fontSize: '11px',
        fontWeight: 'bold',
        cursor: 'pointer',
        border: '1px solid rgba(255,255,255,0.3)',
        transition: 'all 0.2s',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'scale(1.05)';
        e.currentTarget.style.boxShadow = '0 2px 6px rgba(0,0,0,0.2)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'scale(1)';
        e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.1)';
      }}
    >
      {seat.seatCode || 'S'}
    </div>
  );
}

export default LibrarianSeat;

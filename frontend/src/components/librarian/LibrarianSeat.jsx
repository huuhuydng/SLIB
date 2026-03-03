// Seat component cho librarian - hiển thị màu theo trạng thái booking
function LibrarianSeat({ seat, onSeatClick }) {
  const handleClick = (e) => {
    e.stopPropagation();
    if (onSeatClick) {
      onSeatClick(seat);
    }
  };

  // Màu sắc theo trạng thái (phù hợp với legend):
  // AVAILABLE (trống) = xanh lá #22c55e
  // BOOKED (đã đặt) = cam #f97316
  // UNAVAILABLE (bị hạn chế) = xám #9ca3af
  let statusColor = '#22c55e'; // AVAILABLE = xanh lá
  let statusText = 'Trống';

  const status = (seat.seatStatus || 'AVAILABLE').toUpperCase();

  if (status === 'BOOKED') {
    statusColor = '#f97316'; // BOOKED = cam
    statusText = 'Đã đặt';
  } else if (status === 'UNAVAILABLE') {
    statusColor = '#9ca3af'; // UNAVAILABLE = xám
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

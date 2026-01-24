import { useEffect, useRef, useState } from 'react';
import { useLayout } from '../../context/admin/area_management/LayoutContext';
import LibrarianSeat from './LibrarianSeat';
import { calculateDynamicSeatLayout, calculateMinZoneDimensions } from '../../utils/admin/seatLayout';

// Zone component cho librarian - chỉ hiển thị, không kéo thả
// Seats được load từ parent (LibrarianAreas page) theo time slot
function LibrarianZone({ zone, area, onSeatClick }) {
  const { state } = useLayout();
  const { seats } = state;
  const zoneSeats = (seats || []).filter((s) => String(s.zoneId) === String(zone.zoneId));

  // Tính min dimensions để chứa hết seats
  const minDimensions = zoneSeats.length > 0 
    ? calculateMinZoneDimensions(zoneSeats)
    : { minWidth: zone.width || 120, minHeight: zone.height || 100 };

  // Sử dụng kích thước lớn hơn giữa zone design và min dimensions
  const displayWidth = Math.max(zone.width || 120, minDimensions.minWidth);
  const displayHeight = Math.max(zone.height || 100, minDimensions.minHeight);

  return (
    <div
      style={{
        position: 'absolute',
        left: `${zone.positionX || 0}px`,
        top: `${zone.positionY || 0}px`,
        width: `${displayWidth}px`,
        height: `${displayHeight}px`,
        border: '1px solid #9CA3AF',
        borderRadius: '8px',
        padding: '8px',
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#E5E7EB',
      }}
    >
      <div style={{
        fontSize: '12px',
        fontWeight: '600',
        marginBottom: '4px',
        color: '#06361a',
      }}>
        {zone.zoneName || 'Unnamed Zone'}
      </div>

      <div style={{
        flex: 1,
        overflow: 'visible', // Changed from 'hidden' to 'visible' to show all seats
        position: 'relative',
        minHeight: '100px',
      }}>
        {zoneSeats.map((seat) => {
          const layout = calculateDynamicSeatLayout(seat, displayWidth || 120, displayHeight || 100, zoneSeats);

          return (
            <div
              key={seat.seatId}
              style={{
                position: 'absolute',
                left: layout.positionX,
                top: layout.positionY,
                width: layout.width,
                height: layout.height,
              }}
            >
              <LibrarianSeat seat={seat} zone={zone} onSeatClick={onSeatClick} />
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default LibrarianZone;

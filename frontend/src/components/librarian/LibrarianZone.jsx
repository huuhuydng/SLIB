import { useEffect, useRef, useState, useMemo } from 'react';
import { useLayout } from '../../context/admin/area_management/LayoutContext';
import LibrarianSeat from './LibrarianSeat';
import { calculateDynamicSeatLayout, calculateMinZoneDimensions } from '../../utils/admin/seatLayout';

// Tính occupancy & lấy màu/text tương tự mobile
const getOccupancyInfo = (zoneSeats) => {
  const total = zoneSeats.length;
  if (total === 0) return { percent: 0, booked: 0, total: 0, level: 'empty', text: 'Trống', bgColor: '#E5E7EB', borderColor: '#9CA3AF', badgeColor: '#6B7280', textColor: '#06361a' };

  const booked = zoneSeats.filter(s => (s.seatStatus || '').toUpperCase() === 'BOOKED').length;
  const percent = Math.round((booked / total) * 100);

  if (percent >= 90) {
    return { percent, booked, total, level: 'high', text: 'Hết chỗ', bgColor: 'rgba(231, 76, 60, 0.12)', borderColor: 'rgba(231, 76, 60, 0.5)', badgeColor: '#E74C3C', textColor: '#922B21' };
  } else if (percent >= 50) {
    return { percent, booked, total, level: 'medium', text: 'Khá đông', bgColor: 'rgba(243, 156, 18, 0.12)', borderColor: 'rgba(243, 156, 18, 0.5)', badgeColor: '#F39C12', textColor: '#7D6608' };
  } else {
    return { percent, booked, total, level: 'low', text: 'Trống', bgColor: 'rgba(39, 174, 96, 0.1)', borderColor: 'rgba(39, 174, 96, 0.4)', badgeColor: '#27AE60', textColor: '#0E6230' };
  }
};

// Zone component cho librarian - chỉ hiển thị, không kéo thả
// Seats được load từ parent (LibrarianAreas page) theo time slot
function LibrarianZone({ zone, area, onSeatClick }) {
  const { state } = useLayout();
  const { seats } = state;
  const zoneSeats = (seats || []).filter((s) => String(s.zoneId) === String(zone.zoneId));

  // Tính occupancy
  const occ = useMemo(() => getOccupancyInfo(zoneSeats), [zoneSeats]);

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
        border: `2px solid ${occ.borderColor}`,
        borderRadius: '8px',
        padding: '8px',
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: occ.bgColor,
        transition: 'background-color 0.4s ease, border-color 0.4s ease',
      }}
    >
      {/* Zone header với tên + badge mật độ */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: '4px',
        gap: '6px',
      }}>
        <span style={{
          fontSize: '12px',
          fontWeight: '600',
          color: occ.textColor,
          whiteSpace: 'nowrap',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
        }}>
          {zone.zoneName || 'Unnamed Zone'}
        </span>
        {zoneSeats.length > 0 && (
          <span style={{
            fontSize: '10px',
            fontWeight: '600',
            color: '#fff',
            backgroundColor: occ.badgeColor,
            padding: '2px 8px',
            borderRadius: '10px',
            whiteSpace: 'nowrap',
            flexShrink: 0,
          }}>
            {occ.booked}/{occ.total}
          </span>
        )}
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


import { useEffect, useState } from 'react';
import { useLayout } from '../../context/admin/area_management/LayoutContext';
import Seat from './Seat';
import { getSeats } from '../services/api';
import '../styles/admin/AreaMap.css';

function Zone({ zone, area }) {
  const { state, dispatch, actions } = useLayout();
  const { seats, selectedItem } = state;
  const [zoneSeats, setZoneSeats] = useState([]);

  // Load seats for this zone
  useEffect(() => {
    if (!zone?.zoneId) return;
    (async () => {
      try {
        const res = await getSeats(zone.zoneId);
        setZoneSeats(res.data || []);
      } catch (e) {
        console.error('Failed to load seats for zone', zone.zoneId, e);
      }
    })();
  }, [zone?.zoneId]);

  const isSelected = selectedItem?.type === 'zone' && selectedItem?.id === zone.zoneId;

  const handleZoneClick = (e) => {
    e.stopPropagation();
    dispatch({
      type: actions.SELECT_ITEM,
      payload: { type: 'zone', id: zone.zoneId },
    });
  };

  return (
    <div
      className={`zone-card ${isSelected ? 'selected' : ''}`}
      style={{
        position: 'absolute',
        left: zone.positionX || 0,
        top: zone.positionY || 0,
        width: zone.width || 120,
        height: zone.height || 100,
        backgroundColor: zone.color || '#d1f7d8',
        border: isSelected ? '3px solid #1976d2' : '1px solid #34a853',
        borderRadius: '8px',
        padding: '8px',
        boxSizing: 'border-box',
        cursor: 'pointer',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
      }}
      onClick={handleZoneClick}
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
        overflow: 'auto',
        display: 'flex',
        flexWrap: 'wrap',
        gap: '4px',
        alignContent: 'flex-start',
      }}>
        {zoneSeats.map((seat) => (
          <Seat
            key={seat.seatId}
            seat={seat}
            zone={zone}
          />
        ))}
      </div>

      <div style={{
        fontSize: '10px',
        color: '#666',
        marginTop: '4px',
      }}>
        {zoneSeats.length} seats
      </div>
    </div>
  );
}

export default Zone;

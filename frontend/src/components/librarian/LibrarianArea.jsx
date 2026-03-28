import { useEffect, useState } from 'react';
import { useLayout } from '../../context/admin/area_management/LayoutContext';
import LibrarianZone from './LibrarianZone';
import { getZonesByArea, getAreaFactoriesByArea } from '../../services/admin/area_management/api';

// Area component cho librarian - hiển thị zones và factories
function LibrarianArea({ area, onSeatClick }) {
  const { state, dispatch, actions } = useLayout();
  const { zones, factories } = state;
  const [loadingZones, setLoadingZones] = useState(true);
  const [loadingFactories, setLoadingFactories] = useState(true);

  // Load zones for this area
  useEffect(() => {
    if (!area?.areaId) return;
    setLoadingZones(true);
    (async () => {
      try {
        const res = await getZonesByArea(area.areaId);
        const raw = Array.isArray(res?.data) ? res.data : [];
        const zonesNormalized = raw.map((z) => ({
          zoneId: z.zone_id ?? z.zoneId,
          zoneName: z.zone_name ?? z.zoneName,
          zoneDes: z.zone_des ?? z.zoneDes ?? '',
          areaId: z.area_id ?? z.areaId ?? area.areaId,
          positionX: z.position_x ?? z.positionX ?? 0,
          positionY: z.position_y ?? z.positionY ?? 0,
          width: z.width ?? 120,
          height: z.height ?? 100,
          color: z.color ?? '#d1f7d8',
          isLocked: z.is_locked ?? z.isLocked ?? false,
        }));
        dispatch({
          type: actions.MERGE_ZONES,
          payload: {
            areaId: area.areaId,
            zones: zonesNormalized,
          },
        });
      } catch (e) {
        console.error('Failed to load zones for area', area.areaId, e);
      } finally {
        setLoadingZones(false);
      }
    })();
  }, [area?.areaId]);

  // Load factories for this area
  useEffect(() => {
    if (!area?.areaId) return;
    setLoadingFactories(true);
    (async () => {
      try {
        const res = await getAreaFactoriesByArea(area.areaId);
        const convertedFactories = (res.data || []).map(f => ({
          factoryId: f.factory_id ?? f.factoryId,
          factoryName: f.factory_name ?? f.factoryName,
          positionX: f.position_x ?? f.positionX ?? 0,
          positionY: f.position_y ?? f.positionY ?? 0,
          width: f.width ?? 120,
          height: f.height ?? 80,
          color: f.color ?? "#9CA3AF",
          areaId: f.area_id ?? f.areaId,
          isLocked: f.is_locked ?? f.isLocked ?? false,
        }));
        dispatch({
          type: actions.MERGE_FACTORIES,
          payload: {
            areaId: area.areaId,
            factories: convertedFactories,
          },
        });
      } catch (e) {
        console.error('Failed to load factories for area', area.areaId, e);
      } finally {
        setLoadingFactories(false);
      }
    })();
  }, [area?.areaId]);

  const areaZones = zones.filter((z) => z.areaId === area.areaId);
  const areaFactories = factories.filter((f) => f.areaId === area.areaId);

  return (
    <div
      style={{
        position: 'absolute',
        left: `${area.positionX || 0}px`,
        top: `${area.positionY || 0}px`,
        width: `${area.width || 300}px`,
        height: `${area.height || 250}px`,
        minHeight: `${area.height || 250}px`, // Allow height to grow
        border: '2px solid #3b82f6',
        borderRadius: '12px',
        backgroundColor: '#ffffff',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
      }}
    >
      {/* Header */}
      <div style={{
        padding: '12px',
        borderBottom: '1px solid #e5e7eb',
        backgroundColor: '#f8fafc',
        borderRadius: '10px 10px 0 0',
        fontWeight: '600',
        fontSize: '14px',
        color: '#1e293b',
      }}>
        {area.areaName || 'Unnamed Area'}
      </div>

      {/* Content area cho zones và factories */}
      <div style={{
        position: 'relative',
        minHeight: `calc(100% - 48px)`,
        overflow: 'visible', // Changed from 'hidden' to 'visible'
        paddingBottom: '20px', // Add padding to prevent content cutoff
      }}>
        {/* Render factories (fixed elements like bookshelves, walls) */}
        {areaFactories.map((factory) => (
          <div
            key={factory.factoryId}
            style={{
              position: 'absolute',
              left: `${factory.positionX || 0}px`,
              top: `${factory.positionY || 0}px`,
              width: `${factory.width || 120}px`,
              height: `${factory.height || 80}px`,
              backgroundColor: factory.color || '#9CA3AF',
              border: '1px solid #6b7280',
              borderRadius: '4px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '11px',
              fontWeight: '600',
              color: '#fff',
              opacity: 0.7,
            }}
          >
            {factory.factoryName}
          </div>
        ))}

        {/* Render zones with seats */}
        {areaZones.map((zone) => (
          <LibrarianZone 
            key={zone.zoneId} 
            zone={zone} 
            area={area}
            onSeatClick={onSeatClick}
          />
        ))}

        {loadingZones && (
          <div style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            fontSize: '12px',
            color: '#666',
          }}>
            Đang tải zones...
          </div>
        )}
      </div>
    </div>
  );
}

export default LibrarianArea;

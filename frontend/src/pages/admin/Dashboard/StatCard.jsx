import React from "react";
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

const StatCard = ({ icon, value, label, bg, color, trend, trendValue }) => {
  const getTrendIcon = () => {
    if (!trend) return null;
    if (trend === 'up') return <TrendingUp size={12} />;
    if (trend === 'down') return <TrendingDown size={12} />;
    return <Minus size={12} />;
  };

  const getTrendColor = () => {
    if (trend === 'up') return '#DC2626';
    if (trend === 'down') return '#059669';
    return '#6B7280';
  };

  return (
    <div style={{
      background: '#fff',
      borderRadius: '16px',
      padding: '24px',
      boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
      display: 'flex',
      flexDirection: 'column',
      gap: '16px'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{
          width: '48px',
          height: '48px',
          borderRadius: '12px',
          background: bg,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: color
        }}>
          {icon}
        </div>
        {trendValue && (
          <span style={{
            fontSize: '12px',
            fontWeight: '600',
            color: getTrendColor(),
            display: 'flex',
            alignItems: 'center',
            gap: '2px'
          }}>
            {getTrendIcon()}
            {trendValue}
          </span>
        )}
      </div>
      <div>
        <div style={{
          fontSize: '28px',
          fontWeight: '700',
          color: '#1A1A1A',
          marginBottom: '4px'
        }}>{value}</div>
        <div style={{
          fontSize: '13px',
          color: '#A0AEC0'
        }}>{label}</div>
      </div>
    </div>
  );
};

export default StatCard;

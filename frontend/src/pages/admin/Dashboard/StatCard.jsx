import React from "react";
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

const StatCard = ({ icon, value, label, bg, color, trend, trendValue }) => {
  const getTrendIcon = () => {
    if (!trend) return null;
    if (trend === 'up') return <TrendingUp size={12} />;
    if (trend === 'down') return <TrendingDown size={12} />;
    return <Minus size={12} />;
  };

  const trendClass = trend === 'up'
    ? 'statCard__trend--up'
    : trend === 'down'
      ? 'statCard__trend--down'
      : 'statCard__trend--neutral';

  return (
    <div className="statCard">
      <div className="statCard__top">
        <div className="statIcon" style={{ background: bg, color }}>
          {icon}
        </div>
        {trendValue && (
          <span className={`statCard__trend ${trendClass}`}>
            {getTrendIcon()}
            {trendValue}
          </span>
        )}
      </div>
      <div>
        <div className="statValue">{value}</div>
        <div className="statLabel">{label}</div>
      </div>
    </div>
  );
};

export default StatCard;

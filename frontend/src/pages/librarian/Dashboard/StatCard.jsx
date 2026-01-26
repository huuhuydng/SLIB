import React from "react";

const StatCard = ({ icon, value, label, bg, color }) => {
  return (
    <div className="statCard">
      <div className="statIcon" style={{ background: bg, color }}>
        {icon}
      </div>
      <div>
        <div className="statValue">{value}</div>
        <div className="statLabel">{label}</div>
      </div>
    </div>
  );
};

export default StatCard;
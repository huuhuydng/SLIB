
import React from 'react';
import "../../../styles/librarian/librarian-shared.css";
import "../../../styles/librarian/HeatMap.css";

// Mock Data
const MOCK_DATA = {
  occupancy: 69,
  zones: {
    quiet: 70,
    discuss: 30,
    self: 90
  }
};

const Heatmap = () => {
  return (
    <div className="lib-container">
      {/* Page Title + Inline Stats */}
      <div className="lib-page-title">
        <h1>Sơ đồ thư viện</h1>
        <div className="lib-inline-stats">
          <span className="lib-inline-stat">
            <span className="dot green"></span>
            Chiếm dụng <strong>{MOCK_DATA.occupancy}%</strong>
          </span>
          <span className="lib-inline-stat">
            <span className="dot red"></span>
            Khu tự học <strong>Đông</strong>
          </span>
          <span className="lib-inline-stat">
            <span className="dot blue"></span>
            Khu thảo luận <strong>Vắng</strong>
          </span>
        </div>
      </div>

      {/* Map Canvas */}
      <div className="lib-panel hm-map-panel">
        <div className="hm-map-bg"></div>
        <div className="hm-map-layout">
          <div className="hm-block zone-shelf"><span>Kệ sách</span></div>
          <div className="hm-block zone-entrance"><span>Cửa ra vào</span></div>
          <div className="hm-block zone-hall"><span>Sảnh chính</span></div>
          <div className="hm-block zone-library"><span>Thủ thư</span></div>
          <div className="hm-block zone-quiet">
            <span>Khu Yên Tĩnh</span>
            <div className="hm-badge">{MOCK_DATA.zones.quiet}%</div>
          </div>
          <div className="hm-block zone-discuss">
            <span>Khu Thảo Luận</span>
            <div className="hm-badge">{MOCK_DATA.zones.discuss}%</div>
          </div>
          <div className="hm-divider-wall"></div>
          <div className="hm-block zone-self">
            <span>Khu Tự Học</span>
            <div className="hm-badge">{MOCK_DATA.zones.self}%</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Heatmap;
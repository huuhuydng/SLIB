
import React from 'react';
import {
  Armchair as ChairIcon,
  MapPin
} from 'lucide-react';
import Header from "../../../components/shared/Header";
import "../../../styles/librarian/HeatMap.css";
import { handleLogout } from "../../../utils/auth";

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
    <>
    <Header searchPlaceholder="Search for anything..." onLogout={handleLogout} />
    <div style={{ 
      padding: '2rem', 
      backgroundColor: '#f9fafb',
      minHeight: 'calc(100vh - 80px)',
      maxWidth: '1400px',
      margin: '0 auto'
    }}>

      {/* Page Title */}
      <h1 className="page-title">Sơ đồ thư viện</h1>

      {/* Stats Row */}
      <section className="stats-row">
        {/* Card 1: Occupancy */}
        <div className="stat-card green">
          <div className="stat-icon">
            <ChairIcon size={24} />
          </div>
          <div className="stat-content">
            <h3>{MOCK_DATA.occupancy}%</h3>
            <p>Mức độ chiếm dụng hiện tại</p>
          </div>
        </div>

        {/* Card 2: Highest Usage */}
        <div className="stat-card red">
          <div className="stat-icon">
            <MapPin size={24} />
          </div>
          <div className="stat-content">
            <h4>Khu tự học</h4>
            <p>Đang được sử dụng nhiều</p>
          </div>
        </div>

        {/* Card 3: Lowest Usage */}
        <div className="stat-card mint">
          <div className="stat-icon">
            <MapPin size={24} />
          </div>
          <div className="stat-content">
            <h4>Khu thảo luận</h4>
            <p>Đang còn nhiều chỗ trống</p>
          </div>
        </div>
      </section>

      {/* Map Canvas */}
      <section className="map-panel">
        <div className="map-bg-grid"></div>
        
        <div className="map-layout">
          {/* Shelf (Left) */}
          <div className="map-block zone-shelf">
            <span>Kệ sách</span>
          </div>

          {/* Middle Section */}
          <div className="map-block zone-entrance">
            <span>Cửa ra vào</span>
          </div>
          <div className="map-block zone-hall">
            <span>Sảnh chính</span>
          </div>
          <div className="map-block zone-library">
            <span>Thủ thư</span>
          </div>

          {/* Right Section (Quiet) */}
          <div className="map-block zone-quiet">
            <span>Khu Yên Tĩnh</span>
            <div className="map-badge">{MOCK_DATA.zones.quiet}%</div>
          </div>

          {/* Bottom Sections */}
          <div className="map-block zone-discuss">
            <span>Khu Thảo Luận</span>
            <div className="map-badge">{MOCK_DATA.zones.discuss}%</div>
          </div>
          {/* Divider Wall between Discuss and Self */}
          <div className="map-divider-wall"></div>
          <div className="map-block zone-self">
            <span>Khu Tự Học</span>
            <div className="map-badge">{MOCK_DATA.zones.self}%</div>
          </div>
        </div>
      </section>
    </div>
    </>
  );
};

export default Heatmap;
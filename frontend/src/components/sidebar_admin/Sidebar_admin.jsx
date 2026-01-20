import { useNavigate, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  Map,
  Users,
  Monitor,
  Settings,
  Activity,
  Brain,
  LogOut,
} from "lucide-react";
import "../../styles/admin/sidebar_default.css";

const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    { icon: LayoutDashboard, label: "Tổng quan", path: "/dashboard" },
    { icon: Map, label: "Bản đồ thư viện", path: "/library-map" },
    { icon: Users, label: "Quản lý người dùng", path: "/users" },
    { icon: Monitor, label: "Quản lý thiết bị", path: "/devices" },
    { icon: Settings, label: "Cấu hình hệ thống", path: "/config" },
    { icon: Activity, label: "Sức khỏe hệ thống", path: "/health" },
    { icon: Brain, label: "Cấu hình AI", path: "/ai-config" },
  ];

  const handleLogout = () => {
    console.log('🔴🔴🔴 LOGOUT FROM SIDEBAR!');
    localStorage.clear();
    sessionStorage.clear();
    console.log('✅ Storage cleared');
    console.log('🔄 Reloading page...');
    window.location.reload(true); // Force reload from server
  };

  return (
    <aside className="sidebar">
      {/* Logo */}
      <div className="sidebar__brand">
        <div className="sidebar__brandIcon">
          <span style={{ color: "#FF6B00", fontWeight: "bold", fontSize: "24px" }}>
            S
          </span>
        </div>
        <span className="sidebar__brandText">
          <span style={{ fontWeight: 700, color: "#333" }}>SLIB</span>
          <span style={{ fontWeight: 400, color: "#FF6B00" }}>.</span>
        </span>
      </div>

      {/* Navigation */}
      <nav className="sidebar__nav">
        {menuItems.map((item, index) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;

          return (
            <button
              key={index}
              className={`sidebar__item ${isActive ? "sidebar__item--active" : ""}`}
              onClick={() => navigate(item.path)}
            >
              <Icon className="sidebar__itemIcon" size={20} />
              <span className="sidebar__itemLabel">{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Logout Button */}
      <div className="sidebar__footer">
        <button className="sidebar__item sidebar__item--logout" onClick={handleLogout}>
          <LogOut className="sidebar__itemIcon" size={20} />
          <span className="sidebar__itemLabel">Đăng xuất</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;

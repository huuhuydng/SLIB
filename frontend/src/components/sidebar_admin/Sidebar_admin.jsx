import React from "react";
import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Map,
  Users,
  Cpu,
  Settings,
  Activity,
  Sparkles,
  HelpCircle,
  Shield,
  LogOut,
  Film
} from "lucide-react";

import "../../styles/admin/sidebar_default.css";
import appLogo from "../../assets/logo.png";

const Sidebar = () => {
  const adminMenuItems = [
    { icon: LayoutDashboard, label: "Tổng quan", path: "/admin/dashboard" },
    { icon: Map, label: "Bản đồ thư viện", path: "/admin/library-map" },
    { icon: Users, label: "Quản lý người dùng", path: "/admin/users" },
    { icon: Cpu, label: "Quản lý thiết bị", path: "/admin/devices" },
    { icon: Film, label: "Quản lý Kiosk", path: "/admin/slideshow-management" },
  ];

  const systemMenuItems = [
    { icon: Settings, label: "Cấu hình hệ thống", path: "/admin/config" },
    { icon: Activity, label: "Sức khỏe hệ thống", path: "/admin/health" },
    { icon: Sparkles, label: "Cấu hình AI", path: "/admin/ai-config" },
  ];

  const handleLogout = () => {
    if (confirm('Bạn có chắc muốn đăng xuất?')) {
      localStorage.clear();
      sessionStorage.clear();
      window.location.reload(true);
    }
  };

  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <NavLink to="/admin/dashboard" className="sidebar__brandLink" aria-label="Slib">
          <img src={appLogo} alt="Slib" className="sidebar__brandLogo" />
        </NavLink>
      </div>
      <nav className="sidebar__nav">
        <div className="sidebar__section">
          <div className="sidebar__sectionLabel">Quản lý</div>
          {adminMenuItems.map((item, idx) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={idx}
                to={item.path}
                className={({ isActive }) =>
                  `sidebar__item ${isActive ? "sidebar__item--active" : ""}`
                }
              >
                <span className="sidebar__icon">
                  <Icon size={20} strokeWidth={1.8} />
                </span>
                <span className="sidebar__label">{item.label}</span>
              </NavLink>
            );
          })}
        </div>

        <div className="sidebar__section">
          <div className="sidebar__sectionLabel">Hệ thống</div>
          {systemMenuItems.map((item, idx) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={idx}
                to={item.path}
                className={({ isActive }) =>
                  `sidebar__item ${isActive ? "sidebar__item--active" : ""}`
                }
              >
                <span className="sidebar__icon">
                  <Icon size={20} strokeWidth={1.8} />
                </span>
                <span className="sidebar__label">{item.label}</span>
              </NavLink>
            );
          })}
        </div>
      </nav>

      <div className="sidebar__helpWrap">
        <div className="sidebar__helpItem" style={{ cursor: 'pointer' }}>
          <span className="sidebar__icon">
            <HelpCircle size={20} strokeWidth={1.8} />
          </span>
          <span className="sidebar__label">Trợ giúp & Hỗ trợ</span>
        </div>
        <div
          className="sidebar__helpItem sidebar__logoutItem"
          onClick={handleLogout}
          style={{ cursor: 'pointer' }}
        >
          <span className="sidebar__icon">
            <LogOut size={20} strokeWidth={1.8} />
          </span>
          <span className="sidebar__label">Đăng xuất</span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;

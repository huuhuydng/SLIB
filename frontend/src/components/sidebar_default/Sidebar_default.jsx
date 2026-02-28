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
  LogOut
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";

import logo from "../../assets/logonencam.png";
import "../../styles/admin/sidebar_default.css";

const SidebarAdmin = () => {
  const { user, logout } = useAuth();

  // Menu items dành riêng cho Admin
  const adminMenuItems = [
    { icon: LayoutDashboard, label: "Tổng quan", path: "/dashboard" },
    { icon: Map, label: "Sơ đồ thư viện", path: "/library-map" },
    { icon: Users, label: "Quản lý người dùng", path: "/users" },
    { icon: Cpu, label: "Quản lý thiết bị", path: "/devices" },
    { icon: Cpu, label: "Chatting", path: "/chat" },
  ];

  const systemMenuItems = [
    { icon: Settings, label: "Cấu hình hệ thống", path: "/config" },
    { icon: Activity, label: "Giám sát hệ thống", path: "/health" },
    { icon: Sparkles, label: "Cấu hình AI", path: "/ai-config" },
  ];

  const handleLogout = () => {
    if (confirm('Bạn có chắc muốn đăng xuất?')) {
      logout();
    }
  };

  return (
    <aside className="sidebar">
      {/* Brand / Logo */}
      <div className="sidebar__brand">
        <div className="sidebar__brandRow">
          <img src={logo} alt="Slib" className="sidebar__brandIcon" />
        </div>
      </div>

      {/* Admin Badge */}
      <div className="sidebar__badge">
        <Shield size={14} />
        <span className="sidebar__badgeText">Admin</span>
      </div>

      {/* Navigation */}
      <nav className="sidebar__nav">
        {/* Quản lý Section */}
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
                  <Icon size={22} strokeWidth={2} />
                </span>
                <span className="sidebar__label">{item.label}</span>
              </NavLink>
            );
          })}
        </div>

        {/* Hệ thống Section */}
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
                  <Icon size={22} strokeWidth={2} />
                </span>
                <span className="sidebar__label">{item.label}</span>
              </NavLink>
            );
          })}
        </div>
      </nav>

      {/* Footer - User & Logout */}
      <div className="sidebar__helpWrap">
        {user && (
          <div className="sidebar__userInfo" style={{
            padding: '12px 16px',
            borderBottom: '1px solid rgba(255,255,255,0.1)',
            marginBottom: '8px'
          }}>
            <div style={{ fontSize: '13px', color: '#fff', fontWeight: '600', marginBottom: '2px' }}>
              {user.full_name || user.email || 'Admin'}
            </div>
            <div style={{ fontSize: '11px', color: 'rgba(255,255,255,0.6)' }}>
              {user.email}
            </div>
          </div>
        )}
        <div className="sidebar__helpItem" style={{ cursor: 'pointer' }}>
          <span className="sidebar__icon">
            <HelpCircle size={22} strokeWidth={2} />
          </span>
          <span className="sidebar__label">Trợ giúp & Hỗ trợ</span>
        </div>
        <div
          className="sidebar__helpItem sidebar__logoutItem"
          onClick={handleLogout}
          style={{
            cursor: 'pointer',
            color: '#e8600a',
            marginTop: '4px'
          }}
        >
          <span className="sidebar__icon">
            <LogOut size={22} strokeWidth={2} />
          </span>
          <span className="sidebar__label">Đăng xuất</span>
        </div>
      </div>
    </aside>
  );
};

export default SidebarAdmin;

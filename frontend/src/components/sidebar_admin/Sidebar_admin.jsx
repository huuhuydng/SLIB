import React, { useState } from "react";
import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Map,
  Users,
  Router,
  Nfc,
  Monitor,
  Settings,
  Activity,
  Sparkles,
  HelpCircle,
  Shield,
  LogOut,
} from "lucide-react";

import "../../styles/admin/sidebar_default.css";
import appLogo from "../../assets/logo.png";

const Sidebar = () => {
  const [showLogoutModal, setShowLogoutModal] = useState(false);

  const adminMenuItems = [
    { icon: LayoutDashboard, label: "Tổng quan", path: "/admin/dashboard" },
    { icon: Map, label: "Bản đồ thư viện", path: "/admin/library-map" },
    { icon: Users, label: "Quản lý người dùng", path: "/admin/users" },
    { icon: Router, label: "Trạm quét HCE", path: "/admin/devices" },
    { icon: Nfc, label: "Quản lý NFC Tag", path: "/admin/nfc-management" },
    { icon: Monitor, label: "Quản lý Kiosk", path: "/admin/kiosk" },
  ];

  const systemMenuItems = [
    { icon: Settings, label: "Cấu hình hệ thống", path: "/admin/config" },
    { icon: Activity, label: "Sức khỏe hệ thống", path: "/admin/health" },
    { icon: Sparkles, label: "Cấu hình AI", path: "/admin/ai-config" },
  ];

  const confirmLogout = () => {
    localStorage.clear();
    sessionStorage.clear();
    window.location.href = '/login';
  };

  return (
    <>
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
            onClick={() => setShowLogoutModal(true)}
            style={{ cursor: 'pointer' }}
          >
            <span className="sidebar__icon">
              <LogOut size={20} strokeWidth={1.8} />
            </span>
            <span className="sidebar__label">Đăng xuất</span>
          </div>
        </div>
      </aside>

      {/* Logout confirmation modal */}
      {showLogoutModal && (
        <div
          style={{
            position: 'fixed', inset: 0, zIndex: 9999,
            background: 'rgba(0,0,0,0.4)', display: 'flex',
            alignItems: 'center', justifyContent: 'center',
          }}
          onClick={() => setShowLogoutModal(false)}
        >
          <div
            style={{
              background: '#fff', borderRadius: 12, padding: '28px 32px',
              minWidth: 340, maxWidth: 400, boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
              textAlign: 'center',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{
              width: 48, height: 48, borderRadius: '50%', background: '#FEE2E2',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 16px',
            }}>
              <LogOut size={22} color="#DC2626" />
            </div>
            <h3 style={{ fontSize: 16, fontWeight: 600, color: '#1A202C', marginBottom: 8 }}>
              Xác nhận đăng xuất
            </h3>
            <p style={{ fontSize: 14, color: '#718096', marginBottom: 24 }}>
              Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?
            </p>
            <div style={{ display: 'flex', gap: 12 }}>
              <button
                onClick={() => setShowLogoutModal(false)}
                style={{
                  flex: 1, padding: '10px 16px', borderRadius: 8,
                  border: '1px solid #E2E8F0', background: '#fff',
                  fontSize: 14, fontWeight: 500, color: '#4A5568', cursor: 'pointer',
                }}
              >
                Huỷ
              </button>
              <button
                onClick={confirmLogout}
                style={{
                  flex: 1, padding: '10px 16px', borderRadius: 8,
                  border: 'none', background: '#DC2626',
                  fontSize: 14, fontWeight: 500, color: '#fff', cursor: 'pointer',
                }}
              >
                Đăng xuất
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Sidebar;

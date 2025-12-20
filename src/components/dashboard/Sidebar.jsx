import React from "react";
import {
  LayoutDashboard,
  ArrowLeftRight,
  Flame,
  Armchair,
  Users,
  AlertTriangle,
  MessageSquare,
  BarChart2,
  Bell,
  HelpCircle,
} from "lucide-react";

import logo from "../../assets/logo.png";

const Sidebar = () => {
  const menuItems = [
    { icon: LayoutDashboard, label: "Tổng quan", active: true },
    { icon: ArrowLeftRight, label: "Kiểm tra ra/vào" },
    { icon: Flame, label: "Bản đồ nhiệt" },
    { icon: Armchair, label: "Quản lý chỗ ngồi" },
    { icon: Users, label: "Sinh viên" },
    { icon: AlertTriangle, label: "Vi phạm" },
    { icon: MessageSquare, label: "Trò chuyện" },
    { icon: BarChart2, label: "Thống kê" },
    { icon: Bell, label: "Thông báo" },
  ];

  return (
    <aside className="sidebar">
      {/* Brand */}
      <div className="sidebar__brand">
        <div className="sidebar__brandRow">
          <span className="sidebar__brandText">Slib</span>

          {/* ÉP size ngay tại JSX để chắc chắn không bao giờ phóng to */}
          <img
            src={logo}
            alt="Slib"
            className="sidebar__brandIcon"
            width={34}
            height={34}
            draggable={false}
          />
        </div>
      </div>

      {/* Menu */}
      <nav className="sidebar__nav">
        {menuItems.map((item, idx) => {
          const Icon = item.icon;
          return (
            <button
              key={idx}
              type="button"
              className={`sidebar__item ${item.active ? "sidebar__item--active" : ""}`}
            >
              <Icon size={18} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Help */}
      <div className="sidebar__helpWrap">
        <button className="sidebar__helpBtn" type="button" aria-label="Help">
          <HelpCircle size={18} />
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;

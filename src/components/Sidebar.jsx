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

import logo from "../assets/logo.png";

const Sidebar = ({ currentPage, onPageChange }) => {
  const menuItems = [
    { icon: LayoutDashboard, label: "Tổng quan", page: "dashboard" },
    { icon: ArrowLeftRight, label: "Kiểm tra ra/vào", page: "checkinout" },
    { icon: Flame, label: "Sơ đồ thư viện", page: "heatmap" },
    { icon: Armchair, label: "Quản lý chỗ ngồi", page: "seatmanage" },
    { icon: Users, label: "Sinh viên", page: "students" },
    { icon: AlertTriangle, label: "Vi phạm", page: "violation" },
    { icon: MessageSquare, label: "Trò chuyện", page: "chat" },
    { icon: BarChart2, label: "Thống kê", page: "statistic" },
    { icon: Bell, label: "Thông báo", page: "notification" },
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
              className={`sidebar__item ${currentPage === item.page ? "sidebar__item--active" : ""}`}
              onClick={() => onPageChange(item.page)}
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
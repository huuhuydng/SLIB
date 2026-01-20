import React from "react";
import { NavLink } from "react-router-dom";
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

import logo from "../../assets/logonencam.png";
import "../../styles/librarian/sidebar_default.css";

const Sidebar = () => {
  const menuItems = [
    { icon: LayoutDashboard, label: "Tổng quan", path: "/dashboard" },
    { icon: ArrowLeftRight, label: "Kiểm tra ra/vào", path: "/checkinout" },
    { icon: Flame, label: "Sơ đồ thư viện", path: "/areas" },
    { icon: Armchair, label: "Quản lý chỗ ngồi", path: "/seatmanage" },
    { icon: Users, label: "Sinh viên", path: "/students" },
    { icon: AlertTriangle, label: "Vi phạm", path: "/violation" },
    { icon: MessageSquare, label: "Trò chuyện", path: "/chat" },
    { icon: BarChart2, label: "Thống kê", path: "/statistic" },
    { icon: Bell, label: "Thông báo", path: "/notification" },
  ];

  return (
    <aside className="sidebar">
      {/* Brand / Logo */}
      <div className="sidebar__brand">
        {/* Class này giúp canh chỉnh logo đẹp hơn */}
        <div className="sidebar__brandRow"> 
          <img src={logo} alt="Slib" className="sidebar__brandIcon" />
        </div>
      </div>

      {/* Navigation */}
      <nav className="sidebar__nav">
        {menuItems.map((item, idx) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={idx}
              to={item.path}
              className={({ isActive }) =>
                `sidebar__item ${isActive ? "sidebar__item--active" : ""}`
              }
            >
              {/* Tăng độ dày icon một chút cho rõ nét */}
              <Icon size={20} strokeWidth={2} />
              <span className="sidebar__label">{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      {/* Footer / Help */}
      <div className="sidebar__helpWrap">
        {/* Giả lập Help như một button để có hover effect và label giống menu trên */}
        <div className="sidebar__helpItem">
           <HelpCircle size={20} strokeWidth={2} />
           <span className="sidebar__label">Trợ giúp & Hỗ trợ</span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
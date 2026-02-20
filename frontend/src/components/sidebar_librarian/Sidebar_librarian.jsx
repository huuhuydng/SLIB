import React, { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
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
  Newspaper,
  LifeBuoy,
  ChevronDown,
  Monitor,
  ClipboardList,
  ShieldAlert,
  FileText,
  CalendarCheck,
  MessageCircle,
  Star,
} from "lucide-react";

import logo from "../../assets/logonencam.png";
import "../../styles/librarian/sidebar_default.css";

const Sidebar = () => {
  const location = useLocation();
  const [expandedGroups, setExpandedGroups] = useState({});

  const toggleGroup = (groupId) => {
    setExpandedGroups((prev) => ({
      ...prev,
      [groupId]: !prev[groupId],
    }));
  };

  // Kiểm tra có child nào active không
  const isGroupActive = (children) => {
    return children?.some((child) => location.pathname === child.path);
  };

  const menuStructure = [
    {
      id: "dashboard",
      icon: LayoutDashboard,
      label: "Tổng quan",
      path: "/librarian/dashboard",
    },
    {
      id: "monitoring",
      icon: Monitor,
      label: "Giám sát",
      children: [
        {
          icon: ArrowLeftRight,
          label: "Kiểm tra ra/vào",
          path: "/librarian/checkinout",
        },
        {
          icon: Flame,
          label: "Sơ đồ thư viện",
          path: "/librarian/areas",
        },
        {
          icon: Armchair,
          label: "Quản lý chỗ ngồi",
          path: "/librarian/seatmanage",
        },
      ],
    },
    {
      id: "management",
      icon: ClipboardList,
      label: "Quản lý",
      children: [
        {
          icon: CalendarCheck,
          label: "Đặt chỗ",
          path: "/librarian/bookings",
        },
        {
          icon: Users,
          label: "Sinh viên",
          path: "/librarian/students",
        },
      ],
    },
    {
      id: "handling",
      icon: ShieldAlert,
      label: "Xử lý",
      children: [
        {
          icon: AlertTriangle,
          label: "Vi phạm",
          path: "/librarian/violation",
        },
        {
          icon: LifeBuoy,
          label: "Yêu cầu hỗ trợ",
          path: "/librarian/support-requests",
        },
        {
          icon: MessageCircle,
          label: "Khiếu nại",
          path: "/librarian/complaints",
        },
        {
          icon: Star,
          label: "Phản hồi",
          path: "/librarian/feedback",
        },
      ],
    },
    {
      id: "content",
      icon: FileText,
      label: "Nội dung",
      children: [
        {
          icon: Newspaper,
          label: "Tin tức",
          path: "/librarian/news",
        },
        {
          icon: Bell,
          label: "Thông báo",
          path: "/librarian/notification",
        },
      ],
    },
    {
      id: "chat",
      icon: MessageSquare,
      label: "Trò chuyện",
      path: "/librarian/chat",
    },
    {
      id: "statistic",
      icon: BarChart2,
      label: "Thống kê",
      path: "/librarian/statistic",
    },
  ];

  // Auto-expand group nếu child active
  React.useEffect(() => {
    const initialExpanded = {};
    menuStructure.forEach((item) => {
      if (item.children && isGroupActive(item.children)) {
        initialExpanded[item.id] = true;
      }
    });
    setExpandedGroups((prev) => ({ ...prev, ...initialExpanded }));
  }, [location.pathname]);

  return (
    <aside className="sidebar">
      {/* Brand / Logo */}
      <div className="sidebar__brand">
        <div className="sidebar__brandRow">
          <img src={logo} alt="Slib" className="sidebar__brandIcon" />
        </div>
      </div>

      {/* Navigation */}
      <nav className="sidebar__nav">
        {menuStructure.map((item) => {
          const Icon = item.icon;

          // Mục đơn (không có con)
          if (!item.children) {
            return (
              <NavLink
                key={item.id}
                to={item.path}
                className={({ isActive }) =>
                  `sidebar__item ${isActive ? "sidebar__item--active" : ""}`
                }
              >
                <Icon size={20} strokeWidth={2} />
                <span className="sidebar__label">{item.label}</span>
              </NavLink>
            );
          }

          // Mục nhóm (có con)
          const isExpanded = expandedGroups[item.id];
          const groupActive = isGroupActive(item.children);

          return (
            <div key={item.id} className="sidebar__group">
              <button
                className={`sidebar__item sidebar__groupBtn ${groupActive ? "sidebar__item--groupActive" : ""
                  }`}
                onClick={() => toggleGroup(item.id)}
              >
                <Icon size={20} strokeWidth={2} />
                <span className="sidebar__label">{item.label}</span>
                <ChevronDown
                  size={14}
                  className={`sidebar__chevron ${isExpanded ? "sidebar__chevron--open" : ""
                    }`}
                />
              </button>

              <div
                className={`sidebar__submenu ${isExpanded ? "sidebar__submenu--open" : ""
                  }`}
              >
                {item.children.map((child, idx) => {
                  const ChildIcon = child.icon;
                  return (
                    <NavLink
                      key={idx}
                      to={child.path}
                      className={({ isActive }) =>
                        `sidebar__subitem ${isActive ? "sidebar__subitem--active" : ""
                        }`
                      }
                    >
                      <ChildIcon size={16} strokeWidth={2} />
                      <span className="sidebar__label">{child.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            </div>
          );
        })}
      </nav>

      {/* Footer / Help */}
      <div className="sidebar__helpWrap">
        <div className="sidebar__helpItem">
          <HelpCircle size={20} strokeWidth={2} />
          <span className="sidebar__label">Trợ giúp & Hỗ trợ</span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
import React, { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  ArrowLeftRight,
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
  Settings,
  LogOut,
} from "lucide-react";
import { useLibrarianNotification } from "../../context/LibrarianNotificationContext";

import "../../styles/librarian/sidebar_default.css";
import "../../styles/librarian/LibrarianNotification.css";
import appLogo from "../../assets/logo.png";

const Sidebar = () => {
  const location = useLocation();

  const [expandedGroups, setExpandedGroups] = useState({});
  const { pendingCounts, unreadChatCount } = useLibrarianNotification();

  const handleLogout = () => {
    localStorage.removeItem('librarian_token');
    localStorage.removeItem('librarian_user');
    localStorage.removeItem('refresh_token');
    sessionStorage.removeItem('librarian_token');
    sessionStorage.removeItem('librarian_user');
    sessionStorage.removeItem('refresh_token');
    window.location.href = '/login';
  };

  const toggleGroup = (groupId) => {
    setExpandedGroups((prev) => ({
      ...prev,
      [groupId]: !prev[groupId],
    }));
  };

  // Kiem tra co child nao active khong
  const isGroupActive = (children) => {
    return children?.some((child) => location.pathname === child.path);
  };

  // Mapping path -> badge count
  const badgeMap = {
    "/librarian/support-requests": pendingCounts.supportRequests,
    "/librarian/complaints": pendingCounts.complaints,
    "/librarian/feedback": pendingCounts.feedbacks,
    "/librarian/seat-status-reports": pendingCounts.seatStatusReports,
    "/librarian/chat": unreadChatCount || pendingCounts.chats,
    "/librarian/violation": pendingCounts.violations,
  };

  // Tinh tong badge cho group
  const getGroupBadge = (children) => {
    if (!children) return 0;
    return children.reduce((sum, child) => sum + (badgeMap[child.path] || 0), 0);
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
          icon: FileText,
          label: "Tình trạng ghế",
          path: "/librarian/seat-status-reports",
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
          icon: Monitor,
          label: "Quản lý Kiosk",
          path: "/librarian/slideshow-management",
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

  // Auto-expand group neu child active
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
      <div className="sidebar__brand">
        <NavLink to="/librarian/dashboard" className="sidebar__brandLink" aria-label="Slib">
          <img src={appLogo} alt="Slib" className="sidebar__brandLogo" />
        </NavLink>
      </div>
      <nav className="sidebar__nav">
        {menuStructure.map((item) => {
          const Icon = item.icon;

          // Muc don (khong co con)
          if (!item.children) {
            const badge = badgeMap[item.path] || 0;
            return (
              <NavLink
                key={item.id}
                to={item.path}
                className={({ isActive }) =>
                  `sidebar__item ${isActive ? "sidebar__item--active" : ""}`
                }
              >
                <span className="sidebar__iconWrap">
                  <Icon size={20} strokeWidth={2} />
                  {badge > 0 && <span className="sidebar__badge">{badge}</span>}
                </span>
                <span className="sidebar__label">{item.label}</span>
              </NavLink>
            );
          }

          // Muc nhom (co con)
          const isExpanded = expandedGroups[item.id];
          const groupActive = isGroupActive(item.children);
          const groupBadge = getGroupBadge(item.children);

          return (
            <div key={item.id} className="sidebar__group">
              <button
                className={`sidebar__item sidebar__groupBtn ${groupActive ? "sidebar__item--groupActive" : ""
                  }`}
                onClick={() => toggleGroup(item.id)}
              >
                <span className="sidebar__iconWrap">
                  <Icon size={20} strokeWidth={2} />
                  {groupBadge > 0 && <span className="sidebar__badge">{groupBadge}</span>}
                </span>
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
                  const childBadge = badgeMap[child.path] || 0;
                  return (
                    <NavLink
                      key={idx}
                      to={child.path}
                      className={({ isActive }) =>
                        `sidebar__subitem ${isActive ? "sidebar__subitem--active" : ""
                        }`
                      }
                    >
                      <span className="sidebar__iconWrap">
                        <ChildIcon size={16} strokeWidth={2} />
                        {childBadge > 0 && <span className="sidebar__badge">{childBadge}</span>}
                      </span>
                      <span className="sidebar__label">{child.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            </div>
          );
        })}
      </nav>

      {/* Footer - Cài đặt + Đăng xuất */}
      <div className="sidebar__helpWrap">
        <NavLink
          to="/librarian/settings"
          className={({ isActive }) =>
            `sidebar__helpItem ${isActive ? "sidebar__helpItem--active" : ""}`
          }
        >
          <Settings size={20} strokeWidth={2} />
          <span className="sidebar__label">Cài đặt</span>
        </NavLink>
        <button className="sidebar__helpItem sidebar__logoutBtn" onClick={handleLogout}>
          <LogOut size={20} strokeWidth={2} />
          <span className="sidebar__label">Đăng xuất</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;

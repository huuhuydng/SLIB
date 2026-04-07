import { Outlet, useNavigate } from "react-router-dom";
import { useState, useRef, useEffect } from "react";
import {
  Bell,
  LifeBuoy,
  MessageCircle,
  Star,
  ClipboardCheck,
  MessageSquare,
  AlertTriangle,
  CheckCircle2,
  X,
} from "lucide-react";
import Sidebar from "../../components/sidebar_librarian/Sidebar_librarian";
import {
  LibrarianNotificationProvider,
  useLibrarianNotification,
} from "../../context/LibrarianNotificationContext";
import { useToast } from "../../components/common/ToastProvider";
import "../../styles/librarian/MainLayout.css";
import "../../styles/librarian/LibrarianNotification.css";

const PENDING_ICON_MAP = {
  SUPPORT_REQUEST: { icon: LifeBuoy, cls: "support", label: "Yêu cầu hỗ trợ" },
  COMPLAINT: { icon: MessageCircle, cls: "complaint", label: "Khiếu nại" },
  FEEDBACK: { icon: Star, cls: "feedback", label: "Phản hồi" },
  SEAT_STATUS_REPORT: { icon: ClipboardCheck, cls: "seat-status", label: "Tình trạng ghế" },
  CHAT: { icon: MessageSquare, cls: "chat", label: "Trò chuyện" },
  VIOLATION: { icon: AlertTriangle, cls: "violation", label: "Vi phạm" },
};

const STORED_NOTIF_ICON_MAP = {
  SUPPORT_REQUEST: { icon: LifeBuoy, cls: "support", label: "Yêu cầu hỗ trợ" },
  COMPLAINT: { icon: MessageCircle, cls: "complaint", label: "Khiếu nại" },
  SEAT_STATUS_REPORT: { icon: ClipboardCheck, cls: "seat-status", label: "Tình trạng ghế" },
  VIOLATION_REPORT: { icon: AlertTriangle, cls: "violation", label: "Vi phạm" },
  VIOLATION: { icon: AlertTriangle, cls: "violation", label: "Vi phạm" },
  CHAT_MESSAGE: { icon: MessageSquare, cls: "chat", label: "Tin nhắn" },
  REPUTATION: { icon: Star, cls: "feedback", label: "Điểm uy tín" },
  BOOKING: { icon: ClipboardCheck, cls: "seat-status", label: "Đặt chỗ" },
  REMINDER: { icon: ClipboardCheck, cls: "seat-status", label: "Nhắc nhở" },
  NEWS: { icon: Bell, cls: "feedback", label: "Tin tức" },
  SYSTEM: { icon: Bell, cls: "feedback", label: "Hệ thống" },
};

const ACTION_LABELS = {
  CREATED: "mới",
  ESCALATED: "mới",
  STATUS_CHANGED: "đã xử lý",
  RESPONDED: "đã phản hồi",
  ACCEPTED: "đã chấp nhận",
  DENIED: "đã từ chối",
  REVIEWED: "đã xem",
  TAKEN_OVER: "đã tiếp nhận",
  RESOLVED: "đã kết thúc",
  STUDENT_RESOLVED: "sinh viên ngắt kết nối",
  CANCELLED: "đã huỷ",
  VERIFIED: "đã xác minh",
  REJECTED: "đã từ chối",
};

// Map category -> route điều hướng (mặc định vào tab chờ xử lý)
const NOTIF_ROUTE_MAP = {
  SUPPORT_REQUEST: "/librarian/support-requests?tab=PENDING",
  COMPLAINT: "/librarian/complaints?tab=PENDING",
  FEEDBACK: "/librarian/feedback?tab=NEW",
  SEAT_STATUS_REPORT: "/librarian/seat-status-reports?status=PENDING",
  CHAT: "/librarian/chat",
  VIOLATION: "/librarian/violation?tab=PENDING",
};

// Map countKey từ API response
function HeaderBar() {
  const {
    userNotifications,
    unreadNotificationCount,
    markNotificationAsRead,
    deleteNotification,
    unreadChatCount,
  } = useLibrarianNotification();
  const [showDropdown, setShowDropdown] = useState(false);
  const bellRef = useRef(null);
  const navigate = useNavigate();

  const unreadMessageCount = unreadChatCount || 0;
  const bellTotal = (unreadNotificationCount || 0) + unreadMessageCount;

  // Dong dropdown khi click ra ngoai
  useEffect(() => {
    if (!showDropdown) return;
    const handler = (e) => {
      if (bellRef.current && !bellRef.current.contains(e.target)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [showDropdown]);

  const getNotificationRoute = (notification) => {
    const type = notification.notificationType;
    const referenceType = notification.referenceType;
    const key = referenceType || type;

    switch (key) {
      case "SUPPORT_REQUEST":
        return "/librarian/support-requests";
      case "COMPLAINT":
        return "/librarian/complaints";
      case "SEAT_STATUS_REPORT":
        return "/librarian/seat-status-reports";
      case "VIOLATION_REPORT":
      case "VIOLATION":
        return "/librarian/violation";
      case "NEWS":
        return "/librarian/notification";
      case "CHAT_MESSAGE":
        return "/librarian/chat";
      default:
        return "/librarian/dashboard";
    }
  };

  const handleStoredNotificationClick = async (notification) => {
    if (!notification?.isRead) {
      await markNotificationAsRead(notification.id);
    }
    setShowDropdown(false);
    navigate(getNotificationRoute(notification));
  };

  const handleDeleteNotification = async (event, notificationId) => {
    event.stopPropagation();
    await deleteNotification(notificationId);
  };

  const [userData] = useState(() => {
    try {
      const s = localStorage.getItem('librarian_user') || sessionStorage.getItem('librarian_user');
      if (s) { const u = JSON.parse(s); return { name: u.fullName || u.email?.split('@')[0] || 'Thủ thư', role: u.role || 'LIBRARIAN' }; }
    } catch {
      return { name: 'Thủ thư', role: 'LIBRARIAN' };
    }
    return { name: 'Thủ thư', role: 'LIBRARIAN' };
  });

  const getInitials = (n) => n ? n.split(' ').map(w => w[0]).join('').toUpperCase().substring(0, 2) : 'TT';

  return (
    <header className="top-header">
      <div className="top-header__left">
      </div>
      <div className="top-header__right">
        <div ref={bellRef} style={{ position: "relative" }}>
          <button
            className="notif-bell"
            onClick={() => {
              setShowDropdown(!showDropdown);
            }}
            title="Thông báo"
          >
            <Bell className="notif-bell__icon" />
            {bellTotal > 0 && (
              <span className="notif-bell__badge">
                {bellTotal > 99 ? "99+" : bellTotal}
              </span>
            )}
          </button>

          {showDropdown && (
            <div className="notif-dropdown">
              <div className="notif-dropdown__header">
                <h3 className="notif-dropdown__title">Thông báo</h3>
                <span className="notif-dropdown__count">
                  {bellTotal} chưa đọc
                </span>
              </div>

              <div className="notif-dropdown__list">
                {userNotifications.map((notification) => {
                  const config = STORED_NOTIF_ICON_MAP[notification.notificationType]
                    || STORED_NOTIF_ICON_MAP[notification.referenceType]
                    || STORED_NOTIF_ICON_MAP.SYSTEM;
                  const Icon = config.icon;

                  return (
                    <div
                      key={notification.id}
                      className="notif-item notif-item--clickable"
                      onClick={() => handleStoredNotificationClick(notification)}
                    >
                      <div className={`notif-item__icon notif-item__icon--${config.cls}`}>
                        <Icon size={18} />
                      </div>
                      <div className="notif-item__body">
                        <p className="notif-item__title">
                          {notification.title}
                          {!notification.isRead && (
                            <span style={{ marginLeft: 8, color: "#f97316", fontSize: 12, fontWeight: 700 }}>
                              Mới
                            </span>
                          )}
                        </p>
                        <p className="notif-item__desc">
                          {notification.content?.length > 90
                            ? `${notification.content.substring(0, 90)}...`
                            : notification.content}
                        </p>
                      </div>
                      <div className="notif-item__meta">
                        <span className="notif-detail-item__time">
                          {getTimeAgo(notification.createdAt)}
                        </span>
                        <button
                          type="button"
                          className="notif-item__delete"
                          title="Xoá thông báo"
                          aria-label="Xoá thông báo"
                          onClick={(event) => handleDeleteNotification(event, notification.id)}
                        >
                          <X size={14} />
                        </button>
                      </div>
                    </div>
                  );
                })}

                {userNotifications.length === 0 && (
                  <div className="notif-dropdown__empty">
                    <CheckCircle2 size={40} />
                    <p>Chưa có thông báo nào</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
        <div className="top-header__user">
          <div className="top-header__avatar">{getInitials(userData.name)}</div>
          <span className="top-header__name">{userData.name}</span>
        </div>
      </div>
    </header>
  );
}

function getTimeAgo(timestamp) {
  if (!timestamp) return "";
  const diff = Date.now() - new Date(timestamp).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "Vừa xong";
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.floor(hours / 24)} ngày trước`;
}

// Mô tả chi tiết cho từng loại action
const TOAST_DETAIL_MAP = {
  SUPPORT_REQUEST: {
    CREATED: { title: "Yêu cầu hỗ trợ mới", desc: "Có sinh viên vừa gửi yêu cầu hỗ trợ mới, cần được xử lý." },
    ESCALATED: { title: "Yêu cầu hỗ trợ cần xử lý", desc: "Yêu cầu hỗ trợ đã được chuyển tiếp, cần xem xét ngay." },
    STATUS_CHANGED: { title: "Yêu cầu hỗ trợ đã cập nhật", desc: "Trạng thái yêu cầu hỗ trợ đã thay đổi." },
  },
  COMPLAINT: {
    CREATED: { title: "Khiếu nại mới", desc: "Có sinh viên vừa gửi khiếu nại mới, cần xem xét và phản hồi." },
    STATUS_CHANGED: { title: "Khiếu nại đã cập nhật", desc: "Trạng thái khiếu nại đã thay đổi." },
  },
  FEEDBACK: {
    CREATED: { title: "Phản hồi mới", desc: "Sinh viên vừa gửi đánh giá/phản hồi mới về thư viện." },
    STATUS_CHANGED: { title: "Phản hồi đã cập nhật", desc: "Trạng thái phản hồi đã thay đổi." },
  },
  SEAT_STATUS_REPORT: {
    CREATED: { title: "Báo cáo tình trạng ghế mới", desc: "Có báo cáo mới về ghế hỏng, bẩn hoặc thiếu thiết bị cần được kiểm tra." },
    VERIFIED: { title: "Báo cáo ghế đã xác minh", desc: "Một báo cáo tình trạng ghế đã được xác minh và chờ xử lý hoàn tất." },
    REJECTED: { title: "Báo cáo ghế bị từ chối", desc: "Một báo cáo tình trạng ghế đã được từ chối sau khi kiểm tra." },
    RESOLVED: { title: "Báo cáo ghế đã xử lý", desc: "Một báo cáo tình trạng ghế đã được xử lý xong." },
    STATUS_CHANGED: { title: "Tình trạng ghế đã cập nhật", desc: "Trạng thái báo cáo tình trạng ghế vừa được cập nhật." },
  },
  CHAT: {
    CREATED: { title: "Tin nhắn mới", desc: "Có sinh viên đang chờ được hỗ trợ qua trò chuyện." },
    ESCALATED: { title: "Yêu cầu trò chuyện", desc: "Sinh viên yêu cầu nói chuyện trực tiếp với thủ thư." },
    STUDENT_RESOLVED: { title: "Sinh viên đã ngắt kết nối", desc: "Sinh viên đã tự kết thúc cuộc trò chuyện." },
    TAKEN_OVER: { title: "Đã tiếp nhận trò chuyện", desc: "Thủ thư đã tiếp nhận cuộc trò chuyện với sinh viên." },
    RESOLVED: { title: "Trò chuyện đã kết thúc", desc: "Cuộc trò chuyện đã được kết thúc." },
    CANCELLED: { title: "Trò chuyện đã huỷ", desc: "Cuộc trò chuyện đã bị huỷ." },
  },
  VIOLATION: {
    CREATED: { title: "Báo cáo vi phạm mới", desc: "Có báo cáo vi phạm mới cần được xác minh và xử lý." },
    STATUS_CHANGED: { title: "Vi phạm đã cập nhật", desc: "Trạng thái báo cáo vi phạm đã thay đổi." },
  },
};

const TOAST_SUPPRESSION_RULES = [
  {
    source: 'SUPPORT_REQUEST',
    routeIncludes: '/librarian/support-requests',
    actions: ['STATUS_CHANGED', 'RESPONDED', 'DELETED'],
  },
  {
    source: 'COMPLAINT',
    routeIncludes: '/librarian/complaints',
    actions: ['ACCEPTED', 'DENIED', 'DELETED'],
  },
  {
    source: 'FEEDBACK',
    routeIncludes: '/librarian/feedback',
    actions: ['REVIEWED', 'DELETED'],
  },
  {
    source: 'SEAT_STATUS_REPORT',
    routeIncludes: '/librarian/seat-status-reports',
    actions: ['VERIFIED', 'REJECTED', 'RESOLVED', 'STATUS_CHANGED', 'DELETED'],
  },
  {
    source: 'VIOLATION',
    routeIncludes: '/librarian/violation',
    actions: ['VERIFIED', 'REJECTED', 'STATUS_CHANGED', 'DELETED'],
  },
  {
    source: 'CHAT',
    routeIncludes: '/librarian/chat',
    actions: ['TAKEN_OVER', 'RESOLVED', 'STUDENT_RESOLVED', 'CANCELLED'],
  },
];

function shouldSuppressRealtimeToast(source, action, pathname) {
  return TOAST_SUPPRESSION_RULES.some(
    (rule) =>
      rule.source === source &&
      pathname.includes(rule.routeIncludes) &&
      rule.actions.includes(action)
  );
}

// Toast notification component — hiển thị ở góc phải trên khi có notification mới
function ToastNotifications() {
  const { notifications } = useLibrarianNotification();
  const prevCountRef = useRef(0);
  const hasMountedRef = useRef(false);
  const toast = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    if (!hasMountedRef.current) {
      hasMountedRef.current = true;
      prevCountRef.current = notifications.length;
      return;
    }

    if (notifications.length > prevCountRef.current) {
      const enabled = localStorage.getItem('slib_notifications_enabled');
      if (enabled === 'false') {
        prevCountRef.current = notifications.length;
        return;
      }

      const newNotif = notifications[0];
      const currentPath = window.location.pathname;
      const suppressOnCurrentPage = shouldSuppressRealtimeToast(
        newNotif.source,
        newNotif.action,
        currentPath
      );

      if (suppressOnCurrentPage) {
        prevCountRef.current = notifications.length;
        return;
      }

      const config = PENDING_ICON_MAP[newNotif.source] || PENDING_ICON_MAP.SUPPORT_REQUEST;
      const route = NOTIF_ROUTE_MAP[newNotif.source] || '/librarian/dashboard';

      const detailMap = TOAST_DETAIL_MAP[newNotif.source] || {};
      const detail = detailMap[newNotif.action] || {
        title: `${config.label} - ${ACTION_LABELS[newNotif.action] || newNotif.action}`,
        desc: `Có hoạt động mới liên quan đến ${config.label.toLowerCase()}.`,
      };

      toast.info(detail.desc, {
        title: detail.title,
        actionText: 'Xem chi tiết',
        onClick: () => navigate(route),
      });
    }
    prevCountRef.current = notifications.length;
  }, [notifications]);

  return null;
}

// Chat toast notification — hiện khi có tin nhắn chat mới từ student
// Suppress khi đang ở trang chat
function ChatToastNotification() {
  const { chatToast, setChatToast } = useLibrarianNotification();
  const toast = useToast();
  const navigate = useNavigate();
  const shownRef = useRef(null);

  useEffect(() => {
    if (!chatToast) {
      shownRef.current = null;
      return;
    }

    const isOnChatPage = window.location.pathname.includes('/librarian/chat');
    if (isOnChatPage) {
      setChatToast(null);
      return;
    }

    const key = `${chatToast.conversationId}-${chatToast.content}`;
    if (shownRef.current === key) return;
    shownRef.current = key;

    const convId = chatToast.conversationId;
    toast.info(`${chatToast.senderName}: ${chatToast.content}`, {
      title: 'Tin nhắn mới',
      actionText: 'Xem tin nhắn',
      onClick: () => navigate(`/librarian/chat?conversationId=${convId}`),
    });
    setChatToast(null);
  }, [chatToast]);

  return null;
}

function MainLayout() {
  return (
    <LibrarianNotificationProvider>
      <HeaderBar />
      <div className="appLayout">
        <Sidebar />
        <div className="main">
          <Outlet />
        </div>
        <ToastNotifications />
        <ChatToastNotification />
      </div>
    </LibrarianNotificationProvider>
  );
}

export default MainLayout;

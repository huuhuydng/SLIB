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
  ChevronDown,
  X,
} from "lucide-react";
import Sidebar from "../../components/sidebar_librarian/Sidebar_librarian";
import {
  LibrarianNotificationProvider,
  useLibrarianNotification,
} from "../../contexts/LibrarianNotificationContext";
import "../../styles/librarian/MainLayout.css";
import "../../styles/librarian/LibrarianNotification.css";

const NOTIF_ICON_MAP = {
  SUPPORT_REQUEST: { icon: LifeBuoy, cls: "support", label: "Yêu cầu hỗ trợ" },
  COMPLAINT: { icon: MessageCircle, cls: "complaint", label: "Khiếu nại" },
  FEEDBACK: { icon: Star, cls: "feedback", label: "Phản hồi" },
  SEAT_STATUS_REPORT: { icon: ClipboardCheck, cls: "seat-status", label: "Tình trạng ghế" },
  CHAT: { icon: MessageSquare, cls: "chat", label: "Trò chuyện" },
  VIOLATION: { icon: AlertTriangle, cls: "violation", label: "Vi phạm" },
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
const COUNT_KEY_MAP = {
  SUPPORT_REQUEST: "supportRequests",
  COMPLAINT: "complaints",
  FEEDBACK: "feedbacks",
  SEAT_STATUS_REPORT: "seatStatusReports",
  CHAT: "chats",
  VIOLATION: "violations",
};

// Lấy thông tin hiển thị từ item theo category
function getItemDisplayInfo(category, item) {
  switch (category) {
    case "SUPPORT_REQUEST":
      return {
        name: item.studentName || "Sinh viên",
        desc: item.description ? item.description.substring(0, 60) + (item.description.length > 60 ? "..." : "") : "Yêu cầu hỗ trợ mới",
        time: item.createdAt,
        route: NOTIF_ROUTE_MAP.SUPPORT_REQUEST,
      };
    case "COMPLAINT":
      return {
        name: item.studentName || "Sinh viên",
        desc: item.subject || item.content?.substring(0, 60) || "Khiếu nại mới",
        time: item.createdAt,
        route: NOTIF_ROUTE_MAP.COMPLAINT,
      };
    case "FEEDBACK":
      return {
        name: item.studentName || "Sinh viên",
        desc: item.content ? item.content.substring(0, 60) + (item.content.length > 60 ? "..." : "") : `Đánh giá ${item.rating || ""} sao`,
        time: item.createdAt,
        route: NOTIF_ROUTE_MAP.FEEDBACK,
      };
    case "SEAT_STATUS_REPORT":
      return {
        name: item.reporterName || "Sinh viên",
        desc: item.issueTypeLabel || item.description?.substring(0, 60) || "Báo cáo tình trạng ghế",
        time: item.createdAt,
        route: NOTIF_ROUTE_MAP.SEAT_STATUS_REPORT,
      };
    case "CHAT":
      return {
        name: item.studentName || "Sinh viên",
        desc: item.escalationReason || "Đang chờ hỗ trợ",
        time: item.escalatedAt || item.createdAt,
        route: `${NOTIF_ROUTE_MAP.CHAT}?conversationId=${item.id}`,
      };
    case "VIOLATION":
      return {
        name: item.reporterName || "Sinh viên",
        desc: item.violationTypeLabel || item.description?.substring(0, 60) || "Báo cáo vi phạm",
        time: item.createdAt,
        route: NOTIF_ROUTE_MAP.VIOLATION,
      };
    default:
      return { name: "---", desc: "---", time: null, route: "/" };
  }
}

function HeaderBar() {
  const { pendingCounts, notifications, pendingItems, fetchPendingItems, chatMessages, unreadChatCount } = useLibrarianNotification();
  const [showDropdown, setShowDropdown] = useState(false);
  const [expandedCategory, setExpandedCategory] = useState(null);
  const [loadingCategory, setLoadingCategory] = useState(null);
  const bellRef = useRef(null);
  const navigate = useNavigate();

  // Replace chats count in total with unreadChatCount to avoid double-counting
  const chatBadge = unreadChatCount || pendingCounts.chats || 0;
  const bellTotal = (pendingCounts.total - (pendingCounts.chats || 0)) + chatBadge;

  // Dong dropdown khi click ra ngoai
  useEffect(() => {
    if (!showDropdown) return;
    const handler = (e) => {
      if (bellRef.current && !bellRef.current.contains(e.target)) {
        setShowDropdown(false);
        setExpandedCategory(null);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [showDropdown]);

  // Handle click vào category row
  const handleCategoryClick = async (key) => {
    if (expandedCategory === key) {
      setExpandedCategory(null);
      return;
    }
    setExpandedCategory(key);
    // Fetch items nếu chưa có cache
    if (!pendingItems[key]) {
      setLoadingCategory(key);
      await fetchPendingItems(key);
      setLoadingCategory(null);
    }
  };

  // Handle click vào item chi tiết -> navigate
  const handleItemClick = (category, item) => {
    const info = getItemDisplayInfo(category, item);
    setShowDropdown(false);
    setExpandedCategory(null);
    navigate(info.route);
  };

  const [userData] = useState(() => {
    try {
      const s = localStorage.getItem('librarian_user') || sessionStorage.getItem('librarian_user');
      if (s) { const u = JSON.parse(s); return { name: u.fullName || u.email?.split('@')[0] || 'Thủ thư', role: u.role || 'LIBRARIAN' }; }
    } catch { }
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
              if (showDropdown) setExpandedCategory(null);
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
                  {bellTotal} cần xử lý
                </span>
              </div>

              <div className="notif-dropdown__list">
                {/* Tin nhắn mới từ sinh viên */}
                {(unreadChatCount > 0 || chatMessages.length > 0) && (
                  <div className="notif-category">
                    <div
                      className="notif-item notif-item--expandable notif-item--clickable"
                      onClick={() => {
                        setShowDropdown(false);
                        setExpandedCategory(null);
                        navigate('/librarian/chat');
                      }}
                    >
                      <div className="notif-item__icon notif-item__icon--chat">
                        <MessageSquare size={18} />
                      </div>
                      <div className="notif-item__body">
                        <p className="notif-item__title">Tin nhắn</p>
                        <p className="notif-item__desc">{unreadChatCount || chatMessages.length} tin nhắn mới</p>
                      </div>
                      {(unreadChatCount > 0 || chatMessages.length > 0) && (
                        <span className="notif-item__badge">{unreadChatCount || chatMessages.length}</span>
                      )}
                    </div>
                    {chatMessages.length > 0 && (
                      <div className="notif-detail-list" style={{ display: 'block' }}>
                        {chatMessages.slice(0, 5).map((msg) => (
                          <div
                            key={msg.id}
                            className="notif-detail-item"
                            onClick={() => {
                              setShowDropdown(false);
                              setExpandedCategory(null);
                              navigate(`/librarian/chat?conversationId=${msg.conversationId}`);
                            }}
                          >
                            <div className="notif-detail-item__body">
                              <p className="notif-detail-item__name">{msg.senderName || 'Sinh viên'}</p>
                              <p className="notif-detail-item__desc">
                                {msg.content?.length > 50 ? msg.content.substring(0, 50) + '...' : msg.content}
                              </p>
                            </div>
                            {msg.timestamp && (
                              <span className="notif-detail-item__time">
                                {getTimeAgo(msg.timestamp)}
                              </span>
                            )}
                          </div>
                        ))}
                        <div
                          className="notif-detail-viewall"
                          onClick={() => {
                            setShowDropdown(false);
                            setExpandedCategory(null);
                            navigate('/librarian/chat');
                          }}
                        >
                          Xem tất cả tin nhắn
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* Hiển thị summary counts với expand */}
                {Object.entries(NOTIF_ICON_MAP).map(([key, config]) => {
                  const countKey = COUNT_KEY_MAP[key];
                  const count = pendingCounts[countKey] || 0;
                  if (count === 0) return null;

                  const Icon = config.icon;
                  const isExpanded = expandedCategory === key;
                  const isLoading = loadingCategory === key;
                  const items = pendingItems[key] || [];

                  return (
                    <div key={key} className="notif-category">
                      <div
                        className={`notif-item notif-item--expandable ${isExpanded ? "notif-item--expanded" : ""}`}
                        onClick={() => handleCategoryClick(key)}
                      >
                        <div className={`notif-item__icon notif-item__icon--${config.cls}`}>
                          <Icon size={18} />
                        </div>
                        <div className="notif-item__body">
                          <p className="notif-item__title">{config.label}</p>
                          <p className="notif-item__desc">{count} mục cần xử lý</p>
                        </div>
                        <span className="notif-item__badge">{count}</span>
                        <ChevronDown
                          size={16}
                          className={`notif-item__chevron ${isExpanded ? "notif-item__chevron--open" : ""}`}
                        />
                      </div>

                      {/* Detail list */}
                      {isExpanded && (
                        <div className="notif-detail-list">
                          {isLoading ? (
                            <div className="notif-detail-loading">Đang tải...</div>
                          ) : items.length > 0 ? (
                            items.slice(0, 5).map((item, idx) => {
                              const info = getItemDisplayInfo(key, item);
                              return (
                                <div
                                  key={item.id || idx}
                                  className="notif-detail-item"
                                  onClick={() => handleItemClick(key, item)}
                                >
                                  <div className="notif-detail-item__body">
                                    <p className="notif-detail-item__name">{info.name}</p>
                                    <p className="notif-detail-item__desc">{info.desc}</p>
                                  </div>
                                  {info.time && (
                                    <span className="notif-detail-item__time">
                                      {getTimeAgo(info.time)}
                                    </span>
                                  )}
                                </div>
                              );
                            })
                          ) : (
                            <div className="notif-detail-loading">Không có dữ liệu</div>
                          )}
                          {items.length > 5 && (
                            <div
                              className="notif-detail-viewall"
                              onClick={() => {
                                setShowDropdown(false);
                                setExpandedCategory(null);
                                navigate(NOTIF_ROUTE_MAP[key]);
                              }}
                            >
                              Xem tất cả {items.length} mục
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}

                {/* Recent activity */}
                {notifications.length > 0 && (
                  <>
                    <div style={{ padding: "8px 12px 4px", fontSize: 12, color: "#9ca3af", fontWeight: 600 }}>
                      Hoạt động gần đây
                    </div>
                    {notifications.slice(0, 5).map((notif) => {
                      const config = NOTIF_ICON_MAP[notif.source] || NOTIF_ICON_MAP.SUPPORT_REQUEST;
                      const Icon = config.icon;
                      const actionLabel = ACTION_LABELS[notif.action] || notif.action;
                      const timeAgo = getTimeAgo(notif.timestamp);

                      return (
                        <div
                          key={notif.id}
                          className="notif-item notif-item--clickable"
                          onClick={() => {
                            setShowDropdown(false);
                            setExpandedCategory(null);
                            navigate(NOTIF_ROUTE_MAP[notif.source] || "/librarian/dashboard");
                          }}
                        >
                          <div className={`notif-item__icon notif-item__icon--${config.cls}`}>
                            <Icon size={18} />
                          </div>
                          <div className="notif-item__body">
                            <p className="notif-item__title">
                              {config.label} {actionLabel}
                            </p>
                            <p className="notif-item__desc">{timeAgo}</p>
                          </div>
                        </div>
                      );
                    })}
                  </>
                )}

                {pendingCounts.total === 0 && notifications.length === 0 && chatMessages.length === 0 && (
                  <div className="notif-dropdown__empty">
                    <CheckCircle2 size={40} />
                    <p>Không có thông báo mới</p>
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
  },
  VIOLATION: {
    CREATED: { title: "Báo cáo vi phạm mới", desc: "Có báo cáo vi phạm mới cần được xác minh và xử lý." },
    STATUS_CHANGED: { title: "Vi phạm đã cập nhật", desc: "Trạng thái báo cáo vi phạm đã thay đổi." },
  },
};

// Toast notification component — hiển thị ở góc phải trên khi có notification mới
function ToastNotifications() {
  const { notifications, pendingCounts } = useLibrarianNotification();
  const [toasts, setToasts] = useState([]);
  const prevCountRef = useRef(0);
  const navigate = useNavigate();

  useEffect(() => {
    // Chỉ hiển thị toast khi có notification MỚI (không phải lần đầu mount)
    if (notifications.length > prevCountRef.current && prevCountRef.current > 0) {
      // Kiểm tra user có bật thông báo không
      const enabled = localStorage.getItem('slib_notifications_enabled');
      if (enabled === 'false') {
        prevCountRef.current = notifications.length;
        return;
      }

      const newNotif = notifications[0]; // notification mới nhất
      const config = NOTIF_ICON_MAP[newNotif.source] || NOTIF_ICON_MAP.SUPPORT_REQUEST;
      const route = NOTIF_ROUTE_MAP[newNotif.source] || '/librarian/dashboard';

      // Lấy thông tin chi tiết
      const detailMap = TOAST_DETAIL_MAP[newNotif.source] || {};
      const detail = detailMap[newNotif.action] || {
        title: `${config.label} - ${ACTION_LABELS[newNotif.action] || newNotif.action}`,
        desc: `Có hoạt động mới liên quan đến ${config.label.toLowerCase()}.`,
      };

      // Đếm pending cho category này
      const countKey = COUNT_KEY_MAP[newNotif.source];
      const categoryCount = pendingCounts[countKey] || 0;

      const toast = {
        id: newNotif.id,
        icon: config.icon,
        cls: config.cls,
        title: detail.title,
        desc: detail.desc,
        count: categoryCount,
        route,
        time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
      };

      setToasts(prev => [toast, ...prev].slice(0, 3)); // Tối đa 3 toast

      // Tự xóa toast sau 8 giây
      setTimeout(() => {
        setToasts(prev => prev.filter(t => t.id !== toast.id));
      }, 8000);
    }
    prevCountRef.current = notifications.length;
  }, [notifications, pendingCounts]);

  if (toasts.length === 0) return null;

  return (
    <div className="toast-container">
      {toasts.map((toast) => {
        const Icon = toast.icon;
        return (
          <div
            key={toast.id}
            className={`toast-item toast-item--${toast.cls}`}
            onClick={() => {
              setToasts(prev => prev.filter(t => t.id !== toast.id));
              navigate(toast.route);
            }}
          >
            <div className={`toast-item__icon toast-item__icon--${toast.cls}`}>
              <Icon size={20} />
            </div>
            <div className="toast-item__body">
              <div className="toast-item__header">
                <p className="toast-item__title">{toast.title}</p>
                <span className="toast-item__time">{toast.time}</span>
              </div>
              <p className="toast-item__desc">{toast.desc}</p>
              <div className="toast-item__footer">
                {toast.count > 0 && (
                  <span className={`toast-item__count toast-item__count--${toast.cls}`}>
                    {toast.count} mục cần xử lý
                  </span>
                )}
                <span className="toast-item__cta">Xem chi tiết →</span>
              </div>
            </div>
            <button
              className="toast-item__close"
              onClick={(e) => {
                e.stopPropagation();
                setToasts(prev => prev.filter(t => t.id !== toast.id));
              }}
            >
              <X size={14} />
            </button>
          </div>
        );
      })}
    </div>
  );
}

// Chat toast notification — hiện khi có tin nhắn chat mới từ student
// Không hiện khi đang ở trang chat (ChatManage đã có toast riêng)
function ChatToastNotification() {
  const { chatToast, setChatToast } = useLibrarianNotification();
  const navigate = useNavigate();

  // Suppress khi đang ở trang chat
  const isOnChatPage = window.location.pathname.includes('/librarian/chat');
  if (!chatToast || isOnChatPage) return null;

  return (
    <div
      className="toast-container"
      style={{ top: '80px' }}
    >
      <div
        className="toast-item toast-item--chat"
        onClick={() => {
          setChatToast(null);
          navigate(`/librarian/chat?conversationId=${chatToast.conversationId}`);
        }}
      >
        <div className="toast-item__icon toast-item__icon--chat">
          <MessageSquare size={20} />
        </div>
        <div className="toast-item__body">
          <div className="toast-item__header">
            <p className="toast-item__title">Tin nhắn mới</p>
          </div>
          <p className="toast-item__desc">
            <strong>{chatToast.senderName}</strong>: {chatToast.content}
          </p>
          <div className="toast-item__footer">
            <span className="toast-item__cta">Xem tin nhắn →</span>
          </div>
        </div>
        <button
          className="toast-item__close"
          onClick={(e) => {
            e.stopPropagation();
            setChatToast(null);
          }}
        >
          <X size={14} />
        </button>
      </div>
    </div>
  );
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

import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MessageSquare } from 'lucide-react';
import { useLibrarianNotification } from '../../context/LibrarianNotificationContext';
import { handleLogout as redirectLogout } from '../../utils/auth';
import '../../styles/librarian/header.css';

const Header = ({
  showBackButton = false,
  onBackClick = () => { },
  onLogout = null
}) => {
  const navigate = useNavigate();
  const [showDropdown, setShowDropdown] = useState(false);
  const [showMsgDropdown, setShowMsgDropdown] = useState(false);
  const [userData, setUserData] = useState({ name: 'Admin', role: 'ADMIN', email: '' });
  const dropdownRef = useRef(null);
  const msgDropdownRef = useRef(null);

  // Notification context
  let notificationCtx = null;
  try {
    notificationCtx = useLibrarianNotification();
  } catch {
    // Context not available (admin pages)
  }

  const chatMessages = notificationCtx?.chatMessages || [];
  const unreadChatCount = notificationCtx?.unreadChatCount || 0;
  const clearChatMessages = notificationCtx?.clearChatMessages;

  useEffect(() => {
    try {
      const userStr = localStorage.getItem('librarian_user') || sessionStorage.getItem('librarian_user');
      if (userStr) {
        const user = JSON.parse(userStr);
        setUserData({
          name: user.fullName || user.user_metadata?.full_name || user.email?.split('@')[0] || 'Admin',
          role: user.role || user.user_metadata?.role || 'ADMIN',
          email: user.email || ''
        });
      }
    } catch (error) {
      console.error('Error loading user data:', error);
    }
  }, []);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
      if (msgDropdownRef.current && !msgDropdownRef.current.contains(event.target)) {
        setShowMsgDropdown(false);
      }
    };
    if (showDropdown || showMsgDropdown) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showDropdown, showMsgDropdown]);

  const handleLogout = () => {
    setShowDropdown(false);
    if (onLogout) {
      onLogout();
    } else {
      redirectLogout();
    }
  };

  const handleSettings = () => {
    setShowDropdown(false);
    const basePath = userData.role?.toUpperCase() === 'ADMIN' ? '/admin' : '/librarian';
    navigate(`${basePath}/settings`);
  };

  const getInitials = (name) => {
    if (!name) return 'AD';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  };

  const getRoleDisplay = (role) => {
    switch (role?.toUpperCase()) {
      case 'ADMIN': return 'Quản trị viên';
      case 'LIBRARIAN': return 'Thủ thư';
      default: return role;
    }
  };

  const handleMsgClick = (conversationId) => {
    setShowMsgDropdown(false);
    navigate(`/librarian/chat?conversationId=${conversationId}`);
  };

  const handleToggleMsgDropdown = () => {
    const next = !showMsgDropdown;
    setShowMsgDropdown(next);
    if (next && clearChatMessages) {
      // Khi mở dropdown, đánh dấu đã xem
      clearChatMessages();
    }
  };

  const formatMsgTime = (ts) => {
    if (!ts) return '';
    const d = new Date(ts);
    if (isNaN(d.getTime())) return '';
    const now = new Date();
    const diffMs = now - d;
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return 'Vừa xong';
    if (diffMin < 60) return `${diffMin} phút trước`;
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <header className="hdr">
      {/* Left: Back button */}
      <div className="hdr-left">
        {showBackButton && (
          <button className="hdr-back" onClick={onBackClick}>
            &#8592;
          </button>
        )}
      </div>

      {/* Right: Message notification + User profile */}
      <div className="hdr-right-group">
        {/* Message notification icon */}
        {notificationCtx && (
          <div className="hdr-msg-wrap" ref={msgDropdownRef}>
            <button
              className={`hdr-msg-btn ${showMsgDropdown ? 'active' : ''}`}
              onClick={handleToggleMsgDropdown}
            >
              <MessageSquare size={18} />
              {unreadChatCount > 0 && (
                <span className="hdr-msg-badge">{unreadChatCount > 9 ? '9+' : unreadChatCount}</span>
              )}
            </button>

            {showMsgDropdown && (
              <div className="hdr-msg-dropdown">
                <div className="hdr-msg-dropdown-header">
                  <span>Tin nhắn</span>
                </div>
                <div className="hdr-msg-dropdown-body">
                  {chatMessages.length === 0 ? (
                    <div className="hdr-msg-empty">Không có tin nhắn mới</div>
                  ) : (
                    chatMessages.map((msg) => (
                      <div
                        key={msg.id}
                        className="hdr-msg-item"
                        onClick={() => handleMsgClick(msg.conversationId)}
                      >
                        <div className="hdr-msg-item-avatar">
                          {(msg.senderName || '?').charAt(0).toUpperCase()}
                        </div>
                        <div className="hdr-msg-item-content">
                          <span className="hdr-msg-item-name">{msg.senderName || 'Sinh viên'}</span>
                          <span className="hdr-msg-item-text">
                            {msg.content?.length > 50 ? msg.content.substring(0, 50) + '...' : msg.content}
                          </span>
                        </div>
                        <span className="hdr-msg-item-time">{formatMsgTime(msg.timestamp)}</span>
                      </div>
                    ))
                  )}
                </div>
                <div className="hdr-msg-dropdown-footer">
                  <button onClick={() => { setShowMsgDropdown(false); navigate('/librarian/chat'); }}>
                    Xem tất cả
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* User profile */}
        <div className="hdr-right" ref={dropdownRef}>
          <button
            className={`hdr-user ${showDropdown ? 'active' : ''}`}
            onClick={() => setShowDropdown(!showDropdown)}
          >
            <div className="hdr-avatar">
              {getInitials(userData.name)}
            </div>
            <div className="hdr-user-info">
              <span className="hdr-name">{userData.name}</span>
              <span className="hdr-role">{getRoleDisplay(userData.role)}</span>
            </div>
            <span className={`hdr-chevron ${showDropdown ? 'open' : ''}`}>&#9662;</span>
          </button>

          {showDropdown && (
            <div className="hdr-dropdown">
              <div className="hdr-dropdown-info">
                <span className="hdr-dropdown-name">{userData.name}</span>
                <span className="hdr-dropdown-email">{userData.email}</span>
              </div>
              <button className="hdr-dropdown-item" onClick={handleSettings}>
                Cài đặt tài khoản
              </button>
              <button className="hdr-dropdown-item danger" onClick={handleLogout}>
                Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;

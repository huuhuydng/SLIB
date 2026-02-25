import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import '../../styles/librarian/header.css';

const Header = ({
  showBackButton = false,
  onBackClick = () => { },
  onLogout = null
}) => {
  const navigate = useNavigate();
  const [showDropdown, setShowDropdown] = useState(false);
  const [userData, setUserData] = useState({ name: 'Admin', role: 'ADMIN', email: '' });
  const dropdownRef = useRef(null);

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
    };
    if (showDropdown) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showDropdown]);

  const handleLogout = () => {
    setShowDropdown(false);
    if (onLogout) {
      onLogout();
    } else {
      localStorage.removeItem('librarian_token');
      localStorage.removeItem('librarian_user');
      sessionStorage.removeItem('librarian_token');
      sessionStorage.removeItem('librarian_user');
      window.location.href = '/';
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

      {/* Right: User profile */}
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
    </header>
  );
};

export default Header;
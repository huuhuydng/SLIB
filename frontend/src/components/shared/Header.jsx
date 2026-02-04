import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronDown, Search, User, LogOut, Settings } from 'lucide-react';

const Header = ({
  searchValue = '',
  onSearchChange = () => { },
  searchPlaceholder = "Tìm kiếm...",
  showBackButton = false,
  onBackClick = () => { },
  onLogout = null
}) => {
  const navigate = useNavigate();
  const [showDropdown, setShowDropdown] = useState(false);
  const [userData, setUserData] = useState({ name: 'Admin', role: 'ADMIN', email: '' });
  const dropdownRef = useRef(null);

  // Load user data from localStorage
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

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };

    if (showDropdown) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
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
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  };

  const getRoleDisplay = (role) => {
    switch (role?.toUpperCase()) {
      case 'ADMIN': return 'Quản trị viên';
      case 'LIBRARIAN': return 'Thủ thư';
      default: return role;
    }
  };

  return (
    <header style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '1.25rem 1.5rem',
      backgroundColor: '#ffffff',
      boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
      marginBottom: '1.5rem',
      borderRadius: '16px'
    }}>
      {/* Left Section: Back Button or Spacer */}
      {showBackButton ? (
        <button
          onClick={onBackClick}
          style={{
            padding: '10px',
            border: 'none',
            background: '#f3f4f6',
            borderRadius: '12px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'all 0.2s',
            color: '#374151',
            marginRight: '16px'
          }}
          onMouseEnter={(e) => e.currentTarget.style.background = '#e5e7eb'}
          onMouseLeave={(e) => e.currentTarget.style.background = '#f3f4f6'}
        >
          <ChevronLeft size={20} />
        </button>
      ) : null}

      {/* Search Bar */}
      <div style={{
        flex: 1,
        maxWidth: '500px',
        position: 'relative'
      }}>
        <Search
          size={18}
          style={{
            position: 'absolute',
            left: '14px',
            top: '50%',
            transform: 'translateY(-50%)',
            color: '#9CA3AF'
          }}
        />
        <input
          type="text"
          placeholder={searchPlaceholder}
          value={searchValue}
          onChange={onSearchChange}
          style={{
            width: '100%',
            padding: '10px 14px 10px 42px',
            border: '2px solid #E5E7EB',
            borderRadius: '12px',
            fontSize: '14px',
            outline: 'none',
            transition: 'border-color 0.2s'
          }}
          onFocus={(e) => e.target.style.borderColor = '#FF751F'}
          onBlur={(e) => e.target.style.borderColor = '#E5E7EB'}
        />
      </div>

      {/* User Profile */}
      <div
        ref={dropdownRef}
        style={{
          position: 'relative',
          marginLeft: '20px'
        }}
      >
        <button
          onClick={() => setShowDropdown(!showDropdown)}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '8px 16px',
            border: '2px solid #E5E7EB',
            borderRadius: '12px',
            backgroundColor: '#fff',
            cursor: 'pointer',
            transition: 'all 0.2s'
          }}
          onMouseEnter={(e) => e.currentTarget.style.borderColor = '#FF751F'}
          onMouseLeave={(e) => !showDropdown && (e.currentTarget.style.borderColor = '#E5E7EB')}
        >
          <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '8px',
            background: 'linear-gradient(135deg, #FF751F, #FF9B5A)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: '12px',
            fontWeight: '600'
          }}>
            {getInitials(userData.name)}
          </div>
          <div style={{ textAlign: 'left' }}>
            <div style={{ fontSize: '14px', fontWeight: '600', color: '#1F2937' }}>
              {userData.name}
            </div>
            <div style={{ fontSize: '12px', color: '#6B7280' }}>
              {getRoleDisplay(userData.role)}
            </div>
          </div>
          <ChevronDown
            size={16}
            style={{
              color: '#9CA3AF',
              transform: showDropdown ? 'rotate(180deg)' : 'rotate(0)',
              transition: 'transform 0.2s'
            }}
          />
        </button>

        {/* Dropdown Menu */}
        {showDropdown && (
          <div style={{
            position: 'absolute',
            top: 'calc(100% + 8px)',
            right: 0,
            backgroundColor: '#fff',
            borderRadius: '12px',
            boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
            border: '1px solid #E5E7EB',
            minWidth: '220px',
            zIndex: 1000,
            overflow: 'hidden'
          }}>
            {/* User Info */}
            <div style={{
              padding: '12px 16px',
              borderBottom: '1px solid #E5E7EB'
            }}>
              <div style={{ fontSize: '14px', fontWeight: '600', color: '#1F2937' }}>
                {userData.name}
              </div>
              <div style={{ fontSize: '12px', color: '#6B7280', marginTop: '2px' }}>
                {userData.email}
              </div>
            </div>

            {/* Settings */}
            <button
              onClick={handleSettings}
              style={{
                width: '100%',
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                padding: '12px 16px',
                border: 'none',
                background: 'transparent',
                cursor: 'pointer',
                fontSize: '14px',
                color: '#1F2937',
                fontWeight: '500',
                textAlign: 'left',
                transition: 'background-color 0.2s',
                borderBottom: '1px solid #E5E7EB'
              }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#F3F4F6'}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
            >
              <Settings size={16} />
              Cài đặt tài khoản
            </button>

            {/* Logout */}
            <button
              onClick={handleLogout}
              style={{
                width: '100%',
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                padding: '12px 16px',
                border: 'none',
                background: 'transparent',
                cursor: 'pointer',
                fontSize: '14px',
                color: '#DC2626',
                fontWeight: '500',
                textAlign: 'left',
                transition: 'background-color 0.2s'
              }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#FEF2F2'}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
            >
              <LogOut size={16} />
              Đăng xuất
            </button>
          </div>
        )}
      </div>
    </header>
  );
};

export default Header;
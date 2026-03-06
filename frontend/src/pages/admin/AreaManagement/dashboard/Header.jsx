import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronDown, Search, User, LogOut, Bell, Settings } from 'lucide-react';
import avatarImage from "../../../../assets/avatar.svg";

const Header = ({
  searchValue = '',
  onSearchChange = () => { },
  searchPlaceholder = "Tim kiem...",
  onLogout = () => { }
}) => {
  const navigate = useNavigate();
  const [showDropdown, setShowDropdown] = useState(false);
  const [userData, setUserData] = useState({ name: 'User', role: 'Librarian' });
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    try {
      const userStr = localStorage.getItem('librarian_user');
      if (userStr) {
        const user = JSON.parse(userStr);
        setUserData({
          name: user.user_metadata?.name || user.user_metadata?.full_name || user.email?.split('@')[0] || 'User',
          role: user.user_metadata?.role || 'Librarian',
          email: user.email
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

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showDropdown]);

  return (
    <header style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '16px 32px',
      backgroundColor: 'var(--slib-bg-card, #ffffff)',
      boxShadow: 'var(--slib-shadow-sm, 0 1px 2px 0 rgba(0, 0, 0, 0.05))',
      borderRadius: '10px',
      position: 'sticky',
      top: 0,
      zIndex: 50,
      margin: '16px 24px',
      gap: '24px'
    }}>
      {/* Search Bar */}
      <div style={{
        flex: 1,
        maxWidth: '520px',
        position: 'relative'
      }}>
        <Search
          size={18}
          style={{
            position: 'absolute',
            left: '16px',
            top: '50%',
            transform: 'translateY(-50%)',
            color: isSearchFocused ? 'var(--slib-primary, #FF751F)' : 'var(--slib-text-muted, #A0AEC0)',
            transition: 'color 0.2s ease',
            pointerEvents: 'none'
          }}
        />
        <input
          type="text"
          placeholder={searchPlaceholder}
          value={searchValue}
          onChange={onSearchChange}
          onFocus={() => setIsSearchFocused(true)}
          onBlur={() => setIsSearchFocused(false)}
          style={{
            width: '100%',
            padding: '12px 16px 12px 48px',
            border: isSearchFocused
              ? '2px solid var(--slib-primary, #FF751F)'
              : '2px solid var(--slib-border-light, #E2E8F0)',
            borderRadius: '12px',
            fontSize: '14px',
            color: 'var(--slib-text-primary, #1A1A1A)',
            backgroundColor: isSearchFocused
              ? 'var(--slib-bg-card, #ffffff)'
              : 'var(--slib-bg-main, #F7FAFC)',
            outline: 'none',
            transition: 'all 0.2s ease',
            boxShadow: isSearchFocused
              ? '0 0 0 4px rgba(255, 117, 31, 0.1)'
              : 'none'
          }}
        />
      </div>

      {/* Right Section */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '16px'
      }}>
        {/* Notification Bell */}
        <button
          style={{
            position: 'relative',
            width: '44px',
            height: '44px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: 'var(--slib-bg-main, #F7FAFC)',
            border: 'none',
            borderRadius: '12px',
            cursor: 'pointer',
            transition: 'all 0.2s ease'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.backgroundColor = 'var(--slib-primary-subtle, #FFF7F2)';
            e.currentTarget.style.transform = 'translateY(-2px)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = 'var(--slib-bg-main, #F7FAFC)';
            e.currentTarget.style.transform = 'translateY(0)';
          }}
        >
          <Bell size={20} color="var(--slib-text-secondary, #4A5568)" />
          <span style={{
            position: 'absolute',
            top: '8px',
            right: '8px',
            width: '10px',
            height: '10px',
            backgroundColor: 'var(--slib-primary, #FF751F)',
            borderRadius: '50%',
            border: '2px solid var(--slib-bg-card, #ffffff)'
          }} />
        </button>

        {/* Settings */}
        <button
          style={{
            width: '44px',
            height: '44px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: 'var(--slib-bg-main, #F7FAFC)',
            border: 'none',
            borderRadius: '12px',
            cursor: 'pointer',
            transition: 'all 0.2s ease'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.backgroundColor = 'var(--slib-primary-subtle, #FFF7F2)';
            e.currentTarget.style.transform = 'translateY(-2px)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = 'var(--slib-bg-main, #F7FAFC)';
            e.currentTarget.style.transform = 'translateY(0)';
          }}
        >
          <Settings size={20} color="var(--slib-text-secondary, #4A5568)" />
        </button>

        {/* Divider */}
        <div style={{
          width: '1px',
          height: '32px',
          backgroundColor: 'var(--slib-border-light, #E2E8F0)'
        }} />

        {/* User Profile */}
        <div style={{ position: 'relative' }} ref={dropdownRef}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              padding: '8px 16px 8px 8px',
              backgroundColor: 'var(--slib-bg-main, #F7FAFC)',
              borderRadius: '12px',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              border: showDropdown
                ? '2px solid var(--slib-primary, #FF751F)'
                : '2px solid transparent'
            }}
            onClick={() => setShowDropdown(!showDropdown)}
            onMouseEnter={(e) => {
              if (!showDropdown) {
                e.currentTarget.style.backgroundColor = 'var(--slib-primary-subtle, #FFF7F2)';
              }
            }}
            onMouseLeave={(e) => {
              if (!showDropdown) {
                e.currentTarget.style.backgroundColor = 'var(--slib-bg-main, #F7FAFC)';
              }
            }}
          >
            <img
              src={avatarImage}
              alt="Avatar"
              style={{
                width: '40px',
                height: '40px',
                borderRadius: '10px',
                border: '2px solid var(--slib-bg-card, #ffffff)',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)'
              }}
            />
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
              <span style={{ fontSize: '14px', fontWeight: '600', color: 'var(--slib-text-primary, #1A1A1A)' }}>
                {userData.name}
              </span>
              <span style={{ fontSize: '12px', color: 'var(--slib-text-muted, #A0AEC0)', fontWeight: '500' }}>
                {userData.role}
              </span>
            </div>
            <ChevronDown
              size={18}
              color="var(--slib-text-muted, #A0AEC0)"
              style={{ transform: showDropdown ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s ease' }}
            />
          </div>

          {/* Dropdown */}
          {showDropdown && (
            <div style={{
              position: 'absolute',
              top: 'calc(100% + 8px)',
              right: 0,
              backgroundColor: 'var(--slib-bg-card, #ffffff)',
              borderRadius: '12px',
              boxShadow: 'var(--slib-shadow-lg)',
              border: '1px solid var(--slib-border-light, #E2E8F0)',
              minWidth: '200px',
              overflow: 'hidden',
              zIndex: 1000
            }}>
              <div style={{
                padding: '16px',
                borderBottom: '1px solid var(--slib-border-light, #E2E8F0)',
                background: 'var(--slib-bg-main, #F7FAFC)'
              }}>
                <p style={{ fontSize: '14px', fontWeight: '600', color: 'var(--slib-text-primary, #1A1A1A)', margin: '0 0 4px 0' }}>
                  {userData.name}
                </p>
                <p style={{ fontSize: '12px', color: 'var(--slib-text-muted, #A0AEC0)', margin: 0 }}>
                  {userData.email || 'user@example.com'}
                </p>
              </div>

              <div
                onClick={() => setShowDropdown(false)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '12px 16px',
                  cursor: 'pointer',
                  transition: 'background-color 0.2s ease'
                }}
                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--slib-bg-main, #F7FAFC)'}
                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
              >
                <User size={18} color="var(--slib-text-secondary, #4A5568)" />
                <span style={{ fontSize: '14px', fontWeight: '500', color: 'var(--slib-text-secondary, #4A5568)' }}>
                  Ho so ca nhan
                </span>
              </div>

              <div
                onClick={() => {
                  setShowDropdown(false);
                  navigate('/admin/settings');
                }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '12px 16px',
                  cursor: 'pointer',
                  transition: 'background-color 0.2s ease'
                }}
                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--slib-bg-main, #F7FAFC)'}
                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
              >
                <Settings size={18} color="var(--slib-text-secondary, #4A5568)" />
                <span style={{ fontSize: '14px', fontWeight: '500', color: 'var(--slib-text-secondary, #4A5568)' }}>
                  Cai dat tai khoan
                </span>
              </div>

              <div
                onClick={() => {
                  console.log('FORCE LOGOUT!');
                  setShowDropdown(false);

                  // Clear all storage
                  localStorage.clear();
                  sessionStorage.clear();
                  console.log('Storage cleared');

                  // FORCE COMPLETE PAGE RELOAD from server
                  console.log('Reloading from server...');
                  window.location.reload(true);
                }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '12px 16px',
                  cursor: 'pointer',
                  borderTop: '1px solid var(--slib-border-light, #E2E8F0)',
                  transition: 'all 0.2s ease'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = 'var(--slib-status-error-bg, #FFEBEE)';
                  e.currentTarget.querySelector('svg').style.color = 'var(--slib-status-error, #D32F2F)';
                  e.currentTarget.querySelector('span').style.color = 'var(--slib-status-error, #D32F2F)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = 'transparent';
                  e.currentTarget.querySelector('svg').style.color = 'var(--slib-text-secondary, #4A5568)';
                  e.currentTarget.querySelector('span').style.color = 'var(--slib-text-secondary, #4A5568)';
                }}
              >
                <LogOut size={18} style={{ color: 'var(--slib-text-secondary, #4A5568)', transition: 'color 0.2s ease' }} />
                <span style={{ fontSize: '14px', fontWeight: '500', color: 'var(--slib-text-secondary, #4A5568)', transition: 'color 0.2s ease' }}>
                  Dang xuat
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;

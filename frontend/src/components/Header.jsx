import React, { useState, useRef, useEffect } from 'react';
import { ChevronLeft, ChevronDown, Search, User, LogOut } from 'lucide-react';
import avatarImage from "../assets/avatar.svg";

const Header = ({ 
  searchValue = '', 
  onSearchChange = () => {},
  searchPlaceholder = "Search for anything...",
  showBackButton = false,
  onBackClick = () => {},
  onLogout = () => {}
}) => {
  const [showDropdown, setShowDropdown] = useState(false);
  const [userData, setUserData] = useState({ name: 'User', role: 'Librarian' });
  const dropdownRef = useRef(null);

  // Load user data from localStorage
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
        console.log('✅ User data loaded:', user);
      }
    } catch (error) {
      console.error('❌ Error loading user data:', error);
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
      padding: '1.25rem 2rem',
      backgroundColor: '#ffffff',
      boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
      marginBottom: '0',
      borderRadius: '24px',
      position: 'sticky',
      top: 0,
      zIndex: 50
    }}>
      {showBackButton ? (
        <button 
          onClick={onBackClick}
          style={{
            padding: '0.625rem',
            border: 'none',
            background: '#f3f4f6',
            borderRadius: '16px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'all 0.2s',
            color: '#374151'
          }}
          onMouseEnter={(e) => e.currentTarget.style.background = '#e5e7eb'}
          onMouseLeave={(e) => e.currentTarget.style.background = '#f3f4f6'}
        >
          <ChevronLeft size={20} />
        </button>
      ) : (
        <div style={{ width: '42px' }}></div>
      )}
      
      <div style={{
        flex: 1,
        maxWidth: '650px',
        margin: '0 2rem',
        position: 'relative'
      }}>
        <Search 
          size={18} 
          style={{
            position: 'absolute',
            left: '1.125rem',
            top: '50%',
            transform: 'translateY(-50%)',
            color: '#9ca3af',
            pointerEvents: 'none'
          }}
        />
        <input 
          type="text" 
          placeholder={searchPlaceholder}
          value={searchValue}
          onChange={onSearchChange}
          style={{
            width: '100%',
            padding: '0.75rem 1.25rem 0.75rem 3rem',
            border: '1px solid #e5e7eb',
            borderRadius: '20px',
            fontSize: '0.875rem',
            outline: 'none',
            transition: 'all 0.2s',
            backgroundColor: '#f9fafb'
          }}
          onFocus={(e) => {
            e.target.style.borderColor = '#8b5cf6';
            e.target.style.backgroundColor = '#ffffff';
            e.target.style.boxShadow = '0 0 0 3px rgba(139, 92, 246, 0.1)';
          }}
          onBlur={(e) => {
            e.target.style.borderColor = '#e5e7eb';
            e.target.style.backgroundColor = '#f9fafb';
            e.target.style.boxShadow = 'none';
          }}
        />
      </div>
      
      <div style={{ position: 'relative' }} ref={dropdownRef}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.875rem',
          padding: '0.5rem 1rem 0.5rem 0.75rem',
          backgroundColor: '#f9fafb',
          borderRadius: '30px',
          cursor: 'pointer',
          transition: 'all 0.2s',
          border: '1px solid transparent'
        }}
        onClick={() => setShowDropdown(!showDropdown)}
        onMouseEnter={(e) => {
          e.currentTarget.style.backgroundColor = '#f3f4f6';
          e.currentTarget.style.borderColor = '#e5e7eb';
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.backgroundColor = '#f9fafb';
          e.currentTarget.style.borderColor = 'transparent';
        }}
        >
          <img 
            src={avatarImage} 
            alt="Avatar" 
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '50%',
              border: '2px solid #ffffff',
              boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)'
            }} 
          />
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'flex-start'
          }}>
            <span style={{
              fontSize: '0.875rem',
              fontWeight: '600'
            }}>{userData.name}</span>
            <span style={{
              fontSize: '0.75rem',
              color: '#6b7280'
            }}>{userData.role}</span>
          </div>
          <ChevronDown size={16} style={{
            transform: showDropdown ? 'rotate(180deg)' : 'rotate(0deg)',
            transition: 'transform 0.2s'
          }} />
        </div>

        {showDropdown && (
          <div style={{
            position: 'absolute',
            top: 'calc(100% + 8px)',
            right: 0,
            backgroundColor: '#ffffff',
            borderRadius: '16px',
            boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
            border: '1px solid #e5e7eb',
            minWidth: '180px',
            overflow: 'hidden',
            zIndex: 1000
          }}>
            <div 
              onClick={() => {
                setShowDropdown(false);
                // TODO: Navigate to profile page
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.75rem 1rem',
                cursor: 'pointer',
                transition: 'background-color 0.2s',
                borderBottom: '1px solid #f3f4f6'
              }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f9fafb'}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
            >
              <User size={18} style={{ color: '#6b7280' }} />
              <span style={{ fontSize: '0.875rem', fontWeight: '500' }}>Profile</span>
            </div>
            <div 
              onClick={() => {
                setShowDropdown(false);
                onLogout();
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.75rem 1rem',
                cursor: 'pointer',
                transition: 'background-color 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = '#fef2f2';
                e.currentTarget.querySelector('svg').style.color = '#dc2626';
                e.currentTarget.querySelector('span').style.color = '#dc2626';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.querySelector('svg').style.color = '#6b7280';
                e.currentTarget.querySelector('span').style.color = '#000000';
              }}
            >
              <LogOut size={18} style={{ color: '#6b7280', transition: 'color 0.2s' }} />
              <span style={{ fontSize: '0.875rem', fontWeight: '500', transition: 'color 0.2s' }}>Logout</span>
            </div>
          </div>
        )}
      </div>
    </header>
  );
};

export default Header;
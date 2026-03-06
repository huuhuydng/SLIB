import { Outlet, useNavigate } from "react-router-dom";
import { useState, useRef, useEffect } from "react";
import { Bell, Settings, LogOut } from "lucide-react";
import Sidebar from "../../components/sidebar_admin/Sidebar_admin";
import "../../styles/admin/MainLayout.css";

function AdminHeader() {
  const navigate = useNavigate();
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef(null);

  const [userData] = useState(() => {
    try {
      const s = localStorage.getItem('librarian_user') || sessionStorage.getItem('librarian_user');
      if (s) { const u = JSON.parse(s); return { name: u.fullName || u.email?.split('@')[0] || 'Admin', email: u.email || '' }; }
    } catch { }
    return { name: 'Admin', email: '' };
  });

  useEffect(() => {
    if (!showDropdown) return;
    const handler = (e) => { if (dropdownRef.current && !dropdownRef.current.contains(e.target)) setShowDropdown(false); };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [showDropdown]);

  const getInitials = (n) => n ? n.split(' ').map(w => w[0]).join('').toUpperCase().substring(0, 2) : 'AD';

  const handleLogout = () => {
    localStorage.clear();
    sessionStorage.clear();
    window.location.reload(true);
  };

  return (
    <header className="top-header">
      <div className="top-header__left">
      </div>
      <div className="top-header__right">
        <button className="top-header__bell" title="Thông báo">
          <Bell size={18} />
        </button>
        <div ref={dropdownRef} style={{ position: 'relative' }}>
          <button className="top-header__user" onClick={() => setShowDropdown(!showDropdown)}>
            <div className="top-header__avatar">{getInitials(userData.name)}</div>
            <span className="top-header__name">{userData.name}</span>
          </button>
          {showDropdown && (
            <div className="top-header__dropdown">
              <div className="top-header__dropdown-info">
                <span className="top-header__dropdown-name">{userData.name}</span>
                <span className="top-header__dropdown-email">{userData.email}</span>
              </div>
              <button className="top-header__dropdown-item" onClick={() => { setShowDropdown(false); navigate('/admin/config'); }}>
                <Settings size={15} /> Cài đặt
              </button>
              <button className="top-header__dropdown-item top-header__dropdown-item--danger" onClick={handleLogout}>
                <LogOut size={15} /> Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

function MainLayout() {
  return (
    <>
      <AdminHeader />
      <div className="appLayout">
        <Sidebar />
        <div className="main">
          <Outlet />
        </div>
      </div>
    </>
  );
}

export default MainLayout;

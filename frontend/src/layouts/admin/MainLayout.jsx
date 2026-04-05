import { Outlet } from "react-router-dom";
import { useState } from "react";
import Sidebar from "../../components/sidebar_admin/Sidebar_admin";
import "../../styles/admin/MainLayout.css";

function AdminHeader() {
  const [userData] = useState(() => {
    try {
      const s = localStorage.getItem('librarian_user') || sessionStorage.getItem('librarian_user');
      if (s) { const u = JSON.parse(s); return { name: u.fullName || u.email?.split('@')[0] || 'Admin', email: u.email || '' }; }
    } catch { }
    return { name: 'Admin', email: '' };
  });

  const getInitials = (n) => n ? n.split(' ').map(w => w[0]).join('').toUpperCase().substring(0, 2) : 'AD';

  return (
    <header className="top-header">
      <div className="top-header__left">
      </div>
      <div className="top-header__right">
        <div className="top-header__welcome">
          <span className="top-header__welcomeLabel">Chào mừng bạn,</span>
          <span className="top-header__welcomeName">{userData.name}</span>
        </div>
        <div className="top-header__user" style={{ cursor: 'default' }}>
          <div className="top-header__avatar">{getInitials(userData.name)}</div>
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

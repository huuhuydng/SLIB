import React, { useState, useEffect } from "react";
import AuthPage from "./components/AuthPage";
import Dashboard from "./components/Dashboard";
import CheckInOut from "./components/CheckInOut";
import HeatMap from "./components/HeatMap";
import SeatManage from "./components/SeatManage";
import StudentsManage from "./components/StudentsManage";
import ViolationManage from "./components/ViolationManage";
import ChatManage from "./components/ChatManage";
import Statistic from "./components/Statistic";
import NotificationManage from "./components/NotificationManage";
import Sidebar from "./components/Sidebar";

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentPage, setCurrentPage] = useState("dashboard");

  
  const handleLogout = () => {
    // Clear all authentication data from localStorage
    localStorage.removeItem('librarian_token');
    localStorage.removeItem('librarian_user');
    sessionStorage.removeItem('librarian_token');
    sessionStorage.removeItem('librarian_user');
    // Set logged out state
    setIsLoggedIn(false);
    // Reset to dashboard page
    setCurrentPage('dashboard');
  };

  if (!isLoggedIn) {
    return (
      <AuthPage 
        onLogin={() => setIsLoggedIn(true)}
      />
    );
  }

  const renderPage = () => {
    switch (currentPage) {
      case "dashboard":
        return <Dashboard />;
      case "checkinout":
        return <CheckInOut />;
      case "heatmap":
        return <HeatMap />;
      case "seatmanage":
        return <SeatManage />;
      case "students":
        return <StudentsManage />;
      case "violation":
        return <ViolationManage />;
      case "chat":
        return <ChatManage />;
      case "statistic":
        return <Statistic />;
      case "notification":
        return <NotificationManage />;
      default:
        return <Dashboard />;
    }
  };

  return (
    <div className="appLayout">
      <Sidebar currentPage={currentPage} onPageChange={setCurrentPage} />
      <div className="main" style={{ marginLeft: '240px', width: 'calc(100% - 240px)', padding: '18px 22px 26px' }}>
        {renderPage()}
      </div>
    </div>
  );
}

export default App;
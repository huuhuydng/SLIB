import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

// Global/Page styles
import "./styles/Auth.css";
import "./styles/Login.css";
import "./styles/Signup.css";
import "./styles/Dashboard.css";
import "./styles/CheckInOut.css";
import "./styles/HeatMap.css";
import "./styles/SeatManage.css";
import "./styles/StudentsManage.css";
import "./styles/StudentDetail.css";
import "./styles/ViolationManage.css";
import "./styles/ChatManage.css";
import "./styles/Statistic.css";
import "./styles/NotificationManage.css";

// Auth pages (đang nằm trực tiếp trong components)
import AuthPage from "./components/AuthPage";
import Login from "./components/auth/Login";
import Signup from "./components/auth/Signup";

// Pages theo folder
import Dashboard from "./components/dashboard/Dashboard";
import CheckInOut from "./components/checkinout/CheckInOut";
import HeatMap from "./components/heatmap/HeatMap";
import SeatManage from "./components/seatmanage/SeatManage";

import StudentsManage from "./components/students/StudentsManage";
import StudentDetail from "./components/students/StudentDetail";

import ViolationManage from "./components/violation/ViolationManage";
import ChatManage from "./components/chat/ChatManage";

import Statistic from "./components/statistic/Statistic";
import NotificationManage from "./components/notification/NotificationManage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Default */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />

        {/* Auth */}
        <Route path="/auth" element={<AuthPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />

        {/* Main */}
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/checkinout" element={<CheckInOut />} />
        <Route path="/heatmap" element={<HeatMap />} />
        <Route path="/seatmanage" element={<SeatManage />} />
        <Route path="/chat" element={<ChatManage />} />
        <Route path="/statistic" element={<Statistic />} />
        <Route path="/notification" element={<NotificationManage />} />

        {/* Students */}
        <Route path="/students" element={<StudentsManage />} />
        <Route path="/students/:studentId" element={<StudentDetail />} />

        {/* Violation */}
        <Route path="/violation" element={<ViolationManage />} />

        {/* 404 */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

// import AuthPage from "../components/AuthPage";
import MainLayout from "../layouts/admin/area_management/MainLayOut";

import Dashboard from "../pages/admin/AreaManagement/dashboard/Dashboard";
// import CheckInOut from "../components/CheckInOut";
import AreaManagement from "../pages/admin/AreaManagement/AreaManagement";
// import SeatManage from "../components/SeatManage";
// import StudentsManage from "../components/StudentsManage";
// import ViolationManage from "../components/ViolationManage";
// import ChatManage from "../components/ChatManage";
// import Statistic from "../components/Statistic";
// import NotificationManage from "../components/NotificationManage";

function AppRoutes() {
  const isLoggedIn = true;

  if (!isLoggedIn) {
    return <AuthPage />;
  }

  return (
    <BrowserRouter>
      <Routes>
        {/* Layout có Sidebar */}
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/areas" element={<AreaManagement />} />
          {/* <Route path="/checkinout" element={<CheckInOut />} />
          <Route path="/seatmanage" element={<SeatManage />} />
          <Route path="/students" element={<StudentsManage />} />
          <Route path="/violation" element={<ViolationManage />} />
          <Route path="/chat" element={<ChatManage />} />
          <Route path="/statistic" element={<Statistic />} />
          <Route path="/notification" element={<NotificationManage />} /> */}
        </Route>

        {/* Mặc định */}
        <Route path="/" element={<Navigate to="/dashboard" />} />
        <Route path="*" element={<Navigate to="/dashboard" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default AppRoutes;

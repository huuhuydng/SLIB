import { Routes, Route, Navigate } from "react-router-dom";

// Layout
import MainLayout from "../layouts/librarian/MainLayout";

// Librarian Pages
import Dashboard from "../pages/librarian/Dashboard/Dashboard";
import CheckInOut from "../pages/librarian/CheckInOut/CheckInOut";
import HeatMap from "../pages/librarian/HeatMap/HeatMap";
import SeatManage from "../pages/librarian/SeatManage/SeatManage";
import StudentsManage from "../pages/librarian/StudentsManage/StudentsManage";
import ViolationManage from "../pages/librarian/ViolationManage/ViolationManage";
import ChatManage from "../pages/librarian/ChatManage/ChatManage";
import Statistic from "../pages/librarian/Statistic/Statistic";
import NotificationManage from "../pages/librarian/NotificationManage/NotificationManage";
import NewCreate from "../pages/librarian/NotificationManage/NewCreate";
import NewsDetailView from "../pages/librarian/NotificationManage/NewsDetailView";

function LibrarianRoutes() {
  return (
    <Routes>
      {/* Librarian Layout with Sidebar */}
      <Route element={<MainLayout />}>
        {/* Dashboard */}
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />

        {/* Check In/Out */}
        <Route path="checkinout" element={<CheckInOut />} />

        {/* Sơ đồ nhiệt */}
        <Route path="areas" element={<HeatMap />} />

        {/* Quản lý chỗ ngồi */}
        <Route path="seatmanage" element={<SeatManage />} />

        {/* Quản lý sinh viên */}
        <Route path="students" element={<StudentsManage />} />

        {/* Vi phạm */}
        <Route path="violation" element={<ViolationManage />} />

        {/* Trò chuyện */}
        <Route path="chat" element={<ChatManage />} />

        {/* Thống kê */}
        <Route path="statistic" element={<Statistic />} />

        {/* Thông báo */}
        <Route path="notification" element={<NotificationManage />} />
        <Route path="notification/create" element={<NewCreate />} />
        <Route path="notification/edit/:id" element={<NewCreate />} />
        <Route path="notification/view/:id" element={<NewsDetailView />} />
      </Route>

      {/* Redirect any unmatched routes */}
      <Route path="*" element={<Navigate to="dashboard" replace />} />
    </Routes>
  );
}

export default LibrarianRoutes;


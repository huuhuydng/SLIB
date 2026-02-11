import { Routes, Route, Navigate } from "react-router-dom";

// Layout
import MainLayout from "../layouts/librarian/MainLayout";

// Librarian Pages
import Dashboard from "../pages/librarian/Dashboard/Dashboard";
import CheckInOut from "../pages/librarian/CheckInOut/CheckInOut";
import LibrarianAreas from "../pages/librarian/LibrarianAreas/LibrarianAreas";
import StudentsManage from "../pages/librarian/StudentsManage/StudentsManage";
import ViolationManage from "../pages/librarian/ViolationManage/ViolationManage";
import ChatManage from "../pages/librarian/ChatManage/ChatManage";
import Statistic from "../pages/librarian/Statistic/Statistic";
import ChatManagement from "../components/ChatManagement";
import NotificationManage from "../pages/librarian/NewsManage/NotificationManage";
import NewCreate from "../pages/librarian/NewsManage/NewCreate";
import NewsDetailView from "../pages/librarian/NewsManage/NewsDetailView";
import AccountSettings from "../components/AccountSettings";


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

        {/* Quản lý chỗ ngồi */}
        <Route path="seatmanage" element={<LibrarianAreas />} />

        {/* Quản lý sinh viên */}
        <Route path="students" element={<StudentsManage />} />

        {/* Vi phạm */}
        <Route path="violation" element={<ViolationManage />} />

        {/* Trò chuyện */}
        <Route path="chat" element={<ChatManagement />} />

        {/* Thống kê */}
        <Route path="statistic" element={<Statistic />} />

        {/* Tin tức */}
        <Route path="news" element={<NotificationManage />} />
        <Route path="news/create" element={<NewCreate />} />
        <Route path="news/edit/:id" element={<NewCreate />} />
        <Route path="news/view/:id" element={<NewsDetailView />} />

        {/* Thông báo (legacy routes - giữ để tương thích) */}
        <Route path="notification" element={<NotificationManage />} />
        <Route path="notification/create" element={<NewCreate />} />
        <Route path="notification/edit/:id" element={<NewCreate />} />
        <Route path="notification/view/:id" element={<NewsDetailView />} />

        {/* Cài đặt tài khoản */}
        <Route path="settings" element={<AccountSettings />} />
        <Route path="setting" element={<Navigate to="/librarian/settings" replace />} />
      </Route>

      {/* Redirect any unmatched routes */}
      <Route path="*" element={<Navigate to="dashboard" replace />} />
    </Routes>
  );
}

export default LibrarianRoutes;


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

import NotificationManage from "../pages/librarian/NewsManage/NotificationManage";
import NewCreate from "../pages/librarian/NewsManage/NewCreate";
import NewsDetailView from "../pages/librarian/NewsManage/NewsDetailView";
import AccountSettings from "../components/settings/AccountSettings";
import HeatMap from "../pages/librarian/HeatMap/HeatMap";
import SupportRequestManage from "../pages/librarian/SupportRequest/SupportRequestManage";
import BookingManage from "../pages/librarian/BookingManage/BookingManage";
import FeedbackManage from "../pages/librarian/FeedbackManage/FeedbackManage";
import ComplaintManage from "../pages/librarian/ComplaintManage/ComplaintManage";
import SeatStatusReportManage from "../pages/librarian/SeatStatusReportManage/SeatStatusReportManage";
import Attendance from "../pages/kiosk/AttendanceWaitingScreen";
import SlideshowManagement from "../components/admin/kiosk_managements/SlideshowManagement";
import SlideshowPreview from "../components/admin/kiosk_managements/SlideshowPreview";


function LibrarianRoutes() {
  return (
    <Routes>
      {/* Librarian Layout with Sidebar */}
      <Route element={<MainLayout />}>
        {/* Dashboard */}
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />

        {/* Giám sát */}
        <Route path="checkinout" element={<CheckInOut />} />
        <Route path="seatmanage" element={<LibrarianAreas />} />

        {/* Quản lý */}
        <Route path="bookings" element={<BookingManage />} />
        <Route path="students" element={<StudentsManage />} />

        {/* Xử lý */}
        <Route path="violation" element={<ViolationManage />} />
        <Route path="support-requests" element={<SupportRequestManage />} />
        <Route path="complaints" element={<ComplaintManage />} />
        <Route path="seat-status-reports" element={<SeatStatusReportManage />} />
        <Route path="feedback" element={<FeedbackManage />} />

        {/* Trò chuyện */}
        <Route path="chat" element={<ChatManage />} />

        {/* Thống kê */}
        <Route path="statistic" element={<Statistic />} />

        {/* Nội dung */}
        <Route path="news" element={<NotificationManage />} />
        <Route path="news/create" element={<NewCreate />} />
        <Route path="news/edit/:id" element={<NewCreate />} />
        <Route path="news/view/:id" element={<NewsDetailView />} />

        {/* Quản lý Kiosk */}
        <Route path="slideshow-management" element={<SlideshowManagement />} />
        <Route path="slideshow-preview" element={<SlideshowPreview />} />


        {/* Cài đặt tài khoản */}
        <Route path="settings" element={<AccountSettings />} />
        <Route path="setting" element={<Navigate to="/librarian/settings" replace />} />
      </Route>

      {/* Check In/Out */}
      <Route path="attendance" element={<Attendance />} />

      {/* Redirect any unmatched routes */}
      <Route path="*" element={<Navigate to="dashboard" replace />} />
    </Routes>
  );
}

export default LibrarianRoutes;

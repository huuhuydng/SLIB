import { Routes, Route, Navigate } from "react-router-dom";

// Layout
import MainLayout from "../layouts/admin/MainLayout";

// Pages - Admin Dashboard
import Dashboard from "../pages/admin/Dashboard/Dashboard";
import AreaManagement from "../pages/admin/AreaManagement/AreaManagement";

// Admin-specific Components
import UserManagement from "../pages/admin/UserManagement/UserManagement";
import DeviceManagement from "../pages/admin/DeviceManagement/DeviceManagement";
import SystemConfig from "../pages/admin/SystemConfig/SystemConfig";
import SystemHealth from "../pages/admin/SystemHealth/SystemHealth";
import AIConfig from "../pages/admin/AIConfig/AIConfig";
import AccountSettings from "../components/AccountSettings";

function AdminRoutes() {
  return (
    <Routes>
      {/* Admin Layout with Sidebar */}
      <Route element={<MainLayout />}>
        {/* Dashboard */}
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />

        {/* Quản lý bản đồ thư viện */}
        <Route path="library-map" element={<AreaManagement />} />

        {/* Quản lý người dùng */}
        <Route path="users" element={<UserManagement />} />

        {/* Quản lý thiết bị */}
        <Route path="devices" element={<DeviceManagement />} />

        {/* Hệ thống */}
        <Route path="config" element={<SystemConfig />} />
        <Route path="health" element={<SystemHealth />} />
        <Route path="ai-config" element={<AIConfig />} />

        {/* Cài đặt tài khoản */}
        <Route path="settings" element={<AccountSettings />} />
        <Route path="setting" element={<Navigate to="/admin/settings" replace />} />
      </Route>

      {/* Redirect any unmatched routes */}
      <Route path="*" element={<Navigate to="dashboard" replace />} />
    </Routes>
  );
}

export default AdminRoutes;


import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

// Layout
import MainLayout from "../layouts/admin/area_management/MainLayOut";

// Pages - Admin Dashboard
import Dashboard from "../pages/admin/AreaManagement/dashboard/Dashboard";
import AreaManagement from "../pages/admin/AreaManagement/AreaManagement";

// Admin-specific Components
import UserManagement from "../components/UserManagement";
import DeviceManagement from "../components/DeviceManagement";
import SystemConfig from "../components/SystemConfig";
import SystemHealth from "../components/SystemHealth";
import AIConfig from "../components/AIConfig";

function AppRoutes() {
  const isLoggedIn = true;
  const userRole = "admin"; // Should come from auth context

  if (!isLoggedIn) {
    return <Navigate to="/login" />;
  }

  return (
    <BrowserRouter>
      <Routes>
        {/* Admin Layout with Sidebar */}
        <Route element={<MainLayout />}>
          {/* Dashboard */}
          <Route path="/dashboard" element={<Dashboard />} />
          
          {/* Quản lý thư viện */}
          <Route path="/library-map" element={<AreaManagement />} />
          
          {/* Quản lý người dùng */}
          <Route path="/users" element={<UserManagement />} />
          
          {/* Quản lý thiết bị */}
          <Route path="/devices" element={<DeviceManagement />} />

          {/* Hệ thống */}
          <Route path="/config" element={<SystemConfig />} />
          <Route path="/health" element={<SystemHealth />} />
          <Route path="/ai-config" element={<AIConfig />} />
        </Route>

        {/* Default Redirects */}
        <Route path="/" element={<Navigate to="/dashboard" />} />
        <Route path="*" element={<Navigate to="/dashboard" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default AppRoutes;

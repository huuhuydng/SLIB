import { Outlet } from "react-router-dom";
import Sidebar from "../../components/sidebar_admin/Sidebar_admin";
import "../../styles/admin/MainLayout.css";

function MainLayout() {
  return (
    <div className="appLayout">
      <Sidebar />

      <div className="main">
        <Outlet />
      </div>
    </div>
  );
}

export default MainLayout;

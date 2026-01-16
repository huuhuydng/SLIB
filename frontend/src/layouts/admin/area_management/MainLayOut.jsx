import { Outlet } from "react-router-dom";
import Sidebar from "../../../components/sidebar_default/Sidebar_default";

function MainLayout() {
  return (
    <div className="appLayout">
      <Sidebar />

      <div
        className="main"
      >
        <Outlet />
      </div>
    </div>
  );
}

export default MainLayout;

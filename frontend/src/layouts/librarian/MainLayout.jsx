import { Outlet } from "react-router-dom";
import Sidebar from "../../components/sidebar_librarian/Sidebar_librarian";
import "../../styles/librarian/MainLayout.css";

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

import { LayoutProvider, useLayout } from "../../../context/admin/area_management/LayoutContext";
import Sidebar from "../../../components/admin/area_managements/Sidebar";
import CanvasBoard from "../../../components/admin/area_managements/CanvasBoard";
import PropertiesPanel from "../../../components/admin/area_managements/PropertiesPanel";
import "../../../styles/admin/layout.css";

function AreaManagementContent() {
  const { state } = useLayout();
  const { isCanvasFullscreen } = state;

  return (
    <div className="app-layout library-map-page">
      {!isCanvasFullscreen && <Sidebar />}
      <CanvasBoard />
      {!isCanvasFullscreen && <PropertiesPanel />}
    </div>
  );
}

function AreaManagement() {
  return (
    <LayoutProvider>
      <AreaManagementContent />
    </LayoutProvider>
  );
}

export default AreaManagement;

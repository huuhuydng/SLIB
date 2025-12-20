import Sidebar from "./components/dashboard/Sidebar";
import Dashboard from "./components/dashboard/Dashboard";
import "./styles/dashboard.css";

function App() {
  return (
    <div className="appLayout">
      <Sidebar />
      <div className="dashboard">
        <Dashboard />
      </div>
    </div>
  );
}

export default App;

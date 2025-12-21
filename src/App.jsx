import React from "react";
import Sidebar from "./components/dashboard/Sidebar";
import Statistic from "./components/statistic/Statistic";
import "./styles/Statistic.css";

export default function App() {
  return (
    <div className="appLayout">
      <Sidebar />
      <div className="appMain">
        <Statistic />
      </div>
    </div>
  );
}

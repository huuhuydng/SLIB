import React from "react";
import Sidebar from "./components/dashboard/Sidebar";
import ViolationManage from "./components/violation/ViolationManage";

// CSS layout chung (đang dùng cho sidebar + main)
import "./styles/dashboard.css";

export default function App() {
  return (
    <div className="appLayout">
      <Sidebar />
      <main className="main">
        <ViolationManage />
      </main>
    </div>
  );
}
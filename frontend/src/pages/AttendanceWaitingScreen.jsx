import React, { useState, useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import "../styles/Attendance.css";
import logoFpt from '../assets/fpt_logo.png';

let stompClient = null;

const Attendance = () => {
  const [logs, setLogs] = useState([]);
  const [latestActivity, setLatestActivity] = useState(null);

  // Định dạng chỉ lấy Giờ:Phút:Giây
  const formatTimeOnly = (dateStr) => {
    if (!dateStr) return "";
    const d = new Date(dateStr);
    return d.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
  };

  useEffect(() => {
    let isMounted = true;
    let connectTimeout;

    const fetchInitialLogs = async () => {
      try {
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/slib/hce/latest-logs`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const data = await response.json();
        const historyLogs = Array.isArray(data) ? data.slice(0, 10).map((item, index) => ({
          id: `history-${index}-${Date.now()}`,
          name: item.fullName,
          code: item.userCode,
          action: item.type === 'CHECK_IN' ? 'Check in' : 'Check out',
          zone: item.deviceId || 'GATE_01',
          time: formatTimeOnly(item.time)
        })) : [];

        if (isMounted) setLogs(historyLogs);
      } catch (err) {
        console.error("Lỗi fetch:", err);
        if (isMounted) setLogs([]);
      }
    };

    const connect = () => {
      if (stompClient?.connected) stompClient.disconnect();
      const Sock = new SockJS(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/ws`);
      stompClient = Stomp.over(Sock);
      stompClient.debug = null;

      stompClient.activate();
    };

    const onConnected = () => {
      stompClient.subscribe('/topic/access-logs', (payload) => {
        if (!isMounted) return;
        const data = JSON.parse(payload.body);
        const rawTime = data.type === 'CHECK_OUT' ? (data.checkOutTime || data.time) : (data.checkInTime || data.time);

        const newEntry = {
          id: Date.now(),
          name: data.fullName,
          code: data.userCode,
          action: data.type === 'CHECK_IN' ? 'Check in' : 'Check out',
          zone: data.deviceId || 'GATE_01',
          time: formatTimeOnly(rawTime)
        };

        setLatestActivity(newEntry);
        setTimeout(() => { if (isMounted) setLatestActivity(null); }, 5000);

        // Luôn giữ tối đa 10 bản ghi mới nhất
        setLogs(prevLogs => [newEntry, ...prevLogs].slice(0, 10));
      });
    };

    fetchInitialLogs();
    connect();

    return () => {
      isMounted = false;
      if (connectTimeout) clearTimeout(connectTimeout);
      if (stompClient?.active) stompClient.deactivate();
    };
  }, []);

  return (
    <div className="waiting-screen">
      {/* POPUP KHI CÓ HOẠT ĐỘNG MỚI */}
      {latestActivity && (
        <div className={`activity-overlay ${latestActivity.action === 'Check in' ? 'is-in' : 'is-out'}`}>
          <div className="overlay-card">
            <div className="status-label">{latestActivity.action.toUpperCase()}</div>
            <h1 className="student-name">{latestActivity.name}</h1>
            <h2 className="student-id">{latestActivity.code}</h2>
            <div className="zone-info">Vị trí: {latestActivity.zone}</div>
          </div>
        </div>
      )}

      <div className="monitor-header">
        <div className="logo-container">
          <img src={logoFpt} alt="FPT Logo" className="fpt-logo" />
        </div>
        <h1 className="monitor-title">
          DANH SÁCH SINH VIÊN <span className="text-out">RA</span>/<span className="text-in">VÀO</span> THƯ VIỆN
        </h1>
      </div>

      <div className="monitor-content">
        <div className="logs-table-header">
          <span className="col-name">HỌ VÀ TÊN</span>
          <span className="col-code">MÃ SINH VIÊN</span>
          <span className="col-action">HÀNH ĐỘNG</span>
          <span className="col-zone">VỊ TRÍ</span>
          <span className="col-time">THỜI GIAN</span>
        </div>

        <div className="logs-table-body no-scroll">
          {logs.map((log) => (
            <div key={log.id} className="logs-row-item">
              <div className="col-name txt-name">{log.name}</div>
              <div className="col-code txt-code">{log.code}</div>
              <div className="col-action">
                <span className={`status-badge ${log.action === 'Check in' ? 'in' : 'out'}`}>
                  {log.action}
                </span>
              </div>
              <div className="col-zone">{log.zone}</div>
              <div className="col-time">
                <span className="time-text">{log.time}</span>
              </div>
            </div>
          ))}

          {logs.length === 0 && (
            <div className="loading-state">Đang đợi dữ liệu...</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Attendance;
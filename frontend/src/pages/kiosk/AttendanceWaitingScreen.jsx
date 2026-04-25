import React, { useState, useEffect } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { API_BASE_URL } from '../../config/apiConfig';
import kioskService from '../../services/kiosk/kioskService';
import "../../styles/Attendance.css";
import logoFpt from '../../assets/fpt_logo.png';

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

  const getLocalDateKey = (dateValue = new Date()) => {
    const d = dateValue instanceof Date ? dateValue : new Date(dateValue);
    if (Number.isNaN(d.getTime())) return '';

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const isToday = (dateStr) => getLocalDateKey(dateStr) === getLocalDateKey();

  useEffect(() => {
    let isMounted = true;
    let connectTimeout = null;
    let activeDateKey = getLocalDateKey();
    let dayRolloverInterval = null;

    const scheduleReconnect = () => {
      if (!isMounted || connectTimeout) {
        return;
      }

      connectTimeout = setTimeout(() => {
        connectTimeout = null;
        connect();
      }, 10000);
    };

    const fetchInitialLogs = async () => {
      try {
        const data = await kioskService.getRecentLogs(10);
        const historyLogs = Array.isArray(data) ? data
          .filter(item => isToday(item.time))
          .slice(0, 10)
          .map((item, index) => ({
            id: item.id || `history-${index}-${Date.now()}`,
            name: item.fullName,
            code: item.userCode,
            action: item.action === 'CHECK_IN' ? 'Vào' : 'Ra',
            zone: item.deviceName || item.deviceId || 'Cổng thư viện',
            rawTime: item.time,
            time: formatTimeOnly(item.time)
          })) : [];

        if (isMounted) setLogs(historyLogs);
      } catch (err) {
        console.error("Lỗi fetch:", err);
        if (isMounted) setLogs([]);
      }
    };

    const connect = () => {
      const authToken =
        localStorage.getItem('kiosk_device_token') ||
        sessionStorage.getItem('kiosk_device_token') ||
        sessionStorage.getItem('librarian_token') ||
        localStorage.getItem('librarian_token');

      if (stompClient) {
        stompClient.deactivate();
      }

      stompClient = new Client({
        webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
        connectHeaders: authToken ? { Authorization: `Bearer ${authToken}` } : {},
        reconnectDelay: 0,
        debug: () => {},
      });

      stompClient.onConnect = () => {
        if (isMounted) onConnected();
      };

      stompClient.onStompError = () => {
        scheduleReconnect();
      };

      stompClient.onWebSocketClose = () => {
        scheduleReconnect();
      };

      stompClient.activate();
    };

    const onConnected = () => {
      stompClient.subscribe('/topic/access-logs', (payload) => {
        if (!isMounted) return;
        const data = JSON.parse(payload.body);
        const rawTime = data.type === 'CHECK_OUT' ? (data.checkOutTime || data.time) : (data.checkInTime || data.time);
        if (!isToday(rawTime)) return;

        const newEntry = {
          id: data.id ? `${data.id}-${data.type}` : `${Date.now()}-${data.type || data.action || 'access'}`,
          name: data.fullName,
          code: data.userCode,
          action: data.type === 'CHECK_IN' ? 'Vào' : 'Ra',
          zone: data.deviceName || data.deviceId || 'Cổng thư viện',
          rawTime,
          time: formatTimeOnly(rawTime)
        };

        setLatestActivity(newEntry);
        setTimeout(() => { if (isMounted) setLatestActivity(null); }, 5000);

        // Luôn giữ tối đa 10 bản ghi mới nhất
        setLogs(prevLogs => {
          if (prevLogs.some(log => log.id === newEntry.id)) {
            return prevLogs;
          }

          return [newEntry, ...prevLogs]
            .filter(log => isToday(log.rawTime))
            .slice(0, 10);
        });
      });
    };

    fetchInitialLogs();
    connect();
    dayRolloverInterval = setInterval(() => {
      const todayKey = getLocalDateKey();
      if (todayKey !== activeDateKey) {
        activeDateKey = todayKey;
        setLatestActivity(null);
        fetchInitialLogs();
      }
    }, 60000);

    return () => {
      isMounted = false;
      if (connectTimeout) clearTimeout(connectTimeout);
      if (dayRolloverInterval) clearInterval(dayRolloverInterval);
      if (stompClient) {
        stompClient.deactivate();
      }
    };
  }, []);

  return (
    <div className="waiting-screen">
      {/* POPUP KHI CÓ HOẠT ĐỘNG MỚI */}
      {latestActivity && (
        <div className={`activity-overlay ${latestActivity.action === 'Vào' ? 'is-in' : 'is-out'}`}>
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
                <span className={`status-badge ${log.action === 'Vào' ? 'in' : 'out'}`}>
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

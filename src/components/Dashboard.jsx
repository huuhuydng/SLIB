import React, { useMemo, useState } from "react";
import { Users, Armchair, AlertCircle, Sparkles, Clock, Bell, Calendar, ChevronRight, Wrench, BookOpen } from "lucide-react";
import StatCard from "./StatCard";
import Header from "./Header";
import { getLibraryInsights } from "../services/geminiService.jsx";

import "../styles/Dashboard.css";

const MOCK_STUDENTS = [
  { id: "1", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check in", time: "12:21:10", date: "15/12/2025" },
  { id: "2", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check out", time: "12:21:10", date: "15/12/2025" },
  { id: "3", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check out", time: "12:21:10", date: "15/12/2025" },
  { id: "4", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check out", time: "12:21:10", date: "15/12/2025" },
  { id: "5", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check out", time: "12:21:10", date: "15/12/2025" },
];

const MOCK_NOTIFICATIONS = [
  { 
    title: "FPT Techday 2025: Công nghệ tương lai",
    date: "12/12/2025",
    type: "event",
    tag: "SỰ KIỆN"
  },
  { 
    title: "Thông báo bảo trì khu vực thư viện",
    date: "10/12/2025",
    type: "maintenance",
    tag: "QUAN TRỌNG"
  },
  { 
    title: "Top 100 đầu sách AI mới về thư viện",
    date: "08/12/2025",
    type: "info",
    tag: "SÁCH MỚI"
  },
];

const AREA = [
  { name: "Khu yên tĩnh", percentage: 95 },
  { name: "Khu thảo luận", percentage: 45 },
  { name: "Khu tự học", percentage: 70 },
];

const DASHBOARD_STATS = { currentUsers: 69, occupancyRate: 69, violations: 9 };

const Dashboard = () => {
  const [searchText, setSearchText] = useState("");
  const [insights, setInsights] = useState([]);

  React.useEffect(() => {
    (async () => {
      try {
        const data = await getLibraryInsights(DASHBOARD_STATS);
        setInsights(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error(e);
        setInsights([]);
      }
    })();
  }, []);

  const filteredStudents = useMemo(() => {
    const q = searchText.trim().toLowerCase();
    if (!q) return MOCK_STUDENTS;
    return MOCK_STUDENTS.filter((s) => {
      return (
        s.name.toLowerCase().includes(q) ||
        s.studentId.toLowerCase().includes(q) ||
        s.action.toLowerCase().includes(q)
      );
    });
  }, [searchText]);

  const fillClass = (p) => (p >= 90 ? "fillRed" : p >= 60 ? "fillYellow" : "fillGreen");

  return (
    <>
      <Header 
        searchValue={searchText}
        onSearchChange={(e) => setSearchText(e.target.value)}
        searchPlaceholder="Search for anything..."
      />

      <div style={{
        padding: '2rem',
        maxWidth: '1400px',
        margin: '0 auto',
        backgroundColor: '#f9fafb',
        minHeight: 'calc(100vh - 80px)'
      }}>
      <div className="h1">Dashboard</div>

      {/* stats */}
      <div className="statsRow" style={{ marginBottom: '24px' }}>
        <StatCard
          icon={<Users size={20} />}
          value={DASHBOARD_STATS.currentUsers}
          label="Đang trong thư viện"
          bg="#EDE9FE"
          color="#7C3AED"
        />
        <StatCard
          icon={<Armchair size={20} />}
          value={`${DASHBOARD_STATS.occupancyRate}%`}
          label="Chỗ ngồi đã có người"
          bg="#DCFCE7"
          color="#16A34A"
        />
        <StatCard
          icon={<AlertCircle size={20} />}
          value={`0${DASHBOARD_STATS.violations}`}
          label="Vi phạm xảy ra hôm nay"
          bg="#FEE2E2"
          color="#EF4444"
        />
      </div>

      {/* middle */}
      <div style={{ marginBottom: '24px', display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '18px', alignItems: 'start' }}>
        <section style={{ background: '#fff', borderRadius: '18px', padding: '24px', boxShadow: '0 6px 16px rgba(17,24,39,.06)', overflow: 'visible' }}>
          <h3 style={{ fontSize: '14px', fontWeight: '900', color: '#111827', margin: '0 0 20px 0', lineHeight: '1.4' }}>Danh sách sinh viên ra vào</h3>
          
          <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #e5e7eb' }}>
                <th style={{ textAlign: 'left', padding: '12px 12px', fontSize: '13px', fontWeight: '700', color: '#6b7280', width: '25%' }}>Tên sinh viên</th>
                <th style={{ textAlign: 'left', padding: '12px 12px', fontSize: '13px', fontWeight: '700', color: '#6b7280', width: '20%' }}>Mã số sinh viên</th>
                <th style={{ textAlign: 'left', padding: '12px 12px', fontSize: '13px', fontWeight: '700', color: '#6b7280', width: '20%' }}>Hành động</th>
                <th style={{ textAlign: 'left', padding: '12px 12px', fontSize: '13px', fontWeight: '700', color: '#6b7280', width: '35%' }}>Thời gian</th>
              </tr>
            </thead>

            <tbody>
              {filteredStudents.map((s, index) => (
                <tr key={s.id} style={{ borderBottom: index === filteredStudents.length - 1 ? 'none' : '1px solid #f1f5f9' }}>
                  <td style={{ padding: '14px 12px', fontSize: '13px', color: '#111827', fontWeight: '500' }}>{s.name}</td>
                  <td style={{ padding: '14px 12px', fontSize: '13px', color: '#6b7280', fontWeight: '600' }}>{s.studentId}</td>
                  <td style={{ padding: '14px 12px' }}>
                    <span className={`badge ${s.action === "Check in" ? "badgeIn" : "badgeOut"}`} style={{ 
                      padding: '6px 14px', 
                      borderRadius: '6px', 
                      fontSize: '12px', 
                      fontWeight: '700',
                      display: 'inline-block'
                    }}>
                      {s.action}
                    </span>
                  </td>
                  <td style={{ padding: '14px 12px', fontSize: '13px', color: '#6b7280', fontWeight: '500' }}>
                    {s.time} {s.date}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section style={{ background: '#fff', borderRadius: '18px', padding: '24px', boxShadow: '0 6px 16px rgba(17,24,39,.06)', height: '100%', overflow: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
            <Sparkles size={18} color="#f59e0b" />
            <h3 style={{ fontSize: '14px', fontWeight: '900', color: '#111827', margin: 0 }}>AI phân tích</h3>
          </div>
          
          {(insights || []).map((it, idx) => (
            <div
              key={idx}
              style={{
                background: it.type === "warning" ? "#fff7ed" : "#eff6ff",
                border: `1px solid ${it.type === "warning" ? "#fed7aa" : "#bfdbfe"}`,
                borderRadius: '12px',
                padding: '16px',
                marginBottom: idx === insights.length - 1 ? '0' : '12px',
                display: 'flex',
                gap: '12px'
              }}
            >
              <div style={{ paddingTop: '2px', flexShrink: 0 }}>
                {it.type === "warning" ? 
                  <AlertCircle size={18} color="#f59e0b" /> : 
                  <Clock size={18} color="#3b82f6" />
                }
              </div>
              <div>
                <p style={{ fontSize: '13px', fontWeight: '900', color: '#111827', margin: '0 0 6px 0' }}>
                  {it.title}
                </p>
                <p style={{ fontSize: '12px', color: '#374151', margin: 0, lineHeight: '1.5' }}>
                  {it.message}
                </p>
              </div>
            </div>
          ))}
        </section>
      </div>

      {/* bottom */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '18px', marginTop: '24px' }}>
        <section style={{ background: '#fff', borderRadius: '18px', padding: '20px', boxShadow: '0 6px 16px rgba(17,24,39,.06)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <h3 style={{ fontSize: '14px', fontWeight: '900', color: '#111827', margin: 0 }}>Thông báo gần đây</h3>
            <Bell size={16} color="#6b7280" />
          </div>
          
          {MOCK_NOTIFICATIONS.map((notification, idx) => {
            // Determine icon based on notification type
            const getNotificationIcon = (type) => {
              switch(type) {
                case 'event':
                  return <Calendar size={16} />;
                case 'maintenance':
                case 'warning':
                  return <Wrench size={16} />;
                case 'info':
                case 'reminder':
                  return <BookOpen size={16} />;
                default:
                  return <Bell size={16} />;
              }
            };

            return (
            <div 
              key={idx} 
              style={{ 
                padding: '14px 0',
                borderBottom: idx === MOCK_NOTIFICATIONS.length - 1 ? 'none' : '1px solid #f1f5f9',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                display: 'flex',
                alignItems: 'flex-start',
                gap: '12px'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = '#f9fafb';
                e.currentTarget.style.marginLeft = '-20px';
                e.currentTarget.style.marginRight = '-20px';
                e.currentTarget.style.paddingLeft = '20px';
                e.currentTarget.style.paddingRight = '20px';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.style.marginLeft = '0';
                e.currentTarget.style.marginRight = '0';
                e.currentTarget.style.paddingLeft = '0';
                e.currentTarget.style.paddingRight = '0';
              }}
            >
              <div style={{ 
                padding: '10px', 
                borderRadius: '12px', 
                background: notification.type === 'warning' ? '#fef3c7' : 
                           notification.type === 'event' ? '#dbeafe' : 
                           notification.type === 'maintenance' ? '#fee2e2' :
                           notification.type === 'info' ? '#dcfce7' : '#e0e7ff',
                flexShrink: 0,
                marginTop: '2px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <div style={{ color: 
                  notification.type === 'warning' ? '#f59e0b' : 
                  notification.type === 'event' ? '#3b82f6' : 
                  notification.type === 'maintenance' ? '#ef4444' :
                  notification.type === 'info' ? '#22c55e' : '#6366f1'
                }}>
                  {getNotificationIcon(notification.type)}
                </div>
              </div>
              
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                  <span style={{ 
                    fontSize: '10px', 
                    fontWeight: '700',
                    color: notification.type === 'event' ? '#3b82f6' : 
                           notification.type === 'maintenance' ? '#ef4444' : '#22c55e',
                    background: notification.type === 'event' ? '#dbeafe' : 
                                notification.type === 'maintenance' ? '#fee2e2' : '#dcfce7',
                    padding: '3px 8px',
                    borderRadius: '4px',
                    letterSpacing: '0.5px'
                  }}>
                    {notification.tag}
                  </span>
                </div>
                <p style={{ 
                  fontSize: '13px', 
                  color: '#111827', 
                  margin: '0 0 4px 0', 
                  fontWeight: '600',
                  lineHeight: '1.5',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  display: '-webkit-box',
                  WebkitLineClamp: 2,
                  WebkitBoxOrient: 'vertical'
                }}>
                  {notification.title}
                </p>
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Calendar size={11} color="#9ca3af" />
                  <span style={{ fontSize: '11px', color: '#9ca3af', fontWeight: '500' }}>
                    {notification.date}
                  </span>
                </div>
              </div>
              
              <ChevronRight size={16} color="#d1d5db" style={{ flexShrink: 0, marginTop: '8px' }} />
            </div>
            );
          })}
        </section>

        <section style={{ background: '#fff', borderRadius: '18px', padding: '20px', boxShadow: '0 6px 16px rgba(17,24,39,.06)' }}>
          <h3 style={{ fontSize: '14px', fontWeight: '900', color: '#111827', margin: '0 0 16px 0' }}>Trạng thái khu vực</h3>

          <div style={{ display: 'flex', alignItems: 'center', gap: '20px', fontSize: '11px', color: '#6b7280', fontWeight: '700', marginBottom: '16px' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <i style={{ width: '8px', height: '8px', borderRadius: '999px', background: '#22c55e', display: 'inline-block' }} />
              Trống
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <i style={{ width: '8px', height: '8px', borderRadius: '999px', background: '#fbbf24', display: 'inline-block' }} />
              Khá đông
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <i style={{ width: '8px', height: '8px', borderRadius: '999px', background: '#ef4444', display: 'inline-block' }} />
              Full
            </span>
          </div>

          {AREA.map((a, idx) => (
            <div key={idx} style={{ marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '12px', fontWeight: '800', marginBottom: '8px' }}>
                <span style={{ color: '#111827' }}>{a.name}</span>
                <span style={{ color: '#6b7280' }}>{a.percentage}%</span>
              </div>
              <div style={{ height: '8px', background: '#e5e7eb', borderRadius: '999px', overflow: 'hidden' }}>
                <div style={{ 
                  height: '100%', 
                  width: `${a.percentage}%`, 
                  background: a.percentage >= 90 ? '#ef4444' : a.percentage >= 60 ? '#fbbf24' : '#22c55e',
                  borderRadius: '999px'
                }} />
              </div>
            </div>
          ))}
        </section>
      </div>
      </div>
    </>
  );
};

export default Dashboard;
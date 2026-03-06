import React, { useMemo, useState } from "react";
import {
  Users,
  Armchair,
  AlertCircle,
  Sparkles,
  Clock,
  Bell,
  Calendar,
  ChevronRight,
  Wrench,
  BookOpen,
  MapPin,
  Activity
} from "lucide-react";
import StatCard from "./StatCard";
import Header from "./Header";
import { getLibraryInsights } from "../../../../services/geminiService";
import "../../../../styles/Dashboard.css";

// Mock Data
const MOCK_STUDENTS = [
  { id: "1", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check in", time: "12:21:10", date: "15/12/2025", avatar: "NP" },
  { id: "2", name: "Trần Văn An", studentId: "DE170707", action: "Check out", time: "12:15:30", date: "15/12/2025", avatar: "TA" },
  { id: "3", name: "Lê Thị Bình", studentId: "DE170708", action: "Check out", time: "11:45:20", date: "15/12/2025", avatar: "LB" },
  { id: "4", name: "Phạm Minh Cường", studentId: "DE170709", action: "Check in", time: "11:30:00", date: "15/12/2025", avatar: "PC" },
  { id: "5", name: "Đỗ Hải Đăng", studentId: "DE170710", action: "Check out", time: "10:55:45", date: "15/12/2025", avatar: "ĐD" },
];

const MOCK_NOTIFICATIONS = [
  { title: "FPT Techday 2025: Công nghệ tương lai", date: "12/12/2025", type: "event", tag: "SỰ KIỆN" },
  { title: "Thông báo bảo trì khu vực thư viện", date: "10/12/2025", type: "maintenance", tag: "QUAN TRỌNG" },
  { title: "Top 100 đầu sách AI mới về thư viện", date: "08/12/2025", type: "info", tag: "SÁCH MỚI" },
];

const AREAS = [
  { name: "Khu yên tĩnh", percentage: 95, icon: "🤫" },
  { name: "Khu thảo luận", percentage: 45, icon: "💬" },
  { name: "Khu tự học", percentage: 70, icon: "📚" },
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
    return MOCK_STUDENTS.filter((s) =>
      s.name.toLowerCase().includes(q) || s.studentId.toLowerCase().includes(q) || s.action.toLowerCase().includes(q)
    );
  }, [searchText]);

  const getProgressColor = (percentage) => {
    if (percentage >= 90) return { bar: '#D32F2F', bg: '#FFEBEE' };
    if (percentage >= 60) return { bar: '#FF9800', bg: '#FFF3E0' };
    return { bar: '#4CA75B', bg: '#E8F5E9' };
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'event': return <Calendar size={18} />;
      case 'maintenance': return <Wrench size={18} />;
      case 'info': return <BookOpen size={18} />;
      default: return <Bell size={18} />;
    }
  };

  const getNotificationColors = (type) => {
    switch (type) {
      case 'event': return { bg: '#E3F2FD', color: '#0054A6', border: '#BBDEFB' };
      case 'maintenance': return { bg: '#FFEBEE', color: '#D32F2F', border: '#FFCDD2' };
      case 'info': return { bg: '#E8F5E9', color: '#388E3C', border: '#C8E6C9' };
      default: return { bg: '#F7FAFC', color: '#4A5568', border: '#E2E8F0' };
    }
  };

  return (
    <>
      <Header
        searchValue={searchText}
        onSearchChange={(e) => setSearchText(e.target.value)}
        searchPlaceholder="Tìm kiếm sinh viên, mã số..."
      />

      <div style={{
        padding: '0 24px 32px',
        maxWidth: '1440px',
        margin: '0 auto',
        minHeight: 'calc(100vh - 120px)'
      }}>
        {/* Page Title */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '24px'
        }}>
          <div>
            <h1 style={{
              fontSize: '20px',
              fontWeight: '600',
              color: 'var(--slib-text-primary, #1A1A1A)',
              margin: '0 0 4px 0'
            }}>Tổng quan</h1>
            <p style={{
              fontSize: '14px',
              color: 'var(--slib-text-muted, #A0AEC0)',
              margin: 0
            }}>Xin chào! Đây là tổng quan hoạt động thư viện hôm nay.</p>
          </div>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 16px',
            background: 'var(--slib-bg-card, #ffffff)',
            borderRadius: '12px',
            boxShadow: 'var(--slib-shadow-sm)'
          }}>
            <Activity size={18} color="var(--slib-primary, #FF751F)" />
            <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--slib-text-secondary, #4A5568)' }}>
              Cập nhật: {new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
            </span>
          </div>
        </div>

        {/* Stats Cards */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '20px',
          marginBottom: '24px'
        }}>
          <StatCard
            icon={<Users size={24} />}
            value={DASHBOARD_STATS.currentUsers}
            label="Sinh viên trong thư viện"
            bg="#F3E8FF"
            color="#7C3AED"
            trend="up"
            trendValue="+12% so với hôm qua"
          />
          <StatCard
            icon={<Armchair size={24} />}
            value={`${DASHBOARD_STATS.occupancyRate}%`}
            label="Tỷ lệ chỗ ngồi đã sử dụng"
            bg="#E8F5E9"
            color="#388E3C"
            trend="neutral"
            trendValue="Ổn định"
          />
          <StatCard
            icon={<AlertCircle size={24} />}
            value={DASHBOARD_STATS.violations}
            label="Vi phạm xảy ra hôm nay"
            bg="#FFEBEE"
            color="#D32F2F"
            trend="down"
            trendValue="-3 so với tuần trước"
          />
        </div>

        {/* Main Content Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1.5fr 1fr',
          gap: '20px',
          marginBottom: '24px'
        }}>
          {/* Student List Table */}
          <div style={{
            background: 'var(--slib-bg-card, #ffffff)',
            borderRadius: '10px',
            boxShadow: 'var(--slib-shadow-card)',
            overflow: 'hidden'
          }}>
            <div style={{
              padding: '20px 24px',
              borderBottom: '1px solid var(--slib-border-light, #E2E8F0)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div style={{
                  width: '40px',
                  height: '40px',
                  borderRadius: '10px',
                  background: 'var(--slib-primary-subtle, #FFF7F2)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <Users size={20} color="var(--slib-primary, #FF751F)" />
                </div>
                <div>
                  <h3 style={{ fontSize: '16px', fontWeight: '600', color: 'var(--slib-text-primary, #1A1A1A)', margin: 0 }}>
                    Hoạt động ra vào
                  </h3>
                  <p style={{ fontSize: '12px', color: 'var(--slib-text-muted, #A0AEC0)', margin: 0 }}>
                    Cập nhật theo thời gian thực
                  </p>
                </div>
              </div>
              <button style={{
                padding: '8px 16px',
                background: 'var(--slib-bg-main, #F7FAFC)',
                border: 'none',
                borderRadius: '8px',
                fontSize: '13px',
                fontWeight: '500',
                color: 'var(--slib-text-secondary, #4A5568)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                transition: 'all 0.2s ease'
              }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = 'var(--slib-primary-subtle, #FFF7F2)';
                  e.currentTarget.style.color = 'var(--slib-primary, #FF751F)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'var(--slib-bg-main, #F7FAFC)';
                  e.currentTarget.style.color = 'var(--slib-text-secondary, #4A5568)';
                }}
              >
                Xem tất cả
                <ChevronRight size={16} />
              </button>
            </div>

            <div style={{ padding: '0 8px' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    {['Sinh viên', 'Mã số', 'Trạng thái', 'Thời gian'].map((header, idx) => (
                      <th key={idx} style={{
                        textAlign: idx === 2 ? 'center' : idx === 3 ? 'right' : 'left',
                        padding: '16px',
                        fontSize: '12px',
                        fontWeight: '600',
                        color: 'var(--slib-text-muted, #A0AEC0)',
                        textTransform: 'uppercase',
                        letterSpacing: '0.5px'
                      }}>{header}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filteredStudents.map((student, index) => (
                    <tr
                      key={student.id}
                      style={{
                        borderBottom: index === filteredStudents.length - 1 ? 'none' : '1px solid var(--slib-border-light, #E2E8F0)',
                        transition: 'background-color 0.2s ease',
                        cursor: 'pointer'
                      }}
                      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--slib-primary-subtle, #FFF7F2)'}
                      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                    >
                      <td style={{ padding: '16px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <div style={{
                            width: '36px',
                            height: '36px',
                            borderRadius: '10px',
                            background: 'linear-gradient(135deg, var(--slib-primary, #FF751F), var(--slib-primary-light, #FF9B5A))',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#fff',
                            fontSize: '12px',
                            fontWeight: '600'
                          }}>{student.avatar}</div>
                          <span style={{ fontSize: '14px', fontWeight: '500', color: 'var(--slib-text-primary, #1A1A1A)' }}>
                            {student.name}
                          </span>
                        </div>
                      </td>
                      <td style={{ padding: '16px' }}>
                        <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--slib-text-secondary, #4A5568)', fontFamily: 'monospace' }}>
                          {student.studentId}
                        </span>
                      </td>
                      <td style={{ padding: '16px', textAlign: 'center' }}>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          padding: '6px 12px',
                          borderRadius: '8px',
                          fontSize: '12px',
                          fontWeight: '600',
                          background: student.action === "Check in" ? 'var(--slib-status-success-bg, #E8F5E9)' : 'var(--slib-status-error-bg, #FFEBEE)',
                          color: student.action === "Check in" ? 'var(--slib-status-success, #388E3C)' : 'var(--slib-status-error, #D32F2F)'
                        }}>
                          <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'currentColor' }} />
                          {student.action}
                        </span>
                      </td>
                      <td style={{ padding: '16px', textAlign: 'right' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '2px' }}>
                          <span style={{ fontSize: '14px', fontWeight: '600', color: 'var(--slib-text-primary, #1A1A1A)' }}>{student.time}</span>
                          <span style={{ fontSize: '12px', color: 'var(--slib-text-muted, #A0AEC0)' }}>{student.date}</span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* AI Insights Panel */}
          <div style={{
            background: 'var(--slib-bg-card, #ffffff)',
            borderRadius: '10px',
            boxShadow: 'var(--slib-shadow-card)',
            padding: '24px',
            height: 'fit-content'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
              <div style={{
                width: '40px',
                height: '40px',
                borderRadius: '10px',
                background: 'linear-gradient(135deg, #FDB913 0%, #FF9800 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 4px 12px rgba(253, 185, 19, 0.3)'
              }}>
                <Sparkles size={20} color="#fff" />
              </div>
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: '600', color: 'var(--slib-text-primary, #1A1A1A)', margin: 0 }}>AI Phân tích</h3>
                <p style={{ fontSize: '12px', color: 'var(--slib-text-muted, #A0AEC0)', margin: 0 }}>Đề xuất thông minh</p>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {insights.length > 0 ? insights.map((insight, idx) => (
                <div
                  key={idx}
                  style={{
                    background: insight.type === "warning" ? 'var(--slib-status-warning-bg, #FFF3E0)' : 'var(--slib-status-info-bg, #E3F2FD)',
                    border: `1px solid ${insight.type === "warning" ? '#FFE0B2' : '#BBDEFB'}`,
                    borderRadius: '12px',
                    padding: '16px',
                    display: 'flex',
                    gap: '12px',
                    transition: 'transform 0.2s ease',
                    cursor: 'pointer'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.transform = 'translateX(4px)'}
                  onMouseLeave={(e) => e.currentTarget.style.transform = 'translateX(0)'}
                >
                  <div style={{
                    flexShrink: 0,
                    width: '32px',
                    height: '32px',
                    borderRadius: '8px',
                    background: insight.type === "warning" ? '#FF9800' : '#0054A6',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}>
                    {insight.type === "warning" ? <AlertCircle size={16} color="#fff" /> : <Clock size={16} color="#fff" />}
                  </div>
                  <div style={{ flex: 1 }}>
                    <p style={{ fontSize: '14px', fontWeight: '600', color: 'var(--slib-text-primary, #1A1A1A)', margin: '0 0 4px 0' }}>
                      {insight.title}
                    </p>
                    <p style={{ fontSize: '13px', color: 'var(--slib-text-secondary, #4A5568)', margin: 0, lineHeight: '1.5' }}>
                      {insight.message}
                    </p>
                  </div>
                </div>
              )) : (
                <div style={{ padding: '32px', textAlign: 'center', color: 'var(--slib-text-muted, #A0AEC0)', fontSize: '14px' }}>
                  <Sparkles size={32} style={{ marginBottom: '12px', opacity: 0.5 }} />
                  <p>Đang phân tích dữ liệu...</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Bottom Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          {/* Notifications Panel */}
          <div style={{
            background: 'var(--slib-bg-card, #ffffff)',
            borderRadius: '10px',
            boxShadow: 'var(--slib-shadow-card)',
            padding: '24px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div style={{
                  width: '40px',
                  height: '40px',
                  borderRadius: '10px',
                  background: 'var(--slib-status-info-bg, #E3F2FD)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <Bell size={20} color="var(--slib-accent-blue, #0054A6)" />
                </div>
                <h3 style={{ fontSize: '16px', fontWeight: '600', color: 'var(--slib-text-primary, #1A1A1A)', margin: 0 }}>
                  Thông báo gần đây
                </h3>
              </div>
              <span style={{
                padding: '4px 12px',
                background: 'var(--slib-primary, #FF751F)',
                color: '#fff',
                borderRadius: '10px',
                fontSize: '12px',
                fontWeight: '600'
              }}>{MOCK_NOTIFICATIONS.length} mới</span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {MOCK_NOTIFICATIONS.map((notification, idx) => {
                const colors = getNotificationColors(notification.type);
                return (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: '12px',
                      padding: '14px',
                      borderRadius: '12px',
                      border: '1px solid var(--slib-border-light, #E2E8F0)',
                      transition: 'all 0.2s ease',
                      cursor: 'pointer'
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = colors.border;
                      e.currentTarget.style.background = colors.bg;
                      e.currentTarget.style.transform = 'translateX(4px)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = 'var(--slib-border-light, #E2E8F0)';
                      e.currentTarget.style.background = 'transparent';
                      e.currentTarget.style.transform = 'translateX(0)';
                    }}
                  >
                    <div style={{
                      width: '36px',
                      height: '36px',
                      borderRadius: '10px',
                      background: colors.bg,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: colors.color,
                      flexShrink: 0
                    }}>{getNotificationIcon(notification.type)}</div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                        <span style={{
                          fontSize: '10px',
                          fontWeight: '600',
                          color: colors.color,
                          background: colors.bg,
                          padding: '3px 8px',
                          borderRadius: '4px',
                          letterSpacing: '0.5px'
                        }}>{notification.tag}</span>
                      </div>
                      <p style={{ fontSize: '14px', fontWeight: '500', color: 'var(--slib-text-primary, #1A1A1A)', margin: '0 0 6px 0', lineHeight: '1.4' }}>
                        {notification.title}
                      </p>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Calendar size={12} color="var(--slib-text-muted, #A0AEC0)" />
                        <span style={{ fontSize: '12px', color: 'var(--slib-text-muted, #A0AEC0)' }}>{notification.date}</span>
                      </div>
                    </div>
                    <ChevronRight size={18} color="var(--slib-text-muted, #A0AEC0)" style={{ flexShrink: 0 }} />
                  </div>
                );
              })}
            </div>
          </div>

          {/* Area Status Panel */}
          <div style={{
            background: 'var(--slib-bg-card, #ffffff)',
            borderRadius: '10px',
            boxShadow: 'var(--slib-shadow-card)',
            padding: '24px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
              <div style={{
                width: '40px',
                height: '40px',
                borderRadius: '10px',
                background: 'var(--slib-accent-green, #4CA75B)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 4px 12px rgba(76, 167, 91, 0.3)'
              }}>
                <MapPin size={20} color="#fff" />
              </div>
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: '600', color: 'var(--slib-text-primary, #1A1A1A)', margin: 0 }}>
                </h3>
                <p style={{ fontSize: '12px', color: 'var(--slib-text-muted, #A0AEC0)', margin: 0 }}>
                  Mức độ sử dụng theo thời gian thực
                </p>
              </div>
            </div>

            {/* Legend */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '20px',
              marginBottom: '20px',
              padding: '12px 16px',
              background: 'var(--slib-bg-main, #F7FAFC)',
              borderRadius: '10px'
            }}>
              {[
                { color: 'var(--slib-accent-green, #4CA75B)', label: 'Trống' },
                { color: 'var(--slib-accent-yellow, #FDB913)', label: 'Khá đông' },
                { color: 'var(--slib-status-error, #D32F2F)', label: 'Đầy' }
              ].map((item, idx) => (
                <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: item.color }} />
                  <span style={{ fontSize: '12px', color: 'var(--slib-text-secondary, #4A5568)', fontWeight: '500' }}>{item.label}</span>
                </div>
              ))}
            </div>

            {/* Area Progress Bars */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {AREAS.map((area, idx) => {
                const colors = getProgressColor(area.percentage);
                return (
                  <div
                    key={idx}
                    style={{
                      padding: '16px',
                      borderRadius: '12px',
                      border: '1px solid var(--slib-border-light, #E2E8F0)',
                      transition: 'all 0.2s ease'
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = colors.bar;
                      e.currentTarget.style.background = colors.bg;
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = 'var(--slib-border-light, #E2E8F0)';
                      e.currentTarget.style.background = 'transparent';
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <span style={{ fontSize: '18px' }}>{area.icon}</span>
                        <span style={{ fontSize: '14px', fontWeight: '600', color: 'var(--slib-text-primary, #1A1A1A)' }}>
                          {area.name}
                        </span>
                      </div>
                      <span style={{ fontSize: '16px', fontWeight: '600', color: colors.bar }}>{area.percentage}%</span>
                    </div>
                    <div style={{
                      height: '8px',
                      background: 'var(--slib-border-light, #E2E8F0)',
                      borderRadius: '100px',
                      overflow: 'hidden'
                    }}>
                      <div style={{
                        height: '100%',
                        width: `${area.percentage}%`,
                        background: `linear-gradient(90deg, ${colors.bar}, ${colors.bar}CC)`,
                        borderRadius: '100px',
                        transition: 'width 0.5s ease'
                      }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default Dashboard;

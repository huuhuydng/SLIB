
import React, { useState, useMemo } from 'react';
import {
  LogOut,
  Filter,
  LogIn,
  Users,
  Check
} from 'lucide-react';
import Header from "../../../components/shared/Header";
import "../../../styles/librarian/CheckInOut.css";
import { handleLogout } from "../../../utils/auth";


// --- MOCK DATA ---
const MOCK_LOGS = [
  { id: 1, name: 'Nguyễn Hoàng Phúc', code: 'DE170706', action: 'Check in', zone: 'Khu yên tĩnh', time: '12:21:10 15/12/2025' },
  { id: 2, name: 'Trần Thị Mai', code: 'DE170112', action: 'Check out', zone: 'Khu thảo luận', time: '12:20:05 15/12/2025' },
  { id: 3, name: 'Lê Văn Nam', code: 'SE160554', action: 'Check out', zone: 'Khu tự học', time: '12:18:30 15/12/2025' },
  { id: 4, name: 'Phạm Minh Khoa', code: 'DE180001', action: 'Check out', zone: 'Khu thảo luận', time: '12:15:12 15/12/2025' },
  { id: 5, name: 'Đỗ Thảo Vy', code: 'GD150223', action: 'Check out', zone: 'Khu tự học', time: '12:10:45 15/12/2025' },
  { id: 6, name: 'Nguyễn Hoàng Phúc', code: 'DE170706', action: 'Check in', zone: 'Khu yên tĩnh', time: '11:55:10 15/12/2025' },
  { id: 7, name: 'Vũ Thanh Tùng', code: 'SE171111', action: 'Check out', zone: 'Khu thảo luận', time: '11:50:22 15/12/2025' },
  { id: 8, name: 'Hoàng Yến Nhi', code: 'MC170333', action: 'Check out', zone: 'Khu tự học', time: '11:45:15 15/12/2025' },
  { id: 9, name: 'Ngô Kiến Huy', code: 'DE170888', action: 'Check out', zone: 'Khu thảo luận', time: '11:40:00 15/12/2025' },
  { id: 10, name: 'Bùi Anh Tuấn', code: 'SE160999', action: 'Check out', zone: 'Khu tự học', time: '11:35:55 15/12/2025' },
  { id: 11, name: 'Lâm Vỹ Dạ', code: 'DE170706', action: 'Check out', zone: 'Khu thảo luận', time: '11:30:10 15/12/2025' },
  { id: 12, name: 'Trấn Thành', code: 'MC170123', action: 'Check out', zone: 'Khu tự học', time: '11:25:40 15/12/2025' },
  { id: 13, name: 'Trường Giang', code: 'SE150000', action: 'Check in', zone: 'Khu yên tĩnh', time: '11:20:10 15/12/2025' },
  { id: 14, name: 'Ninh Dương L.N', code: 'DE170456', action: 'Check out', zone: 'Khu thảo luận', time: '11:15:12 15/12/2025' },
  { id: 15, name: 'Thúy Ngân', code: 'GD170789', action: 'Check out', zone: 'Khu tự học', time: '11:10:05 15/12/2025' },
];

const CheckInOut = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  // filterSort: '' | 'zone' | 'action'
  const [filterSort, setFilterSort] = useState(''); 
  // For zone filtering: '' | 'Khu yên tĩnh' | 'Khu thảo luận' | 'Khu tự học'
  const [selectedZone, setSelectedZone] = useState('');

  const displayedLogs = useMemo(() => {
    let data = MOCK_LOGS.filter(log => 
      log.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
      log.code.toLowerCase().includes(searchTerm.toLowerCase())
    );

    // Filter by selected zone
    if (selectedZone) {
      data = data.filter(log => log.zone === selectedZone);
    }

    // Simple sorting logic based on the filter selection
    if (filterSort === 'zone') {
      data = [...data].sort((a, b) => a.zone.localeCompare(b.zone));
    } else if (filterSort === 'action') {
      data = [...data].sort((a, b) => a.action.localeCompare(b.action));
    }
    
    return data;
  }, [searchTerm, filterSort, selectedZone]);

  const handleFilterClick = (type) => {
    // Toggle if clicking the same one, otherwise set new
    if (type === 'zone') {
      // Don't close filter for zone, show zone options
      setFilterSort('zone');
    } else {
      setFilterSort(prev => prev === type ? '' : type);
      setIsFilterOpen(false);
    }
  };

  const handleZoneSelect = (zone) => {
    setSelectedZone(prev => prev === zone ? '' : zone);
    setIsFilterOpen(false);
  };

  const getZoneClass = (zoneName) => {
    const lower = zoneName.toLowerCase();
    if (lower.includes('yên tĩnh')) return 'quiet';
    if (lower.includes('thảo luận')) return 'discuss';
    if (lower.includes('tự học')) return 'self-study';
    return '';
  };

  return (
    <>
      <Header 
        searchValue={searchTerm}
        onSearchChange={(e) => setSearchTerm(e.target.value)}
        searchPlaceholder="Search for anything..."
        onLogout={handleLogout}
      />

      <div style={{
        padding: '2rem',
        maxWidth: '1400px',
        margin: '0 auto',
        backgroundColor: '#f9fafb',
        minHeight: 'calc(100vh - 80px)'
      }}>
      {/* Page Title */}
      <h1 className="page-title" style={{
        fontSize: '28px',
        fontWeight: '700',
        color: '#1f2937',
        marginBottom: '24px'
      }}>Kiểm tra ra/vào</h1>

      {/* Stats Cards */}
      <section className="stats-row" style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 1fr)',
        gap: '20px',
        marginBottom: '24px',
        padding: '0 32px'
      }}>
        <StatCard 
          number="78" 
          label="Đã check in hôm nay" 
          icon={<LogIn size={28} color="#6366f1" />} 
          iconBg="#e0e7ff" 
        />
        <StatCard 
          number="60" 
          label="Đã check out hôm nay" 
          icon={<LogOut size={28} color="#ec4899" />} 
          iconBg="#fce7f3" 
        />
        <StatCard 
          number="18" 
          label="Đang trong thư viện" 
          icon={<Users size={28} color="#8b5cf6" />} 
          iconBg="#ede9fe" 
        />
      </section>

      {/* Table Panel */}
      <section className="table-panel" style={{
        backgroundColor: '#fff',
        borderRadius: '12px',
        padding: '24px',
        margin: '0 32px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <div className="panel-header" style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '20px'
        }}>
          <h2 style={{
            fontSize: '18px',
            fontWeight: '600',
            color: '#1f2937'
          }}>Danh sách sinh viên ra vào</h2>
          <div className="filter-container" style={{ position: 'relative' }}>
            <button className="filter-btn" onClick={() => setIsFilterOpen(!isFilterOpen)} style={{
              padding: '8px 12px',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              backgroundColor: '#fff',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <Filter size={18} />
            </button>
            {isFilterOpen && (
              <div className="filter-dropdown" style={{
                position: 'absolute',
                top: '100%',
                right: 0,
                marginTop: '8px',
                backgroundColor: '#fff',
                border: '1px solid #e5e7eb',
                borderRadius: '8px',
                boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                minWidth: '180px',
                zIndex: 10
              }}>
                <div 
                  className={`filter-item ${filterSort === 'zone' ? 'active' : ''}`} 
                  onClick={() => handleFilterClick('zone')}
                  style={{
                    padding: '10px 16px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: filterSort === 'zone' ? '#f76b1c' : '#1f2937',
                    backgroundColor: filterSort === 'zone' ? '#fff5f0' : 'transparent',
                    borderBottom: filterSort === 'zone' ? '1px solid #f1f5f9' : 'none'
                  }}
                >
                  Khu vực
                  <ChevronDown size={14} />
                </div>
                
                {filterSort === 'zone' && (
                  <>
                    <div 
                      onClick={() => handleZoneSelect('Khu yên tĩnh')}
                      style={{
                        padding: '10px 16px 10px 32px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        fontSize: '13px',
                        color: selectedZone === 'Khu yên tĩnh' ? '#f76b1c' : '#6b7280',
                        backgroundColor: selectedZone === 'Khu yên tĩnh' ? '#fff5f0' : 'transparent'
                      }}
                    >
                      Khu yên tĩnh
                      {selectedZone === 'Khu yên tĩnh' && <Check size={14} />}
                    </div>
                    <div 
                      onClick={() => handleZoneSelect('Khu thảo luận')}
                      style={{
                        padding: '10px 16px 10px 32px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        fontSize: '13px',
                        color: selectedZone === 'Khu thảo luận' ? '#f76b1c' : '#6b7280',
                        backgroundColor: selectedZone === 'Khu thảo luận' ? '#fff5f0' : 'transparent'
                      }}
                    >
                      Khu thảo luận
                      {selectedZone === 'Khu thảo luận' && <Check size={14} />}
                    </div>
                    <div 
                      onClick={() => handleZoneSelect('Khu tự học')}
                      style={{
                        padding: '10px 16px 10px 32px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        fontSize: '13px',
                        color: selectedZone === 'Khu tự học' ? '#f76b1c' : '#6b7280',
                        backgroundColor: selectedZone === 'Khu tự học' ? '#fff5f0' : 'transparent'
                      }}
                    >
                      Khu tự học
                      {selectedZone === 'Khu tự học' && <Check size={14} />}
                    </div>
                  </>
                )}
                
                <div 
                  className={`filter-item ${filterSort === 'action' ? 'active' : ''}`} 
                  onClick={() => handleFilterClick('action')}
                  style={{
                    padding: '10px 16px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    fontSize: '14px',
                    color: filterSort === 'action' ? '#f76b1c' : '#1f2937',
                    backgroundColor: filterSort === 'action' ? '#fff5f0' : 'transparent',
                    borderTop: filterSort === 'zone' ? '1px solid #f1f5f9' : 'none'
                  }}
                >
                  Hành động
                  {filterSort === 'action' && <Check size={14} />}
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="table-wrapper" style={{
          overflowX: 'auto'
        }}>
          <table className="log-table" style={{
            width: '100%',
            borderCollapse: 'collapse'
          }}>
            <thead>
              <tr style={{
                borderBottom: '2px solid #e5e7eb'
              }}>
                <th style={{
                  padding: '12px',
                  textAlign: 'left',
                  fontSize: '13px',
                  fontWeight: '600',
                  color: '#6b7280',
                  textTransform: 'uppercase'
                }}>Tên sinh viên</th>
                <th style={{
                  padding: '12px',
                  textAlign: 'left',
                  fontSize: '13px',
                  fontWeight: '600',
                  color: '#6b7280',
                  textTransform: 'uppercase'
                }}>Mã số sinh viên</th>
                <th style={{
                  padding: '12px',
                  textAlign: 'left',
                  fontSize: '13px',
                  fontWeight: '600',
                  color: '#6b7280',
                  textTransform: 'uppercase'
                }}>Hành động</th>
                <th style={{
                  padding: '12px',
                  textAlign: 'left',
                  fontSize: '13px',
                  fontWeight: '600',
                  color: '#6b7280',
                  textTransform: 'uppercase'
                }}>Khu vực</th>
                <th style={{
                  padding: '12px',
                  textAlign: 'left',
                  fontSize: '13px',
                  fontWeight: '600',
                  color: '#6b7280',
                  textTransform: 'uppercase'
                }}>Thời gian</th>
              </tr>
            </thead>
            <tbody>
              {displayedLogs.map((log) => (
                <tr key={log.id} style={{
                  borderBottom: '1px solid #f3f4f6'
                }}>
                  <td className="fw-500" style={{
                    padding: '12px',
                    fontSize: '14px',
                    color: '#1f2937',
                    fontWeight: '500'
                  }}>{log.name}</td>
                  <td className="code-cell" style={{
                    padding: '12px',
                    fontSize: '14px',
                    color: '#6b7280'
                  }}>{log.code}</td>
                  <td style={{
                    padding: '12px'
                  }}>
                    <span className={`badge action ${log.action === 'Check in' ? 'in' : 'out'}`} style={{
                      display: 'inline-block',
                      padding: '4px 12px',
                      borderRadius: '12px',
                      fontSize: '12px',
                      fontWeight: '600',
                      backgroundColor: log.action === 'Check in' ? '#dcfce7' : '#fee2e2',
                      color: log.action === 'Check in' ? '#166534' : '#991b1b'
                    }}>
                      {log.action}
                    </span>
                  </td>
                  <td style={{
                    padding: '12px'
                  }}>
                    <span className={`badge zone ${getZoneClass(log.zone)}`} style={{
                      display: 'inline-block',
                      padding: '4px 12px',
                      borderRadius: '12px',
                      fontSize: '12px',
                      fontWeight: '600',
                      backgroundColor: getZoneClass(log.zone) === 'quiet' ? '#fecaca' : 
                                       getZoneClass(log.zone) === 'discuss' ? '#86efac' : '#fef08a',
                      color: getZoneClass(log.zone) === 'quiet' ? '#7f1d1d' : 
                             getZoneClass(log.zone) === 'discuss' ? '#14532d' : '#854d0e'
                    }}>
                      {log.zone}
                    </span>
                  </td>
                  <td className="time-cell" style={{
                    padding: '12px',
                    fontSize: '14px',
                    color: '#6b7280'
                  }}>{log.time}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      </div>
    </>
  );
};

// --- Sub Components ---

const StatCard = ({ number, label, icon, iconBg }) => (
  <div className="stat-card" style={{
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '20px',
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  }}>
    <div className="stat-icon-wrapper" style={{ 
      backgroundColor: iconBg,
      padding: '12px',
      borderRadius: '12px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}>
      {icon}
    </div>
    <div className="stat-info">
      <div className="stat-number" style={{
        fontSize: '24px',
        fontWeight: '700',
        color: '#1f2937',
        marginBottom: '4px'
      }}>{number}</div>
      <div className="stat-label" style={{
        fontSize: '14px',
        color: '#6b7280'
      }}>{label}</div>
    </div>
  </div>
);

export default CheckInOut;
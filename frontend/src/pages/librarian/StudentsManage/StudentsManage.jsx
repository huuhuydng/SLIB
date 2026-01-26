import React, { useState, useMemo, useEffect } from 'react';
import { 
  Users, 
  AlertTriangle, 
  Filter, 
  Clock
} from 'lucide-react';
import StudentDetail from '../../../components/librarian/StudentDetail';
import Header from "../../../components/shared/Header";
import '../../../styles/librarian/StudentsManage.css';
import { handleLogout } from "../../../utils/auth";

// --- MOCK DATA ---
const MOCK_STUDENTS = [
  { id: 1, studentId: 'DE170706', name: 'Nguyễn Hoàng Phúc', email: 'phucnhde170706@fpt.edu.vn', score: 90, seat: 'A1', checkInTime: '2025-12-26T08:30:00' },
  { id: 2, studentId: 'DE170707', name: 'Trần Văn An', email: 'antv@fpt.edu.vn', score: 69, seat: 'B1', checkInTime: '2025-12-26T09:15:00' },
  { id: 3, studentId: 'DE170708', name: 'Lê Thị Bình', email: 'binhlt@fpt.edu.vn', score: 59, seat: 'C1', checkInTime: '2025-12-26T07:45:00' },
  { id: 4, studentId: 'DE170709', name: 'Phạm Minh Cường', email: 'cuongpm@fpt.edu.vn', score: 95, seat: 'A2', checkInTime: '2025-12-26T08:00:00' },
  { id: 5, studentId: 'DE170710', name: 'Đỗ Hải Đăng', email: 'dangdh@fpt.edu.vn', score: 75, seat: 'B2', checkInTime: '2025-12-26T10:20:00' },
  { id: 6, studentId: 'DE170711', name: 'Hoàng Thùy Linh', email: 'linhht@fpt.edu.vn', score: 45, seat: 'C2', checkInTime: '2025-12-26T09:30:00' },
  { id: 7, studentId: 'DE170712', name: 'Ngô Kiến Huy', email: 'huynk@fpt.edu.vn', score: 82, seat: 'A3', checkInTime: '2025-12-26T08:45:00' },
  { id: 8, studentId: 'DE170713', name: 'Sơn Tùng MTP', email: 'tungmtp@fpt.edu.vn', score: 68, seat: 'B3', checkInTime: '2025-12-26T11:00:00' },
  { id: 9, studentId: 'DE170714', name: 'Đen Vâu', email: 'denvau@fpt.edu.vn', score: 60, seat: 'C3', checkInTime: '2025-12-26T07:30:00' },
  { id: 10, studentId: 'DE170715', name: 'Bích Phương', email: 'phuongb@fpt.edu.vn', score: 88, seat: 'A4', checkInTime: '2025-12-26T09:00:00' },
];

const StudentsManage = () => {
  const [currentTime, setCurrentTime] = useState(new Date());
  const [globalSearch, setGlobalSearch] = useState('');
  const [tableSearch, setTableSearch] = useState('');
  const [filterRank, setFilterRank] = useState('all'); // all, good, average, bad
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState(null); // For detail view

  // Update current time every minute
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 60000); // Update every minute

    return () => clearInterval(timer);
  }, []);

  // --- LOGIC ---
  const getRank = (score) => {
    if (score >= 80) return 'good'; // Gương mẫu
    if (score >= 65) return 'average'; // Khá
    return 'bad'; // Trung bình (kém)
  };

  const formatCheckInTime = (dateString) => {
    const date = new Date(dateString);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  };

  const getTimeDuration = (checkInTime) => {
    const checkIn = new Date(checkInTime);
    const now = currentTime;
    const diff = Math.floor((now - checkIn) / 1000 / 60); // minutes
    
    if (diff < 60) {
      return `${diff} phút`;
    } else {
      const hours = Math.floor(diff / 60);
      const minutes = diff % 60;
      return minutes > 0 ? `${hours}h ${minutes}ph` : `${hours} giờ`;
    }
  };

  const filteredStudents = useMemo(() => {
    return MOCK_STUDENTS.filter(s => {
      const matchGlobal = s.name.toLowerCase().includes(globalSearch.toLowerCase()) || 
                          s.studentId.toLowerCase().includes(globalSearch.toLowerCase());
      const matchTable = s.studentId.toLowerCase().includes(tableSearch.toLowerCase());
      
      let matchFilter = true;
      const rank = getRank(s.score);
      if (filterRank !== 'all') {
        matchFilter = rank === filterRank;
      }

      return matchGlobal && matchTable && matchFilter;
    });
  }, [globalSearch, tableSearch, filterRank]);

  const handleRowClick = (studentId) => {
    const student = MOCK_STUDENTS.find(s => s.studentId === studentId);
    if (student) {
      setSelectedStudent(student);
    }
  };

  // If a student is selected, show StudentDetail
  if (selectedStudent) {
    return <StudentDetail student={selectedStudent} onBack={() => setSelectedStudent(null)} />;
  }

  return (
    <>
      <Header 
        searchValue={globalSearch}
        onSearchChange={(e) => setGlobalSearch(e.target.value)}
        searchPlaceholder="Search for anything..."
        onLogout={handleLogout}
      />

        {/* Content Body */}
        <div style={{
          padding: '2rem',
          maxWidth: '1400px',
          margin: '0 auto',
          backgroundColor: '#f9fafb',
          minHeight: 'calc(100vh - 80px)'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '1.5rem'
          }}>
            <h2 style={{
              fontSize: '1.875rem',
              fontWeight: '700',
              color: '#1f2937'
            }}>Quản lý sinh viên</h2>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              backgroundColor: '#f3f4f6',
              padding: '0.5rem 1rem',
              borderRadius: '8px'
            }}>
              <Clock size={20} color="#6b7280" />
              <span style={{
                fontSize: '0.875rem',
                fontWeight: '500',
                color: '#374151'
              }}>
                {currentTime.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
              </span>
            </div>
          </div>

          {/* Stats Cards */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
            gap: '1.5rem',
            marginBottom: '2rem'
          }}>
            <div style={{
              backgroundColor: '#fff',
              padding: '1.5rem',
              borderRadius: '12px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
              display: 'flex',
              alignItems: 'center',
              gap: '1rem'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '12px',
                backgroundColor: '#8b5cf6',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff'
              }}>
                <Users size={24} />
              </div>
              <div>
                <h3 style={{
                  fontSize: '1.875rem',
                  fontWeight: '700',
                  marginBottom: '0.25rem'
                }}>69</h3>
                <p style={{
                  fontSize: '0.875rem',
                  color: '#6b7280'
                }}>Đang trong thư viện</p>
              </div>
            </div>
            <div style={{
              backgroundColor: '#fff',
              padding: '1.5rem',
              borderRadius: '12px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
              display: 'flex',
              alignItems: 'center',
              gap: '1rem'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '12px',
                backgroundColor: '#ef4444',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff'
              }}>
                <AlertTriangle size={24} />
              </div>
              <div>
                <h3 style={{
                  fontSize: '1.875rem',
                  fontWeight: '700',
                  marginBottom: '0.25rem'
                }}>18</h3>
                <p style={{
                  fontSize: '0.875rem',
                  color: '#6b7280'
                }}>Điểm đánh giá dưới 65%</p>
              </div>
            </div>
          </div>

        {/* Table Section */}
        <div style={{
          backgroundColor: '#fff',
          borderRadius: '12px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          overflow: 'hidden',
          marginBottom: '2rem'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1.5rem',
            borderBottom: '1px solid #e5e7eb'
          }}>
              <h3 style={{
                fontSize: '1.125rem',
                fontWeight: '600',
                color: '#1f2937'
              }}>Danh sách sinh viên đang có mặt tại thư viện</h3>
              <div style={{
                display: 'flex',
                gap: '0.75rem',
                alignItems: 'center'
              }}>
                <div>
                  <input 
                    type="text" 
                    placeholder="Tìm kiếm mã số sinh viên" 
                    value={tableSearch}
                    onChange={(e) => setTableSearch(e.target.value)}
                    style={{
                      padding: '0.5rem 1rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '0.875rem',
                      width: '220px'
                    }}
                  />
                </div>
                
                <div style={{ position: 'relative' }}>
                  <button 
                    onClick={() => setIsFilterOpen(!isFilterOpen)}
                    style={{
                      padding: '0.5rem',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      backgroundColor: '#fff',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  >
                    <Filter size={20} />
                  </button>
                  {isFilterOpen && (
                    <div style={{
                      position: 'absolute',
                      top: 'calc(100% + 0.5rem)',
                      right: 0,
                      backgroundColor: '#fff',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                      minWidth: '200px',
                      zIndex: 10
                    }}>
                      <div style={{
                        padding: '0.75rem 1rem',
                        fontSize: '0.875rem',
                        fontWeight: '600',
                        color: '#6b7280',
                        borderBottom: '1px solid #e5e7eb'
                      }}>Điểm đánh giá</div>
                      <div 
                        onClick={() => { setFilterRank('all'); setIsFilterOpen(false); }}
                        style={{
                          padding: '0.75rem 1rem',
                          fontSize: '0.875rem',
                          cursor: 'pointer'
                        }}
                      >Tất cả</div>
                      <div 
                        onClick={() => { setFilterRank('good'); setIsFilterOpen(false); }}
                        style={{
                          padding: '0.75rem 1rem',
                          fontSize: '0.875rem',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.5rem'
                        }}
                      >
                        <span style={{
                          width: '8px',
                          height: '8px',
                          borderRadius: '50%',
                          backgroundColor: '#10b981'
                        }}></span> Gương mẫu
                      </div>
                      <div 
                        onClick={() => { setFilterRank('average'); setIsFilterOpen(false); }}
                        style={{
                          padding: '0.75rem 1rem',
                          fontSize: '0.875rem',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.5rem'
                        }}
                      >
                        <span style={{
                          width: '8px',
                          height: '8px',
                          borderRadius: '50%',
                          backgroundColor: '#f59e0b'
                        }}></span> Khá
                      </div>
                      <div 
                        onClick={() => { setFilterRank('bad'); setIsFilterOpen(false); }}
                        style={{
                          padding: '0.75rem 1rem',
                          fontSize: '0.875rem',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.5rem'
                        }}
                      >
                        <span style={{
                          width: '8px',
                          height: '8px',
                          borderRadius: '50%',
                          backgroundColor: '#ef4444'
                        }}></span> Trung bình
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>

          <div style={{
            overflowX: 'auto',
            padding: '1.5rem'
          }}>
            <table style={{
              width: '100%',
              borderCollapse: 'collapse'
            }}>
                <thead>
                  <tr style={{
                    borderBottom: '2px solid #e5e7eb'
                  }}>
                    <th style={{
                      textAlign: 'left',
                      padding: '0.75rem',
                      fontSize: '0.875rem',
                      fontWeight: '600',
                      color: '#6b7280'
                    }}>Tên sinh viên</th>
                    <th style={{
                      textAlign: 'left',
                      padding: '0.75rem',
                      fontSize: '0.875rem',
                      fontWeight: '600',
                      color: '#6b7280'
                    }}>Mã số sinh viên</th>
                    <th style={{
                      textAlign: 'center',
                      padding: '0.75rem',
                      fontSize: '0.875rem',
                      fontWeight: '600',
                      color: '#6b7280'
                    }}>Giờ check in</th>
                    <th style={{
                      textAlign: 'center',
                      padding: '0.75rem',
                      fontSize: '0.875rem',
                      fontWeight: '600',
                      color: '#6b7280'
                    }}>Thời gian có mặt</th>
                    <th style={{
                      textAlign: 'center',
                      padding: '0.75rem',
                      fontSize: '0.875rem',
                      fontWeight: '600',
                      color: '#6b7280'
                    }}>Điểm đánh giá</th>
                    <th style={{
                      textAlign: 'center',
                      padding: '0.75rem',
                      fontSize: '0.875rem',
                      fontWeight: '600',
                      color: '#6b7280'
                    }}>Vị trí</th>
                  </tr>
                </thead>
                <tbody>
                {filteredStudents.map((student) => {
                  const rank = getRank(student.score);
                  const scoreBgColor = rank === 'good' ? '#dcfce7' : rank === 'average' ? '#fef3c7' : '#fee2e2';
                  const scoreTextColor = rank === 'good' ? '#166534' : rank === 'average' ? '#92400e' : '#991b1b';
                  return (
                    <tr 
                      key={student.id} 
                      onClick={() => handleRowClick(student.studentId)}
                      style={{
                        borderBottom: '1px solid #f3f4f6',
                        cursor: 'pointer'
                      }}
                    >
                      <td style={{
                        padding: '1rem 0.75rem',
                        fontSize: '0.875rem',
                        fontWeight: '500'
                      }}>{student.name}</td>
                      <td style={{
                        padding: '1rem 0.75rem',
                        fontSize: '0.875rem',
                        color: '#6b7280'
                      }}>{student.studentId}</td>
                      <td style={{
                        padding: '1rem 0.75rem',
                        textAlign: 'center',
                        fontSize: '0.875rem',
                        fontWeight: '500',
                        color: '#374151'
                      }}>
                        <div style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: '0.25rem'
                        }}>
                          <Clock size={14} color="#6b7280" />
                          {formatCheckInTime(student.checkInTime)}
                        </div>
                      </td>
                      <td style={{
                        padding: '1rem 0.75rem',
                        textAlign: 'center',
                        fontSize: '0.875rem',
                        fontWeight: '500',
                        color: '#6b7280'
                      }}>
                        {getTimeDuration(student.checkInTime)}
                      </td>
                      <td style={{
                        padding: '1rem 0.75rem',
                        textAlign: 'center'
                      }}>
                        <span style={{
                          display: 'inline-block',
                          padding: '0.25rem 0.75rem',
                          borderRadius: '12px',
                          fontSize: '0.875rem',
                          fontWeight: '600',
                          backgroundColor: scoreBgColor,
                          color: scoreTextColor
                        }}>
                          {student.score}
                        </span>
                      </td>
                      <td style={{
                        padding: '1rem 0.75rem',
                        textAlign: 'center'
                      }}>
                        <span style={{
                          display: 'inline-block',
                          padding: '0.25rem 0.75rem',
                          borderRadius: '8px',
                          fontSize: '0.875rem',
                          fontWeight: '500',
                          backgroundColor: '#f3f4f6',
                          color: '#374151'
                        }}>{student.seat}</span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {filteredStudents.length === 0 && (
              <div style={{
                padding: '3rem',
                textAlign: 'center',
                color: '#9ca3af',
                fontSize: '0.875rem'
              }}>Không tìm thấy sinh viên nào.</div>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default StudentsManage;
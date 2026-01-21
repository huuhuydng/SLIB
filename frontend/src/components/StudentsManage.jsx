import React, { useState, useMemo, useEffect } from 'react';
import { 
  Users, 
  AlertTriangle, 
  Filter, 
  Clock,
  Search,
  ChevronDown,
  Star,
  TrendingUp,
  MapPin
} from 'lucide-react';
import StudentDetail from './StudentDetail';
import Header from './Header';
import '../styles/StudentsManage.css';

// Mock Data with reputation scores
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
  const [filterRank, setFilterRank] = useState('all');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState(null);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 60000);
    return () => clearInterval(timer);
  }, []);

  const getRank = (score) => {
    if (score >= 80) return 'good';
    if (score >= 65) return 'average';
    return 'bad';
  };

  const getRankLabel = (rank) => {
    switch (rank) {
      case 'good': return 'Gương mẫu';
      case 'average': return 'Khá';
      case 'bad': return 'Trung bình';
      default: return '';
    }
  };

  const formatCheckInTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  const getTimeDuration = (checkInTime) => {
    const checkIn = new Date(checkInTime);
    const diff = Math.floor((currentTime - checkIn) / 1000 / 60);
    
    if (diff < 60) return `${diff} phút`;
    const hours = Math.floor(diff / 60);
    const minutes = diff % 60;
    return minutes > 0 ? `${hours}h ${minutes}ph` : `${hours} giờ`;
  };

  const getScoreColor = (score) => {
    const rank = getRank(score);
    switch (rank) {
      case 'good': return { bg: 'var(--slib-status-success-bg, #E8F5E9)', color: 'var(--slib-status-success, #388E3C)' };
      case 'average': return { bg: 'var(--slib-status-warning-bg, #FFF3E0)', color: 'var(--slib-status-warning, #FF9800)' };
      case 'bad': return { bg: 'var(--slib-status-error-bg, #FFEBEE)', color: 'var(--slib-status-error, #D32F2F)' };
      default: return { bg: '#F7FAFC', color: '#4A5568' };
    }
  };

  const filteredStudents = useMemo(() => {
    return MOCK_STUDENTS.filter(s => {
      const matchGlobal = s.name.toLowerCase().includes(globalSearch.toLowerCase()) || 
                          s.studentId.toLowerCase().includes(globalSearch.toLowerCase());
      const matchTable = s.studentId.toLowerCase().includes(tableSearch.toLowerCase());
      
      let matchFilter = true;
      if (filterRank !== 'all') {
        matchFilter = getRank(s.score) === filterRank;
      }

      return matchGlobal && matchTable && matchFilter;
    });
  }, [globalSearch, tableSearch, filterRank]);

  const stats = useMemo(() => {
    const total = MOCK_STUDENTS.length;
    const lowScore = MOCK_STUDENTS.filter(s => s.score < 65).length;
    const avgScore = Math.round(MOCK_STUDENTS.reduce((acc, s) => acc + s.score, 0) / total);
    return { total, lowScore, avgScore };
  }, []);

  if (selectedStudent) {
    return <StudentDetail student={selectedStudent} onBack={() => setSelectedStudent(null)} />;
  }

  return (
    <>
      <Header 
        searchValue={globalSearch}
        onSearchChange={(e) => setGlobalSearch(e.target.value)}
        searchPlaceholder="Tìm kiếm sinh viên..."
      />

      <div style={{
        padding: '0 24px 32px',
        maxWidth: '1440px',
        margin: '0 auto',
        minHeight: 'calc(100vh - 120px)'
      }}>
        {/* Page Header */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px'
        }}>
          <div>
            <h1 style={{
              fontSize: '28px',
              fontWeight: '700',
              color: 'var(--slib-text-primary, #1A1A1A)',
              margin: '0 0 4px 0'
            }}>Quản lý sinh viên</h1>
            <p style={{
              fontSize: '14px',
              color: 'var(--slib-text-muted, #A0AEC0)',
              margin: 0
            }}>Theo dõi và quản lý sinh viên trong thư viện</p>
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
            <Clock size={18} color="var(--slib-primary, #FF751F)" />
            <span style={{
              fontSize: '13px',
              fontWeight: '600',
              color: 'var(--slib-text-secondary, #4A5568)'
            }}>
              {currentTime.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
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
          {/* Current Students */}
          <div style={{
            background: 'var(--slib-bg-card, #ffffff)',
            borderRadius: '16px',
            padding: '24px',
            boxShadow: 'var(--slib-shadow-card)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            position: 'relative',
            overflow: 'hidden',
            transition: 'all 0.3s ease'
          }}>
            <div style={{
              position: 'absolute',
              top: 0,
              right: 0,
              width: '100px',
              height: '100px',
              background: 'linear-gradient(135deg, #F3E8FF40 0%, transparent 60%)',
              borderRadius: '0 16px 0 100%'
            }} />
            <div style={{
              width: '56px',
              height: '56px',
              borderRadius: '14px',
              background: '#F3E8FF',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 12px #F3E8FF80'
            }}>
              <Users size={24} color="#7C3AED" />
            </div>
            <div>
              <div style={{
                fontSize: '32px',
                fontWeight: '700',
                color: 'var(--slib-text-primary, #1A1A1A)',
                lineHeight: '1.2'
              }}>{stats.total}</div>
              <div style={{
                fontSize: '13px',
                color: 'var(--slib-text-secondary, #4A5568)',
                fontWeight: '500'
              }}>Sinh viên trong thư viện</div>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                marginTop: '8px',
                padding: '4px 10px',
                background: 'var(--slib-status-success-bg, #E8F5E9)',
                borderRadius: '20px',
                width: 'fit-content'
              }}>
                <TrendingUp size={12} color="var(--slib-status-success, #388E3C)" />
                <span style={{
                  fontSize: '12px',
                  fontWeight: '600',
                  color: 'var(--slib-status-success, #388E3C)'
                }}>+15% so với hôm qua</span>
              </div>
            </div>
          </div>

          {/* Low Score Students */}
          <div style={{
            background: 'var(--slib-bg-card, #ffffff)',
            borderRadius: '16px',
            padding: '24px',
            boxShadow: 'var(--slib-shadow-card)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            position: 'relative',
            overflow: 'hidden'
          }}>
            <div style={{
              position: 'absolute',
              top: 0,
              right: 0,
              width: '100px',
              height: '100px',
              background: 'linear-gradient(135deg, #FFEBEE40 0%, transparent 60%)',
              borderRadius: '0 16px 0 100%'
            }} />
            <div style={{
              width: '56px',
              height: '56px',
              borderRadius: '14px',
              background: 'var(--slib-status-error-bg, #FFEBEE)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 12px rgba(211, 47, 47, 0.2)'
            }}>
              <AlertTriangle size={24} color="var(--slib-status-error, #D32F2F)" />
            </div>
            <div>
              <div style={{
                fontSize: '32px',
                fontWeight: '700',
                color: 'var(--slib-text-primary, #1A1A1A)',
                lineHeight: '1.2'
              }}>{stats.lowScore}</div>
              <div style={{
                fontSize: '13px',
                color: 'var(--slib-text-secondary, #4A5568)',
                fontWeight: '500'
              }}>Điểm đánh giá dưới 65%</div>
              <div style={{
                marginTop: '8px',
                fontSize: '12px',
                color: 'var(--slib-text-muted, #A0AEC0)'
              }}>Cần chú ý theo dõi</div>
            </div>
          </div>

          {/* Average Score */}
          <div style={{
            background: 'var(--slib-bg-card, #ffffff)',
            borderRadius: '16px',
            padding: '24px',
            boxShadow: 'var(--slib-shadow-card)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            position: 'relative',
            overflow: 'hidden'
          }}>
            <div style={{
              position: 'absolute',
              top: 0,
              right: 0,
              width: '100px',
              height: '100px',
              background: 'linear-gradient(135deg, #E8F5E940 0%, transparent 60%)',
              borderRadius: '0 16px 0 100%'
            }} />
            <div style={{
              width: '56px',
              height: '56px',
              borderRadius: '14px',
              background: 'var(--slib-status-success-bg, #E8F5E9)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 12px rgba(56, 142, 60, 0.2)'
            }}>
              <Star size={24} color="var(--slib-status-success, #388E3C)" />
            </div>
            <div>
              <div style={{
                fontSize: '32px',
                fontWeight: '700',
                color: 'var(--slib-text-primary, #1A1A1A)',
                lineHeight: '1.2'
              }}>{stats.avgScore}%</div>
              <div style={{
                fontSize: '13px',
                color: 'var(--slib-text-secondary, #4A5568)',
                fontWeight: '500'
              }}>Điểm đánh giá trung bình</div>
              <div style={{
                marginTop: '8px',
                fontSize: '12px',
                color: 'var(--slib-status-success, #388E3C)',
                fontWeight: '500'
              }}>Khá tốt</div>
            </div>
          </div>
        </div>

        {/* Table Section */}
        <div style={{
          background: 'var(--slib-bg-card, #ffffff)',
          borderRadius: '16px',
          boxShadow: 'var(--slib-shadow-card)',
          overflow: 'hidden'
        }}>
          {/* Table Header */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '20px 24px',
            borderBottom: '1px solid var(--slib-border-light, #E2E8F0)'
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
                <h3 style={{
                  fontSize: '16px',
                  fontWeight: '600',
                  color: 'var(--slib-text-primary, #1A1A1A)',
                  margin: 0
                }}>Danh sách sinh viên có mặt</h3>
                <p style={{
                  fontSize: '12px',
                  color: 'var(--slib-text-muted, #A0AEC0)',
                  margin: 0
                }}>{filteredStudents.length} sinh viên</p>
              </div>
            </div>
            
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              {/* Table Search */}
              <div style={{ position: 'relative' }}>
                <Search 
                  size={16} 
                  style={{
                    position: 'absolute',
                    left: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    color: 'var(--slib-text-muted, #A0AEC0)'
                  }}
                />
                <input 
                  type="text"
                  placeholder="Tìm mã số sinh viên"
                  value={tableSearch}
                  onChange={(e) => setTableSearch(e.target.value)}
                  style={{
                    padding: '10px 12px 10px 38px',
                    border: '2px solid var(--slib-border-light, #E2E8F0)',
                    borderRadius: '10px',
                    fontSize: '13px',
                    width: '200px',
                    transition: 'all 0.2s ease',
                    outline: 'none'
                  }}
                  onFocus={(e) => {
                    e.target.style.borderColor = 'var(--slib-primary, #FF751F)';
                    e.target.style.boxShadow = '0 0 0 4px rgba(255, 117, 31, 0.1)';
                  }}
                  onBlur={(e) => {
                    e.target.style.borderColor = 'var(--slib-border-light, #E2E8F0)';
                    e.target.style.boxShadow = 'none';
                  }}
                />
              </div>
              
              {/* Filter Dropdown */}
              <div style={{ position: 'relative' }}>
                <button 
                  onClick={() => setIsFilterOpen(!isFilterOpen)}
                  style={{
                    padding: '10px 16px',
                    border: '2px solid var(--slib-border-light, #E2E8F0)',
                    borderRadius: '10px',
                    background: 'var(--slib-bg-card, #ffffff)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    fontSize: '13px',
                    fontWeight: '500',
                    color: 'var(--slib-text-secondary, #4A5568)',
                    transition: 'all 0.2s ease'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.borderColor = 'var(--slib-primary, #FF751F)';
                    e.currentTarget.style.color = 'var(--slib-primary, #FF751F)';
                  }}
                  onMouseLeave={(e) => {
                    if (!isFilterOpen) {
                      e.currentTarget.style.borderColor = 'var(--slib-border-light, #E2E8F0)';
                      e.currentTarget.style.color = 'var(--slib-text-secondary, #4A5568)';
                    }
                  }}
                >
                  <Filter size={16} />
                  <span>Lọc</span>
                  <ChevronDown size={14} style={{
                    transform: isFilterOpen ? 'rotate(180deg)' : 'rotate(0)',
                    transition: 'transform 0.2s ease'
                  }} />
                </button>
                
                {isFilterOpen && (
                  <div style={{
                    position: 'absolute',
                    top: 'calc(100% + 8px)',
                    right: 0,
                    background: 'var(--slib-bg-card, #ffffff)',
                    border: '1px solid var(--slib-border-light, #E2E8F0)',
                    borderRadius: '12px',
                    boxShadow: 'var(--slib-shadow-lg)',
                    minWidth: '200px',
                    zIndex: 100,
                    overflow: 'hidden'
                  }}>
                    <div style={{
                      padding: '12px 16px',
                      fontSize: '12px',
                      fontWeight: '600',
                      color: 'var(--slib-text-muted, #A0AEC0)',
                      borderBottom: '1px solid var(--slib-border-light, #E2E8F0)',
                      textTransform: 'uppercase',
                      letterSpacing: '0.5px'
                    }}>Điểm đánh giá</div>
                    
                    {[
                      { value: 'all', label: 'Tất cả', dot: null },
                      { value: 'good', label: 'Gương mẫu (≥80%)', dot: 'var(--slib-status-success, #388E3C)' },
                      { value: 'average', label: 'Khá (65-79%)', dot: 'var(--slib-status-warning, #FF9800)' },
                      { value: 'bad', label: 'Trung bình (<65%)', dot: 'var(--slib-status-error, #D32F2F)' }
                    ].map((option) => (
                      <div 
                        key={option.value}
                        onClick={() => { setFilterRank(option.value); setIsFilterOpen(false); }}
                        style={{
                          padding: '12px 16px',
                          fontSize: '14px',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '10px',
                          transition: 'background-color 0.2s ease',
                          background: filterRank === option.value ? 'var(--slib-primary-subtle, #FFF7F2)' : 'transparent'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.background = 'var(--slib-bg-main, #F7FAFC)'}
                        onMouseLeave={(e) => e.currentTarget.style.background = filterRank === option.value ? 'var(--slib-primary-subtle, #FFF7F2)' : 'transparent'}
                      >
                        {option.dot && (
                          <span style={{
                            width: '8px',
                            height: '8px',
                            borderRadius: '50%',
                            background: option.dot
                          }} />
                        )}
                        <span style={{
                          fontWeight: filterRank === option.value ? '600' : '400',
                          color: filterRank === option.value ? 'var(--slib-primary, #FF751F)' : 'var(--slib-text-secondary, #4A5568)'
                        }}>{option.label}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Table Content */}
          <div style={{ overflow: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr>
                  <th style={{
                    textAlign: 'left',
                    padding: '16px 24px',
                    fontSize: '12px',
                    fontWeight: '600',
                    color: 'var(--slib-text-muted, #A0AEC0)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px',
                    background: 'var(--slib-bg-main, #F7FAFC)'
                  }}>Sinh viên</th>
                  <th style={{
                    textAlign: 'left',
                    padding: '16px 24px',
                    fontSize: '12px',
                    fontWeight: '600',
                    color: 'var(--slib-text-muted, #A0AEC0)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px',
                    background: 'var(--slib-bg-main, #F7FAFC)'
                  }}>Mã số</th>
                  <th style={{
                    textAlign: 'center',
                    padding: '16px 24px',
                    fontSize: '12px',
                    fontWeight: '600',
                    color: 'var(--slib-text-muted, #A0AEC0)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px',
                    background: 'var(--slib-bg-main, #F7FAFC)'
                  }}>Giờ check-in</th>
                  <th style={{
                    textAlign: 'center',
                    padding: '16px 24px',
                    fontSize: '12px',
                    fontWeight: '600',
                    color: 'var(--slib-text-muted, #A0AEC0)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px',
                    background: 'var(--slib-bg-main, #F7FAFC)'
                  }}>Thời gian</th>
                  <th style={{
                    textAlign: 'center',
                    padding: '16px 24px',
                    fontSize: '12px',
                    fontWeight: '600',
                    color: 'var(--slib-text-muted, #A0AEC0)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px',
                    background: 'var(--slib-bg-main, #F7FAFC)'
                  }}>Điểm đánh giá</th>
                  <th style={{
                    textAlign: 'center',
                    padding: '16px 24px',
                    fontSize: '12px',
                    fontWeight: '600',
                    color: 'var(--slib-text-muted, #A0AEC0)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px',
                    background: 'var(--slib-bg-main, #F7FAFC)'
                  }}>Vị trí</th>
                </tr>
              </thead>
              <tbody>
                {filteredStudents.map((student, index) => {
                  const scoreColors = getScoreColor(student.score);
                  return (
                    <tr 
                      key={student.id}
                      onClick={() => setSelectedStudent(student)}
                      style={{
                        borderBottom: index === filteredStudents.length - 1 
                          ? 'none' 
                          : '1px solid var(--slib-border-light, #E2E8F0)',
                        cursor: 'pointer',
                        transition: 'background-color 0.2s ease'
                      }}
                      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--slib-primary-subtle, #FFF7F2)'}
                      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                    >
                      <td style={{ padding: '16px 24px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <div style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: '10px',
                            background: 'linear-gradient(135deg, var(--slib-primary, #FF751F), var(--slib-primary-light, #FF9B5A))',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#fff',
                            fontSize: '13px',
                            fontWeight: '600'
                          }}>
                            {student.name.split(' ').slice(-2).map(n => n[0]).join('')}
                          </div>
                          <div>
                            <div style={{
                              fontSize: '14px',
                              fontWeight: '600',
                              color: 'var(--slib-text-primary, #1A1A1A)'
                            }}>{student.name}</div>
                            <div style={{
                              fontSize: '12px',
                              color: 'var(--slib-text-muted, #A0AEC0)'
                            }}>{student.email}</div>
                          </div>
                        </div>
                      </td>
                      <td style={{ padding: '16px 24px' }}>
                        <span style={{
                          fontSize: '13px',
                          fontWeight: '600',
                          color: 'var(--slib-text-secondary, #4A5568)',
                          fontFamily: 'monospace'
                        }}>{student.studentId}</span>
                      </td>
                      <td style={{ padding: '16px 24px', textAlign: 'center' }}>
                        <div style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          padding: '6px 12px',
                          background: 'var(--slib-bg-main, #F7FAFC)',
                          borderRadius: '8px'
                        }}>
                          <Clock size={14} color="var(--slib-text-muted, #A0AEC0)" />
                          <span style={{
                            fontSize: '14px',
                            fontWeight: '600',
                            color: 'var(--slib-text-primary, #1A1A1A)'
                          }}>{formatCheckInTime(student.checkInTime)}</span>
                        </div>
                      </td>
                      <td style={{ padding: '16px 24px', textAlign: 'center' }}>
                        <span style={{
                          fontSize: '13px',
                          fontWeight: '500',
                          color: 'var(--slib-text-secondary, #4A5568)'
                        }}>{getTimeDuration(student.checkInTime)}</span>
                      </td>
                      <td style={{ padding: '16px 24px', textAlign: 'center' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
                          <span style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '6px',
                            padding: '6px 14px',
                            borderRadius: '20px',
                            fontSize: '14px',
                            fontWeight: '700',
                            background: scoreColors.bg,
                            color: scoreColors.color
                          }}>
                            <Star size={14} />
                            {student.score}
                          </span>
                          <span style={{
                            fontSize: '11px',
                            color: scoreColors.color,
                            fontWeight: '500'
                          }}>{getRankLabel(getRank(student.score))}</span>
                        </div>
                      </td>
                      <td style={{ padding: '16px 24px', textAlign: 'center' }}>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          padding: '8px 14px',
                          borderRadius: '10px',
                          fontSize: '13px',
                          fontWeight: '600',
                          background: 'var(--slib-bg-main, #F7FAFC)',
                          color: 'var(--slib-text-secondary, #4A5568)'
                        }}>
                          <MapPin size={14} />
                          {student.seat}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            
            {filteredStudents.length === 0 && (
              <div style={{
                padding: '48px',
                textAlign: 'center'
              }}>
                <Users size={48} color="var(--slib-text-muted, #A0AEC0)" style={{ marginBottom: '16px', opacity: 0.5 }} />
                <p style={{
                  fontSize: '16px',
                  fontWeight: '500',
                  color: 'var(--slib-text-muted, #A0AEC0)',
                  margin: 0
                }}>Không tìm thấy sinh viên nào</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default StudentsManage;

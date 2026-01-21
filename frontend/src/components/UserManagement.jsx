import React, { useState, useMemo } from 'react';
import { 
  Users, 
  Search, 
  Filter, 
  Plus, 
  Upload, 
  MoreVertical,
  Mail,
  Shield,
  Lock,
  Unlock,
  Trash2,
  Eye,
  Edit,
  UserPlus,
  Download,
  CheckCircle,
  XCircle,
  AlertTriangle,
  X,
  ChevronDown,
  FileSpreadsheet,
  Key
} from 'lucide-react';
import Header from './Header';

// Mock Data
const MOCK_USERS = [
  { id: 1, name: 'Nguyễn Văn Admin', email: 'admin@fpt.edu.vn', role: 'Admin', status: 'active', lastActive: '2025-01-16T10:30:00', avatar: 'NA' },
  { id: 2, name: 'Trần Thị Thủ Thư', email: 'librarian1@fpt.edu.vn', role: 'Librarian', status: 'active', lastActive: '2025-01-16T09:15:00', avatar: 'TT' },
  { id: 3, name: 'Lê Văn Thủ Thư', email: 'librarian2@fpt.edu.vn', role: 'Librarian', status: 'locked', lastActive: '2025-01-10T14:20:00', avatar: 'LT' },
  { id: 4, name: 'Nguyễn Hoàng Phúc', email: 'phucnh@fpt.edu.vn', role: 'Student', status: 'active', lastActive: '2025-01-16T08:45:00', avatar: 'NP', studentId: 'DE170706', score: 90 },
  { id: 5, name: 'Trần Văn An', email: 'antv@fpt.edu.vn', role: 'Student', status: 'active', lastActive: '2025-01-15T16:30:00', avatar: 'TA', studentId: 'DE170707', score: 75 },
  { id: 6, name: 'Lê Thị Bình', email: 'binhlt@fpt.edu.vn', role: 'Student', status: 'locked', lastActive: '2025-01-12T11:00:00', avatar: 'LB', studentId: 'DE170708', score: 45 },
  { id: 7, name: 'Phạm Minh Cường', email: 'cuongpm@fpt.edu.vn', role: 'Student', status: 'active', lastActive: '2025-01-16T07:30:00', avatar: 'PC', studentId: 'DE170709', score: 88 },
  { id: 8, name: 'Đỗ Hải Đăng', email: 'dangdh@fpt.edu.vn', role: 'Student', status: 'active', lastActive: '2025-01-14T13:45:00', avatar: 'ĐD', studentId: 'DE170710', score: 62 },
];

const ROLES = ['Tất cả', 'Admin', 'Librarian', 'Student'];
const STATUSES = ['Tất cả', 'Hoạt động', 'Đã khóa'];

const UserManagement = () => {
  const [searchText, setSearchText] = useState('');
  const [roleFilter, setRoleFilter] = useState('Tất cả');
  const [statusFilter, setStatusFilter] = useState('Tất cả');
  const [showAddModal, setShowAddModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [showLockModal, setShowLockModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [showActionMenu, setShowActionMenu] = useState(null);

  const filteredUsers = useMemo(() => {
    return MOCK_USERS.filter(user => {
      const matchSearch = user.name.toLowerCase().includes(searchText.toLowerCase()) ||
                         user.email.toLowerCase().includes(searchText.toLowerCase());
      const matchRole = roleFilter === 'Tất cả' || user.role === roleFilter;
      const matchStatus = statusFilter === 'Tất cả' || 
                         (statusFilter === 'Hoạt động' && user.status === 'active') ||
                         (statusFilter === 'Đã khóa' && user.status === 'locked');
      return matchSearch && matchRole && matchStatus;
    });
  }, [searchText, roleFilter, statusFilter]);

  const stats = useMemo(() => ({
    total: MOCK_USERS.length,
    admins: MOCK_USERS.filter(u => u.role === 'Admin').length,
    librarians: MOCK_USERS.filter(u => u.role === 'Librarian').length,
    students: MOCK_USERS.filter(u => u.role === 'Student').length,
    locked: MOCK_USERS.filter(u => u.status === 'locked').length,
  }), []);

  const getRoleColor = (role) => {
    switch(role) {
      case 'Admin': return { bg: '#FEE2E2', color: '#DC2626' };
      case 'Librarian': return { bg: '#DBEAFE', color: '#2563EB' };
      case 'Student': return { bg: '#D1FAE5', color: '#059669' };
      default: return { bg: '#F3F4F6', color: '#6B7280' };
    }
  };

  const formatLastActive = (dateStr) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000 / 60);
    if (diff < 60) return `${diff} phút trước`;
    if (diff < 1440) return `${Math.floor(diff / 60)} giờ trước`;
    return `${Math.floor(diff / 1440)} ngày trước`;
  };

  return (
    <>
      <Header 
        searchValue={searchText}
        onSearchChange={(e) => setSearchText(e.target.value)}
        searchPlaceholder="Tìm kiếm người dùng..."
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
              color: '#1A1A1A',
              margin: '0 0 4px 0'
            }}>Quản lý người dùng</h1>
            <p style={{
              fontSize: '14px',
              color: '#A0AEC0',
              margin: 0
            }}>Quản lý tài khoản Admin, Thủ thư và Sinh viên</p>
          </div>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={() => setShowImportModal(true)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '12px 20px',
                background: '#F7FAFC',
                border: '2px solid #E2E8F0',
                borderRadius: '12px',
                fontSize: '14px',
                fontWeight: '600',
                color: '#4A5568',
                cursor: 'pointer',
                transition: 'all 0.2s ease'
              }}
            >
              <Upload size={18} />
              Import CSV
            </button>
            <button
              onClick={() => setShowAddModal(true)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '12px 20px',
                background: '#FF751F',
                border: 'none',
                borderRadius: '12px',
                fontSize: '14px',
                fontWeight: '600',
                color: '#fff',
                cursor: 'pointer',
                boxShadow: '0 4px 14px rgba(255, 117, 31, 0.25)',
                transition: 'all 0.2s ease'
              }}
            >
              <UserPlus size={18} />
              Thêm Thủ thư
            </button>
          </div>
        </div>

        {/* Stats Cards */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(5, 1fr)',
          gap: '16px',
          marginBottom: '24px'
        }}>
          {[
            { label: 'Tổng người dùng', value: stats.total, icon: Users, color: '#7C3AED', bg: '#F3E8FF' },
            { label: 'Admin', value: stats.admins, icon: Shield, color: '#DC2626', bg: '#FEE2E2' },
            { label: 'Thủ thư', value: stats.librarians, icon: Key, color: '#2563EB', bg: '#DBEAFE' },
            { label: 'Sinh viên', value: stats.students, icon: Users, color: '#059669', bg: '#D1FAE5' },
            { label: 'Đã khóa', value: stats.locked, icon: Lock, color: '#DC2626', bg: '#FEE2E2' },
          ].map((stat, idx) => (
            <div key={idx} style={{
              background: '#fff',
              borderRadius: '12px',
              padding: '20px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
              display: 'flex',
              alignItems: 'center',
              gap: '16px'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '12px',
                background: stat.bg,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <stat.icon size={22} color={stat.color} />
              </div>
              <div>
                <div style={{ fontSize: '24px', fontWeight: '700', color: '#1A1A1A' }}>{stat.value}</div>
                <div style={{ fontSize: '13px', color: '#A0AEC0', fontWeight: '500' }}>{stat.label}</div>
              </div>
            </div>
          ))}
        </div>

        {/* Filters & Table */}
        <div style={{
          background: '#fff',
          borderRadius: '16px',
          boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
          overflow: 'hidden'
        }}>
          {/* Filter Bar */}
          <div style={{
            padding: '20px 24px',
            borderBottom: '1px solid #E2E8F0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '16px'
          }}>
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              <div style={{ position: 'relative' }}>
                <Search size={16} style={{
                  position: 'absolute',
                  left: '12px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: '#A0AEC0'
                }} />
                <input
                  type="text"
                  placeholder="Tìm theo tên, email..."
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  style={{
                    padding: '10px 12px 10px 40px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '10px',
                    fontSize: '14px',
                    width: '280px',
                    outline: 'none',
                    transition: 'border-color 0.2s'
                  }}
                />
              </div>
              
              <select
                value={roleFilter}
                onChange={(e) => setRoleFilter(e.target.value)}
                style={{
                  padding: '10px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '10px',
                  fontSize: '14px',
                  color: '#4A5568',
                  background: '#fff',
                  cursor: 'pointer',
                  outline: 'none'
                }}
              >
                {ROLES.map(role => (
                  <option key={role} value={role}>{role === 'Tất cả' ? 'Tất cả vai trò' : role}</option>
                ))}
              </select>

              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                style={{
                  padding: '10px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '10px',
                  fontSize: '14px',
                  color: '#4A5568',
                  background: '#fff',
                  cursor: 'pointer',
                  outline: 'none'
                }}
              >
                {STATUSES.map(status => (
                  <option key={status} value={status}>{status === 'Tất cả' ? 'Tất cả trạng thái' : status}</option>
                ))}
              </select>
            </div>

            <button
              onClick={() => setShowRoleModal(true)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '10px 16px',
                background: '#F7FAFC',
                border: '2px solid #E2E8F0',
                borderRadius: '10px',
                fontSize: '13px',
                fontWeight: '600',
                color: '#4A5568',
                cursor: 'pointer'
              }}
            >
              <Shield size={16} />
              Phân quyền
            </button>
          </div>

          {/* Table */}
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: '#F7FAFC' }}>
                  {['Người dùng', 'Email', 'Vai trò', 'Trạng thái', 'Hoạt động gần nhất', 'Thao tác'].map((header, idx) => (
                    <th key={idx} style={{
                      textAlign: idx === 5 ? 'center' : 'left',
                      padding: '16px 24px',
                      fontSize: '12px',
                      fontWeight: '600',
                      color: '#A0AEC0',
                      textTransform: 'uppercase',
                      letterSpacing: '0.5px'
                    }}>{header}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user, index) => {
                  const roleColors = getRoleColor(user.role);
                  return (
                    <tr 
                      key={user.id}
                      style={{
                        borderBottom: index === filteredUsers.length - 1 ? 'none' : '1px solid #E2E8F0',
                        transition: 'background-color 0.2s'
                      }}
                      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#FFF7F2'}
                      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                    >
                      <td style={{ padding: '16px 24px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <div style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: '10px',
                            background: 'linear-gradient(135deg, #FF751F, #FF9B5A)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#fff',
                            fontSize: '13px',
                            fontWeight: '600'
                          }}>{user.avatar}</div>
                          <div>
                            <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A' }}>{user.name}</div>
                            {user.studentId && (
                              <div style={{ fontSize: '12px', color: '#A0AEC0' }}>{user.studentId}</div>
                            )}
                          </div>
                        </div>
                      </td>
                      <td style={{ padding: '16px 24px' }}>
                        <span style={{ fontSize: '14px', color: '#4A5568' }}>{user.email}</span>
                      </td>
                      <td style={{ padding: '16px 24px' }}>
                        <span style={{
                          padding: '6px 12px',
                          borderRadius: '20px',
                          fontSize: '12px',
                          fontWeight: '600',
                          background: roleColors.bg,
                          color: roleColors.color
                        }}>{user.role}</span>
                      </td>
                      <td style={{ padding: '16px 24px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          {user.status === 'active' ? (
                            <>
                              <CheckCircle size={16} color="#059669" />
                              <span style={{ fontSize: '13px', color: '#059669', fontWeight: '500' }}>Hoạt động</span>
                            </>
                          ) : (
                            <>
                              <Lock size={16} color="#DC2626" />
                              <span style={{ fontSize: '13px', color: '#DC2626', fontWeight: '500' }}>Đã khóa</span>
                            </>
                          )}
                        </div>
                      </td>
                      <td style={{ padding: '16px 24px' }}>
                        <span style={{ fontSize: '13px', color: '#A0AEC0' }}>{formatLastActive(user.lastActive)}</span>
                      </td>
                      <td style={{ padding: '16px 24px', textAlign: 'center' }}>
                        <div style={{ position: 'relative', display: 'inline-block' }}>
                          <button
                            onClick={() => setShowActionMenu(showActionMenu === user.id ? null : user.id)}
                            style={{
                              padding: '8px',
                              background: '#F7FAFC',
                              border: 'none',
                              borderRadius: '8px',
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center'
                            }}
                          >
                            <MoreVertical size={18} color="#4A5568" />
                          </button>
                          
                          {showActionMenu === user.id && (
                            <div style={{
                              position: 'absolute',
                              top: '100%',
                              right: 0,
                              marginTop: '4px',
                              background: '#fff',
                              borderRadius: '12px',
                              boxShadow: '0 10px 40px rgba(0,0,0,0.15)',
                              border: '1px solid #E2E8F0',
                              minWidth: '180px',
                              zIndex: 100,
                              overflow: 'hidden'
                            }}>
                              <div 
                                onClick={() => { setSelectedUser(user); setShowActionMenu(null); }}
                                style={{
                                  padding: '12px 16px',
                                  display: 'flex',
                                  alignItems: 'center',
                                  gap: '10px',
                                  cursor: 'pointer',
                                  transition: 'background 0.2s'
                                }}
                                onMouseEnter={(e) => e.currentTarget.style.background = '#F7FAFC'}
                                onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                              >
                                <Eye size={16} color="#4A5568" />
                                <span style={{ fontSize: '14px', color: '#4A5568' }}>Xem chi tiết</span>
                              </div>
                              <div 
                                onClick={() => { setSelectedUser(user); setShowLockModal(true); setShowActionMenu(null); }}
                                style={{
                                  padding: '12px 16px',
                                  display: 'flex',
                                  alignItems: 'center',
                                  gap: '10px',
                                  cursor: 'pointer',
                                  transition: 'background 0.2s'
                                }}
                                onMouseEnter={(e) => e.currentTarget.style.background = '#F7FAFC'}
                                onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                              >
                                {user.status === 'active' ? (
                                  <>
                                    <Lock size={16} color="#F59E0B" />
                                    <span style={{ fontSize: '14px', color: '#F59E0B' }}>Khóa tài khoản</span>
                                  </>
                                ) : (
                                  <>
                                    <Unlock size={16} color="#059669" />
                                    <span style={{ fontSize: '14px', color: '#059669' }}>Mở khóa</span>
                                  </>
                                )}
                              </div>
                              <div 
                                style={{
                                  padding: '12px 16px',
                                  display: 'flex',
                                  alignItems: 'center',
                                  gap: '10px',
                                  cursor: 'pointer',
                                  borderTop: '1px solid #E2E8F0',
                                  transition: 'background 0.2s'
                                }}
                                onMouseEnter={(e) => e.currentTarget.style.background = '#FEE2E2'}
                                onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                              >
                                <Trash2 size={16} color="#DC2626" />
                                <span style={{ fontSize: '14px', color: '#DC2626' }}>Xóa tài khoản</span>
                              </div>
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Add Librarian Modal */}
      {showAddModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 2000
        }}>
          <div style={{
            background: '#fff',
            borderRadius: '20px',
            width: '500px',
            maxHeight: '90vh',
            overflow: 'auto',
            boxShadow: '0 20px 60px rgba(0,0,0,0.2)'
          }}>
            <div style={{
              padding: '24px',
              borderBottom: '1px solid #E2E8F0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A1A1A', margin: 0 }}>Thêm Thủ thư mới</h2>
              <button onClick={() => setShowAddModal(false)} style={{
                padding: '8px',
                background: '#F7FAFC',
                border: 'none',
                borderRadius: '8px',
                cursor: 'pointer'
              }}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div style={{ padding: '24px' }}>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Họ và tên
                </label>
                <input type="text" placeholder="Nhập họ và tên" style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  outline: 'none'
                }} />
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Email
                </label>
                <input type="email" placeholder="email@fpt.edu.vn" style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  outline: 'none'
                }} />
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Mật khẩu tạm thời
                </label>
                <input type="password" placeholder="••••••••" style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  outline: 'none'
                }} />
              </div>
              <div style={{ display: 'flex', gap: '12px', marginTop: '24px' }}>
                <button onClick={() => setShowAddModal(false)} style={{
                  flex: 1,
                  padding: '14px',
                  background: '#F7FAFC',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#4A5568',
                  cursor: 'pointer'
                }}>Hủy</button>
                <button style={{
                  flex: 1,
                  padding: '14px',
                  background: '#FF751F',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>Tạo tài khoản</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Import CSV Modal */}
      {showImportModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 2000
        }}>
          <div style={{
            background: '#fff',
            borderRadius: '20px',
            width: '550px',
            boxShadow: '0 20px 60px rgba(0,0,0,0.2)'
          }}>
            <div style={{
              padding: '24px',
              borderBottom: '1px solid #E2E8F0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A1A1A', margin: 0 }}>Import danh sách sinh viên</h2>
              <button onClick={() => setShowImportModal(false)} style={{
                padding: '8px',
                background: '#F7FAFC',
                border: 'none',
                borderRadius: '8px',
                cursor: 'pointer'
              }}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div style={{ padding: '24px' }}>
              <div style={{
                border: '2px dashed #E2E8F0',
                borderRadius: '16px',
                padding: '48px 24px',
                textAlign: 'center',
                background: '#F7FAFC',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}>
                <FileSpreadsheet size={48} color="#FF751F" style={{ marginBottom: '16px' }} />
                <p style={{ fontSize: '16px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 8px' }}>
                  Kéo thả file CSV/Excel vào đây
                </p>
                <p style={{ fontSize: '14px', color: '#A0AEC0', margin: '0 0 16px' }}>
                  hoặc nhấn để chọn file
                </p>
                <button style={{
                  padding: '10px 20px',
                  background: '#FF751F',
                  border: 'none',
                  borderRadius: '10px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>Chọn file</button>
              </div>
              <div style={{ marginTop: '20px', display: 'flex', alignItems: 'center', gap: '12px' }}>
                <Download size={18} color="#FF751F" />
                <a href="#" style={{ fontSize: '14px', color: '#FF751F', fontWeight: '500' }}>
                  Tải template mẫu (.xlsx)
                </a>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Lock/Unlock Modal */}
      {showLockModal && selectedUser && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 2000
        }}>
          <div style={{
            background: '#fff',
            borderRadius: '20px',
            width: '450px',
            boxShadow: '0 20px 60px rgba(0,0,0,0.2)'
          }}>
            <div style={{ padding: '32px', textAlign: 'center' }}>
              <div style={{
                width: '64px',
                height: '64px',
                borderRadius: '16px',
                background: selectedUser.status === 'active' ? '#FEF3C7' : '#D1FAE5',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 20px'
              }}>
                {selectedUser.status === 'active' ? (
                  <Lock size={28} color="#F59E0B" />
                ) : (
                  <Unlock size={28} color="#059669" />
                )}
              </div>
              <h3 style={{ fontSize: '20px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 8px' }}>
                {selectedUser.status === 'active' ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}
              </h3>
              <p style={{ fontSize: '14px', color: '#4A5568', margin: '0 0 20px' }}>
                Bạn có chắc muốn {selectedUser.status === 'active' ? 'khóa' : 'mở khóa'} tài khoản <strong>{selectedUser.name}</strong>?
              </p>
              {selectedUser.status === 'active' && (
                <div style={{ marginBottom: '20px', textAlign: 'left' }}>
                  <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                    Lý do khóa tài khoản
                  </label>
                  <textarea placeholder="Nhập lý do..." style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none',
                    resize: 'none',
                    height: '80px'
                  }} />
                </div>
              )}
              <div style={{ display: 'flex', gap: '12px' }}>
                <button onClick={() => { setShowLockModal(false); setSelectedUser(null); }} style={{
                  flex: 1,
                  padding: '14px',
                  background: '#F7FAFC',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#4A5568',
                  cursor: 'pointer'
                }}>Hủy</button>
                <button style={{
                  flex: 1,
                  padding: '14px',
                  background: selectedUser.status === 'active' ? '#F59E0B' : '#059669',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>{selectedUser.status === 'active' ? 'Khóa tài khoản' : 'Mở khóa'}</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Role Configuration Modal */}
      {showRoleModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 2000
        }}>
          <div style={{
            background: '#fff',
            borderRadius: '20px',
            width: '700px',
            maxHeight: '90vh',
            overflow: 'auto',
            boxShadow: '0 20px 60px rgba(0,0,0,0.2)'
          }}>
            <div style={{
              padding: '24px',
              borderBottom: '1px solid #E2E8F0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A1A1A', margin: 0 }}>Phân quyền vai trò</h2>
              <button onClick={() => setShowRoleModal(false)} style={{
                padding: '8px',
                background: '#F7FAFC',
                border: 'none',
                borderRadius: '8px',
                cursor: 'pointer'
              }}>
                <X size={20} color="#4A5568" />
              </button>
            </div>
            <div style={{ padding: '24px' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    <th style={{ textAlign: 'left', padding: '12px', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', borderBottom: '2px solid #E2E8F0' }}>Chức năng</th>
                    <th style={{ textAlign: 'center', padding: '12px', fontSize: '14px', fontWeight: '600', color: '#DC2626', borderBottom: '2px solid #E2E8F0' }}>Admin</th>
                    <th style={{ textAlign: 'center', padding: '12px', fontSize: '14px', fontWeight: '600', color: '#2563EB', borderBottom: '2px solid #E2E8F0' }}>Librarian</th>
                  </tr>
                </thead>
                <tbody>
                  {[
                    'Quản lý người dùng',
                    'Quản lý chỗ ngồi',
                    'Xem báo cáo thống kê',
                    'Cấu hình hệ thống',
                    'Quản lý thiết bị NFC',
                    'Quản lý thông báo',
                    'Xử lý vi phạm',
                    'Cấu hình AI'
                  ].map((feature, idx) => (
                    <tr key={idx}>
                      <td style={{ padding: '12px', fontSize: '14px', color: '#4A5568', borderBottom: '1px solid #E2E8F0' }}>{feature}</td>
                      <td style={{ padding: '12px', textAlign: 'center', borderBottom: '1px solid #E2E8F0' }}>
                        <input type="checkbox" defaultChecked style={{ width: '18px', height: '18px', cursor: 'pointer' }} />
                      </td>
                      <td style={{ padding: '12px', textAlign: 'center', borderBottom: '1px solid #E2E8F0' }}>
                        <input type="checkbox" defaultChecked={idx < 5} style={{ width: '18px', height: '18px', cursor: 'pointer' }} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div style={{ display: 'flex', gap: '12px', marginTop: '24px', justifyContent: 'flex-end' }}>
                <button onClick={() => setShowRoleModal(false)} style={{
                  padding: '12px 24px',
                  background: '#F7FAFC',
                  border: '2px solid #E2E8F0',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#4A5568',
                  cursor: 'pointer'
                }}>Hủy</button>
                <button style={{
                  padding: '12px 24px',
                  background: '#FF751F',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>Lưu thay đổi</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default UserManagement;

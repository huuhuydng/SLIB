    import React, { useState, useMemo } from 'react';
    import { 
  Filter, ThumbsUp, AlertTriangle, CheckCircle2
} from 'lucide-react';
    import Header from "../../../components/shared/Header";
    import '../../../styles/librarian/ViolationManage.css';
    import { handleLogout } from "../../../utils/auth";
    const INITIAL_STUDENTS = [
    { id: 1, studentId: 'DE170706', name: 'Nguyễn Hoàng Phúc', email: 'phucnhde170706@fpt.edu.vn', score: 90, avatar: 'https://i.pravatar.cc/150?u=1' },
    { id: 2, studentId: 'DE170707', name: 'Trần Văn An', email: 'antv@fpt.edu.vn', score: 69, avatar: 'https://i.pravatar.cc/150?u=2' },
    { id: 3, studentId: 'DE170708', name: 'Lê Thị Bình', email: 'binhlt@fpt.edu.vn', score: 59, avatar: 'https://i.pravatar.cc/150?u=3' },
    { id: 4, studentId: 'DE170709', name: 'Phạm Minh Cường', email: 'cuongpm@fpt.edu.vn', score: 90, avatar: 'https://i.pravatar.cc/150?u=4' },
    { id: 5, studentId: 'DE170710', name: 'Đỗ Hải Đăng', email: 'dangdh@fpt.edu.vn', score: 69, avatar: 'https://i.pravatar.cc/150?u=5' },
    { id: 6, studentId: 'DE170711', name: 'Hoàng Thùy Linh', email: 'linhht@fpt.edu.vn', score: 59, avatar: 'https://i.pravatar.cc/150?u=6' },
    { id: 7, studentId: 'DE170712', name: 'Ngô Kiến Huy', email: 'huynk@fpt.edu.vn', score: 90, avatar: 'https://i.pravatar.cc/150?u=7' },
    { id: 8, studentId: 'DE170713', name: 'Sơn Tùng MTP', email: 'tungmtp@fpt.edu.vn', score: 69, avatar: 'https://i.pravatar.cc/150?u=8' },
    { id: 9, studentId: 'DE170714', name: 'Đen Vâu', email: 'denvau@fpt.edu.vn', score: 59, avatar: 'https://i.pravatar.cc/150?u=9' },
    { id: 10, studentId: 'DE170715', name: 'Bích Phương', email: 'phuongb@fpt.edu.vn', score: 90, avatar: 'https://i.pravatar.cc/150?u=10' },
    { id: 11, studentId: 'DE170716', name: 'Tóc Tiên', email: 'tien@fpt.edu.vn', score: 69, avatar: 'https://i.pravatar.cc/150?u=11' },
    { id: 12, studentId: 'DE170717', name: 'Mỹ Tâm', email: 'tam@fpt.edu.vn', score: 59, avatar: 'https://i.pravatar.cc/150?u=12' },
    ];

    const MOCK_VIOLATIONS = [
    { id: 1, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 5, level: 'bad' },
    { id: 2, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 10, level: 'average' },
    { id: 3, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 5, level: 'bad' },
    { id: 4, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 9, level: 'good' },
    { id: 5, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 5, level: 'average' },
    { id: 6, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 5, level: 'bad' },
    { id: 7, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 5, level: 'bad' },
    { id: 8, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 10, level: 'average' },
    { id: 9, date: '15/12/2025', reason: 'Gây mất trật tự', minus: 1, level: 'average' },
    { id: 10, date: '15/12/2025', reason: 'Sử dụng thức ăn trong thư viện', minus: 5, level: 'bad' },
    ];

    const ViolationManage = () => {
    // State
    const [currentView, setCurrentView] = useState('list'); // 'list' | 'detail'
    const [students, setStudents] = useState(INITIAL_STUDENTS);
    const [selectedStudentId, setSelectedStudentId] = useState(null);
    
    // Search & Filter State (List View)
    const [searchQuery, setSearchQuery] = useState('');
    const [filterRank, setFilterRank] = useState('all'); // 'all', 'good', 'average', 'bad'
    const [isFilterOpen, setIsFilterOpen] = useState(false);

    // Edit State (Detail View)
    const [newScore, setNewScore] = useState('');
    const [editReason, setEditReason] = useState('');

    // --- LOGIC HELPER ---
    const getRankInfo = (score) => {
        if (score >= 85) return { key: 'good', label: 'Gương mẫu', color: 'var(--green)', class: 'good' };
        if (score >= 65) return { key: 'average', label: 'Khá', color: 'var(--yellow)', class: 'average' };
        return { key: 'bad', label: 'Trung bình', color: 'var(--red)', class: 'bad' };
    };

    const selectedStudent = useMemo(() => {
        return students.find(s => s.studentId === selectedStudentId);
    }, [students, selectedStudentId]);

    const filteredStudents = useMemo(() => {
        return students.filter(s => {
        const matchSearch = s.studentId.toLowerCase().includes(searchQuery.toLowerCase()) || 
                            s.name.toLowerCase().includes(searchQuery.toLowerCase());
        
        let matchFilter = true;
        const rank = getRankInfo(s.score);
        if (filterRank !== 'all') {
            matchFilter = rank.key === filterRank;
        }
        return matchSearch && matchFilter;
        });
    }, [students, searchQuery, filterRank]);

    // --- HANDLERS ---
    const handleRowClick = (studentId) => {
        setSelectedStudentId(studentId);
        // Reset form
        const st = students.find(s => s.studentId === studentId);
        setNewScore(st ? st.score : '');
        setEditReason('');
        setCurrentView('detail');
    };

    const handleBack = () => {
        setCurrentView('list');
        setSelectedStudentId(null);
    };

    const handleUpdateScore = () => {
  if (!selectedStudentId) return;

  const scoreNum = parseInt(newScore, 10);

  // ❌ Không alert khi sai, chỉ return
  if (isNaN(scoreNum) || scoreNum < 0 || scoreNum > 100) {
    return;
  }

  // ✅ Cập nhật điểm
  setStudents(prev =>
    prev.map(s =>
      s.studentId === selectedStudentId
        ? { ...s, score: scoreNum }
        : s
    )
  );

  // ✅ Reset lí do
  setEditReason('');

  // ✅ Reset chiều cao textarea
  if (textareaRef.current) {
    textareaRef.current.style.height = '120px';
  }

  // ✅ ALERT CHỈ KHI THÀNH CÔNG
  alert('Đã cập nhật điểm đánh giá thành công!');
};


    // --- RENDER COMPONENTS ---

    const renderTopbar = () => (
        <Header 
            searchValue={searchQuery}
            onSearchChange={(e) => setSearchQuery(e.target.value)}
            searchPlaceholder="Search for anything..."
            showBackButton={currentView === 'detail'}
            onBackClick={handleBack}
            onLogout={handleLogout}
        />
    );

    const renderListView = () => (
        <>
        <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '20px',
            marginBottom: '24px'
        }}>
            <div style={{
                backgroundColor: '#fff',
                borderRadius: '16px',
                padding: '24px',
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
            }}>
            <div style={{
                width: '56px',
                height: '56px',
                borderRadius: '14px',
                backgroundColor: '#d4f4dd',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#16a34a'
            }}><CheckCircle2 size={24} /></div>
            <div>
                <h3 style={{ fontSize: '28px', fontWeight: '700', color: '#1a1a1a', margin: 0 }}>69</h3>
                <p style={{ fontSize: '14px', color: '#666', margin: 0 }}>sinh viên</p>
            </div>
            </div>
            <div style={{
                backgroundColor: '#fff',
                borderRadius: '16px',
                padding: '24px',
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
            }}>
            <div style={{
                width: '56px',
                height: '56px',
                borderRadius: '14px',
                backgroundColor: '#fef3c7',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#f59e0b'
            }}><ThumbsUp size={24} /></div>
            <div>
                <h3 style={{ fontSize: '28px', fontWeight: '700', color: '#1a1a1a', margin: 0 }}>36</h3>
                <p style={{ fontSize: '14px', color: '#666', margin: 0 }}>Điểm đánh giá trên 65%</p>
            </div>
            </div>
            <div style={{
                backgroundColor: '#fff',
                borderRadius: '16px',
                padding: '24px',
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
            }}>
            <div style={{
                width: '56px',
                height: '56px',
                borderRadius: '14px',
                backgroundColor: '#fecaca',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#dc2626'
            }}><AlertTriangle size={24} /></div>
            <div>
                <h3 style={{ fontSize: '28px', fontWeight: '700', color: '#1a1a1a', margin: 0 }}>18</h3>
                <p style={{ fontSize: '14px', color: '#666', margin: 0 }}>Điểm đánh giá dưới 65%</p>
            </div>
            </div>
        </div>

        <div style={{
            backgroundColor: '#fff',
            borderRadius: '16px',
            padding: '24px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
        }}>
            <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: '20px',
                flexWrap: 'wrap',
                gap: '16px'
            }}>
            <h3 style={{ fontSize: '18px', fontWeight: '600', color: '#1a1a1a', margin: 0 }}>Danh sách điểm đánh giá sinh viên</h3>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div>
                <input 
                    type="text" 
                    placeholder="Tìm kiếm mã số sinh viên"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    style={{
                        padding: '8px 16px',
                        border: '1px solid #e0e0e0',
                        borderRadius: '10px',
                        fontSize: '14px',
                        outline: 'none',
                        width: '220px'
                    }}
                />
                </div>
                <div style={{ position: 'relative' }}>
                <button onClick={() => setIsFilterOpen(!isFilterOpen)} style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: '40px',
                    height: '40px',
                    border: '1px solid #e0e0e0',
                    borderRadius: '10px',
                    backgroundColor: '#fff',
                    cursor: 'pointer',
                    transition: 'all 0.2s'
                }}>
                    <Filter size={20} />
                </button>
                {isFilterOpen && (
                    <div style={{
                        position: 'absolute',
                        top: '48px',
                        right: 0,
                        backgroundColor: '#fff',
                        borderRadius: '12px',
                        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                        padding: '8px',
                        minWidth: '200px',
                        zIndex: 10
                    }}>
                    <div style={{ padding: '8px 12px', fontSize: '12px', fontWeight: '600', color: '#666', textTransform: 'uppercase' }}>Điểm đánh giá</div>
                    <div onClick={() => { setFilterRank('all'); setIsFilterOpen(false); }} style={{
                        padding: '10px 12px',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontSize: '14px',
                        transition: 'background 0.2s'
                    }} onMouseEnter={(e) => e.target.style.backgroundColor = '#f5f5f5'} onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}>
                        Tất cả
                    </div>
                    <div onClick={() => { setFilterRank('good'); setIsFilterOpen(false); }} style={{
                        padding: '10px 12px',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontSize: '14px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        transition: 'background 0.2s'
                    }} onMouseEnter={(e) => e.target.style.backgroundColor = '#f5f5f5'} onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}>
                        <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#16a34a' }}></span> Gương mẫu
                    </div>
                    <div onClick={() => { setFilterRank('average'); setIsFilterOpen(false); }} style={{
                        padding: '10px 12px',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontSize: '14px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        transition: 'background 0.2s'
                    }} onMouseEnter={(e) => e.target.style.backgroundColor = '#f5f5f5'} onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}>
                        <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#f59e0b' }}></span> Khá
                    </div>
                    <div onClick={() => { setFilterRank('bad'); setIsFilterOpen(false); }} style={{
                        padding: '10px 12px',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontSize: '14px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        transition: 'background 0.2s'
                    }} onMouseEnter={(e) => e.target.style.backgroundColor = '#f5f5f5'} onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}>
                        <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#dc2626' }}></span> Trung bình
                    </div>
                    </div>
                )}
                </div>
            </div>
            </div>

            <div style={{ overflowX: 'auto' }}>
            <table style={{
                width: '100%',
                borderCollapse: 'collapse'
            }}>
                <thead>
                <tr style={{ borderBottom: '2px solid #e5e7eb' }}>
                    <th style={{ 
                        padding: '12px 16px',
                        textAlign: 'left',
                        fontSize: '13px',
                        fontWeight: '600',
                        color: '#6b7280',
                        textTransform: 'uppercase'
                    }}>Tên sinh viên</th>
                    <th style={{ 
                        padding: '12px 16px',
                        textAlign: 'left',
                        fontSize: '13px',
                        fontWeight: '600',
                        color: '#6b7280',
                        textTransform: 'uppercase'
                    }}>Mã số sinh viên</th>
                    <th style={{ 
                        padding: '12px 16px',
                        textAlign: 'center',
                        fontSize: '13px',
                        fontWeight: '600',
                        color: '#6b7280',
                        textTransform: 'uppercase'
                    }}>Điểm đánh giá</th>
                </tr>
                </thead>
                <tbody>
                {filteredStudents.map(student => {
                    const rank = getRankInfo(student.score);
                    return (
                    <tr key={student.id} onClick={() => handleRowClick(student.studentId)} style={{
                        borderBottom: '1px solid #f3f4f6',
                        cursor: 'pointer'
                    }}>
                        <td style={{ 
                            padding: '14px 16px',
                            fontSize: '14px',
                            fontWeight: '500',
                            color: '#1f2937'
                        }}>{student.name}</td>
                        <td style={{ 
                            padding: '14px 16px',
                            fontSize: '14px',
                            color: '#6b7280'
                        }}>{student.studentId}</td>
                        <td style={{ 
                            padding: '14px 16px',
                            textAlign: 'center'
                        }}>
                        <span className={`score-badge ${rank.class}`}>{student.score}</span>
                        </td>
                    </tr>
                    )
                })}
                </tbody>
            </table>
            </div>
        </div>
        </>
    );

    const renderDetailView = () => {
        if (!selectedStudent) return null;
        const rank = getRankInfo(selectedStudent.score);

        return (
        <>
            {/* Info & Rank Row */}
            <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
                gap: '20px',
                marginBottom: '24px'
            }}>
            <div style={{
                backgroundColor: '#fff',
                borderRadius: '16px',
                padding: '24px',
                display: 'flex',
                alignItems: 'center',
                gap: '20px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
            }}>
                <img src={selectedStudent.avatar} alt="Avatar" style={{
                    width: '80px',
                    height: '80px',
                    borderRadius: '16px',
                    objectFit: 'cover'
                }} />
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <p style={{ fontSize: '14px', color: '#666', margin: 0 }}>Tên: <span style={{ fontWeight: '600', color: '#1a1a1a' }}>{selectedStudent.name}</span></p>
                <p style={{ fontSize: '14px', color: '#666', margin: 0 }}>Mã sinh viên: {selectedStudent.studentId}</p>
                <p style={{ fontSize: '14px', color: '#666', margin: 0 }}>Email: <span style={{ color: '#3b82f6' }}>{selectedStudent.email}</span></p>
                </div>
            </div>

            <div style={{
                backgroundColor: '#fff',
                borderRadius: '16px',
                padding: '24px',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '16px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
            }}>
                <div style={{ position: 'relative', width: '120px', height: '120px' }}>
                <div style={{
                    width: '120px',
                    height: '120px',
                    borderRadius: '50%',
                    background: `conic-gradient(${rank.color} ${selectedStudent.score}%, #eee 0)`,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                }}>
                    <div style={{
                        width: '90px',
                        height: '90px',
                        borderRadius: '50%',
                        backgroundColor: '#fff',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}>
                    <span style={{ fontSize: '32px', fontWeight: '700', color: rank.color }}>{selectedStudent.score}</span>
                    <span style={{ fontSize: '14px', color: '#999' }}>/100</span>
                    </div>
                </div>
                </div>
                <div style={{ textAlign: 'center' }}>
                <h4 style={{ fontSize: '16px', fontWeight: '600', color: '#1a1a1a', margin: 0, display: 'flex', alignItems: 'center', gap: '6px', justifyContent: 'center' }}>Hạng: {rank.label} {rank.key === 'good' && <CheckCircle2 size={18} style={{ color: '#16a34a' }} />}</h4>
                </div>
            </div>
            </div>

            {/* Bottom Split */}
            <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
                gap: '20px'
            }}>
            {/* Left: Violation List */}
            <div style={{
                backgroundColor: '#fff',
                borderRadius: '16px',
                padding: '24px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
            }}>
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: '20px'
                }}>
                <h3 style={{ fontSize: '18px', fontWeight: '600', color: '#1a1a1a', margin: 0 }}>Danh sách vi phạm của sinh viên</h3>
                <Filter size={20} color="#888" />
                </div>
                <div style={{ overflowX: 'auto' }}>
                <table style={{
                    width: '100%',
                    borderCollapse: 'collapse'
                }}>
                    <thead>
                    <tr>
                        <th style={{
                            textAlign: 'left',
                            padding: '12px 16px',
                            fontSize: '13px',
                            fontWeight: '600',
                            color: '#666',
                            borderBottom: '1px solid #e0e0e0',
                            textTransform: 'uppercase',
                            letterSpacing: '0.5px'
                        }}>Thời gian</th>
                        <th style={{
                            textAlign: 'left',
                            padding: '12px 16px',
                            fontSize: '13px',
                            fontWeight: '600',
                            color: '#666',
                            borderBottom: '1px solid #e0e0e0',
                            textTransform: 'uppercase',
                            letterSpacing: '0.5px'
                        }}>Lỗi vi phạm</th>
                        <th style={{
                            textAlign: 'center',
                            padding: '12px 16px',
                            fontSize: '13px',
                            fontWeight: '600',
                            color: '#666',
                            borderBottom: '1px solid #e0e0e0',
                            textTransform: 'uppercase',
                            letterSpacing: '0.5px'
                        }}>Số điểm trừ</th>
                    </tr>
                    </thead>
                    <tbody>
                        {MOCK_VIOLATIONS.map((v, i) => {
                        const badgeClass = v.minus >= 85 ? 'good' : (v.minus >= 65 ? 'average' : 'bad');
                        const badgeStyles = {
                            good: { backgroundColor: '#d4f4dd', color: '#16a34a' },
                            average: { backgroundColor: '#fef3c7', color: '#f59e0b' },
                            bad: { backgroundColor: '#fecaca', color: '#dc2626' }
                        };
                        return (
                            <tr key={i}>
                            <td style={{ padding: '14px 16px', fontSize: '14px', color: '#666', borderBottom: '1px solid #f0f0f0' }}>{v.date}</td>
                            <td style={{ padding: '14px 16px', fontSize: '14px', color: '#666', borderBottom: '1px solid #f0f0f0' }}>{v.reason}</td>
                            <td style={{ padding: '14px 16px', textAlign: 'center', borderBottom: '1px solid #f0f0f0' }}>
                                <span style={{
                                    display: 'inline-block',
                                    padding: '4px 12px',
                                    borderRadius: '8px',
                                    fontSize: '13px',
                                    fontWeight: '600',
                                    ...badgeStyles[badgeClass]
                                }}>{v.minus}</span>
                            </td>
                            </tr>
                        )
                        })}
                    </tbody>
                </table>
                </div>
            </div>

            {/* Right: Edit Form */}
            <div style={{
                backgroundColor: '#fff',
                borderRadius: '16px',
                padding: '24px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
            }}>
    <h3 style={{ fontSize: '18px', fontWeight: '600', color: '#1a1a1a', marginBottom: '20px' }}>Chỉnh sửa điểm đánh giá sinh viên</h3>
    
    <div style={{ marginBottom: '20px' }}>
        <label style={{ display: 'block', fontSize: '14px', fontWeight: '500', color: '#666', marginBottom: '8px' }}>Số điểm muốn thay đổi:</label>
        <div style={{ display: 'flex', gap: '12px' }}>
        <input 
            type="number" 
            value={newScore}
            onChange={(e) => setNewScore(e.target.value)}
            style={{
                flex: 1,
                padding: '10px 16px',
                border: '1px solid #e0e0e0',
                borderRadius: '10px',
                fontSize: '14px',
                outline: 'none'
            }}
        />
        <button onClick={handleUpdateScore} style={{
            padding: '10px 24px',
            backgroundColor: '#3b82f6',
            color: '#fff',
            border: 'none',
            borderRadius: '10px',
            fontSize: '14px',
            fontWeight: '600',
            cursor: 'pointer',
            transition: 'background 0.2s',
            whiteSpace: 'nowrap'
        }} onMouseEnter={(e) => e.target.style.backgroundColor = '#2563eb'} onMouseLeave={(e) => e.target.style.backgroundColor = '#3b82f6'}>
            Xác nhận
        </button>
        </div>
    </div>

    <div>
        <textarea
        value={editReason}
        onChange={(e) => setEditReason(e.target.value)}
        placeholder="Nhập lí do thay đổi điểm..."
        style={{
            width: '100%',
            minHeight: '120px',
            padding: '12px 16px',
            border: '1px solid #e0e0e0',
            borderRadius: '10px',
            fontSize: '14px',
            outline: 'none',
            resize: 'vertical',
            fontFamily: 'inherit'
        }}
        />
    </div>
    </div>
            </div>
        </>
        );
    };

    return (
        <>
            {renderTopbar()}
            
            <div style={{
                padding: '2rem',
                maxWidth: '1400px',
                margin: '0 auto',
                backgroundColor: '#f9fafb',
                minHeight: 'calc(100vh - 80px)'
            }}>
            <h2 style={{ fontSize: '28px', fontWeight: '700', color: '#1a1a1a', marginBottom: '24px' }}>Quản lý vi phạm</h2>
            {currentView === 'list' ? renderListView() : renderDetailView()}
            </div>
        </>
    );
    };

    export default ViolationManage;
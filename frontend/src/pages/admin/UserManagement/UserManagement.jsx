import React, { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import {
  Users,
  Search,
  Plus,
  Upload,
  MoreVertical,
  Shield,
  Lock,
  Unlock,
  Trash2,
  Eye,
  UserPlus,
  Download,
  CheckCircle,
  XCircle,
  AlertTriangle,
  X,
  FileSpreadsheet,
  Key,
  RefreshCw,
  Loader2
} from 'lucide-react';

import userService from '../../../services/userService';
import UserDetailsModal from '../../../components/admin/UserDetailsModal';
import DeleteUserModal from '../../../components/admin/DeleteUserModal';

const ROLES = ['Tất cả', 'ADMIN', 'LIBRARIAN', 'STUDENT'];
const STATUSES = ['Tất cả', 'Hoạt động', 'Đã khóa'];

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [searchText, setSearchText] = useState('');
  const [roleFilter, setRoleFilter] = useState('Tất cả');
  const [statusFilter, setStatusFilter] = useState('Tất cả');

  const [showAddModal, setShowAddModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [showLockModal, setShowLockModal] = useState(false);
  const [showResetPasswordModal, setShowResetPasswordModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [showActionMenu, setShowActionMenu] = useState(null);
  const [showUserDetailsModal, setShowUserDetailsModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  // Import state
  const [importStep, setImportStep] = useState('upload'); // upload, processing, preview, uploading, result
  const [importData, setImportData] = useState([]);
  const [importResult, setImportResult] = useState(null);
  const [importing, setImporting] = useState(false);
  const [avatarFiles, setAvatarFiles] = useState({}); // { userCode: File }
  const [validationErrors, setValidationErrors] = useState({}); // { userCode: { field: error } }
  const [uploadProgress, setUploadProgress] = useState(0);
  const [processingStatus, setProcessingStatus] = useState('');
  const [previewTab, setPreviewTab] = useState('success'); // 'success' or 'error'

  // Server-side import (new advanced import)
  const [importMode, setImportMode] = useState('client'); // 'client' or 'server'
  const [serverImportProgress, setServerImportProgress] = useState(null); // ImportJob status

  // Track uploaded avatars for cleanup on cancel
  const uploadedAvatarUrlsRef = useRef([]);

  // Add librarian state
  const [newLibrarian, setNewLibrarian] = useState({ fullName: '', email: '' });
  const [addingLibrarian, setAddingLibrarian] = useState(false);

  // Action states
  const [actionLoading, setActionLoading] = useState(false);

  // Fetch users
  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await userService.getAllUsers();
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error fetching users:', err);
      setError('Không thể tải danh sách người dùng');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const filteredUsers = useMemo(() => {
    return users.filter(user => {
      const matchSearch =
        (user.fullName || '').toLowerCase().includes(searchText.toLowerCase()) ||
        (user.email || '').toLowerCase().includes(searchText.toLowerCase()) ||
        (user.userCode || '').toLowerCase().includes(searchText.toLowerCase());
      const matchRole = roleFilter === 'Tất cả' || user.role === roleFilter;
      const matchStatus = statusFilter === 'Tất cả' ||
        (statusFilter === 'Hoạt động' && user.isActive !== false) ||
        (statusFilter === 'Đã khóa' && user.isActive === false);
      return matchSearch && matchRole && matchStatus;
    });
  }, [users, searchText, roleFilter, statusFilter]);

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const USERS_PER_PAGE = 50;
  const totalPages = Math.ceil(filteredUsers.length / USERS_PER_PAGE);

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchText, roleFilter, statusFilter]);

  const paginatedUsers = useMemo(() => {
    const startIndex = (currentPage - 1) * USERS_PER_PAGE;
    return filteredUsers.slice(startIndex, startIndex + USERS_PER_PAGE);
  }, [filteredUsers, currentPage]);

  const stats = useMemo(() => ({
    total: users.length,
    admins: users.filter(u => u.role === 'ADMIN').length,
    librarians: users.filter(u => u.role === 'LIBRARIAN').length,
    students: users.filter(u => u.role === 'STUDENT').length,
    locked: users.filter(u => u.isActive === false).length,
  }), [users]);

  const getRoleColor = (role) => {
    switch (role) {
      case 'ADMIN': return { bg: '#FEE2E2', color: '#DC2626' };
      case 'LIBRARIAN': return { bg: '#DBEAFE', color: '#2563EB' };
      case 'STUDENT': return { bg: '#D1FAE5', color: '#059669' };
      default: return { bg: '#F3F4F6', color: '#6B7280' };
    }
  };

  const getRoleLabel = (role) => {
    switch (role) {
      case 'ADMIN': return 'Admin';
      case 'LIBRARIAN': return 'Thủ thư';
      case 'STUDENT': return 'Sinh viên';
      default: return role;
    }
  };

  const formatLastActive = (dateStr) => {
    if (!dateStr) return 'Chưa hoạt động';
    const date = new Date(dateStr);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000 / 60);
    if (diff < 60) return `${diff} phút trước`;
    if (diff < 1440) return `${Math.floor(diff / 60)} giờ trước`;
    return `${Math.floor(diff / 1440)} ngày trước`;
  };

  // Handle file upload for import (Excel or Zip)
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const fileName = file.name.toLowerCase();

    // Skip Mac metadata files
    if (fileName.startsWith('._') || fileName.startsWith('__macosx')) {
      console.log('⏭️ Skipping metadata file:', fileName);
      return;
    }

    // Reset input to allow re-upload of same file
    event.target.value = '';

    // Always use fast server-side import for Excel files (no preview needed)
    if (fileName.endsWith('.xlsx') || fileName.endsWith('.xls')) {
      handleServerImport(file);
      return;
    }

    // ZIP files - extract and show preview (for avatar support)
    if (fileName.endsWith('.zip')) {
      try {
        setImportStep('processing');
        setProcessingStatus('Đang giải nén file ZIP...');

        const result = await userService.parseZipFile(file);

        if (!result.users || result.users.length === 0) {
          throw new Error('Không tìm thấy dữ liệu người dùng trong file ZIP');
        }

        const avatars = result.avatars || {};
        setAvatarFiles(avatars);
        setProcessingStatus(`Tìm thấy ${result.users.length} người dùng và ${Object.keys(avatars).length} ảnh đại diện`);

        // Validate locally
        setProcessingStatus('Đang kiểm tra dữ liệu...');
        const errors = userService.validateUsersLocally(result.users);
        setValidationErrors(errors);

        // Mark users with avatars and create preview URLs
        const usersWithAvatars = result.users.map(user => {
          const avatarFile = avatars[user.userCode?.toUpperCase()];
          return {
            ...user,
            hasAvatar: !!avatarFile,
            avatarPreviewUrl: avatarFile ? URL.createObjectURL(avatarFile) : null
          };
        });

        setImportData(usersWithAvatars);
        setImportStep('preview');
      } catch (err) {
        console.error('ZIP processing error:', err);
        alert('Lỗi xử lý file ZIP: ' + err.message);
        setImportStep('upload');
      }
      return;
    }

    // Unsupported format
    alert('Định dạng file không được hỗ trợ. Vui lòng sử dụng file Excel (.xlsx) hoặc ZIP.');
    setImportStep('upload');
  };

  // Execute import with avatar upload
  const handleImport = async () => {
    if (importData.length === 0) return;

    // Check for validation errors
    const hasErrors = Object.keys(validationErrors).length > 0;
    if (hasErrors) {
      const confirm = window.confirm('Có một số lỗi trong dữ liệu. Bạn có muốn tiếp tục import những người dùng hợp lệ không?');
      if (!confirm) return;
    }

    try {
      setImporting(true);
      setImportStep('uploading');
      setUploadProgress(0);
      setServerImportProgress(null);

      // Filter out users with errors
      let usersToImport = importData.filter(user => !validationErrors[user.userCode]);
      const totalUsers = usersToImport.length;

      // Upload avatars in CHUNKS (progress visible!)
      const avatarUserCodes = Object.keys(avatarFiles);
      const avatarUrlMap = {}; // Define outside block for rollback access

      if (avatarUserCodes.length > 0) {
        const total = avatarUserCodes.length;
        const CHUNK_SIZE = 100; // Upload 100 avatars at a time for speed
        let uploadedCount = 0;
        let uploadCancelled = false;

        setProcessingStatus(`Đang tải ${total} ảnh đại diện lên...`);
        setUploadProgress(10);
        setServerImportProgress({ status: 'UPLOADING_AVATARS', totalRows: total, importedCount: 0 });

        try {
          // Split avatarFiles into chunks
          const chunks = [];
          const entries = Object.entries(avatarFiles);
          for (let i = 0; i < entries.length; i += CHUNK_SIZE) {
            chunks.push(Object.fromEntries(entries.slice(i, i + CHUNK_SIZE)));
          }

          // Upload chunks with limited concurrency, update progress as each completes
          const CONCURRENCY = 5; // 5 concurrent uploads x 100 = 500 images at once
          let chunkIndex = 0;

          const uploadNextChunk = async () => {
            if (uploadCancelled || chunkIndex >= chunks.length) return;

            const currentIndex = chunkIndex++;
            const chunk = chunks[currentIndex];

            try {
              const batchResult = await userService.uploadAvatarsBatch(chunk);

              if (batchResult.results) {
                batchResult.results.forEach(r => {
                  if (r.success && r.url) {
                    avatarUrlMap[r.userCode] = r.url;
                  }
                });
              }

              // Update progress immediately when this chunk completes
              uploadedCount = Object.keys(avatarUrlMap).length;
              uploadedAvatarUrlsRef.current = Object.values(avatarUrlMap); // Track for cancel cleanup
              setServerImportProgress({ status: 'UPLOADING_AVATARS', totalRows: total, importedCount: uploadedCount });
              setProcessingStatus(`Đang tải ảnh đại diện... (${uploadedCount}/${total})`);
              setUploadProgress(10 + Math.round((uploadedCount / total) * 30));

            } catch (err) {
              console.error(`Chunk ${currentIndex} failed:`, err);
              uploadCancelled = true; // Stop other uploads
              throw err;
            }
          };

          // Run workers in parallel
          const runWorker = async () => {
            while (!uploadCancelled && chunkIndex < chunks.length) {
              await uploadNextChunk();
            }
          };

          // Start CONCURRENCY workers
          const workers = [];
          for (let i = 0; i < Math.min(CONCURRENCY, chunks.length); i++) {
            workers.push(runWorker());
          }
          await Promise.all(workers);

          // Update user data with avatar URLs
          usersToImport = usersToImport.map(user => {
            const avatarUrl = avatarUrlMap[user.userCode?.toUpperCase()];
            return avatarUrl ? { ...user, avtUrl: avatarUrl } : user;
          });

          setProcessingStatus(`Đã tải ${Object.keys(avatarUrlMap).length}/${total} ảnh thành công`);
          setUploadProgress(40);
        } catch (err) {
          console.error('Avatar upload failed:', err);

          // Cleanup: delete uploaded avatars on error
          const uploadedUrls = Object.values(avatarUrlMap);
          if (uploadedUrls.length > 0) {
            setProcessingStatus(`Lỗi! Đang xóa ${uploadedUrls.length} ảnh đã upload...`);
            try {
              await userService.deleteAvatarsBatch(uploadedUrls);
              console.log('[Cleanup] Đã xóa', uploadedUrls.length, 'avatars');
            } catch (cleanupErr) {
              console.error('[Cleanup] Lỗi xóa avatars:', cleanupErr);
            }
          }

          setProcessingStatus('Lỗi upload ảnh đã được dọn dẹp');
          setUploadProgress(40);
          throw err; // Re-throw to stop import
        }
      }

      // Import users in batches for progress tracking
      setProcessingStatus(`Đang import ${totalUsers} người dùng...`);
      setUploadProgress(50);
      setServerImportProgress({ status: 'IMPORTING', totalRows: totalUsers, importedCount: 0 });

      const BATCH_SIZE = 100;
      let importedCount = 0;
      let successCount = 0;
      let failedCount = 0;
      const failedUsers = [];
      const uploadedAvatarUrls = Object.values(avatarUrlMap); // Track for rollback

      for (let i = 0; i < usersToImport.length; i += BATCH_SIZE) {
        const batch = usersToImport.slice(i, i + BATCH_SIZE);

        try {
          const result = await userService.importUsers(batch);
          successCount += result.successCount || batch.length;
          failedCount += result.failedCount || 0;
          if (result.failed) {
            failedUsers.push(...result.failed);
          }
        } catch (err) {
          console.error('Batch import error:', err);
          failedCount += batch.length;

          // If first batch fails completely (likely auth issue), rollback avatars
          if (i === 0 && uploadedAvatarUrls.length > 0) {
            setProcessingStatus('Lỗi import! Đang xóa ảnh đã upload...');
            try {
              await userService.deleteAvatarsBatch(uploadedAvatarUrls);
              console.log('[Rollback] Đã xóa', uploadedAvatarUrls.length, 'avatars');
            } catch (rollbackErr) {
              console.error('[Rollback] Lỗi xóa avatars:', rollbackErr);
            }
            throw err; // Re-throw to exit
          }
        }

        importedCount += batch.length;

        // Update progress
        const progress = 50 + Math.round((importedCount / totalUsers) * 45);
        setUploadProgress(progress);
        setProcessingStatus(`Đang import... (${importedCount}/${totalUsers})`);
        setServerImportProgress({
          status: 'IMPORTING',
          totalRows: totalUsers,
          importedCount: importedCount
        });
      }

      setUploadProgress(100);
      setImportResult({
        successCount,
        failedCount,
        success: Array.from({ length: successCount }, (_, i) => ({ userCode: `User ${i + 1}` })),
        failed: failedUsers
      });
      setImportStep('result');

      // Clear ref so modal close won't delete these avatars
      uploadedAvatarUrlsRef.current = [];

      // Refresh user list
      await fetchUsers();
    } catch (err) {
      alert('Lỗi import: ' + (err.response?.data?.message || err.message));
      setImportStep('preview');
    } finally {
      setImporting(false);
    }
  };

  // Server-side Excel import with progress tracking
  const handleServerImport = async (excelFile) => {
    try {
      setImporting(true);
      setImportStep('uploading');
      setProcessingStatus('Đang tải file lên server...');
      setUploadProgress(0);
      setServerImportProgress(null);

      // Start async import
      const startResult = await userService.importExcelAsync(excelFile);
      const batchId = startResult.batchId;

      setProcessingStatus('Đang xử lý dữ liệu trên server...');
      setUploadProgress(20);

      // Poll for progress
      const finalStatus = await userService.pollImportStatus(batchId, (status) => {
        setServerImportProgress(status);

        // Calculate progress based on status
        let progress = 20;
        switch (status.status) {
          case 'PARSING': progress = 30; break;
          case 'VALIDATING': progress = 50; break;
          case 'IMPORTING': progress = 70; break;
          case 'ENRICHING': progress = 90; break;
          case 'COMPLETED': progress = 100; break;
          default: progress = 30;
        }
        setUploadProgress(progress);

        // Update status message
        const statusMessages = {
          'PARSING': 'Đang đọc file Excel...',
          'VALIDATING': `Đang kiểm tra dữ liệu... (${status.validCount || 0} hợp lệ)`,
          'IMPORTING': `Đang import... (${status.importedCount || 0}/${status.totalRows || 0})`,
          'ENRICHING': 'Đang xử lý ảnh đại diện...',
          'COMPLETED': 'Hoàn thành!',
          'FAILED': `Lỗi: ${status.errorMessage || 'Unknown error'}`
        };
        setProcessingStatus(statusMessages[status.status] || status.status);
      });

      // Get errors if any
      let errors = [];
      if (finalStatus.invalidCount > 0) {
        try {
          const errorResult = await userService.getImportErrors(batchId);
          errors = errorResult.errors || [];
        } catch (e) {
          console.error('Failed to get import errors:', e);
        }
      }

      // Set result
      setImportResult({
        successCount: finalStatus.importedCount,
        failedCount: finalStatus.invalidCount,
        success: Array.from({ length: finalStatus.importedCount }, (_, i) => ({
          userCode: `Imported ${i + 1}`,
          email: '-'
        })),
        failed: errors.map(e => ({
          userCode: e.userCode,
          email: e.email,
          reason: e.errorMessage
        }))
      });
      setImportStep('result');

      // Refresh user list
      await fetchUsers();

    } catch (err) {
      console.error('Server import error:', err);
      alert('Lỗi import: ' + (err.response?.data?.error || err.message));
      setImportStep('upload');
    } finally {
      setImporting(false);
    }
  };

  // Handle lock/unlock
  const handleLockUnlock = async () => {
    if (!selectedUser) return;

    try {
      setActionLoading(true);
      const newStatus = selectedUser.isActive === false;
      await userService.updateUserStatus(selectedUser.id, newStatus);
      await fetchUsers();
      setShowLockModal(false);
      setSelectedUser(null);
    } catch (err) {
      alert('Lỗi: ' + (err.response?.data?.message || err.message));
    } finally {
      setActionLoading(false);
    }
  };

  // Handle reset password
  const handleResetPassword = async () => {
    if (!selectedUser) return;

    try {
      setActionLoading(true);
      await userService.resetPasswordToDefault(selectedUser.email);
      alert('Đã reset mật khẩu về mặc định: Slib@2025');
      setShowResetPasswordModal(false);
      setSelectedUser(null);
    } catch (err) {
      alert('Lỗi: ' + (err.response?.data?.message || err.message));
    } finally {
      setActionLoading(false);
    }
  };

  // Handle add librarian
  const handleAddLibrarian = async () => {
    if (!newLibrarian.fullName || !newLibrarian.email) {
      alert('Vui lòng nhập đầy đủ thông tin');
      return;
    }

    try {
      setAddingLibrarian(true);
      await userService.createLibrarian(newLibrarian);
      await fetchUsers();
      setShowAddModal(false);
      setNewLibrarian({ fullName: '', email: '' });
      alert('Đã tạo tài khoản thủ thư với mật khẩu mặc định: Slib@2025');
    } catch (err) {
      alert('Lỗi: ' + (err.response?.data?.message || err.message));
    } finally {
      setAddingLibrarian(false);
    }
  };

  // Reset import modal (with cleanup of uploaded avatars)
  const resetImportModal = async () => {
    // Cleanup uploaded avatars if any (when user clicks X during upload)
    if (uploadedAvatarUrlsRef.current.length > 0) {
      console.log('[Cancel] Cleaning up', uploadedAvatarUrlsRef.current.length, 'uploaded avatars');
      try {
        await userService.deleteAvatarsBatch(uploadedAvatarUrlsRef.current);
        console.log('[Cancel] Cleanup completed');
      } catch (err) {
        console.error('[Cancel] Cleanup failed:', err);
      }
      uploadedAvatarUrlsRef.current = [];
    }

    setShowImportModal(false);
    setImportStep('upload');
    setImportData([]);
    setImportResult(null);
    setAvatarFiles({});
    setValidationErrors({});
    setUploadProgress(0);
    setProcessingStatus('');
    setImportMode('client');
    setServerImportProgress(null);
  };

  return (
    <>

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
              onClick={fetchUsers}
              disabled={loading}
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
                cursor: loading ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s ease',
                opacity: loading ? 0.6 : 1
              }}
            >
              <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
              Làm mới
            </button>
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

        {/* Error Message */}
        {error && (
          <div style={{
            padding: '16px 20px',
            background: '#FEE2E2',
            borderRadius: '12px',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            color: '#DC2626'
          }}>
            <AlertTriangle size={20} />
            <span>{error}</span>
          </div>
        )}

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
                  placeholder="Tìm theo tên, email, mã..."
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
                  <option key={role} value={role}>
                    {role === 'Tất cả' ? 'Tất cả vai trò' : getRoleLabel(role)}
                  </option>
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
                  <option key={status} value={status}>
                    {status === 'Tất cả' ? 'Tất cả trạng thái' : status}
                  </option>
                ))}
              </select>
            </div>

            <div style={{ fontSize: '14px', color: '#A0AEC0' }}>
              Hiển thị {filteredUsers.length} / {users.length} người dùng
            </div>
          </div>

          {/* Table */}
          <div style={{ overflowX: 'auto' }}>
            {loading ? (
              <div style={{
                padding: '60px',
                textAlign: 'center',
                color: '#A0AEC0'
              }}>
                <Loader2 size={32} style={{ animation: 'spin 1s linear infinite', marginBottom: '12px' }} />
                <p>Đang tải danh sách người dùng...</p>
              </div>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ background: '#F7FAFC' }}>
                    {['Người dùng', 'Email', 'Mã', 'Vai trò', 'Trạng thái', 'Thao tác'].map((header, idx) => (
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
                  {filteredUsers.length === 0 ? (
                    <tr>
                      <td colSpan="6" style={{
                        padding: '60px',
                        textAlign: 'center',
                        color: '#A0AEC0'
                      }}>
                        Không tìm thấy người dùng phù hợp
                      </td>
                    </tr>
                  ) : paginatedUsers.map((user, index) => {
                    const roleColors = getRoleColor(user.role);
                    const initials = (user.fullName || user.email || 'U')
                      .split(' ')
                      .map(n => n[0])
                      .slice(0, 2)
                      .join('')
                      .toUpperCase();

                    return (
                      <tr
                        key={user.id}
                        style={{
                          borderBottom: index === paginatedUsers.length - 1 ? 'none' : '1px solid #E2E8F0',
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
                              background: user.avtUrl
                                ? `url(${user.avtUrl}) center/cover no-repeat`
                                : 'linear-gradient(135deg, #FF751F, #FF9B5A)',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              color: '#fff',
                              fontSize: '13px',
                              fontWeight: '600'
                            }}>
                              {!user.avtUrl && initials}
                            </div>
                            <div>
                              <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A' }}>
                                {user.fullName || 'Chưa có tên'}
                              </div>
                              {user.passwordChanged === false && (
                                <div style={{
                                  fontSize: '11px',
                                  color: '#F59E0B',
                                  display: 'flex',
                                  alignItems: 'center',
                                  gap: '4px'
                                }}>
                                  <AlertTriangle size={10} />
                                  Chưa đổi mật khẩu
                                </div>
                              )}
                            </div>
                          </div>
                        </td>
                        <td style={{ padding: '16px 24px' }}>
                          <span style={{ fontSize: '14px', color: '#4A5568' }}>{user.email}</span>
                        </td>
                        <td style={{ padding: '16px 24px' }}>
                          <span style={{
                            fontSize: '13px',
                            color: '#6B7280',
                            fontFamily: 'monospace',
                            background: '#F3F4F6',
                            padding: '4px 8px',
                            borderRadius: '6px'
                          }}>
                            {user.userCode || '-'}
                          </span>
                        </td>
                        <td style={{ padding: '16px 24px' }}>
                          <span style={{
                            padding: '6px 12px',
                            borderRadius: '20px',
                            fontSize: '12px',
                            fontWeight: '600',
                            background: roleColors.bg,
                            color: roleColors.color
                          }}>{getRoleLabel(user.role)}</span>
                        </td>
                        <td style={{ padding: '16px 24px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            {user.isActive !== false ? (
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
                                minWidth: '200px',
                                zIndex: 100,
                                overflow: 'hidden'
                              }}>
                                <div
                                  onClick={() => { setSelectedUser(user); setShowUserDetailsModal(true); setShowActionMenu(null); }}
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
                                  <Eye size={16} color="#2563EB" />
                                  <span style={{ fontSize: '14px', color: '#2563EB' }}>Xem chi tiết</span>
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
                                  {user.isActive !== false ? (
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
                                  onClick={() => { setSelectedUser(user); setShowResetPasswordModal(true); setShowActionMenu(null); }}
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
                                  <Key size={16} color="#7C3AED" />
                                  <span style={{ fontSize: '14px', color: '#7C3AED' }}>Reset mật khẩu</span>
                                </div>
                                <div
                                  onClick={() => { setSelectedUser(user); setShowDeleteModal(true); setShowActionMenu(null); }}
                                  style={{
                                    padding: '12px 16px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '10px',
                                    cursor: 'pointer',
                                    transition: 'background 0.2s',
                                    borderTop: '1px solid #E2E8F0'
                                  }}
                                  onMouseEnter={(e) => e.currentTarget.style.background = '#FEF2F2'}
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
            )}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '16px 24px',
              borderTop: '1px solid #E2E8F0',
              background: '#F8FAFC'
            }}>
              <span style={{ fontSize: '14px', color: '#6B7280' }}>
                Hiển thị {((currentPage - 1) * USERS_PER_PAGE) + 1} - {Math.min(currentPage * USERS_PER_PAGE, filteredUsers.length)} / {filteredUsers.length} người dùng
              </span>
              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                <button
                  onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                  disabled={currentPage === 1}
                  style={{
                    padding: '8px 16px',
                    background: currentPage === 1 ? '#E2E8F0' : '#fff',
                    border: '1px solid #E2E8F0',
                    borderRadius: '8px',
                    fontSize: '14px',
                    color: currentPage === 1 ? '#A0AEC0' : '#4A5568',
                    cursor: currentPage === 1 ? 'not-allowed' : 'pointer'
                  }}
                >
                  Trước
                </button>

                {/* Page numbers */}
                {Array.from({ length: totalPages }, (_, i) => i + 1)
                  .filter(page => page === 1 || page === totalPages || Math.abs(page - currentPage) <= 1)
                  .map((page, idx, arr) => (
                    <React.Fragment key={page}>
                      {idx > 0 && arr[idx - 1] !== page - 1 && (
                        <span style={{ color: '#A0AEC0' }}>...</span>
                      )}
                      <button
                        onClick={() => setCurrentPage(page)}
                        style={{
                          padding: '8px 12px',
                          background: currentPage === page ? '#FF751F' : '#fff',
                          border: currentPage === page ? 'none' : '1px solid #E2E8F0',
                          borderRadius: '8px',
                          fontSize: '14px',
                          fontWeight: currentPage === page ? '600' : '400',
                          color: currentPage === page ? '#fff' : '#4A5568',
                          cursor: 'pointer',
                          minWidth: '36px'
                        }}
                      >
                        {page}
                      </button>
                    </React.Fragment>
                  ))}

                <button
                  onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                  disabled={currentPage === totalPages}
                  style={{
                    padding: '8px 16px',
                    background: currentPage === totalPages ? '#E2E8F0' : '#fff',
                    border: '1px solid #E2E8F0',
                    borderRadius: '8px',
                    fontSize: '14px',
                    color: currentPage === totalPages ? '#A0AEC0' : '#4A5568',
                    cursor: currentPage === totalPages ? 'not-allowed' : 'pointer'
                  }}
                >
                  Sau
                </button>
              </div>
            </div>
          )}
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
            borderRadius: '10px',
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
                <input
                  type="text"
                  placeholder="Nhập họ và tên"
                  value={newLibrarian.fullName}
                  onChange={(e) => setNewLibrarian({ ...newLibrarian, fullName: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Email
                </label>
                <input
                  type="email"
                  placeholder="email@fpt.edu.vn"
                  value={newLibrarian.email}
                  onChange={(e) => setNewLibrarian({ ...newLibrarian, email: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
              </div>
              <div style={{
                padding: '16px',
                background: '#FEF3C7',
                borderRadius: '12px',
                marginBottom: '20px',
                display: 'flex',
                alignItems: 'flex-start',
                gap: '12px'
              }}>
                <AlertTriangle size={20} color="#F59E0B" style={{ flexShrink: 0, marginTop: '2px' }} />
                <div style={{ fontSize: '13px', color: '#92400E' }}>
                  <strong>Lưu ý:</strong> Tài khoản sẽ được tạo với mật khẩu mặc định <code style={{ background: '#FDE68A', padding: '2px 6px', borderRadius: '4px' }}>Slib@2025</code>.
                  Người dùng sẽ được yêu cầu đổi mật khẩu khi đăng nhập lần đầu.
                </div>
              </div>
              <div style={{ display: 'flex', gap: '12px', marginTop: '24px' }}>
                <button
                  onClick={() => setShowAddModal(false)}
                  disabled={addingLibrarian}
                  style={{
                    flex: 1,
                    padding: '14px',
                    background: '#F7FAFC',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: '#4A5568',
                    cursor: addingLibrarian ? 'not-allowed' : 'pointer'
                  }}
                >
                  Hủy
                </button>
                <button
                  onClick={handleAddLibrarian}
                  disabled={addingLibrarian}
                  style={{
                    flex: 1,
                    padding: '14px',
                    background: '#e8600a',
                    border: 'none',
                    borderRadius: '12px',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: '#fff',
                    cursor: addingLibrarian ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    opacity: addingLibrarian ? 0.7 : 1
                  }}
                >
                  {addingLibrarian && <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />}
                  Tạo tài khoản
                </button>
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
            borderRadius: '10px',
            width: importStep === 'preview' ? '800px' : '550px',
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
              <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A1A1A', margin: 0 }}>
                {importStep === 'upload' && 'Import danh sách người dùng'}
                {importStep === 'processing' && 'Đang xử lý file...'}
                {importStep === 'preview' && 'Xem trước dữ liệu'}
                {importStep === 'uploading' && 'Đang import...'}
                {importStep === 'result' && 'Kết quả import'}
              </h2>
              <button onClick={resetImportModal} style={{
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
              {/* Upload Step */}
              {importStep === 'upload' && (
                <>

                  <div style={{
                    border: '2px dashed #E2E8F0',
                    borderRadius: '10px',
                    padding: '48px 24px',
                    textAlign: 'center',
                    background: '#F7FAFC',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    position: 'relative'
                  }}>
                    <input
                      type="file"
                      accept=".xlsx,.xls,.zip"
                      onChange={handleFileUpload}
                      style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        opacity: 0,
                        cursor: 'pointer'
                      }}
                    />
                    <FileSpreadsheet size={48} color="#e8600a" style={{ marginBottom: '16px' }} />
                    <p style={{ fontSize: '16px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 8px' }}>
                      Kéo thả file Excel (.xlsx) hoặc Zip (.zip) vào đây
                    </p>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: '0 0 16px' }}>
                      hoặc nhấn để chọn file
                    </p>
                  </div>
                  <div style={{ marginTop: '20px', display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <Download size={18} color="#e8600a" />
                    <button
                      onClick={() => userService.downloadTemplate()}
                      style={{
                        background: 'none',
                        border: 'none',
                        fontSize: '14px',
                        color: '#e8600a',
                        fontWeight: '500',
                        cursor: 'pointer',
                        textDecoration: 'underline'
                      }}
                    >
                      Tải template mẫu (.xlsx)
                    </button>
                  </div>
                  <div style={{
                    marginTop: '20px',
                    padding: '16px',
                    background: '#F7FAFC',
                    borderRadius: '12px',
                    fontSize: '13px',
                    color: '#4A5568',
                    lineHeight: '1.8'
                  }}>
                    <strong style={{ color: '#1A1A1A', fontSize: '14px' }}>📋 Quy định file tải lên:</strong>
                    <ul style={{ margin: '12px 0 0', paddingLeft: '20px' }}>
                      <li><strong>Chỉ nhập thông tin:</strong> Dùng file template .xlsx</li>
                      <li><strong>Nhập kèm ảnh đại diện (Avatar):</strong> Nén file template và toàn bộ hình ảnh vào một file .zip</li>
                      <li><strong>Lưu ý ảnh:</strong> Đặt tên file ảnh trùng với Mã số (Ví dụ: SV001.jpg, TT002.png)</li>
                    </ul>
                    <div style={{ marginTop: '12px', padding: '10px', background: '#E2E8F0', borderRadius: '8px' }}>
                      <strong>Cấu trúc cột:</strong> Họ và tên, Mã số, Vai trò, Email <span style={{ color: '#DC2626' }}>(bắt buộc)</span>; Ngày sinh, Số điện thoại <span style={{ color: '#6B7280' }}>(tùy chọn)</span>
                    </div>
                  </div>
                </>
              )}

              {/* Processing Step */}
              {importStep === 'processing' && (
                <div style={{ textAlign: 'center', padding: '48px 24px' }}>
                  <Loader2 size={48} color="#e8600a" style={{ animation: 'spin 1s linear infinite', marginBottom: '16px' }} />
                  <p style={{ fontSize: '16px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 8px' }}>
                    {processingStatus || 'Đang xử lý...'}
                  </p>
                </div>
              )}

              {/* Uploading Step */}
              {importStep === 'uploading' && (() => {
                // Calculate actual progress based on serverImportProgress
                let displayProgress = uploadProgress;
                let countText = '';
                let percentText = `${uploadProgress}%`;

                if (serverImportProgress && serverImportProgress.totalRows > 0) {
                  const total = serverImportProgress.totalRows;
                  const current = serverImportProgress.importedCount || 0;

                  if (serverImportProgress.status === 'IMPORTING') {
                    // More accurate progress during importing phase
                    displayProgress = Math.round(50 + (current / total) * 45); // 50% to 95%
                    percentText = `${displayProgress}%`;
                  } else if (serverImportProgress.status === 'UPLOADING_AVATARS') {
                    // Progress during avatar upload phase
                    displayProgress = Math.round(10 + (current / total) * 30); // 10% to 40%
                    percentText = `${displayProgress}%`;
                  }

                  countText = `${current}/${total}`;
                }

                return (
                  <div style={{ textAlign: 'center', padding: '48px 24px' }}>
                    <Loader2 size={48} color="#e8600a" style={{ animation: 'spin 1s linear infinite', marginBottom: '16px' }} />

                    {/* Main status text */}
                    <p style={{ fontSize: '16px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 8px' }}>
                      {processingStatus || 'Đang import...'}
                    </p>

                    {/* Count display */}
                    {countText && (
                      <p style={{
                        fontSize: '24px',
                        fontWeight: '700',
                        color: '#e8600a',
                        margin: '12px 0',
                        fontFamily: 'monospace'
                      }}>
                        {countText}
                      </p>
                    )}

                    {/* Progress bar */}
                    <div style={{
                      marginTop: '16px',
                      height: '12px',
                      background: '#E2E8F0',
                      borderRadius: '6px',
                      overflow: 'hidden',
                      position: 'relative'
                    }}>
                      <div style={{
                        width: `${displayProgress}%`,
                        height: '100%',
                        background: 'linear-gradient(90deg, #e8600a, #f0853e)',
                        transition: 'width 0.3s',
                        borderRadius: '6px'
                      }} />
                    </div>

                    {/* Percentage and stage info */}
                    <div style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      marginTop: '8px',
                      fontSize: '13px',
                      color: '#6B7280'
                    }}>
                      <span>{percentText} hoàn thành</span>
                      {serverImportProgress && (
                        <span style={{
                          padding: '2px 8px',
                          background: '#FFF7ED',
                          borderRadius: '4px',
                          color: '#EA580C',
                          fontWeight: '500'
                        }}>
                          {serverImportProgress.status}
                        </span>
                      )}
                    </div>

                    {/* Stage explanation */}
                    {serverImportProgress && (
                      <p style={{
                        fontSize: '12px',
                        color: '#9CA3AF',
                        marginTop: '16px',
                        fontStyle: 'italic'
                      }}>
                        {serverImportProgress.status === 'PARSING' && '📄 Đang đọc file Excel (streaming)...'}
                        {serverImportProgress.status === 'VALIDATING' && '🔍 Đang kiểm tra trùng lặp email, mã số...'}
                        {serverImportProgress.status === 'IMPORTING' && '💾 Đang lưu vào database...'}
                        {serverImportProgress.status === 'ENRICHING' && '🖼️ Đang xử lý ảnh đại diện...'}
                      </p>
                    )}
                  </div>
                );
              })()}

              {/* Preview Step */}
              {importStep === 'preview' && (() => {
                const errorCount = Object.keys(validationErrors).length;
                const successCount = importData.length - errorCount;

                // Separate users into valid and invalid
                const validUsers = importData.map((user, idx) => ({ ...user, rowIndex: idx + 2 }))
                  .filter(user => !validationErrors[user.userCode]);
                const invalidUsers = importData.map((user, idx) => ({ ...user, rowIndex: idx + 2 }))
                  .filter(user => validationErrors[user.userCode]);

                const displayUsers = previewTab === 'success' ? validUsers : invalidUsers;

                return (
                  <>
                    {/* Tabs */}
                    <div style={{ display: 'flex', gap: '0', marginBottom: '16px', borderBottom: '2px solid #E2E8F0' }}>
                      <button
                        onClick={() => setPreviewTab('success')}
                        style={{
                          padding: '12px 20px',
                          background: previewTab === 'success' ? '#fff' : 'transparent',
                          border: 'none',
                          borderBottom: previewTab === 'success' ? '2px solid #059669' : '2px solid transparent',
                          marginBottom: '-2px',
                          fontSize: '14px',
                          fontWeight: '600',
                          color: previewTab === 'success' ? '#059669' : '#6B7280',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '8px'
                        }}
                      >
                        Thành công
                        <span style={{
                          padding: '2px 8px',
                          background: '#D1FAE5',
                          color: '#059669',
                          borderRadius: '10px',
                          fontSize: '12px'
                        }}>
                          {successCount}
                        </span>
                      </button>
                      <button
                        onClick={() => setPreviewTab('error')}
                        style={{
                          padding: '12px 20px',
                          background: previewTab === 'error' ? '#fff' : 'transparent',
                          border: 'none',
                          borderBottom: previewTab === 'error' ? '2px solid #DC2626' : '2px solid transparent',
                          marginBottom: '-2px',
                          fontSize: '14px',
                          fontWeight: '600',
                          color: previewTab === 'error' ? '#DC2626' : '#6B7280',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '8px'
                        }}
                      >
                        Lỗi
                        <span style={{
                          padding: '2px 8px',
                          background: errorCount > 0 ? '#FEE2E2' : '#F3F4F6',
                          color: errorCount > 0 ? '#DC2626' : '#6B7280',
                          borderRadius: '10px',
                          fontSize: '12px'
                        }}>
                          {errorCount}
                        </span>
                      </button>
                    </div>

                    {/* Table */}
                    <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                      {displayUsers.length === 0 ? (
                        <div style={{
                          padding: '40px',
                          textAlign: 'center',
                          color: '#A0AEC0',
                          background: '#F9FAFB',
                          borderRadius: '8px'
                        }}>
                          {previewTab === 'success' ? 'Không có dữ liệu hợp lệ' : 'Không có lỗi nào'}
                        </div>
                      ) : (
                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                          <thead>
                            <tr style={{ background: '#F7FAFC', position: 'sticky', top: 0 }}>
                              <th style={{ padding: '10px', textAlign: 'center', borderBottom: '2px solid #E2E8F0', width: '50px' }}>Dòng</th>
                              <th style={{ padding: '10px', textAlign: 'center', borderBottom: '2px solid #E2E8F0', width: '50px' }}>Ảnh</th>
                              <th style={{ padding: '10px', textAlign: 'left', borderBottom: '2px solid #E2E8F0' }}>Mã số</th>
                              <th style={{ padding: '10px', textAlign: 'left', borderBottom: '2px solid #E2E8F0' }}>Email</th>
                              <th style={{ padding: '10px', textAlign: 'left', borderBottom: '2px solid #E2E8F0' }}>Họ tên</th>
                              {previewTab === 'error' && (
                                <th style={{ padding: '10px', textAlign: 'left', borderBottom: '2px solid #E2E8F0' }}>Lý do lỗi</th>
                              )}
                            </tr>
                          </thead>
                          <tbody>
                            {displayUsers.slice(0, 100).map((user, idx) => {
                              const userErrors = validationErrors[user.userCode] || {};
                              const errorMessages = Object.values(userErrors).join(', ');

                              return (
                                <tr key={idx} style={{ background: previewTab === 'error' ? '#FEF2F2' : 'transparent' }}>
                                  <td style={{ padding: '8px', borderBottom: '1px solid #E2E8F0', textAlign: 'center', color: '#6B7280', fontSize: '12px' }}>
                                    {user.rowIndex}
                                  </td>
                                  <td style={{ padding: '8px', borderBottom: '1px solid #E2E8F0', textAlign: 'center' }}>
                                    {user.avatarPreviewUrl ? (
                                      <img
                                        src={user.avatarPreviewUrl}
                                        alt={user.fullName}
                                        style={{
                                          width: '32px',
                                          height: '32px',
                                          borderRadius: '50%',
                                          objectFit: 'cover',
                                          border: '2px solid #D1FAE5'
                                        }}
                                      />
                                    ) : (
                                      <div style={{
                                        width: '32px',
                                        height: '32px',
                                        borderRadius: '50%',
                                        background: '#F3F4F6',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        color: '#9CA3AF',
                                        fontSize: '12px',
                                        margin: '0 auto'
                                      }}>
                                        {user.fullName?.charAt(0)?.toUpperCase() || '?'}
                                      </div>
                                    )}
                                  </td>
                                  <td style={{ padding: '10px', borderBottom: '1px solid #E2E8F0', fontWeight: '500' }}>
                                    {user.userCode}
                                  </td>
                                  <td style={{ padding: '10px', borderBottom: '1px solid #E2E8F0' }}>
                                    {user.email}
                                  </td>
                                  <td style={{ padding: '10px', borderBottom: '1px solid #E2E8F0' }}>
                                    {user.fullName}
                                  </td>
                                  {previewTab === 'error' && (
                                    <td style={{ padding: '10px', borderBottom: '1px solid #E2E8F0', color: '#DC2626', fontSize: '12px' }}>
                                      {errorMessages}
                                    </td>
                                  )}
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      )}
                      {displayUsers.length > 100 && (
                        <div style={{ padding: '12px', textAlign: 'center', color: '#A0AEC0', fontSize: '13px' }}>
                          ... và {displayUsers.length - 100} người dùng khác
                        </div>
                      )}
                    </div>

                    {/* Actions */}
                    <div style={{ display: 'flex', gap: '12px', marginTop: '24px' }}>
                      <button
                        onClick={() => { setImportStep('upload'); setImportData([]); setAvatarFiles({}); setValidationErrors({}); setPreviewTab('success'); }}
                        style={{
                          flex: 1,
                          padding: '14px',
                          background: '#F7FAFC',
                          border: '2px solid #E2E8F0',
                          borderRadius: '12px',
                          fontSize: '14px',
                          fontWeight: '600',
                          color: '#4A5568',
                          cursor: 'pointer'
                        }}
                      >
                        Quay lại
                      </button>
                      <button
                        onClick={handleImport}
                        disabled={importing || successCount === 0}
                        style={{
                          flex: 1,
                          padding: '14px',
                          background: successCount > 0 ? '#e8600a' : '#E2E8F0',
                          border: 'none',
                          borderRadius: '12px',
                          fontSize: '14px',
                          fontWeight: '600',
                          color: successCount > 0 ? '#fff' : '#A0AEC0',
                          cursor: importing || successCount === 0 ? 'not-allowed' : 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: '8px',
                          opacity: importing ? 0.7 : 1
                        }}
                      >
                        {importing && <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />}
                        Import {successCount} người dùng hợp lệ
                      </button>
                    </div>
                  </>
                );
              })()}

              {/* Result Step */}
              {importStep === 'result' && importResult && (
                <>
                  <div style={{ display: 'flex', gap: '16px', marginBottom: '24px' }}>
                    <div style={{
                      flex: 1,
                      padding: '20px',
                      background: '#D1FAE5',
                      borderRadius: '12px',
                      textAlign: 'center'
                    }}>
                      <CheckCircle size={32} color="#059669" style={{ marginBottom: '8px' }} />
                      <div style={{ fontSize: '24px', fontWeight: '700', color: '#059669' }}>
                        {importResult.successCount}
                      </div>
                      <div style={{ fontSize: '13px', color: '#047857' }}>Thành công</div>
                    </div>
                    <div style={{
                      flex: 1,
                      padding: '20px',
                      background: '#FEE2E2',
                      borderRadius: '12px',
                      textAlign: 'center'
                    }}>
                      <XCircle size={32} color="#DC2626" style={{ marginBottom: '8px' }} />
                      <div style={{ fontSize: '24px', fontWeight: '700', color: '#DC2626' }}>
                        {importResult.failedCount}
                      </div>
                      <div style={{ fontSize: '13px', color: '#B91C1C' }}>Thất bại</div>
                    </div>
                  </div>

                  {importResult.failed && importResult.failed.length > 0 && (
                    <div style={{ marginBottom: '24px' }}>
                      <h4 style={{ fontSize: '14px', fontWeight: '600', marginBottom: '12px', color: '#DC2626' }}>
                        Chi tiết lỗi:
                      </h4>
                      <div style={{ maxHeight: '200px', overflowY: 'auto' }}>
                        {importResult.failed.map((fail, idx) => (
                          <div key={idx} style={{
                            padding: '12px',
                            background: '#FEE2E2',
                            borderRadius: '8px',
                            marginBottom: '8px',
                            fontSize: '13px'
                          }}>
                            <strong>{fail.email || fail.userCode}</strong>: {fail.reason}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  <button
                    onClick={resetImportModal}
                    style={{
                      width: '100%',
                      padding: '14px',
                      background: '#e8600a',
                      border: 'none',
                      borderRadius: '12px',
                      fontSize: '14px',
                      fontWeight: '600',
                      color: '#fff',
                      cursor: 'pointer'
                    }}
                  >
                    Đóng
                  </button>
                </>
              )}
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
            borderRadius: '10px',
            width: '450px',
            boxShadow: '0 20px 60px rgba(0,0,0,0.2)'
          }}>
            <div style={{ padding: '32px', textAlign: 'center' }}>
              <div style={{
                width: '64px',
                height: '64px',
                borderRadius: '10px',
                background: selectedUser.isActive !== false ? '#FEF3C7' : '#D1FAE5',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 20px'
              }}>
                {selectedUser.isActive !== false ? (
                  <Lock size={28} color="#F59E0B" />
                ) : (
                  <Unlock size={28} color="#059669" />
                )}
              </div>
              <h3 style={{ fontSize: '20px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 8px' }}>
                {selectedUser.isActive !== false ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}
              </h3>
              <p style={{ fontSize: '14px', color: '#4A5568', margin: '0 0 20px' }}>
                Bạn có chắc muốn {selectedUser.isActive !== false ? 'khóa' : 'mở khóa'} tài khoản <strong>{selectedUser.fullName}</strong>?
              </p>
              <div style={{ display: 'flex', gap: '12px' }}>
                <button
                  onClick={() => { setShowLockModal(false); setSelectedUser(null); }}
                  disabled={actionLoading}
                  style={{
                    flex: 1,
                    padding: '14px',
                    background: '#F7FAFC',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: '#4A5568',
                    cursor: actionLoading ? 'not-allowed' : 'pointer'
                  }}
                >
                  Hủy
                </button>
                <button
                  onClick={handleLockUnlock}
                  disabled={actionLoading}
                  style={{
                    flex: 1,
                    padding: '14px',
                    background: selectedUser.isActive !== false ? '#F59E0B' : '#059669',
                    border: 'none',
                    borderRadius: '12px',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: '#fff',
                    cursor: actionLoading ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    opacity: actionLoading ? 0.7 : 1
                  }}
                >
                  {actionLoading && <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />}
                  {selectedUser.isActive !== false ? 'Khóa tài khoản' : 'Mở khóa'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Reset Password Modal */}
      {showResetPasswordModal && selectedUser && (
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
            borderRadius: '10px',
            width: '450px',
            boxShadow: '0 20px 60px rgba(0,0,0,0.2)'
          }}>
            <div style={{ padding: '32px', textAlign: 'center' }}>
              <div style={{
                width: '64px',
                height: '64px',
                borderRadius: '10px',
                background: '#F3E8FF',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 20px'
              }}>
                <Key size={28} color="#7C3AED" />
              </div>
              <h3 style={{ fontSize: '20px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 8px' }}>
                Reset mật khẩu
              </h3>
              <p style={{ fontSize: '14px', color: '#4A5568', margin: '0 0 12px' }}>
                Bạn có chắc muốn reset mật khẩu của <strong>{selectedUser.fullName}</strong>?
              </p>
              <div style={{
                padding: '12px 16px',
                background: '#FEF3C7',
                borderRadius: '8px',
                marginBottom: '20px',
                fontSize: '13px',
                color: '#92400E'
              }}>
                Mật khẩu sẽ được đặt lại thành: <code style={{ background: '#FDE68A', padding: '2px 6px', borderRadius: '4px' }}>Slib@2025</code>
              </div>
              <div style={{ display: 'flex', gap: '12px' }}>
                <button
                  onClick={() => { setShowResetPasswordModal(false); setSelectedUser(null); }}
                  disabled={actionLoading}
                  style={{
                    flex: 1,
                    padding: '14px',
                    background: '#F7FAFC',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: '#4A5568',
                    cursor: actionLoading ? 'not-allowed' : 'pointer'
                  }}
                >
                  Hủy
                </button>
                <button
                  onClick={handleResetPassword}
                  disabled={actionLoading}
                  style={{
                    flex: 1,
                    padding: '14px',
                    background: '#7C3AED',
                    border: 'none',
                    borderRadius: '12px',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: '#fff',
                    cursor: actionLoading ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    opacity: actionLoading ? 0.7 : 1
                  }}
                >
                  {actionLoading && <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />}
                  Reset mật khẩu
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* User Details Modal */}
      <UserDetailsModal
        user={selectedUser}
        isOpen={showUserDetailsModal}
        onClose={() => { setShowUserDetailsModal(false); setSelectedUser(null); }}
        onEdit={(user) => {
          setShowUserDetailsModal(false);
          // TODO: Open edit modal
          alert('Tính năng chỉnh sửa đang phát triển');
        }}
        onLock={(user) => {
          setShowUserDetailsModal(false);
          setShowLockModal(true);
        }}
        onDelete={(user) => {
          setShowUserDetailsModal(false);
          setShowDeleteModal(true);
        }}
      />

      {/* Delete User Modal */}
      <DeleteUserModal
        user={selectedUser}
        isOpen={showDeleteModal}
        onClose={() => { setShowDeleteModal(false); setSelectedUser(null); }}
        onDeleted={() => {
          fetchUsers();
          setSelectedUser(null);
        }}
        currentUserId={JSON.parse(localStorage.getItem('librarian_user'))?.id}
      />

      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </>
  );
};

export default UserManagement;
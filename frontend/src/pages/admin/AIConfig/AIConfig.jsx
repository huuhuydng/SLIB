import React, { useState, useEffect } from 'react';
import { useToast } from '../../../components/common/ToastProvider';
import {
  Bot, Brain, MessageSquare, Sparkles, RefreshCw, Plus, Trash2, X, CheckCircle,
  AlertTriangle, BookOpen, Send, User, RotateCcw, Database, Upload, FileText,
  FolderOpen, Package, Layers, Edit, ChevronDown, ChevronRight, Eye, Save
} from 'lucide-react';

import { testAPIConnection, sendTestMessage, sendTestMessageWithDebug, getChatHistory, clearChatSession } from '../../../services/admin/ai/pythonAiApi';
import {
  getMaterials, createMaterial, updateMaterial, deleteMaterial, addTextItem, addFileItem, deleteItem, updateItem,
  getKnowledgeStores, createKnowledgeStore, updateKnowledgeStore, deleteKnowledgeStore, syncKnowledgeStore
} from '../../../services/admin/ai/materialsApi';

const TABS = [
  { id: 'materials', label: 'Tài liệu', icon: Package },
  { id: 'knowledge', label: 'Kho tri thức', icon: Database },
  { id: 'testing', label: 'Kiểm tra chat', icon: MessageSquare },
];

const STATUS_STYLES = {
  CHANGED: { bg: '#FEF3C7', color: '#D97706', label: 'Đã thay đổi' },
  SYNCING: { bg: '#DBEAFE', color: '#2563EB', label: 'Đang đồng bộ...' },
  SYNCED: { bg: '#D1FAE5', color: '#059669', label: 'Đã đồng bộ' },
  ERROR: { bg: '#FEE2E2', color: '#DC2626', label: 'Lỗi' },
};

// CSS animation cho loading dots
const LOADING_ANIMATION_CSS = `
@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.5; }
  40% { transform: translateY(-6px); opacity: 1; }
}
`;

const AIConfig = () => {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState('materials');
  const [apiStatus, setApiStatus] = useState('unknown');

  // Materials State
  const [materials, setMaterials] = useState([]);
  const [expandedMaterial, setExpandedMaterial] = useState(null);
  const [showMaterialModal, setShowMaterialModal] = useState(false);
  const [materialForm, setMaterialForm] = useState({ name: '', description: '' });
  const [showItemModal, setShowItemModal] = useState(false);
  const [itemForm, setItemForm] = useState({ name: '', type: 'TEXT', content: '', file: null });
  const [selectedMaterialForItem, setSelectedMaterialForItem] = useState(null);
  const [editingMaterial, setEditingMaterial] = useState(null);
  const [editingItem, setEditingItem] = useState(null);
  const [viewingItem, setViewingItem] = useState(null);

  // Knowledge Store State
  const [knowledgeStores, setKnowledgeStores] = useState([]);
  const [showKSModal, setShowKSModal] = useState(false);
  const [ksForm, setKsForm] = useState({ name: '', description: '', itemIds: [] });
  const [selectedMaterialForKS, setSelectedMaterialForKS] = useState(null);
  const [isSyncing, setIsSyncing] = useState({});
  const [editingKS, setEditingKS] = useState(null);
  const [viewingKS, setViewingKS] = useState(null);

  // Chat State
  const [chatHistory, setChatHistory] = useState([]);
  const [testMessage, setTestMessage] = useState('');
  const [sessionId, setSessionId] = useState(() => {
    // Load session from localStorage or create new
    const saved = localStorage.getItem('slib_test_chat_session');
    if (saved) return saved;
    const newId = `test-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    localStorage.setItem('slib_test_chat_session', newId);
    return newId;
  });
  const [isSending, setIsSending] = useState(false);
  const [debugMode, setDebugMode] = useState(true);  // Enable debug panel by default
  const [selectedDebugInfo, setSelectedDebugInfo] = useState(null);  // Debug info of selected message
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [expandedChunks, setExpandedChunks] = useState({});  // Track which chunks are expanded

  useEffect(() => { loadAllData(); }, []);

  // Load chat history from MongoDB when component mounts
  useEffect(() => {
    if (sessionId) {
      loadChatHistoryFromDB();
    }
  }, [sessionId]);

  // Inject CSS animation cho loading dots
  useEffect(() => {
    const styleId = 'ai-config-loading-style';
    if (!document.getElementById(styleId)) {
      const style = document.createElement('style');
      style.id = styleId;
      style.textContent = LOADING_ANIMATION_CSS;
      document.head.appendChild(style);
    }
  }, []);

  const loadAllData = async () => {
    await Promise.all([checkHealth(), loadMaterials(), loadKnowledgeStores()]);
  };

  // Load chat history from MongoDB
  const loadChatHistoryFromDB = async () => {
    if (!sessionId) return;
    setIsLoadingHistory(true);
    try {
      const res = await getChatHistory(sessionId, 50);
      if (res.data.success && res.data.messages.length > 0) {
        // Convert API response to chat format
        const messages = res.data.messages.map(msg => ({
          role: msg.role,
          content: msg.content,
          debug: msg.debug,
          action: msg.action
        }));
        setChatHistory(messages);
      } else if (res.data.messages.length === 0) {
        // No history, show welcome message
        setChatHistory([
          { role: 'assistant', content: 'Xin chào! Tôi là SLIB AI Assistant. Hãy đặt câu hỏi để test.' }
        ]);
      }
    } catch (e) {
      console.error('Lỗi tải lịch sử chat:', e);
      // Show welcome message on error
      setChatHistory([
        { role: 'assistant', content: 'Xin chào! Tôi là SLIB AI Assistant. Hãy đặt câu hỏi để test.' }
      ]);
    }
    setIsLoadingHistory(false);
  };

  // Clear chat and delete from MongoDB
  const handleClearChat = async () => {
    if (!confirm('Bạn có chắc muốn xóa toàn bộ tin nhắn?')) return;
    try {
      await clearChatSession(sessionId);
      setChatHistory([
        { role: 'assistant', content: 'Xin chào! Tôi là SLIB AI Assistant. Hãy đặt câu hỏi để test.' }
      ]);
      setSelectedDebugInfo(null);
    } catch (e) {
      console.error('Lỗi xóa chat:', e);
      toast.error('Không thể xóa tin nhắn');
    }
  };

  const checkHealth = async () => {
    try {
      const res = await testAPIConnection();
      setApiStatus(res.data.success ? 'connected' : 'error');
    } catch { setApiStatus('error'); }
  };

  const loadMaterials = async () => {
    try {
      const res = await getMaterials();
      setMaterials(res.data);
    } catch (e) { console.error('Lỗi tải tài liệu:', e); }
  };

  const loadKnowledgeStores = async () => {
    try {
      const res = await getKnowledgeStores();
      setKnowledgeStores(res.data);
    } catch (e) { console.error('Lỗi tải kho tri thức:', e); }
  };

  // Material Handlers
  const handleCreateMaterial = async () => {
    try {
      await createMaterial(materialForm);
      setShowMaterialModal(false);
      setMaterialForm({ name: '', description: '' });
      await loadMaterials();
    } catch (e) { toast.error('Lỗi: ' + e.message); }
  };

  const handleUpdateMaterial = async () => {
    try {
      await updateMaterial(editingMaterial.id, { name: editingMaterial.name, description: editingMaterial.description });
      setEditingMaterial(null);
      await loadMaterials();
    } catch (e) { toast.error('Lỗi: ' + e.message); }
  };

  const handleDeleteMaterial = async (id) => {
    if (!confirm('Bạn có chắc muốn xóa tài liệu này?')) return;
    try {
      await deleteMaterial(id);
      await loadMaterials();
    } catch (e) { toast.error('Lỗi: ' + e.message); }
  };

  // Item Handlers
  const handleAddItem = async () => {
    try {
      if (itemForm.type === 'TEXT') {
        await addTextItem(selectedMaterialForItem, { name: itemForm.name, type: 'TEXT', content: itemForm.content });
      } else if (itemForm.file) {
        await addFileItem(selectedMaterialForItem, itemForm.file, itemForm.name);
      }
      setShowItemModal(false);
      setItemForm({ name: '', type: 'TEXT', content: '', file: null });
      await loadMaterials();
    } catch (e) { toast.error('Lỗi: ' + e.message); }
  };

  const handleUpdateItem = async () => {
    try {
      await updateItem(editingItem.materialId, editingItem.id, { name: editingItem.name, content: editingItem.content });
      setEditingItem(null);
      await loadMaterials();
    } catch (e) { toast.error('Lỗi: ' + e.message); }
  };

  const handleDeleteItem = async (materialId, itemId) => {
    if (!confirm('Bạn có chắc muốn xóa mục này?')) return;
    try {
      await deleteItem(materialId, itemId);
      await loadMaterials();
    } catch (e) { toast.error('Lỗi: ' + e.message); }
  };

  // Knowledge Store Handlers
  const handleCreateKS = async () => {
    try {
      await createKnowledgeStore(ksForm);
      setShowKSModal(false);
      setKsForm({ name: '', description: '', itemIds: [] });
      await loadKnowledgeStores();
    } catch (e) { toast.error('Lỗi: ' + e.message); }
  };

  const handleUpdateKS = async () => {
    try {
      const itemIds = editingKS.items?.map(i => i.id) || [];
      await updateKnowledgeStore(editingKS.id, { name: editingKS.name, description: editingKS.description, itemIds });
      setEditingKS(null);
      await loadKnowledgeStores();
    } catch (e) { toast.error('Lỗi: ' + e.message); }
  };

  const handleDeleteKS = async (id) => {
    if (!confirm('Bạn có chắc muốn xóa kho tri thức này?')) return;
    try {
      await deleteKnowledgeStore(id);
      await loadKnowledgeStores();
    } catch (e) { toast.error('Lỗi: ' + e.message); }
  };

  const handleSync = async (id) => {
    setIsSyncing(prev => ({ ...prev, [id]: true }));
    try {
      const res = await syncKnowledgeStore(id);
      toast.success(`Đồng bộ thành công! Số chunks: ${res.data.chunksCreated}`);
      await loadKnowledgeStores();
    } catch (e) {
      toast.error('Đồng bộ thất bại: ' + e.message);
    } finally {
      setIsSyncing(prev => ({ ...prev, [id]: false }));
    }
  };

  // Chat Handlers
  const handleSendMessage = async () => {
    if (!testMessage.trim() || isSending) return;
    setChatHistory(prev => [...prev, { role: 'user', content: testMessage }]);
    const msg = testMessage;
    setTestMessage('');
    setIsSending(true);

    // Thêm message loading với animation "..."
    const loadingId = Date.now();
    setChatHistory(prev => [...prev, { role: 'assistant', content: '...', isLoading: true, id: loadingId }]);

    try {
      // Always use debug API for admin testing
      const res = await sendTestMessageWithDebug(msg, sessionId);
      setSessionId(res.data.sessionId);

      const debugInfo = res.data.debug || {};
      const messageObj = {
        role: 'assistant',
        content: res.data.reply,
        action: res.data.action,
        debug: debugInfo,
        confidence: debugInfo.retrieval?.best_score || 0
      };

      // Xóa loading message và thay bằng response thực
      setChatHistory(prev => prev.filter(m => m.id !== loadingId).concat(messageObj));
      setSelectedDebugInfo(debugInfo);

    } catch (err) {
      console.error('Chat error:', err);
      // Xóa loading message và thêm error message
      setChatHistory(prev => prev.filter(m => m.id !== loadingId).concat({ role: 'assistant', content: 'Lỗi kết nối đến AI Service. Vui lòng thử lại!', isError: true }));
    } finally {
      setIsSending(false);
    }
  };

  // Styles
  const cardStyle = { background: '#fff', borderRadius: '10px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' };
  const inputStyle = { width: '100%', padding: '12px 16px', border: '2px solid #E2E8F0', borderRadius: '12px', fontSize: '14px', outline: 'none' };
  const btnPrimary = { display: 'flex', alignItems: 'center', gap: '8px', padding: '12px 20px', background: '#e8600a', border: 'none', borderRadius: '12px', fontSize: '14px', fontWeight: '600', color: '#fff', cursor: 'pointer' };
  const btnSecondary = { ...btnPrimary, background: '#F7FAFC', color: '#4A5568', border: '2px solid #E2E8F0' };
  const btnIcon = { padding: '8px', background: '#fff', border: '1px solid #E2E8F0', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' };
  const modalOverlay = { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000 };
  const modalContent = { background: '#fff', borderRadius: '10px', width: '600px', maxHeight: '85vh', overflow: 'auto', padding: '24px' };

  // Get all items from all materials for KS selection
  const allItems = materials.flatMap(m => (m.items || []).map(item => ({ ...item, materialName: m.name, materialId: m.id })));

  return (
    <>

      <div style={{ padding: '0 24px 100px', maxWidth: '1440px', margin: '0 auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h1 style={{ fontSize: '20px', fontWeight: '600', color: '#1A1A1A', margin: '0 0 4px 0', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Sparkles size={32} color="#e8600a" />
              Quản lý Tri thức AI
            </h1>
            <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>Tài liệu → Kho tri thức → Vector DB</p>
          </div>
          <button onClick={loadAllData} style={btnPrimary}><RefreshCw size={18} /> Làm mới</button>
        </div>

        {/* Main Layout */}
        <div style={{ display: 'flex', gap: '24px' }}>
          {/* Sidebar */}
          <div style={{ width: '260px', flexShrink: 0, ...cardStyle, padding: '16px', height: 'fit-content' }}>
            <div style={{ background: apiStatus === 'connected' ? '#D1FAE5' : '#FEE2E2', borderRadius: '12px', padding: '16px', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Bot size={20} color={apiStatus === 'connected' ? '#059669' : '#DC2626'} />
                <span style={{ fontSize: '14px', fontWeight: '600', color: apiStatus === 'connected' ? '#059669' : '#DC2626' }}>
                  {apiStatus === 'connected' ? 'AI Trực tuyến' : 'AI Ngoại tuyến'}
                </span>
              </div>
            </div>

            <div style={{ background: '#F7FAFC', borderRadius: '12px', padding: '16px', marginBottom: '16px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div><div style={{ fontSize: '20px', fontWeight: '600', color: '#e8600a' }}>{materials.length}</div><div style={{ fontSize: '11px', color: '#A0AEC0' }}>Tài liệu</div></div>
                <div><div style={{ fontSize: '20px', fontWeight: '600', color: '#2563EB' }}>{knowledgeStores.length}</div><div style={{ fontSize: '11px', color: '#A0AEC0' }}>Kho tri thức</div></div>
              </div>
            </div>

            {TABS.map(tab => (
              <button key={tab.id} onClick={() => setActiveTab(tab.id)} style={{
                width: '100%', display: 'flex', alignItems: 'center', gap: '12px', padding: '14px 16px',
                background: activeTab === tab.id ? '#fef6f0' : 'transparent',
                border: activeTab === tab.id ? '2px solid #e8600a' : '2px solid transparent',
                borderRadius: '12px', fontSize: '14px', fontWeight: activeTab === tab.id ? '600' : '500',
                color: activeTab === tab.id ? '#e8600a' : '#4A5568', cursor: 'pointer', marginBottom: '8px', textAlign: 'left'
              }}><tab.icon size={20} />{tab.label}</button>
            ))}
          </div>

          {/* Content */}
          <div style={{ flex: 1 }}>
            {/* Materials Tab */}
            {activeTab === 'materials' && (
              <div style={cardStyle}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between' }}>
                  <div>
                    <h2 style={{ fontSize: '18px', fontWeight: '600', margin: '0 0 4px 0' }}>Tài liệu gốc</h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>Nguồn tài liệu gốc (PDF, DOCX, Text)</p>
                  </div>
                  <button onClick={() => setShowMaterialModal(true)} style={btnPrimary}><Plus size={18} /> Thêm tài liệu</button>
                </div>
                <div style={{ padding: '24px' }}>
                  {materials.length === 0 ? (
                    <p style={{ color: '#A0AEC0', textAlign: 'center', padding: '40px' }}>Chưa có tài liệu nào</p>
                  ) : materials.map(m => (
                    <div key={m.id} style={{ border: '2px solid #E2E8F0', borderRadius: '12px', marginBottom: '16px', overflow: 'hidden' }}>
                      <div onClick={() => setExpandedMaterial(expandedMaterial === m.id ? null : m.id)}
                        style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px', cursor: 'pointer', background: '#F7FAFC' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          {expandedMaterial === m.id ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
                          <FolderOpen size={20} color="#e8600a" />
                          <div>
                            <div style={{ fontWeight: '600' }}>{m.name}</div>
                            <div style={{ fontSize: '12px', color: '#A0AEC0' }}>{m.itemCount || 0} mục • {m.createdBy}</div>
                          </div>
                        </div>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button onClick={(e) => { e.stopPropagation(); setEditingMaterial({ ...m }); }} style={btnIcon} title="Chỉnh sửa"><Edit size={16} color="#2563EB" /></button>
                          <button onClick={(e) => { e.stopPropagation(); setSelectedMaterialForItem(m.id); setShowItemModal(true); }} style={btnIcon} title="Thêm mục"><Plus size={16} /></button>
                          <button onClick={(e) => { e.stopPropagation(); handleDeleteMaterial(m.id); }} style={btnIcon} title="Xóa"><Trash2 size={16} color="#DC2626" /></button>
                        </div>
                      </div>
                      {expandedMaterial === m.id && (
                        <div style={{ padding: '16px', borderTop: '1px solid #E2E8F0' }}>
                          {m.description && <p style={{ margin: '0 0 12px', color: '#4A5568', fontSize: '13px' }}>{m.description}</p>}
                          {(!m.items || m.items.length === 0) ? (
                            <p style={{ color: '#A0AEC0', fontSize: '13px' }}>Chưa có mục nào</p>
                          ) : m.items.map(item => (
                            <div key={item.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 12px', background: '#fff', border: '1px solid #E2E8F0', borderRadius: '8px', marginBottom: '8px' }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1 }}>
                                {item.type === 'FILE' ? <Upload size={16} color="#2563EB" /> : <FileText size={16} color="#059669" />}
                                <span style={{ fontSize: '13px' }}>{item.name}</span>
                                <span style={{ fontSize: '11px', color: '#A0AEC0', background: '#F7FAFC', padding: '2px 8px', borderRadius: '4px' }}>{item.type === 'FILE' ? 'Tệp' : 'Văn bản'}</span>
                              </div>
                              <div style={{ display: 'flex', gap: '4px' }}>
                                <button onClick={() => setViewingItem({ ...item, materialId: m.id })} style={{ ...btnIcon, padding: '4px' }} title="Xem chi tiết"><Eye size={14} color="#2563EB" /></button>
                                {item.type === 'TEXT' && (
                                  <button onClick={() => setEditingItem({ ...item, materialId: m.id })} style={{ ...btnIcon, padding: '4px' }} title="Chỉnh sửa"><Edit size={14} color="#059669" /></button>
                                )}
                                <button onClick={() => handleDeleteItem(m.id, item.id)} style={{ ...btnIcon, padding: '4px' }} title="Xóa"><Trash2 size={14} color="#DC2626" /></button>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Knowledge Store Tab */}
            {activeTab === 'knowledge' && (
              <div style={cardStyle}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between' }}>
                  <div>
                    <h2 style={{ fontSize: '18px', fontWeight: '600', margin: '0 0 4px 0' }}>Kho tri thức</h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>Chọn mục để đồng bộ xuống Vector DB</p>
                  </div>
                  <button onClick={() => setShowKSModal(true)} style={btnPrimary}><Plus size={18} /> Thêm kho</button>
                </div>
                <div style={{ padding: '24px' }}>
                  {knowledgeStores.length === 0 ? (
                    <p style={{ color: '#A0AEC0', textAlign: 'center', padding: '40px' }}>Chưa có kho tri thức nào</p>
                  ) : knowledgeStores.map(ks => {
                    const statusStyle = STATUS_STYLES[ks.status] || STATUS_STYLES.CHANGED;
                    return (
                      <div key={ks.id} style={{ border: '2px solid #E2E8F0', borderRadius: '12px', padding: '20px', marginBottom: '16px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                          <div style={{ flex: 1 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
                              <Layers size={20} color="#2563EB" />
                              <span style={{ fontSize: '16px', fontWeight: '600' }}>{ks.name}</span>
                              <span style={{ fontSize: '11px', fontWeight: '600', padding: '4px 10px', borderRadius: '10px', background: statusStyle.bg, color: statusStyle.color }}>{statusStyle.label}</span>
                            </div>
                            {ks.description && <p style={{ margin: '0 0 8px', color: '#4A5568', fontSize: '13px' }}>{ks.description}</p>}
                            <div style={{ fontSize: '12px', color: '#A0AEC0' }}>
                              {ks.itemCount || 0} mục • Tạo bởi {ks.createdBy}
                              {ks.lastSyncedAt && ` • Đồng bộ lần cuối: ${new Date(ks.lastSyncedAt).toLocaleString('vi-VN')}`}
                            </div>
                          </div>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button onClick={() => setViewingKS(ks)} style={btnIcon} title="Xem chi tiết"><Eye size={16} color="#2563EB" /></button>
                            <button onClick={() => setEditingKS({ ...ks })} style={btnIcon} title="Chỉnh sửa"><Edit size={16} color="#059669" /></button>
                            {ks.status === 'CHANGED' && (
                              <button onClick={() => handleSync(ks.id)} disabled={isSyncing[ks.id]}
                                style={{ ...btnPrimary, padding: '8px 16px', opacity: isSyncing[ks.id] ? 0.6 : 1 }}>
                                {isSyncing[ks.id] ? <RefreshCw size={16} className="spin" /> : <Database size={16} />}
                                Đồng bộ
                              </button>
                            )}
                            <button onClick={() => handleDeleteKS(ks.id)} style={btnIcon}><Trash2 size={16} color="#DC2626" /></button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Testing Tab */}
            {activeTab === 'testing' && (
              <div style={{ display: 'flex', gap: '16px', height: 'calc(100vh - 220px)' }}>
                {/* Chat Area - Left */}
                <div style={{ ...cardStyle, flex: 1, display: 'flex', flexDirection: 'column' }}>
                  <div style={{ padding: '20px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <h2 style={{ fontSize: '18px', fontWeight: '600', margin: '0 0 4px 0' }}>Kiểm tra chat</h2>
                      <p style={{ fontSize: '13px', color: '#A0AEC0', margin: 0 }}>Kiểm tra RAG với dữ liệu đã đồng bộ</p>
                    </div>
                    <button onClick={handleClearChat} style={btnSecondary}><RotateCcw size={16} /> Xóa chat</button>
                  </div>
                  <div style={{ flex: 1, padding: '16px', overflowY: 'auto' }}>
                    {chatHistory.map((msg, idx) => (
                      <div key={idx}
                        onClick={() => msg.debug && setSelectedDebugInfo(msg.debug)}
                        style={{
                          display: 'flex', gap: '10px', marginBottom: '12px',
                          flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
                          cursor: msg.debug ? 'pointer' : 'default'
                        }}>
                        <div style={{ width: '28px', height: '28px', borderRadius: '8px', background: msg.role === 'user' ? '#DBEAFE' : '#fef6f0', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                          {msg.role === 'user' ? <User size={14} color="#2563EB" /> : <Bot size={14} color="#e8600a" />}
                        </div>
                        <div style={{
                          maxWidth: '75%', padding: '10px 14px', borderRadius: '12px',
                          background: msg.role === 'user' ? '#DBEAFE' : selectedDebugInfo === msg.debug ? '#fef6f0' : '#F7FAFC',
                          fontSize: '13px',
                          border: selectedDebugInfo === msg.debug ? '2px solid #e8600a' : '2px solid transparent'
                        }}>
                          {msg.isLoading ? (
                            <span style={{ display: 'inline-flex', gap: '4px' }}>
                              <span style={{ animation: 'bounce 1s infinite', animationDelay: '0ms' }}>.</span>
                              <span style={{ animation: 'bounce 1s infinite', animationDelay: '200ms' }}>.</span>
                              <span style={{ animation: 'bounce 1s infinite', animationDelay: '400ms' }}>.</span>
                            </span>
                          ) : msg.content}
                          {msg.confidence !== undefined && msg.confidence > 0 && (
                            <div style={{ marginTop: '6px', fontSize: '11px', color: '#A0AEC0' }}>
                              Score: {(msg.confidence * 100).toFixed(0)}% | {msg.action === 'ESCALATE_TO_LIBRARIAN' ? 'Chuyển thủ thư' : 'RAG OK'}
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                  <div style={{ padding: '12px 16px', borderTop: '1px solid #E2E8F0', background: '#F7FAFC' }}>
                    <div style={{ display: 'flex', gap: '10px' }}>
                      <input placeholder="Nhập câu hỏi..." value={testMessage} onChange={e => setTestMessage(e.target.value)}
                        onKeyPress={e => e.key === 'Enter' && handleSendMessage()} style={{ ...inputStyle, flex: 1, background: '#fff', padding: '10px 14px' }} />
                      <button onClick={handleSendMessage} disabled={isSending} style={{ ...btnPrimary, opacity: isSending ? 0.6 : 1, padding: '10px 16px' }}><Send size={16} /></button>
                    </div>
                  </div>
                </div>

                {/* Debug Panel - Right */}
                <div style={{ ...cardStyle, width: '420px', flexShrink: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                  <div style={{ padding: '16px 20px', borderBottom: '1px solid #E2E8F0', background: '#F7FAFC' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Brain size={18} color="#e8600a" /> Debug RAG
                    </h3>
                    <p style={{ fontSize: '11px', color: '#A0AEC0', margin: '4px 0 0' }}>Click vào tin nhắn để xem AI xử lý thế nào</p>
                  </div>
                  <div style={{ flex: 1, padding: '16px', overflowY: 'auto', fontSize: '12px' }}>
                    {selectedDebugInfo ? (
                      <>
                        {/* Bước 1: Phân Tích Câu Hỏi */}
                        <div style={{ marginBottom: '16px' }}>
                          <div style={{ fontWeight: '600', color: '#4A5568', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <MessageSquare size={14} /> Bước 1: Phân Tích Câu Hỏi
                          </div>
                          <p style={{ fontSize: '10px', color: '#A0AEC0', margin: '0 0 8px 0' }}>Hệ thống chuẩn hóa câu hỏi và kiểm tra có phải lời chào không</p>
                          <div style={{ background: '#F7FAFC', borderRadius: '8px', padding: '10px' }}>
                            <div style={{ marginBottom: '6px' }}><span style={{ color: '#A0AEC0' }}>Câu gốc:</span> <span style={{ color: '#1A1A1A' }}>{selectedDebugInfo.query_analysis?.original}</span></div>
                            <div style={{ marginBottom: '6px' }}><span style={{ color: '#A0AEC0' }}>Sau chuẩn hóa:</span> <span style={{ color: '#1A1A1A', fontFamily: 'monospace', fontSize: '11px' }}>{selectedDebugInfo.query_analysis?.normalized}</span></div>
                            <div>
                              <span style={{ color: '#A0AEC0' }}>Là lời chào:</span>
                              <span style={{ marginLeft: '6px', background: selectedDebugInfo.query_analysis?.is_greeting ? '#FEF3C7' : '#D1FAE5', color: selectedDebugInfo.query_analysis?.is_greeting ? '#D97706' : '#059669', padding: '2px 8px', borderRadius: '4px', fontSize: '10px', fontWeight: '600' }}>
                                {selectedDebugInfo.query_analysis?.is_greeting ? `CÓ: "${selectedDebugInfo.query_analysis?.greeting_pattern}"` : 'KHÔNG'}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* Bước 2: Tìm Kiếm Vector */}
                        <div style={{ marginBottom: '16px' }}>
                          <div style={{ fontWeight: '600', color: '#4A5568', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <Database size={14} /> Bước 2: Tìm Kiếm Vector
                          </div>
                          <p style={{ fontSize: '10px', color: '#A0AEC0', margin: '0 0 8px 0' }}>Tìm các đoạn văn trong Kho Tri Thức có nội dung tương tự câu hỏi</p>
                          <div style={{ background: '#F7FAFC', borderRadius: '8px', padding: '10px' }}>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '10px' }}>
                              <div>
                                <div style={{ color: '#A0AEC0', fontSize: '10px' }}>Điểm tương đồng cao nhất</div>
                                <div style={{ fontSize: '16px', fontWeight: '600', color: selectedDebugInfo.retrieval?.passed_threshold ? '#059669' : '#DC2626' }}>
                                  {((selectedDebugInfo.retrieval?.best_score || 0) * 100).toFixed(1)}%
                                </div>
                              </div>
                              <div>
                                <div style={{ color: '#A0AEC0', fontSize: '10px' }}>Ngưỡng tối thiểu</div>
                                <div style={{ fontSize: '16px', fontWeight: '600', color: '#4A5568' }}>
                                  {((selectedDebugInfo.retrieval?.threshold || 0) * 100).toFixed(0)}%
                                </div>
                              </div>
                            </div>
                            <div style={{ display: 'flex', gap: '12px', fontSize: '11px', color: '#4A5568' }}>
                              <span>Tìm thấy: <b>{selectedDebugInfo.retrieval?.chunks_found || 0}</b></span>
                              <span>Sử dụng: <b>{selectedDebugInfo.retrieval?.chunks_used || 0}</b></span>
                              <span style={{ marginLeft: 'auto', background: selectedDebugInfo.retrieval?.passed_threshold ? '#D1FAE5' : '#FEE2E2', color: selectedDebugInfo.retrieval?.passed_threshold ? '#059669' : '#DC2626', padding: '2px 8px', borderRadius: '4px', fontWeight: '600' }}>
                                {selectedDebugInfo.retrieval?.passed_threshold ? 'ĐẠT' : 'KHÔNG ĐẠT'}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* Bước 3: Đoạn Văn Tìm Được */}
                        {selectedDebugInfo.retrieval?.chunks?.length > 0 && (
                          <div style={{ marginBottom: '16px' }}>
                            <div style={{ fontWeight: '600', color: '#4A5568', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                              <Layers size={14} /> Bước 3: Đoạn Văn Tìm Được ({selectedDebugInfo.retrieval.chunks.length})
                            </div>
                            <p style={{ fontSize: '10px', color: '#A0AEC0', margin: '0 0 8px 0' }}>Các đoạn văn từ Kho Tri Thức có liên quan nhất đến câu hỏi</p>
                            <div style={{ maxHeight: '300px', overflowY: 'auto' }}>
                              {selectedDebugInfo.retrieval.chunks.map((chunk, i) => {
                                const isExpanded = expandedChunks[i] || false;
                                const hasFullContent = chunk.full_content && chunk.full_content.length > 200;
                                return (
                                  <div key={i} style={{ background: i === 0 ? '#fef6f0' : '#F7FAFC', borderRadius: '8px', padding: '10px', marginBottom: '8px', border: i === 0 ? '1px solid #FFE4D6' : '1px solid #E2E8F0' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                                      <span style={{ fontWeight: '600', color: '#4A5568' }}>#{chunk.rank} {i === 0 && '⭐ Phù hợp nhất'}</span>
                                      <span style={{ background: chunk.score >= (selectedDebugInfo.retrieval?.threshold || 0) ? '#D1FAE5' : '#FEE2E2', color: chunk.score >= (selectedDebugInfo.retrieval?.threshold || 0) ? '#059669' : '#DC2626', padding: '2px 8px', borderRadius: '4px', fontSize: '10px', fontWeight: '600' }}>
                                        {(chunk.score * 100).toFixed(1)}%
                                      </span>
                                    </div>
                                    <div style={{ fontSize: '10px', color: '#A0AEC0', marginBottom: '4px' }}>Đến từ: {chunk.source}</div>
                                    <div style={{ fontSize: '11px', color: '#4A5568', lineHeight: '1.5', whiteSpace: 'pre-wrap', background: '#FFFFFF', padding: '8px', borderRadius: '6px', border: '1px solid #E2E8F0' }}>
                                      {isExpanded ? chunk.full_content : chunk.content}
                                    </div>
                                    {hasFullContent && (
                                      <button
                                        onClick={() => setExpandedChunks(prev => ({ ...prev, [i]: !prev[i] }))}
                                        style={{
                                          marginTop: '6px',
                                          fontSize: '10px',
                                          color: '#e8600a',
                                          background: 'none',
                                          border: 'none',
                                          cursor: 'pointer',
                                          padding: 0,
                                          fontWeight: '600'
                                        }}
                                      >
                                        {isExpanded ? '▲ Thu gọn' : '▼ Xem đầy đủ'}
                                      </button>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        )}

                        {/* Bước 4: Sinh Câu Trả Lời */}
                        <div>
                          <div style={{ fontWeight: '600', color: '#4A5568', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <Sparkles size={14} /> Bước 4: Sinh Câu Trả Lời
                          </div>
                          <p style={{ fontSize: '10px', color: '#A0AEC0', margin: '0 0 8px 0' }}>AI LLM tạo câu trả lời dựa trên các đoạn văn tìm được</p>
                          <div style={{ background: '#F7FAFC', borderRadius: '8px', padding: '10px' }}>
                            <div style={{ display: 'flex', gap: '8px', marginBottom: '8px', flexWrap: 'wrap' }}>
                              <span style={{ background: selectedDebugInfo.generation?.used_llm ? '#DBEAFE' : '#F7FAFC', color: selectedDebugInfo.generation?.used_llm ? '#2563EB' : '#A0AEC0', padding: '2px 8px', borderRadius: '4px', fontSize: '10px', fontWeight: '600' }}>
                                Dùng AI: {selectedDebugInfo.generation?.used_llm ? 'CÓ' : 'KHÔNG'}
                              </span>
                              {selectedDebugInfo.generation?.used_chunks_count > 0 && (
                                <span style={{ background: '#D1FAE5', color: '#059669', padding: '2px 8px', borderRadius: '4px', fontSize: '10px', fontWeight: '600' }}>
                                  Dùng {selectedDebugInfo.generation?.used_chunks_count} chunks
                                </span>
                              )}
                              {selectedDebugInfo.generation?.llm_returned_idk && (
                                <span style={{ background: '#FEF3C7', color: '#D97706', padding: '2px 8px', borderRadius: '4px', fontSize: '10px', fontWeight: '600' }}>Không biết</span>
                              )}
                            </div>

                            <div style={{ fontSize: '11px', color: '#4A5568' }}>
                              <span style={{ color: '#A0AEC0' }}>Kết quả:</span> {selectedDebugInfo.generation?.action_reason}
                            </div>
                          </div>
                        </div>
                      </>
                    ) : (
                      <div style={{ textAlign: 'center', color: '#A0AEC0', padding: '40px 20px' }}>
                        <Brain size={40} color="#E2E8F0" style={{ marginBottom: '12px' }} />
                        <p style={{ margin: 0 }}>Gửi tin nhắn để xem thông tin debug</p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Material Modal - Create */}
      {showMaterialModal && (
        <div style={modalOverlay}>
          <div style={{ ...modalContent, width: '500px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Thêm tài liệu mới</h3>
              <button onClick={() => setShowMaterialModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <input placeholder="Tên tài liệu" value={materialForm.name} onChange={e => setMaterialForm({ ...materialForm, name: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }} />
            <textarea placeholder="Mô tả" value={materialForm.description} onChange={e => setMaterialForm({ ...materialForm, description: e.target.value })} style={{ ...inputStyle, minHeight: '80px', marginBottom: '16px' }} />
            <button onClick={handleCreateMaterial} style={{ ...btnPrimary, width: '100%', justifyContent: 'center' }}>Tạo tài liệu</button>
          </div>
        </div>
      )}

      {/* Material Modal - Edit */}
      {editingMaterial && (
        <div style={modalOverlay}>
          <div style={{ ...modalContent, width: '500px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Chỉnh sửa tài liệu</h3>
              <button onClick={() => setEditingMaterial(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <input placeholder="Tên tài liệu" value={editingMaterial.name} onChange={e => setEditingMaterial({ ...editingMaterial, name: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }} />
            <textarea placeholder="Mô tả" value={editingMaterial.description || ''} onChange={e => setEditingMaterial({ ...editingMaterial, description: e.target.value })} style={{ ...inputStyle, minHeight: '80px', marginBottom: '16px' }} />
            <button onClick={handleUpdateMaterial} style={{ ...btnPrimary, width: '100%', justifyContent: 'center' }}><Save size={18} /> Lưu thay đổi</button>
          </div>
        </div>
      )}

      {/* Item Modal - Create */}
      {showItemModal && (
        <div style={modalOverlay}>
          <div style={{ ...modalContent, width: '500px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Thêm mục mới</h3>
              <button onClick={() => { setShowItemModal(false); setItemForm({ name: '', type: 'TEXT', content: '', file: null }); }} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <input placeholder="Tên mục" value={itemForm.name} onChange={e => setItemForm({ ...itemForm, name: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }} />
            <select value={itemForm.type} onChange={e => setItemForm({ ...itemForm, type: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }}>
              <option value="TEXT">Văn bản</option>
              <option value="FILE">Tệp (PDF/DOCX)</option>
            </select>
            {itemForm.type === 'TEXT' ? (
              <textarea placeholder="Nội dung" value={itemForm.content} onChange={e => setItemForm({ ...itemForm, content: e.target.value })} style={{ ...inputStyle, minHeight: '120px', marginBottom: '16px' }} />
            ) : (
              <input type="file" accept=".pdf,.docx,.doc,.txt" onChange={e => setItemForm({ ...itemForm, file: e.target.files[0] })} style={{ marginBottom: '16px' }} />
            )}
            <button onClick={handleAddItem} style={{ ...btnPrimary, width: '100%', justifyContent: 'center' }}>Thêm mục</button>
          </div>
        </div>
      )}

      {/* Item Modal - View */}
      {viewingItem && (
        <div style={modalOverlay}>
          <div style={{ ...modalContent, width: '600px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                {viewingItem.type === 'FILE' ? <Upload size={20} color="#2563EB" /> : <FileText size={20} color="#059669" />}
                Chi tiết mục
              </h3>
              <button onClick={() => setViewingItem(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <div style={{ background: '#F7FAFC', borderRadius: '12px', padding: '16px', marginBottom: '16px' }}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ fontSize: '12px', color: '#A0AEC0', display: 'block', marginBottom: '4px' }}>Tên</label>
                <div style={{ fontWeight: '600' }}>{viewingItem.name}</div>
              </div>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ fontSize: '12px', color: '#A0AEC0', display: 'block', marginBottom: '4px' }}>Loại</label>
                <span style={{ fontSize: '12px', background: viewingItem.type === 'FILE' ? '#DBEAFE' : '#D1FAE5', color: viewingItem.type === 'FILE' ? '#2563EB' : '#059669', padding: '4px 12px', borderRadius: '10px' }}>{viewingItem.type === 'FILE' ? 'Tệp' : 'Văn bản'}</span>
              </div>
              {viewingItem.type === 'FILE' && (
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ fontSize: '12px', color: '#A0AEC0', display: 'block', marginBottom: '4px' }}>Tên tệp</label>
                  <div>{viewingItem.fileName}</div>
                  {viewingItem.fileSize && <div style={{ fontSize: '12px', color: '#A0AEC0' }}>{(viewingItem.fileSize / 1024).toFixed(1)} KB</div>}
                </div>
              )}
              {viewingItem.type === 'TEXT' && viewingItem.content && (
                <div>
                  <label style={{ fontSize: '12px', color: '#A0AEC0', display: 'block', marginBottom: '4px' }}>Nội dung</label>
                  <div style={{ background: '#fff', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '12px', whiteSpace: 'pre-wrap', fontSize: '13px', maxHeight: '300px', overflow: 'auto' }}>{viewingItem.content}</div>
                </div>
              )}
            </div>
            <button onClick={() => setViewingItem(null)} style={{ ...btnSecondary, width: '100%', justifyContent: 'center' }}>Đóng</button>
          </div>
        </div>
      )}

      {/* Item Modal - Edit */}
      {editingItem && (
        <div style={modalOverlay}>
          <div style={{ ...modalContent, width: '600px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Chỉnh sửa mục</h3>
              <button onClick={() => setEditingItem(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <input placeholder="Tên mục" value={editingItem.name} onChange={e => setEditingItem({ ...editingItem, name: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }} />
            <textarea placeholder="Nội dung" value={editingItem.content || ''} onChange={e => setEditingItem({ ...editingItem, content: e.target.value })} style={{ ...inputStyle, minHeight: '200px', marginBottom: '16px' }} />
            <button onClick={handleUpdateItem} style={{ ...btnPrimary, width: '100%', justifyContent: 'center' }}><Save size={18} /> Lưu thay đổi</button>
          </div>
        </div>
      )}

      {/* Knowledge Store Modal - Create */}
      {showKSModal && (
        <div style={modalOverlay}>
          <div style={modalContent}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Thêm kho tri thức mới</h3>
              <button onClick={() => { setShowKSModal(false); setKsForm({ name: '', description: '', itemIds: [] }); setSelectedMaterialForKS(null); }} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <input placeholder="Tên kho" value={ksForm.name} onChange={e => setKsForm({ ...ksForm, name: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }} />
            <textarea placeholder="Mô tả" value={ksForm.description} onChange={e => setKsForm({ ...ksForm, description: e.target.value })} style={{ ...inputStyle, minHeight: '60px', marginBottom: '16px' }} />

            <h4 style={{ margin: '0 0 12px', fontSize: '14px' }}>Chọn tài liệu</h4>
            <select value={selectedMaterialForKS || ''} onChange={e => setSelectedMaterialForKS(e.target.value ? Number(e.target.value) : null)} style={{ ...inputStyle, marginBottom: '16px' }}>
              <option value="">-- Chọn tài liệu --</option>
              {materials.map(m => <option key={m.id} value={m.id}>{m.name} ({m.itemCount} mục)</option>)}
            </select>

            {selectedMaterialForKS && (
              <>
                <h4 style={{ margin: '0 0 12px', fontSize: '14px' }}>Chọn các mục</h4>
                <div style={{ maxHeight: '200px', overflow: 'auto', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '12px' }}>
                  {materials.find(m => m.id === selectedMaterialForKS)?.items?.map(item => (
                    <label key={item.id} style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '8px 0', cursor: 'pointer' }}>
                      <input type="checkbox" checked={ksForm.itemIds.includes(item.id)}
                        onChange={e => {
                          if (e.target.checked) setKsForm({ ...ksForm, itemIds: [...ksForm.itemIds, item.id] });
                          else setKsForm({ ...ksForm, itemIds: ksForm.itemIds.filter(id => id !== item.id) });
                        }} />
                      {item.type === 'FILE' ? <Upload size={14} /> : <FileText size={14} />}
                      {item.name}
                    </label>
                  ))}
                </div>
              </>
            )}

            <button onClick={handleCreateKS} disabled={!ksForm.name || ksForm.itemIds.length === 0}
              style={{ ...btnPrimary, width: '100%', justifyContent: 'center', marginTop: '20px', opacity: (!ksForm.name || ksForm.itemIds.length === 0) ? 0.6 : 1 }}>
              Tạo kho tri thức
            </button>
          </div>
        </div>
      )}

      {/* Knowledge Store Modal - View */}
      {viewingKS && (
        <div style={modalOverlay}>
          <div style={modalContent}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Layers size={20} color="#2563EB" />
                Chi tiết kho tri thức
              </h3>
              <button onClick={() => setViewingKS(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <div style={{ background: '#F7FAFC', borderRadius: '12px', padding: '16px', marginBottom: '16px' }}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ fontSize: '12px', color: '#A0AEC0', display: 'block', marginBottom: '4px' }}>Tên</label>
                <div style={{ fontWeight: '600' }}>{viewingKS.name}</div>
              </div>
              {viewingKS.description && (
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ fontSize: '12px', color: '#A0AEC0', display: 'block', marginBottom: '4px' }}>Mô tả</label>
                  <div>{viewingKS.description}</div>
                </div>
              )}
              <div style={{ marginBottom: '12px' }}>
                <label style={{ fontSize: '12px', color: '#A0AEC0', display: 'block', marginBottom: '4px' }}>Trạng thái</label>
                <span style={{ fontSize: '12px', fontWeight: '600', padding: '4px 12px', borderRadius: '10px', background: STATUS_STYLES[viewingKS.status]?.bg, color: STATUS_STYLES[viewingKS.status]?.color }}>{STATUS_STYLES[viewingKS.status]?.label}</span>
              </div>
              <div>
                <label style={{ fontSize: '12px', color: '#A0AEC0', display: 'block', marginBottom: '8px' }}>Danh sách mục ({viewingKS.items?.length || 0})</label>
                {viewingKS.items?.length > 0 ? (
                  <div style={{ background: '#fff', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '8px' }}>
                    {viewingKS.items.map(item => (
                      <div key={item.id} style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '8px', borderRadius: '6px', marginBottom: '4px', background: '#F7FAFC' }}>
                        {item.type === 'FILE' ? <Upload size={14} color="#2563EB" /> : <FileText size={14} color="#059669" />}
                        <span style={{ fontSize: '13px' }}>{item.name}</span>
                        <span style={{ fontSize: '11px', color: '#A0AEC0' }}>{item.type === 'FILE' ? 'Tệp' : 'Văn bản'}</span>
                      </div>
                    ))}
                  </div>
                ) : <p style={{ color: '#A0AEC0', fontSize: '13px' }}>Chưa có mục nào</p>}
              </div>
            </div>
            <button onClick={() => setViewingKS(null)} style={{ ...btnSecondary, width: '100%', justifyContent: 'center' }}>Đóng</button>
          </div>
        </div>
      )}

      {/* Knowledge Store Modal - Edit */}
      {editingKS && (
        <div style={modalOverlay}>
          <div style={modalContent}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Chỉnh sửa kho tri thức</h3>
              <button onClick={() => setEditingKS(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <input placeholder="Tên kho" value={editingKS.name} onChange={e => setEditingKS({ ...editingKS, name: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }} />
            <textarea placeholder="Mô tả" value={editingKS.description || ''} onChange={e => setEditingKS({ ...editingKS, description: e.target.value })} style={{ ...inputStyle, minHeight: '60px', marginBottom: '16px' }} />

            <h4 style={{ margin: '0 0 12px', fontSize: '14px' }}>Các mục trong kho</h4>
            <div style={{ background: '#F7FAFC', borderRadius: '8px', padding: '12px', marginBottom: '16px' }}>
              {editingKS.items?.length > 0 ? editingKS.items.map(item => (
                <div key={item.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px', background: '#fff', borderRadius: '6px', marginBottom: '4px', border: '1px solid #E2E8F0' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    {item.type === 'FILE' ? <Upload size={14} color="#2563EB" /> : <FileText size={14} color="#059669" />}
                    <span style={{ fontSize: '13px' }}>{item.name}</span>
                  </div>
                  <button onClick={() => setEditingKS({ ...editingKS, items: editingKS.items.filter(i => i.id !== item.id) })} style={{ ...btnIcon, padding: '4px' }}><X size={14} color="#DC2626" /></button>
                </div>
              )) : <p style={{ color: '#A0AEC0', fontSize: '13px', margin: 0 }}>Chưa có mục nào</p>}
            </div>

            <h4 style={{ margin: '0 0 12px', fontSize: '14px' }}>Thêm mục từ tài liệu</h4>
            <div style={{ maxHeight: '200px', overflow: 'auto', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '12px' }}>
              {allItems.filter(item => !editingKS.items?.some(i => i.id === item.id)).map(item => (
                <label key={item.id} style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '8px 0', cursor: 'pointer' }}>
                  <button onClick={() => setEditingKS({ ...editingKS, items: [...(editingKS.items || []), item] })} style={{ ...btnIcon, padding: '4px' }}><Plus size={14} color="#059669" /></button>
                  {item.type === 'FILE' ? <Upload size={14} /> : <FileText size={14} />}
                  <span style={{ fontSize: '13px' }}>{item.name}</span>
                  <span style={{ fontSize: '11px', color: '#A0AEC0' }}>({item.materialName})</span>
                </label>
              ))}
            </div>

            <button onClick={handleUpdateKS} style={{ ...btnPrimary, width: '100%', justifyContent: 'center', marginTop: '20px' }}><Save size={18} /> Lưu thay đổi</button>
          </div>
        </div>
      )}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } } .spin { animation: spin 1s linear infinite; }`}</style>
    </>
  );
};

export default AIConfig;
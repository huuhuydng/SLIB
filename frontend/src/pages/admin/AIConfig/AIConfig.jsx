import React, { useState, useEffect } from 'react';
import {
  Bot, Brain, MessageSquare, Sparkles, RefreshCw, Plus, Trash2, X, CheckCircle,
  AlertTriangle, BookOpen, Send, User, RotateCcw, Database, Upload, FileText,
  FolderOpen, Package, Layers, Edit, ChevronDown, ChevronRight
} from 'lucide-react';
import Header from '../../../components/shared/Header';
import { testAPIConnection, sendTestMessage } from '../../../services/admin/ai/pythonAiApi';
import {
  getMaterials, createMaterial, deleteMaterial, addTextItem, addFileItem, deleteItem,
  getKnowledgeStores, createKnowledgeStore, updateKnowledgeStore, deleteKnowledgeStore, syncKnowledgeStore
} from '../../../services/admin/ai/materialsApi';

const TABS = [
  { id: 'materials', label: 'Materials', icon: Package },
  { id: 'knowledge', label: 'Knowledge Store', icon: Database },
  { id: 'testing', label: 'Test Chat', icon: MessageSquare },
];

const STATUS_STYLES = {
  CHANGED: { bg: '#FEF3C7', color: '#D97706', label: 'Changed' },
  SYNCING: { bg: '#DBEAFE', color: '#2563EB', label: 'Syncing...' },
  SYNCED: { bg: '#D1FAE5', color: '#059669', label: 'Synced' },
  ERROR: { bg: '#FEE2E2', color: '#DC2626', label: 'Error' },
};

const AIConfig = () => {
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

  // Knowledge Store State
  const [knowledgeStores, setKnowledgeStores] = useState([]);
  const [showKSModal, setShowKSModal] = useState(false);
  const [ksForm, setKsForm] = useState({ name: '', description: '', itemIds: [] });
  const [selectedMaterialForKS, setSelectedMaterialForKS] = useState(null);
  const [isSyncing, setIsSyncing] = useState({});

  // Chat State
  const [chatHistory, setChatHistory] = useState([
    { role: 'assistant', content: 'Xin chào! Tôi là SLIB AI. Hãy đặt câu hỏi để test.' }
  ]);
  const [testMessage, setTestMessage] = useState('');
  const [sessionId, setSessionId] = useState(null);
  const [isSending, setIsSending] = useState(false);

  useEffect(() => { loadAllData(); }, []);

  const loadAllData = async () => {
    await Promise.all([checkHealth(), loadMaterials(), loadKnowledgeStores()]);
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
    } catch (e) { console.error('Error loading materials:', e); }
  };

  const loadKnowledgeStores = async () => {
    try {
      const res = await getKnowledgeStores();
      setKnowledgeStores(res.data);
    } catch (e) { console.error('Error loading knowledge stores:', e); }
  };

  // Material Handlers
  const handleCreateMaterial = async () => {
    try {
      await createMaterial(materialForm);
      setShowMaterialModal(false);
      setMaterialForm({ name: '', description: '' });
      await loadMaterials();
    } catch (e) { alert('Error: ' + e.message); }
  };

  const handleDeleteMaterial = async (id) => {
    if (!confirm('Xóa material này?')) return;
    try {
      await deleteMaterial(id);
      await loadMaterials();
    } catch (e) { alert('Error: ' + e.message); }
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
    } catch (e) { alert('Error: ' + e.message); }
  };

  const handleDeleteItem = async (materialId, itemId) => {
    if (!confirm('Xóa item này?')) return;
    try {
      await deleteItem(materialId, itemId);
      await loadMaterials();
    } catch (e) { alert('Error: ' + e.message); }
  };

  // Knowledge Store Handlers
  const handleCreateKS = async () => {
    try {
      await createKnowledgeStore(ksForm);
      setShowKSModal(false);
      setKsForm({ name: '', description: '', itemIds: [] });
      await loadKnowledgeStores();
    } catch (e) { alert('Error: ' + e.message); }
  };

  const handleDeleteKS = async (id) => {
    if (!confirm('Xóa knowledge store này?')) return;
    try {
      await deleteKnowledgeStore(id);
      await loadKnowledgeStores();
    } catch (e) { alert('Error: ' + e.message); }
  };

  const handleSync = async (id) => {
    setIsSyncing(prev => ({ ...prev, [id]: true }));
    try {
      const res = await syncKnowledgeStore(id);
      alert(`✅ Sync thành công! Chunks: ${res.data.chunksCreated}`);
      await loadKnowledgeStores();
    } catch (e) {
      alert('❌ Sync failed: ' + e.message);
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

    try {
      const res = await sendTestMessage(msg, sessionId);
      setSessionId(res.data.sessionId);
      setChatHistory(prev => [...prev, {
        role: 'assistant', content: res.data.reply,
        needsLibrarian: res.data.needsLibrarian, confidence: res.data.confidence
      }]);
    } catch {
      setChatHistory(prev => [...prev, { role: 'assistant', content: '❌ Lỗi kết nối', isError: true }]);
    } finally {
      setIsSending(false);
    }
  };

  // Styles
  const cardStyle = { background: '#fff', borderRadius: '16px', boxShadow: '0 4px 20px rgba(0,0,0,0.06)' };
  const inputStyle = { width: '100%', padding: '12px 16px', border: '2px solid #E2E8F0', borderRadius: '12px', fontSize: '14px', outline: 'none' };
  const btnPrimary = { display: 'flex', alignItems: 'center', gap: '8px', padding: '12px 20px', background: '#FF751F', border: 'none', borderRadius: '12px', fontSize: '14px', fontWeight: '600', color: '#fff', cursor: 'pointer' };
  const btnSecondary = { ...btnPrimary, background: '#F7FAFC', color: '#4A5568', border: '2px solid #E2E8F0' };

  // Get all items from all materials for KS selection
  const allItems = materials.flatMap(m => (m.items || []).map(item => ({ ...item, materialName: m.name, materialId: m.id })));

  return (
    <>
      <Header searchPlaceholder="Tìm kiếm..." />
      <div style={{ padding: '0 24px 100px', maxWidth: '1440px', margin: '0 auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h1 style={{ fontSize: '28px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Sparkles size={32} color="#FF751F" />
              AI Knowledge Management
            </h1>
            <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>Materials → Knowledge Store → Vector DB</p>
          </div>
          <button onClick={loadAllData} style={btnPrimary}><RefreshCw size={18} /> Refresh</button>
        </div>

        {/* Main Layout */}
        <div style={{ display: 'flex', gap: '24px' }}>
          {/* Sidebar */}
          <div style={{ width: '260px', flexShrink: 0, ...cardStyle, padding: '16px', height: 'fit-content' }}>
            <div style={{ background: apiStatus === 'connected' ? '#D1FAE5' : '#FEE2E2', borderRadius: '12px', padding: '16px', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Bot size={20} color={apiStatus === 'connected' ? '#059669' : '#DC2626'} />
                <span style={{ fontSize: '14px', fontWeight: '600', color: apiStatus === 'connected' ? '#059669' : '#DC2626' }}>
                  {apiStatus === 'connected' ? 'AI Online' : 'AI Offline'}
                </span>
              </div>
            </div>

            <div style={{ background: '#F7FAFC', borderRadius: '12px', padding: '16px', marginBottom: '16px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div><div style={{ fontSize: '20px', fontWeight: '700', color: '#FF751F' }}>{materials.length}</div><div style={{ fontSize: '11px', color: '#A0AEC0' }}>Materials</div></div>
                <div><div style={{ fontSize: '20px', fontWeight: '700', color: '#2563EB' }}>{knowledgeStores.length}</div><div style={{ fontSize: '11px', color: '#A0AEC0' }}>Stores</div></div>
              </div>
            </div>

            {TABS.map(tab => (
              <button key={tab.id} onClick={() => setActiveTab(tab.id)} style={{
                width: '100%', display: 'flex', alignItems: 'center', gap: '12px', padding: '14px 16px',
                background: activeTab === tab.id ? '#FFF7F2' : 'transparent',
                border: activeTab === tab.id ? '2px solid #FF751F' : '2px solid transparent',
                borderRadius: '12px', fontSize: '14px', fontWeight: activeTab === tab.id ? '600' : '500',
                color: activeTab === tab.id ? '#FF751F' : '#4A5568', cursor: 'pointer', marginBottom: '8px', textAlign: 'left'
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
                    <h2 style={{ fontSize: '18px', fontWeight: '700', margin: '0 0 4px 0' }}>📦 Materials</h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>Nguồn tài liệu gốc (PDF, DOCX, Text)</p>
                  </div>
                  <button onClick={() => setShowMaterialModal(true)} style={btnPrimary}><Plus size={18} /> Add Material</button>
                </div>
                <div style={{ padding: '24px' }}>
                  {materials.length === 0 ? (
                    <p style={{ color: '#A0AEC0', textAlign: 'center', padding: '40px' }}>Chưa có material nào</p>
                  ) : materials.map(m => (
                    <div key={m.id} style={{ border: '2px solid #E2E8F0', borderRadius: '12px', marginBottom: '16px', overflow: 'hidden' }}>
                      <div onClick={() => setExpandedMaterial(expandedMaterial === m.id ? null : m.id)}
                        style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px', cursor: 'pointer', background: '#F7FAFC' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          {expandedMaterial === m.id ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
                          <FolderOpen size={20} color="#FF751F" />
                          <div>
                            <div style={{ fontWeight: '600' }}>{m.name}</div>
                            <div style={{ fontSize: '12px', color: '#A0AEC0' }}>{m.itemCount || 0} items • {m.createdBy}</div>
                          </div>
                        </div>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button onClick={(e) => { e.stopPropagation(); setSelectedMaterialForItem(m.id); setShowItemModal(true); }}
                            style={{ padding: '8px', background: '#fff', border: '1px solid #E2E8F0', borderRadius: '8px', cursor: 'pointer' }}><Plus size={16} /></button>
                          <button onClick={(e) => { e.stopPropagation(); handleDeleteMaterial(m.id); }}
                            style={{ padding: '8px', background: '#fff', border: '1px solid #E2E8F0', borderRadius: '8px', cursor: 'pointer' }}><Trash2 size={16} color="#DC2626" /></button>
                        </div>
                      </div>
                      {expandedMaterial === m.id && (
                        <div style={{ padding: '16px', borderTop: '1px solid #E2E8F0' }}>
                          {m.description && <p style={{ margin: '0 0 12px', color: '#4A5568', fontSize: '13px' }}>{m.description}</p>}
                          {(!m.items || m.items.length === 0) ? (
                            <p style={{ color: '#A0AEC0', fontSize: '13px' }}>Chưa có items</p>
                          ) : m.items.map(item => (
                            <div key={item.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 12px', background: '#fff', border: '1px solid #E2E8F0', borderRadius: '8px', marginBottom: '8px' }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                {item.type === 'FILE' ? <Upload size={16} color="#2563EB" /> : <FileText size={16} color="#059669" />}
                                <span style={{ fontSize: '13px' }}>{item.name}</span>
                                <span style={{ fontSize: '11px', color: '#A0AEC0', background: '#F7FAFC', padding: '2px 8px', borderRadius: '4px' }}>{item.type}</span>
                              </div>
                              <button onClick={() => handleDeleteItem(m.id, item.id)} style={{ padding: '4px', background: 'transparent', border: 'none', cursor: 'pointer' }}><Trash2 size={14} color="#DC2626" /></button>
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
                    <h2 style={{ fontSize: '18px', fontWeight: '700', margin: '0 0 4px 0' }}>🗄️ Knowledge Store</h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>Chọn items để sync xuống Vector DB</p>
                  </div>
                  <button onClick={() => setShowKSModal(true)} style={btnPrimary}><Plus size={18} /> Add Store</button>
                </div>
                <div style={{ padding: '24px' }}>
                  {knowledgeStores.length === 0 ? (
                    <p style={{ color: '#A0AEC0', textAlign: 'center', padding: '40px' }}>Chưa có knowledge store nào</p>
                  ) : knowledgeStores.map(ks => {
                    const statusStyle = STATUS_STYLES[ks.status] || STATUS_STYLES.CHANGED;
                    return (
                      <div key={ks.id} style={{ border: '2px solid #E2E8F0', borderRadius: '12px', padding: '20px', marginBottom: '16px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                          <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
                              <Layers size={20} color="#2563EB" />
                              <span style={{ fontSize: '16px', fontWeight: '600' }}>{ks.name}</span>
                              <span style={{ fontSize: '11px', fontWeight: '600', padding: '4px 10px', borderRadius: '20px', background: statusStyle.bg, color: statusStyle.color }}>{statusStyle.label}</span>
                            </div>
                            {ks.description && <p style={{ margin: '0 0 8px', color: '#4A5568', fontSize: '13px' }}>{ks.description}</p>}
                            <div style={{ fontSize: '12px', color: '#A0AEC0' }}>
                              {ks.itemCount || 0} items • Created by {ks.createdBy}
                              {ks.lastSyncedAt && ` • Last sync: ${new Date(ks.lastSyncedAt).toLocaleString()}`}
                            </div>
                          </div>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            {ks.status === 'CHANGED' && (
                              <button onClick={() => handleSync(ks.id)} disabled={isSyncing[ks.id]}
                                style={{ ...btnPrimary, padding: '8px 16px', opacity: isSyncing[ks.id] ? 0.6 : 1 }}>
                                {isSyncing[ks.id] ? <RefreshCw size={16} className="spin" /> : <Database size={16} />}
                                Sync
                              </button>
                            )}
                            <button onClick={() => handleDeleteKS(ks.id)} style={{ padding: '8px', background: '#fff', border: '1px solid #E2E8F0', borderRadius: '8px', cursor: 'pointer' }}>
                              <Trash2 size={16} color="#DC2626" />
                            </button>
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
              <div style={{ ...cardStyle, display: 'flex', flexDirection: 'column', height: 'calc(100vh - 220px)' }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between' }}>
                  <div>
                    <h2 style={{ fontSize: '18px', fontWeight: '700', margin: '0 0 4px 0' }}>🧪 Test Chat</h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>Test RAG với dữ liệu đã sync</p>
                  </div>
                  <button onClick={() => { setChatHistory([{ role: 'assistant', content: 'Chat reset!' }]); setSessionId(null); }} style={btnSecondary}><RotateCcw size={16} /> Clear</button>
                </div>
                <div style={{ flex: 1, padding: '24px', overflowY: 'auto' }}>
                  {chatHistory.map((msg, idx) => (
                    <div key={idx} style={{ display: 'flex', gap: '12px', marginBottom: '16px', flexDirection: msg.role === 'user' ? 'row-reverse' : 'row' }}>
                      <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: msg.role === 'user' ? '#DBEAFE' : '#FFF7F2', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        {msg.role === 'user' ? <User size={16} color="#2563EB" /> : <Bot size={16} color="#FF751F" />}
                      </div>
                      <div style={{ maxWidth: '70%', padding: '12px 16px', borderRadius: '12px', background: msg.role === 'user' ? '#DBEAFE' : '#F7FAFC', fontSize: '14px' }}>
                        {msg.content}
                        {msg.confidence !== undefined && <div style={{ marginTop: '8px', fontSize: '11px', color: '#A0AEC0' }}>Score: {(msg.confidence * 100).toFixed(0)}%</div>}
                      </div>
                    </div>
                  ))}
                </div>
                <div style={{ padding: '16px 24px', borderTop: '1px solid #E2E8F0', background: '#F7FAFC' }}>
                  <div style={{ display: 'flex', gap: '12px' }}>
                    <input placeholder="Nhập câu hỏi..." value={testMessage} onChange={e => setTestMessage(e.target.value)}
                      onKeyPress={e => e.key === 'Enter' && handleSendMessage()} style={{ ...inputStyle, flex: 1, background: '#fff' }} />
                    <button onClick={handleSendMessage} disabled={isSending} style={{ ...btnPrimary, opacity: isSending ? 0.6 : 1 }}><Send size={18} /></button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Material Modal */}
      {showMaterialModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000 }}>
          <div style={{ background: '#fff', borderRadius: '20px', width: '500px', padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Add Material</h3>
              <button onClick={() => setShowMaterialModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <input placeholder="Name" value={materialForm.name} onChange={e => setMaterialForm({ ...materialForm, name: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }} />
            <textarea placeholder="Description" value={materialForm.description} onChange={e => setMaterialForm({ ...materialForm, description: e.target.value })} style={{ ...inputStyle, minHeight: '80px', marginBottom: '16px' }} />
            <button onClick={handleCreateMaterial} style={{ ...btnPrimary, width: '100%', justifyContent: 'center' }}>Create Material</button>
          </div>
        </div>
      )}

      {/* Item Modal */}
      {showItemModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000 }}>
          <div style={{ background: '#fff', borderRadius: '20px', width: '500px', padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Add Item</h3>
              <button onClick={() => { setShowItemModal(false); setItemForm({ name: '', type: 'TEXT', content: '', file: null }); }} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <input placeholder="Name" value={itemForm.name} onChange={e => setItemForm({ ...itemForm, name: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }} />
            <select value={itemForm.type} onChange={e => setItemForm({ ...itemForm, type: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }}>
              <option value="TEXT">Text</option>
              <option value="FILE">File (PDF/DOCX)</option>
            </select>
            {itemForm.type === 'TEXT' ? (
              <textarea placeholder="Content" value={itemForm.content} onChange={e => setItemForm({ ...itemForm, content: e.target.value })} style={{ ...inputStyle, minHeight: '120px', marginBottom: '16px' }} />
            ) : (
              <input type="file" accept=".pdf,.docx,.doc,.txt" onChange={e => setItemForm({ ...itemForm, file: e.target.files[0] })} style={{ marginBottom: '16px' }} />
            )}
            <button onClick={handleAddItem} style={{ ...btnPrimary, width: '100%', justifyContent: 'center' }}>Add Item</button>
          </div>
        </div>
      )}

      {/* Knowledge Store Modal */}
      {showKSModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000 }}>
          <div style={{ background: '#fff', borderRadius: '20px', width: '600px', maxHeight: '80vh', overflow: 'auto', padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>Add Knowledge Store</h3>
              <button onClick={() => { setShowKSModal(false); setKsForm({ name: '', description: '', itemIds: [] }); setSelectedMaterialForKS(null); }} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <input placeholder="Name" value={ksForm.name} onChange={e => setKsForm({ ...ksForm, name: e.target.value })} style={{ ...inputStyle, marginBottom: '12px' }} />
            <textarea placeholder="Description" value={ksForm.description} onChange={e => setKsForm({ ...ksForm, description: e.target.value })} style={{ ...inputStyle, minHeight: '60px', marginBottom: '16px' }} />

            <h4 style={{ margin: '0 0 12px', fontSize: '14px' }}>Select Material</h4>
            <select value={selectedMaterialForKS || ''} onChange={e => setSelectedMaterialForKS(e.target.value ? Number(e.target.value) : null)} style={{ ...inputStyle, marginBottom: '16px' }}>
              <option value="">-- Chọn Material --</option>
              {materials.map(m => <option key={m.id} value={m.id}>{m.name} ({m.itemCount} items)</option>)}
            </select>

            {selectedMaterialForKS && (
              <>
                <h4 style={{ margin: '0 0 12px', fontSize: '14px' }}>Select Items</h4>
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
              Create Knowledge Store
            </button>
          </div>
        </div>
      )}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } } .spin { animation: spin 1s linear infinite; }`}</style>
    </>
  );
};

export default AIConfig;
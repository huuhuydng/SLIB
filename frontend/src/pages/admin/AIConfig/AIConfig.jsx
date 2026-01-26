import React, { useState, useEffect } from 'react';
import { sendTestMessage, testAPIConnection } from '../../../services/admin/ai/pythonAiApi';
import {
  getAIConfig, saveAIConfig, resetAIConfig,
  getKnowledge, createKnowledge, updateKnowledge, deleteKnowledge,
  getPrompts, createPrompt, updatePrompt, deletePrompt
} from '../../../services/admin/ai/aiConfigApi';
import {
  Bot,
  Key,
  Brain,
  MessageSquare,
  Sparkles,
  Settings,
  Save,
  RefreshCw,
  Plus,
  Edit,
  Trash2,
  X,
  CheckCircle,
  AlertTriangle,
  Eye,
  EyeOff,
  Copy,
  ExternalLink,
  BookOpen,
  FileText,
  Lightbulb,
  Zap,
  Shield,
  Clock,
  ChevronRight,
  Send,
  User,
  RotateCcw,
  Wand2,
  Database,
  Globe
} from 'lucide-react';
import Header from '../Dashboard/Header';

// Type mapping for display
const TYPE_MAP = { RULES: 'rules', GUIDE: 'guide', INFO: 'info' };
const REVERSE_TYPE_MAP = { rules: 'RULES', guide: 'GUIDE', info: 'INFO' };

// Initial welcome message
const INITIAL_MESSAGE = {
  role: 'assistant',
  content: 'Xin chào! Tôi là SLIB AI Assistant. Bạn có thể đặt câu hỏi để test khả năng trả lời của AI.'
};

const AIConfig = () => {
  const [activeTab, setActiveTab] = useState('settings');
  const [showApiKey, setShowApiKey] = useState(false);
  const [isTestingApi, setIsTestingApi] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [apiStatus, setApiStatus] = useState('unknown');
  const [showKnowledgeModal, setShowKnowledgeModal] = useState(false);
  const [showPromptModal, setShowPromptModal] = useState(false);
  const [testMessage, setTestMessage] = useState('');
  const [editingKnowledge, setEditingKnowledge] = useState(null);
  const [editingPrompt, setEditingPrompt] = useState(null);

  // Data from backend
  const [knowledgeList, setKnowledgeList] = useState([]);
  const [promptsList, setPromptsList] = useState([]);
  const [systemPrompt, setSystemPrompt] = useState('');

  // Modal form state
  const [knowledgeForm, setKnowledgeForm] = useState({ title: '', content: '', type: 'info' });
  const [promptForm, setPromptForm] = useState({ name: '', prompt: '', context: 'GENERAL' });

  // Chat testing state
  const [chatHistory, setChatHistory] = useState([INITIAL_MESSAGE]);
  const [sessionId, setSessionId] = useState(null);
  const [isSending, setIsSending] = useState(false);
  const [chatError, setChatError] = useState(null);

  // AI Settings State
  const [aiSettings, setAiSettings] = useState({
    provider: 'ollama',
    ollamaModel: 'llama3.2',
    ollamaUrl: 'http://localhost:11434',
    geminiModel: 'gemini-2.0-flash',
    temperature: 0.7,
    maxTokens: 1024,
    enableContext: true,
    enableHistory: true,
    responseLanguage: 'vi',
    autoSuggest: true,
  });

  const [connectedModel, setConnectedModel] = useState('llama3.2');

  const tabs = [
    { id: 'settings', label: 'Cài đặt API', icon: Key },
    { id: 'knowledge', label: 'Knowledge Base', icon: BookOpen },
    { id: 'prompts', label: 'Prompt Templates', icon: Wand2 },
    { id: 'testing', label: 'Test & Preview', icon: MessageSquare },
  ];

  // Load all data on mount
  useEffect(() => {
    loadAllData();
  }, []);

  const loadAllData = async () => {
    await Promise.all([loadConfig(), loadKnowledge(), loadPrompts(), checkAIHealth()]);
  };

  // Load config from Java backend
  const loadConfig = async () => {
    try {
      const res = await getAIConfig();
      if (res.data.configured && res.data.config) {
        const c = res.data.config;
        setAiSettings({
          provider: c.provider || 'ollama',
          ollamaModel: c.ollamaModel || 'llama3.2',
          ollamaUrl: c.ollamaUrl || 'http://localhost:11434',
          geminiModel: c.geminiModel || 'gemini-2.0-flash',
          temperature: c.temperature || 0.7,
          maxTokens: c.maxTokens || 1024,
          enableContext: c.enableContext ?? true,
          enableHistory: c.enableHistory ?? true,
          responseLanguage: c.responseLanguage || 'vi',
          autoSuggest: c.autoSuggest ?? true,
        });
        setSystemPrompt(c.systemPrompt || '');
      }
    } catch (e) {
      console.error('Error loading config:', e);
    }
  };

  // Load knowledge from Java backend
  const loadKnowledge = async () => {
    try {
      const res = await getKnowledge();
      const data = res.data.map(k => ({
        ...k,
        type: TYPE_MAP[k.type] || k.type?.toLowerCase() || 'info',
        updatedAt: k.updatedAt?.split('T')[0] || ''
      }));
      setKnowledgeList(data);
    } catch (e) {
      console.error('Error loading knowledge:', e);
    }
  };

  // Load prompts from Java backend
  const loadPrompts = async () => {
    try {
      const res = await getPrompts();
      setPromptsList(res.data);
    } catch (e) {
      console.error('Error loading prompts:', e);
    }
  };

  // Save config to Java backend
  const handleSaveConfig = async () => {
    setIsSaving(true);
    try {
      await saveAIConfig({
        provider: aiSettings.provider,
        ollamaModel: aiSettings.ollamaModel,
        ollamaUrl: aiSettings.ollamaUrl,
        geminiModel: aiSettings.geminiModel,
        temperature: aiSettings.temperature,
        maxTokens: aiSettings.maxTokens,
        enableContext: aiSettings.enableContext,
        enableHistory: aiSettings.enableHistory,
        autoSuggest: aiSettings.autoSuggest,
        responseLanguage: aiSettings.responseLanguage,
        systemPrompt: systemPrompt,
      });
      alert('Đã lưu cấu hình thành công!');
    } catch (e) {
      console.error('Error saving config:', e);
      alert('Lỗi lưu cấu hình: ' + e.message);
    } finally {
      setIsSaving(false);
    }
  };

  // Reset config to defaults
  const handleResetConfig = async () => {
    if (!confirm('Bạn có chắc muốn reset cấu hình về mặc định?')) return;
    try {
      await resetAIConfig();
      await loadConfig();
      alert('Đã reset cấu hình về mặc định!');
    } catch (e) {
      console.error('Error resetting config:', e);
      alert('Lỗi reset cấu hình: ' + e.message);
    }
  };

  // Knowledge CRUD
  const handleSaveKnowledge = async () => {
    try {
      const payload = {
        title: knowledgeForm.title,
        content: knowledgeForm.content,
        type: REVERSE_TYPE_MAP[knowledgeForm.type] || 'INFO',
        isActive: true
      };
      if (editingKnowledge) {
        await updateKnowledge(editingKnowledge.id, payload);
      } else {
        await createKnowledge(payload);
      }
      await loadKnowledge();
      setShowKnowledgeModal(false);
      setEditingKnowledge(null);
      setKnowledgeForm({ title: '', content: '', type: 'info' });
    } catch (e) {
      console.error('Error saving knowledge:', e);
      alert('Lỗi lưu kiến thức: ' + e.message);
    }
  };

  const handleDeleteKnowledge = async (id) => {
    if (!confirm('Bạn có chắc muốn xóa kiến thức này?')) return;
    try {
      await deleteKnowledge(id);
      await loadKnowledge();
    } catch (e) {
      console.error('Error deleting knowledge:', e);
      alert('Lỗi xóa kiến thức: ' + e.message);
    }
  };

  // Prompt CRUD
  const handleSavePrompt = async () => {
    try {
      const payload = {
        name: promptForm.name,
        prompt: promptForm.prompt,
        context: promptForm.context,
        isActive: true
      };
      if (editingPrompt) {
        await updatePrompt(editingPrompt.id, payload);
      } else {
        await createPrompt(payload);
      }
      await loadPrompts();
      setShowPromptModal(false);
      setEditingPrompt(null);
      setPromptForm({ name: '', prompt: '', context: 'GENERAL' });
    } catch (e) {
      console.error('Error saving prompt:', e);
      alert('Lỗi lưu prompt: ' + e.message);
    }
  };

  const handleDeletePrompt = async (id) => {
    if (!confirm('Bạn có chắc muốn xóa prompt này?')) return;
    try {
      await deletePrompt(id);
      await loadPrompts();
    } catch (e) {
      console.error('Error deleting prompt:', e);
      alert('Lỗi xóa prompt: ' + e.message);
    }
  };

  const checkAIHealth = async () => {
    try {
      const result = await testAPIConnection();
      setApiStatus(result.data.success ? 'connected' : 'error');
      if (result.data.model) {
        setConnectedModel(result.data.model);
      }
    } catch (e) {
      setApiStatus('error');
    }
  };

  const handleTestApi = async () => {
    setIsTestingApi(true);
    try {
      const result = await testAPIConnection();
      setApiStatus(result.data.success ? 'connected' : 'error');
      if (result.data.model) {
        setConnectedModel(result.data.model);
      }
    } catch (e) {
      setApiStatus('error');
    } finally {
      setIsTestingApi(false);
    }
  };

  // Send test message to AI
  const handleSendTestMessage = async () => {
    if (!testMessage.trim() || isSending) return;

    const userMsg = { role: 'user', content: testMessage };
    setChatHistory(prev => [...prev, userMsg]);
    setTestMessage('');
    setIsSending(true);
    setChatError(null);

    try {
      const res = await sendTestMessage(testMessage, sessionId);
      const { reply, sessionId: newSessionId, needsLibrarian, confidence } = res.data;

      setSessionId(newSessionId);

      const botMsg = {
        role: 'assistant',
        content: reply,
        needsLibrarian,
        confidence
      };
      setChatHistory(prev => [...prev, botMsg]);
    } catch (e) {
      console.error('AI Test Error:', e);
      setChatError('Không thể kết nối AI Service. Kiểm tra xem AI Service đang chạy trên port 8001.');
      setChatHistory(prev => [...prev, { role: 'assistant', content: '❌ Lỗi: Không thể kết nối với AI Service.', isError: true }]);
    } finally {
      setIsSending(false);
    }
  };

  // Clear chat history
  const handleClearChat = () => {
    setChatHistory([INITIAL_MESSAGE]);
    setSessionId(null);
    setChatError(null);
  };

  const getTypeStyle = (type) => {
    switch (type) {
      case 'rules': return { bg: '#FEE2E2', color: '#DC2626', label: 'Quy định' };
      case 'guide': return { bg: '#DBEAFE', color: '#2563EB', label: 'Hướng dẫn' };
      case 'info': return { bg: '#D1FAE5', color: '#059669', label: 'Thông tin' };
      default: return { bg: '#F3F4F6', color: '#6B7280', label: 'Khác' };
    }
  };

  return (
    <>
      <Header searchPlaceholder="Tìm kiếm..." />

      <div style={{
        padding: '0 24px 100px',
        maxWidth: '1440px',
        margin: '0 auto',
      }}>
        {/* Page Header */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px'
        }}>
          <div>
            <h1 style={{ fontSize: '28px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Sparkles size={32} color="#FF751F" />
              Cấu hình AI Assistant
            </h1>
            <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
              Ollama AI - Local, không giới hạn, không cần API Key
            </p>
          </div>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={handleResetConfig}
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
                cursor: 'pointer'
              }}>
              <RotateCcw size={18} />
              Reset mặc định
            </button>
            <button
              onClick={handleSaveConfig}
              disabled={isSaving}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '12px 20px',
                background: isSaving ? '#A0AEC0' : '#FF751F',
                border: 'none',
                borderRadius: '12px',
                fontSize: '14px',
                fontWeight: '600',
                color: '#fff',
                cursor: isSaving ? 'not-allowed' : 'pointer',
                boxShadow: '0 4px 14px rgba(255, 117, 31, 0.25)'
              }}>
              {isSaving ? <RefreshCw size={18} className="spin" /> : <Save size={18} />}
              {isSaving ? 'Đang lưu...' : 'Lưu cấu hình'}
            </button>
          </div>
        </div>

        {/* Main Content */}
        <div style={{ display: 'flex', gap: '24px' }}>
          {/* Sidebar Tabs */}
          <div style={{
            width: '280px',
            flexShrink: 0,
            background: '#fff',
            borderRadius: '16px',
            padding: '16px',
            boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
            height: 'fit-content'
          }}>
            {/* AI Status Card */}
            <div style={{
              background: apiStatus === 'connected' ? '#D1FAE5' : '#FEE2E2',
              borderRadius: '12px',
              padding: '16px',
              marginBottom: '16px'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                <Bot size={20} color={apiStatus === 'connected' ? '#059669' : '#DC2626'} />
                <span style={{ fontSize: '14px', fontWeight: '600', color: apiStatus === 'connected' ? '#059669' : '#DC2626' }}>
                  {apiStatus === 'connected' ? 'AI Đang hoạt động' : 'AI Không kết nối'}
                </span>
              </div>
              <div style={{ fontSize: '12px', color: apiStatus === 'connected' ? '#059669' : '#DC2626', opacity: 0.8 }}>
                🦙 Ollama - {connectedModel}
              </div>
            </div>

            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '14px 16px',
                  background: activeTab === tab.id ? '#FFF7F2' : 'transparent',
                  border: activeTab === tab.id ? '2px solid #FF751F' : '2px solid transparent',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: activeTab === tab.id ? '600' : '500',
                  color: activeTab === tab.id ? '#FF751F' : '#4A5568',
                  cursor: 'pointer',
                  marginBottom: '8px',
                  textAlign: 'left',
                  transition: 'all 0.2s ease'
                }}
              >
                <tab.icon size={20} />
                {tab.label}
                {activeTab === tab.id && <ChevronRight size={16} style={{ marginLeft: 'auto' }} />}
              </button>
            ))}
          </div>

          {/* Content Area */}
          <div style={{ flex: 1 }}>
            {/* API Settings Tab */}
            {activeTab === 'settings' && (
              <div style={{
                background: '#fff',
                borderRadius: '16px',
                boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0' }}>
                  <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                    🦙 Cài đặt Ollama AI
                  </h2>
                  <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                    Cấu hình AI local với Ollama - Không giới hạn, không cần API Key
                  </p>
                </div>
                <div style={{ padding: '24px' }}>
                  {/* Ollama Connection */}
                  <div style={{ marginBottom: '32px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Globe size={18} color="#FF751F" />
                      Kết nối Ollama
                    </h3>
                    <div style={{ display: 'flex', gap: '12px', marginBottom: '12px' }}>
                      <div style={{ flex: 1 }}>
                        <input
                          type="text"
                          value={aiSettings.ollamaUrl}
                          onChange={(e) => setAiSettings({ ...aiSettings, ollamaUrl: e.target.value })}
                          placeholder="http://localhost:11434"
                          style={{
                            width: '100%',
                            padding: '14px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            fontFamily: 'monospace',
                            outline: 'none'
                          }}
                        />
                      </div>
                      <button
                        onClick={handleTestApi}
                        disabled={isTestingApi}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '8px',
                          padding: '14px 20px',
                          background: '#FF751F',
                          border: 'none',
                          borderRadius: '12px',
                          fontSize: '14px',
                          fontWeight: '600',
                          color: '#fff',
                          cursor: isTestingApi ? 'not-allowed' : 'pointer',
                          opacity: isTestingApi ? 0.7 : 1
                        }}
                      >
                        {isTestingApi ? <RefreshCw size={18} className="spin" /> : <Zap size={18} />}
                        {isTestingApi ? 'Đang test...' : 'Test kết nối'}
                      </button>
                    </div>
                    <div style={{
                      padding: '12px 16px',
                      background: apiStatus === 'connected' ? '#D1FAE5' : '#FEF3C7',
                      borderRadius: '8px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px'
                    }}>
                      {apiStatus === 'connected' ? (
                        <CheckCircle size={16} color="#059669" />
                      ) : (
                        <AlertTriangle size={16} color="#D97706" />
                      )}
                      <span style={{ fontSize: '13px', color: apiStatus === 'connected' ? '#059669' : '#D97706' }}>
                        {apiStatus === 'connected'
                          ? `Đã kết nối! Model: ${connectedModel}`
                          : 'Chưa kết nối. Chạy: ollama serve'}
                      </span>
                    </div>
                  </div>

                  {/* Model Settings */}
                  <div style={{ marginBottom: '32px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Brain size={18} color="#FF751F" />
                      Cài đặt Model
                    </h3>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Ollama Model
                        </label>
                        <select
                          value={aiSettings.ollamaModel}
                          onChange={(e) => setAiSettings({ ...aiSettings, ollamaModel: e.target.value })}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            color: '#4A5568',
                            background: '#fff',
                            cursor: 'pointer'
                          }}
                        >
                          <option value="llama3.2">🦙 Llama 3.2 (3B)</option>
                          <option value="llama3.1">🦙 Llama 3.1 (8B)</option>
                          <option value="mistral">🌀 Mistral (7B)</option>
                          <option value="phi3">🔷 Phi-3 (3.8B)</option>
                          <option value="gemma2">💎 Gemma 2 (9B)</option>
                        </select>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Ngôn ngữ phản hồi
                        </label>
                        <select
                          value={aiSettings.responseLanguage}
                          onChange={(e) => setAiSettings({ ...aiSettings, responseLanguage: e.target.value })}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            color: '#4A5568',
                            background: '#fff',
                            cursor: 'pointer'
                          }}
                        >
                          <option value="vi">Tiếng Việt</option>
                          <option value="en">English</option>
                          <option value="auto">Tự động nhận diện</option>
                        </select>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Temperature: {aiSettings.temperature}
                        </label>
                        <input
                          type="range"
                          min="0"
                          max="1"
                          step="0.1"
                          value={aiSettings.temperature}
                          onChange={(e) => setAiSettings({ ...aiSettings, temperature: parseFloat(e.target.value) })}
                          style={{ width: '100%', accentColor: '#FF751F' }}
                        />
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#A0AEC0' }}>
                          <span>Chính xác</span>
                          <span>Sáng tạo</span>
                        </div>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Max Tokens
                        </label>
                        <input
                          type="number"
                          value={aiSettings.maxTokens}
                          onChange={(e) => setAiSettings({ ...aiSettings, maxTokens: parseInt(e.target.value) })}
                          style={{
                            width: '100%',
                            padding: '12px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            outline: 'none'
                          }}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Features */}
                  <div>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Settings size={18} color="#FF751F" />
                      Tính năng
                    </h3>
                    {[
                      { key: 'enableContext', label: 'Sử dụng Knowledge Base', description: 'AI sẽ tham khảo dữ liệu đã huấn luyện' },
                      { key: 'enableHistory', label: 'Nhớ lịch sử hội thoại', description: 'AI nhớ các tin nhắn trước đó trong phiên chat' },
                      { key: 'autoSuggest', label: 'Gợi ý câu hỏi', description: 'Tự động gợi ý câu hỏi cho sinh viên' },
                    ].map((feature) => (
                      <div key={feature.key} style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '16px',
                        background: '#F7FAFC',
                        borderRadius: '12px',
                        marginBottom: '12px'
                      }}>
                        <div>
                          <div style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A' }}>{feature.label}</div>
                          <div style={{ fontSize: '13px', color: '#A0AEC0' }}>{feature.description}</div>
                        </div>
                        <label style={{ position: 'relative', display: 'inline-block', width: '50px', height: '28px' }}>
                          <input
                            type="checkbox"
                            checked={aiSettings[feature.key]}
                            onChange={(e) => setAiSettings({ ...aiSettings, [feature.key]: e.target.checked })}
                            style={{ opacity: 0, width: 0, height: 0 }}
                          />
                          <span style={{
                            position: 'absolute',
                            cursor: 'pointer',
                            top: 0,
                            left: 0,
                            right: 0,
                            bottom: 0,
                            background: aiSettings[feature.key] ? '#FF751F' : '#E2E8F0',
                            borderRadius: '14px',
                            transition: 'all 0.3s'
                          }}>
                            <span style={{
                              position: 'absolute',
                              height: '22px',
                              width: '22px',
                              left: aiSettings[feature.key] ? '25px' : '3px',
                              bottom: '3px',
                              background: '#fff',
                              borderRadius: '50%',
                              transition: 'all 0.3s',
                              boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                            }} />
                          </span>
                        </label>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* Knowledge Base Tab */}
            {activeTab === 'knowledge' && (
              <div style={{
                background: '#fff',
                borderRadius: '16px',
                boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                      Knowledge Base
                    </h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                      Dữ liệu huấn luyện AI về thư viện của bạn
                    </p>
                  </div>
                  <button
                    onClick={() => { setEditingKnowledge(null); setShowKnowledgeModal(true); }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '10px 16px',
                      background: '#FF751F',
                      border: 'none',
                      borderRadius: '10px',
                      fontSize: '13px',
                      fontWeight: '600',
                      color: '#fff',
                      cursor: 'pointer'
                    }}
                  >
                    <Plus size={16} />
                    Thêm kiến thức
                  </button>
                </div>
                <div style={{ padding: '24px' }}>
                  <div style={{
                    background: '#FFF7F2',
                    borderRadius: '12px',
                    padding: '16px',
                    marginBottom: '24px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px'
                  }}>
                    <Lightbulb size={20} color="#FF751F" />
                    <p style={{ fontSize: '14px', color: '#4A5568', margin: 0 }}>
                      <strong>Tip:</strong> Thêm càng nhiều thông tin về thư viện, AI sẽ trả lời càng chính xác. Bao gồm quy định, hướng dẫn sử dụng, giờ hoạt động, v.v.
                    </p>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {knowledgeList.length === 0 ? (
                      <p style={{ color: '#A0AEC0', textAlign: 'center', padding: '20px' }}>Chưa có kiến thức nào. Bấm "Thêm kiến thức" để bắt đầu.</p>
                    ) : knowledgeList.map((item) => {
                      const typeStyle = getTypeStyle(item.type);
                      return (
                        <div key={item.id} style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '16px 20px',
                          background: '#F7FAFC',
                          borderRadius: '12px',
                          border: '2px solid #E2E8F0',
                          transition: 'all 0.2s'
                        }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.borderColor = '#FF751F';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.borderColor = '#E2E8F0';
                          }}
                        >
                          <div style={{ flex: 1 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '6px' }}>
                              <span style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A' }}>{item.title}</span>
                              <span style={{
                                padding: '3px 10px',
                                borderRadius: '20px',
                                fontSize: '11px',
                                fontWeight: '600',
                                background: typeStyle.bg,
                                color: typeStyle.color
                              }}>{typeStyle.label}</span>
                            </div>
                            <p style={{ fontSize: '13px', color: '#A0AEC0', margin: 0 }}>
                              {item.content?.length > 80 ? item.content.substring(0, 80) + '...' : item.content}
                            </p>
                            <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '8px' }}>
                              Cập nhật: {item.updatedAt}
                            </div>
                          </div>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              onClick={() => {
                                setEditingKnowledge(item);
                                setKnowledgeForm({ title: item.title, content: item.content, type: item.type });
                                setShowKnowledgeModal(true);
                              }}
                              style={{
                                padding: '8px',
                                background: '#fff',
                                border: '1px solid #E2E8F0',
                                borderRadius: '8px',
                                cursor: 'pointer'
                              }}
                            >
                              <Edit size={16} color="#4A5568" />
                            </button>
                            <button
                              onClick={() => handleDeleteKnowledge(item.id)}
                              style={{
                                padding: '8px',
                                background: '#fff',
                                border: '1px solid #E2E8F0',
                                borderRadius: '8px',
                                cursor: 'pointer'
                              }}>
                              <Trash2 size={16} color="#DC2626" />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            )}

            {/* Prompts Tab */}
            {activeTab === 'prompts' && (
              <div style={{
                background: '#fff',
                borderRadius: '16px',
                boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
                overflow: 'hidden'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                      Prompt Templates
                    </h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                      Mẫu hướng dẫn cách AI phản hồi
                    </p>
                  </div>
                  <button
                    onClick={() => setShowPromptModal(true)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '10px 16px',
                      background: '#FF751F',
                      border: 'none',
                      borderRadius: '10px',
                      fontSize: '13px',
                      fontWeight: '600',
                      color: '#fff',
                      cursor: 'pointer'
                    }}
                  >
                    <Plus size={16} />
                    Thêm Prompt
                  </button>
                </div>
                <div style={{ padding: '24px' }}>
                  {/* System Prompt */}
                  <div style={{ marginBottom: '24px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '12px' }}>System Prompt (Prompt chính)</h3>
                    <textarea
                      value={systemPrompt}
                      onChange={(e) => setSystemPrompt(e.target.value)}
                      placeholder="Bạn là SLIB AI Assistant..."
                      style={{
                        width: '100%',
                        padding: '16px',
                        border: '2px solid #E2E8F0',
                        borderRadius: '12px',
                        fontSize: '14px',
                        outline: 'none',
                        resize: 'vertical',
                        minHeight: '120px',
                        lineHeight: '1.6'
                      }}
                    />
                  </div>

                  {/* Additional Prompts */}
                  <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px' }}>Prompt bổ sung theo ngữ cảnh</h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {promptsList.length === 0 ? (
                      <p style={{ color: '#A0AEC0', textAlign: 'center', padding: '20px' }}>Chưa có prompt nào. Bấm "Thêm Prompt" để bắt đầu.</p>
                    ) : promptsList.map((template) => (
                      <div key={template.id} style={{
                        padding: '16px 20px',
                        background: '#F7FAFC',
                        borderRadius: '12px',
                        border: '2px solid #E2E8F0'
                      }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            <span style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A' }}>{template.name}</span>
                            {template.isActive && (
                              <span style={{
                                padding: '2px 8px',
                                borderRadius: '20px',
                                fontSize: '10px',
                                fontWeight: '600',
                                background: '#D1FAE5',
                                color: '#059669'
                              }}>Đang dùng</span>
                            )}
                            <span style={{
                              padding: '2px 8px',
                              borderRadius: '20px',
                              fontSize: '10px',
                              fontWeight: '500',
                              background: '#E2E8F0',
                              color: '#4A5568'
                            }}>{template.context}</span>
                          </div>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              onClick={() => {
                                setEditingPrompt(template);
                                setPromptForm({ name: template.name, prompt: template.prompt, context: template.context });
                                setShowPromptModal(true);
                              }}
                              style={{
                                padding: '6px',
                                background: '#fff',
                                border: '1px solid #E2E8F0',
                                borderRadius: '6px',
                                cursor: 'pointer'
                              }}>
                              <Edit size={14} color="#4A5568" />
                            </button>
                            <button
                              onClick={() => handleDeletePrompt(template.id)}
                              style={{
                                padding: '6px',
                                background: '#fff',
                                border: '1px solid #E2E8F0',
                                borderRadius: '6px',
                                cursor: 'pointer'
                              }}>
                              <Trash2 size={14} color="#DC2626" />
                            </button>
                          </div>
                        </div>
                        <p style={{ fontSize: '13px', color: '#4A5568', margin: 0, lineHeight: '1.5' }}>{template.prompt}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* Testing Tab */}
            {activeTab === 'testing' && (
              <div style={{
                background: '#fff',
                borderRadius: '16px',
                boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column',
                height: 'calc(100vh - 220px)'
              }}>
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                      Test & Preview
                    </h2>
                    <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                      Thử nghiệm AI thực tế - kết nối trực tiếp AI Service
                    </p>
                  </div>
                  <button
                    onClick={handleClearChat}
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
                    <RotateCcw size={16} />
                    Xóa lịch sử
                  </button>
                </div>

                {/* Error Banner */}
                {chatError && (
                  <div style={{ padding: '12px 24px', background: '#FEE2E2', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <AlertTriangle size={16} color="#DC2626" />
                    <span style={{ fontSize: '13px', color: '#DC2626' }}>{chatError}</span>
                  </div>
                )}

                {/* Chat Area */}
                <div style={{ flex: 1, padding: '24px', overflowY: 'auto' }}>
                  {chatHistory.map((msg, idx) => (
                    <div key={idx} style={{
                      display: 'flex',
                      gap: '12px',
                      marginBottom: '20px',
                      flexDirection: msg.role === 'user' ? 'row-reverse' : 'row'
                    }}>
                      <div style={{
                        width: '36px',
                        height: '36px',
                        borderRadius: '10px',
                        background: msg.role === 'user' ? '#DBEAFE' : (msg.isError ? '#FEE2E2' : 'linear-gradient(135deg, #FF751F, #FF9B5A)'),
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0
                      }}>
                        {msg.role === 'user' ? (
                          <User size={18} color="#2563EB" />
                        ) : (
                          <Bot size={18} color={msg.isError ? '#DC2626' : '#fff'} />
                        )}
                      </div>
                      <div style={{
                        maxWidth: '70%',
                        padding: '14px 18px',
                        borderRadius: msg.role === 'user' ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                        background: msg.role === 'user' ? '#DBEAFE' : (msg.isError ? '#FEE2E2' : '#F7FAFC'),
                        fontSize: '14px',
                        color: msg.isError ? '#DC2626' : '#1A1A1A',
                        lineHeight: '1.6',
                        whiteSpace: 'pre-line'
                      }}>
                        {msg.content}
                        {msg.needsLibrarian && (
                          <div style={{ marginTop: '8px', paddingTop: '8px', borderTop: '1px solid #E2E8F0', fontSize: '12px', color: '#F59E0B' }}>
                            ⚠️ AI không chắc chắn - có thể cần thủ thư hỗ trợ
                          </div>
                        )}
                        {msg.confidence !== undefined && (
                          <div style={{ marginTop: '4px', fontSize: '11px', color: '#A0AEC0' }}>
                            Độ tin cậy: {(msg.confidence * 100).toFixed(0)}%
                          </div>
                        )}
                      </div>
                    </div>
                  ))}

                  {/* Typing indicator */}
                  {isSending && (
                    <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
                      <div style={{
                        width: '36px',
                        height: '36px',
                        borderRadius: '10px',
                        background: 'linear-gradient(135deg, #FF751F, #FF9B5A)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}>
                        <Bot size={18} color="#fff" />
                      </div>
                      <div style={{
                        padding: '14px 18px',
                        borderRadius: '16px 16px 16px 4px',
                        background: '#F7FAFC',
                        fontSize: '14px',
                        color: '#A0AEC0'
                      }}>
                        <RefreshCw size={16} className="spin" style={{ animation: 'spin 1s linear infinite' }} /> Đang suy nghĩ...
                      </div>
                    </div>
                  )}
                </div>

                {/* Input Area */}
                <div style={{
                  padding: '20px 24px',
                  borderTop: '1px solid #E2E8F0',
                  background: '#F7FAFC'
                }}>
                  <div style={{ display: 'flex', gap: '12px' }}>
                    <input
                      type="text"
                      placeholder="Nhập câu hỏi để test AI..."
                      value={testMessage}
                      onChange={(e) => setTestMessage(e.target.value)}
                      onKeyPress={(e) => e.key === 'Enter' && handleSendTestMessage()}
                      disabled={isSending}
                      style={{
                        flex: 1,
                        padding: '14px 18px',
                        border: '2px solid #E2E8F0',
                        borderRadius: '12px',
                        fontSize: '14px',
                        outline: 'none',
                        background: '#fff',
                        opacity: isSending ? 0.6 : 1
                      }}
                    />
                    <button
                      onClick={handleSendTestMessage}
                      disabled={isSending || !testMessage.trim()}
                      style={{
                        padding: '14px 24px',
                        background: isSending ? '#A0AEC0' : '#FF751F',
                        border: 'none',
                        borderRadius: '12px',
                        cursor: isSending ? 'not-allowed' : 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px'
                      }}>
                      <Send size={18} color="#fff" />
                    </button>
                  </div>
                  <div style={{ display: 'flex', gap: '8px', marginTop: '12px', flexWrap: 'wrap' }}>
                    {['Làm sao để đặt chỗ?', 'Giờ mở cửa thư viện?', 'Cho em gặp thủ thư'].map((suggestion, idx) => (
                      <button
                        key={idx}
                        onClick={() => setTestMessage(suggestion)}
                        style={{
                          padding: '8px 14px',
                          background: '#fff',
                          border: '1px solid #E2E8F0',
                          borderRadius: '20px',
                          fontSize: '12px',
                          color: '#4A5568',
                          cursor: 'pointer'
                        }}
                      >
                        {suggestion}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Knowledge Modal */}
      {showKnowledgeModal && (
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
            width: '600px',
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
                {editingKnowledge ? 'Sửa kiến thức' : 'Thêm kiến thức mới'}
              </h2>
              <button onClick={() => { setShowKnowledgeModal(false); setEditingKnowledge(null); setKnowledgeForm({ title: '', content: '', type: 'info' }); }} style={{
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
                  Tiêu đề
                </label>
                <input
                  type="text"
                  placeholder="VD: Quy định đặt chỗ"
                  value={knowledgeForm.title}
                  onChange={(e) => setKnowledgeForm({ ...knowledgeForm, title: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none'
                  }}
                />
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Loại
                </label>
                <select
                  value={knowledgeForm.type}
                  onChange={(e) => setKnowledgeForm({ ...knowledgeForm, type: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    color: '#4A5568',
                    background: '#fff',
                    cursor: 'pointer'
                  }}
                >
                  <option value="info">Thông tin chung</option>
                  <option value="rules">Quy định</option>
                  <option value="guide">Hướng dẫn</option>
                </select>
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Nội dung
                </label>
                <textarea
                  placeholder="Nhập nội dung chi tiết để AI có thể học và trả lời..."
                  value={knowledgeForm.content}
                  onChange={(e) => setKnowledgeForm({ ...knowledgeForm, content: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '14px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none',
                    resize: 'vertical',
                    minHeight: '160px',
                    lineHeight: '1.6'
                  }}
                />
              </div>
              <div style={{ display: 'flex', gap: '12px' }}>
                <button onClick={() => { setShowKnowledgeModal(false); setEditingKnowledge(null); setKnowledgeForm({ title: '', content: '', type: 'info' }); }} style={{
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
                <button onClick={handleSaveKnowledge} style={{
                  flex: 1,
                  padding: '14px',
                  background: '#FF751F',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>Lưu kiến thức</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Prompt Modal */}
      {showPromptModal && (
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
            width: '600px',
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
                {editingPrompt ? 'Sửa Prompt' : 'Thêm Prompt mới'}
              </h2>
              <button onClick={() => { setShowPromptModal(false); setEditingPrompt(null); setPromptForm({ name: '', prompt: '', context: 'GENERAL' }); }} style={{
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
                  Tên Prompt
                </label>
                <input
                  type="text"
                  placeholder="VD: Trả lời câu hỏi chung"
                  value={promptForm.name}
                  onChange={(e) => setPromptForm({ ...promptForm, name: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none'
                  }}
                />
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Ngữ cảnh
                </label>
                <select
                  value={promptForm.context}
                  onChange={(e) => setPromptForm({ ...promptForm, context: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    color: '#4A5568',
                    background: '#fff',
                    cursor: 'pointer'
                  }}
                >
                  <option value="GENERAL">Câu hỏi chung</option>
                  <option value="BOOKING">Hỗ trợ đặt chỗ</option>
                  <option value="RULES">Giải đáp quy định</option>
                </select>
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Nội dung Prompt
                </label>
                <textarea
                  placeholder="Nhập hướng dẫn cho AI khi trả lời..."
                  value={promptForm.prompt}
                  onChange={(e) => setPromptForm({ ...promptForm, prompt: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '14px 16px',
                    border: '2px solid #E2E8F0',
                    borderRadius: '12px',
                    fontSize: '14px',
                    outline: 'none',
                    resize: 'vertical',
                    minHeight: '160px',
                    lineHeight: '1.6'
                  }}
                />
              </div>
              <div style={{ display: 'flex', gap: '12px' }}>
                <button onClick={() => { setShowPromptModal(false); setEditingPrompt(null); setPromptForm({ name: '', prompt: '', context: 'GENERAL' }); }} style={{
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
                <button onClick={handleSavePrompt} style={{
                  flex: 1,
                  padding: '14px',
                  background: '#FF751F',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: '#fff',
                  cursor: 'pointer'
                }}>Lưu Prompt</button>
              </div>
            </div>
          </div>
        </div>
      )}

      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        .spin {
          animation: spin 1s linear infinite;
        }
      `}</style>
    </>
  );
};

export default AIConfig;
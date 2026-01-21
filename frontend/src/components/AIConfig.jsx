import React, { useState, useEffect, useCallback } from 'react';
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
  ExternalLink,
  BookOpen,
  Lightbulb,
  Zap,
  ChevronRight,
  Send,
  User,
  RotateCcw,
  Wand2,
  Loader2
} from 'lucide-react';
import Header from './Header';
import * as aiApi from '../services/admin/ai/pythonAiApi';

const AIConfig = () => {
  const [activeTab, setActiveTab] = useState('settings');
  const [showApiKey, setShowApiKey] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isTestingApi, setIsTestingApi] = useState(false);
  const [apiStatus, setApiStatus] = useState('unknown'); // connected, error, unknown
  const [showKnowledgeModal, setShowKnowledgeModal] = useState(false);
  const [showPromptModal, setShowPromptModal] = useState(false);
  const [testMessage, setTestMessage] = useState('');
  const [editingKnowledge, setEditingKnowledge] = useState(null);
  const [editingPrompt, setEditingPrompt] = useState(null);

  // Data from API
  const [knowledgeList, setKnowledgeList] = useState([]);
  const [promptList, setPromptList] = useState([]);
  const [chatHistory, setChatHistory] = useState([]);
  const [testSessionId, setTestSessionId] = useState(null);

  // Form states for modals
  const [knowledgeForm, setKnowledgeForm] = useState({ title: '', content: '', type: 'INFO' });
  const [promptForm, setPromptForm] = useState({ name: '', prompt: '', context: 'GENERAL' });

  // AI Settings State
  const [aiSettings, setAiSettings] = useState({
    apiKey: '',
    model: 'gemini-2.0-flash',
    temperature: 0.7,
    maxTokens: 1024,
    systemPrompt: 'Bạn là SLIB AI Assistant - trợ lý thông minh của hệ thống Thư viện thông minh SLIB. Hãy trả lời ngắn gọn, thân thiện và chính xác bằng tiếng Việt.',
    enableContext: true,
    enableHistory: true,
    responseLanguage: 'vi',
  });

  const tabs = [
    { id: 'settings', label: 'Cài đặt API', icon: Key },
    { id: 'knowledge', label: 'Knowledge Base', icon: BookOpen },
    { id: 'prompts', label: 'Prompt Templates', icon: Wand2 },
    { id: 'testing', label: 'Test & Preview', icon: MessageSquare },
  ];

  // Load data on mount
  useEffect(() => {
    loadAllData();
  }, []);

  const loadAllData = async () => {
    setIsLoading(true);
    try {
      const [configRes, knowledgeRes, promptsRes] = await Promise.all([
        aiApi.getAIConfig().catch(() => ({ data: { configured: false } })),
        aiApi.getKnowledge().catch(() => ({ data: [] })),
        aiApi.getPrompts().catch(() => ({ data: [] }))
      ]);

      // Load config
      if (configRes.data?.configured && configRes.data?.config) {
        const config = configRes.data.config;
        setAiSettings({
          apiKey: config.apiKey || '',
          model: config.model || 'gemini-2.0-flash',
          temperature: config.temperature || 0.7,
          maxTokens: config.maxTokens || 1024,
          systemPrompt: config.systemPrompt || '',
          enableContext: config.enableContext ?? true,
          enableHistory: config.enableHistory ?? true,
          responseLanguage: config.responseLanguage || 'vi',
        });
        setApiStatus('connected');
      }

      // Load knowledge
      setKnowledgeList(Array.isArray(knowledgeRes.data) ? knowledgeRes.data : []);

      // Load prompts
      setPromptList(Array.isArray(promptsRes.data) ? promptsRes.data : []);

    } catch (error) {
      console.error('Failed to load AI config:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // Save AI Config
  const handleSaveConfig = async () => {
    setIsSaving(true);
    try {
      await aiApi.saveAIConfig({
        apiKey: aiSettings.apiKey,
        model: aiSettings.model,
        temperature: aiSettings.temperature,
        maxTokens: aiSettings.maxTokens,
        systemPrompt: aiSettings.systemPrompt,
        enableContext: aiSettings.enableContext,
        enableHistory: aiSettings.enableHistory,
        responseLanguage: aiSettings.responseLanguage,
      });
      alert('Đã lưu cấu hình thành công!');
    } catch (error) {
      alert('Lỗi lưu cấu hình: ' + (error.response?.data?.message || error.message));
    } finally {
      setIsSaving(false);
    }
  };

  // Test API Connection
  const handleTestApi = async () => {
    setIsTestingApi(true);
    try {
      const res = await aiApi.testAPIConnection();
      setApiStatus(res.data?.success ? 'connected' : 'error');
      alert(res.data?.message || (res.data?.success ? 'Kết nối thành công!' : 'Kết nối thất bại!'));
    } catch (error) {
      setApiStatus('error');
      alert('Lỗi test kết nối: ' + (error.response?.data?.message || error.message));
    } finally {
      setIsTestingApi(false);
    }
  };

  // Knowledge CRUD
  const handleSaveKnowledge = async () => {
    try {
      if (editingKnowledge) {
        await aiApi.updateKnowledge(editingKnowledge.id, knowledgeForm);
      } else {
        await aiApi.createKnowledge(knowledgeForm);
      }
      setShowKnowledgeModal(false);
      setEditingKnowledge(null);
      setKnowledgeForm({ title: '', content: '', type: 'INFO' });
      // Reload
      const res = await aiApi.getKnowledge();
      setKnowledgeList(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      alert('Lỗi: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleDeleteKnowledge = async (id) => {
    if (!confirm('Bạn có chắc muốn xóa kiến thức này?')) return;
    try {
      await aiApi.deleteKnowledge(id);
      const res = await aiApi.getKnowledge();
      setKnowledgeList(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      alert('Lỗi xóa: ' + (error.response?.data?.message || error.message));
    }
  };

  // Prompt CRUD
  const handleSavePrompt = async () => {
    try {
      if (editingPrompt) {
        await aiApi.updatePrompt(editingPrompt.id, promptForm);
      } else {
        await aiApi.createPrompt(promptForm);
      }
      setShowPromptModal(false);
      setEditingPrompt(null);
      setPromptForm({ name: '', prompt: '', context: 'GENERAL' });
      // Reload
      const res = await aiApi.getPrompts();
      setPromptList(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      alert('Lỗi: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleDeletePrompt = async (id) => {
    if (!confirm('Bạn có chắc muốn xóa prompt này?')) return;
    try {
      await aiApi.deletePrompt(id);
      const res = await aiApi.getPrompts();
      setPromptList(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      alert('Lỗi xóa: ' + (error.response?.data?.message || error.message));
    }
  };

  // Test Chat
  const handleSendTestMessage = async () => {
    if (!testMessage.trim()) return;

    // Add user message to chat
    const userMsg = { role: 'user', content: testMessage };
    setChatHistory(prev => [...prev, userMsg]);
    setTestMessage('');

    try {
      const res = await aiApi.sendTestMessage(testMessage, testSessionId);
      if (res.data?.success) {
        setTestSessionId(res.data.sessionId);
        const aiMsg = { role: 'assistant', content: res.data.reply };
        setChatHistory(prev => [...prev, aiMsg]);

        if (res.data.needsLibrarian) {
          setChatHistory(prev => [...prev, {
            role: 'system',
            content: '⚠️ AI không chắc chắn - đã chuyển đến thủ thư xử lý'
          }]);
        }
      }
    } catch (error) {
      setChatHistory(prev => [...prev, {
        role: 'system',
        content: '❌ Lỗi: ' + (error.response?.data?.message || error.message)
      }]);
    }
  };

  const getTypeStyle = (type) => {
    const typeUpper = (type || '').toUpperCase();
    switch (typeUpper) {
      case 'RULES': return { bg: '#FEE2E2', color: '#DC2626', label: 'Quy định' };
      case 'GUIDE': return { bg: '#DBEAFE', color: '#2563EB', label: 'Hướng dẫn' };
      case 'INFO': return { bg: '#D1FAE5', color: '#059669', label: 'Thông tin' };
      default: return { bg: '#F3F4F6', color: '#6B7280', label: 'Khác' };
    }
  };

  const getContextStyle = (context) => {
    const ctx = (context || '').toUpperCase();
    switch (ctx) {
      case 'BOOKING': return { bg: '#DBEAFE', color: '#2563EB', label: 'Đặt chỗ' };
      case 'RULES': return { bg: '#FEE2E2', color: '#DC2626', label: 'Quy định' };
      case 'GENERAL': return { bg: '#D1FAE5', color: '#059669', label: 'Chung' };
      default: return { bg: '#F3F4F6', color: '#6B7280', label: 'Khác' };
    }
  };

  if (isLoading) {
    return (
      <>
        <Header searchPlaceholder="Tìm kiếm..." />
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '50vh' }}>
          <Loader2 size={32} className="animate-spin" color="#FF751F" />
          <span style={{ marginLeft: 12, fontSize: 16, color: '#4A5568' }}>Đang tải cấu hình AI...</span>
        </div>
      </>
    );
  }

  return (
    <>
      <Header searchPlaceholder="Tìm kiếm..." />

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
            <h1 style={{ fontSize: '28px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Sparkles size={32} color="#FF751F" />
              Cấu hình AI Assistant
            </h1>
            <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
              Thiết lập và huấn luyện trợ lý AI Gemini cho SLIB
            </p>
          </div>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={loadAllData}
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
              Tải lại
            </button>
            <button
              onClick={handleSaveConfig}
              disabled={isSaving}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '12px 20px',
                background: isSaving ? '#CBD5E0' : '#FF751F',
                border: 'none',
                borderRadius: '12px',
                fontSize: '14px',
                fontWeight: '600',
                color: '#fff',
                cursor: isSaving ? 'not-allowed' : 'pointer',
                boxShadow: '0 4px 14px rgba(255, 117, 31, 0.25)'
              }}>
              {isSaving ? <Loader2 size={18} className="animate-spin" /> : <Save size={18} />}
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
              background: apiStatus === 'connected' ? '#D1FAE5' : apiStatus === 'error' ? '#FEE2E2' : '#FEF3C7',
              borderRadius: '12px',
              padding: '16px',
              marginBottom: '16px'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                <Bot size={20} color={apiStatus === 'connected' ? '#059669' : apiStatus === 'error' ? '#DC2626' : '#D97706'} />
                <span style={{ fontSize: '14px', fontWeight: '600', color: apiStatus === 'connected' ? '#059669' : apiStatus === 'error' ? '#DC2626' : '#D97706' }}>
                  {apiStatus === 'connected' ? 'AI Đang hoạt động' : apiStatus === 'error' ? 'AI Không kết nối' : 'Chưa cấu hình'}
                </span>
              </div>
              <div style={{ fontSize: '12px', color: apiStatus === 'connected' ? '#059669' : apiStatus === 'error' ? '#DC2626' : '#D97706', opacity: 0.8 }}>
                {aiSettings.model}
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
                    Cài đặt Gemini API
                  </h2>
                  <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                    Cấu hình kết nối với Google Gemini AI
                  </p>
                </div>
                <div style={{ padding: '24px' }}>
                  {/* API Key */}
                  <div style={{ marginBottom: '32px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Key size={18} color="#FF751F" />
                      API Key
                    </h3>
                    <div style={{ display: 'flex', gap: '12px', marginBottom: '12px' }}>
                      <div style={{ flex: 1, position: 'relative' }}>
                        <input
                          type={showApiKey ? 'text' : 'password'}
                          value={aiSettings.apiKey}
                          onChange={(e) => setAiSettings({ ...aiSettings, apiKey: e.target.value })}
                          placeholder="Nhập Gemini API Key của bạn"
                          style={{
                            width: '100%',
                            padding: '14px 48px 14px 16px',
                            border: '2px solid #E2E8F0',
                            borderRadius: '12px',
                            fontSize: '14px',
                            fontFamily: 'monospace',
                            outline: 'none'
                          }}
                        />
                        <button
                          onClick={() => setShowApiKey(!showApiKey)}
                          style={{
                            position: 'absolute',
                            right: '12px',
                            top: '50%',
                            transform: 'translateY(-50%)',
                            background: 'none',
                            border: 'none',
                            cursor: 'pointer',
                            padding: '4px'
                          }}
                        >
                          {showApiKey ? <EyeOff size={18} color="#A0AEC0" /> : <Eye size={18} color="#A0AEC0" />}
                        </button>
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
                        {isTestingApi ? <Loader2 size={18} className="animate-spin" /> : <Zap size={18} />}
                        {isTestingApi ? 'Đang test...' : 'Test kết nối'}
                      </button>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <ExternalLink size={14} color="#FF751F" />
                      <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noopener noreferrer"
                        style={{ fontSize: '13px', color: '#FF751F', textDecoration: 'none' }}>
                        Lấy API Key từ Google AI Studio
                      </a>
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
                          Model
                        </label>
                        <select
                          value={aiSettings.model}
                          onChange={(e) => setAiSettings({ ...aiSettings, model: e.target.value })}
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
                          <option value="gemini-2.0-flash">Gemini 2.0 Flash (Nhanh)</option>
                          <option value="gemini-2.5-flash">Gemini 2.5 Flash (Mới nhất)</option>
                          <option value="gemini-2.5-pro">Gemini 2.5 Pro (Cao cấp)</option>
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

                  {/* System Prompt */}
                  <div style={{ marginBottom: '32px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '12px' }}>System Prompt</h3>
                    <textarea
                      value={aiSettings.systemPrompt}
                      onChange={(e) => setAiSettings({ ...aiSettings, systemPrompt: e.target.value })}
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

                  {/* Features */}
                  <div>
                    <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#1A1A1A', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Settings size={18} color="#FF751F" />
                      Tính năng
                    </h3>
                    {[
                      { key: 'enableContext', label: 'Sử dụng Knowledge Base', description: 'AI sẽ tham khảo dữ liệu đã huấn luyện' },
                      { key: 'enableHistory', label: 'Nhớ lịch sử hội thoại', description: 'AI nhớ các tin nhắn trước đó trong phiên chat' },
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
                      Dữ liệu huấn luyện AI về thư viện ({knowledgeList.length} mục)
                    </p>
                  </div>
                  <button
                    onClick={() => { setEditingKnowledge(null); setKnowledgeForm({ title: '', content: '', type: 'INFO' }); setShowKnowledgeModal(true); }}
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
                      <strong>Tip:</strong> Thêm càng nhiều thông tin về thư viện, AI sẽ trả lời càng chính xác.
                    </p>
                  </div>

                  {knowledgeList.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '40px', color: '#A0AEC0' }}>
                      Chưa có kiến thức nào. Bấm "Thêm kiến thức" để bắt đầu.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      {knowledgeList.map((item) => {
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
                          }}>
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
                                {item.content?.length > 100 ? item.content.substring(0, 100) + '...' : item.content}
                              </p>
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
                  )}
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
                      Mẫu hướng dẫn cách AI phản hồi ({promptList.length} mẫu)
                    </p>
                  </div>
                  <button
                    onClick={() => { setEditingPrompt(null); setPromptForm({ name: '', prompt: '', context: 'GENERAL' }); setShowPromptModal(true); }}
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
                  {promptList.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '40px', color: '#A0AEC0' }}>
                      Chưa có prompt nào. Bấm "Thêm Prompt" để bắt đầu.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      {promptList.map((template) => {
                        const contextStyle = getContextStyle(template.context);
                        return (
                          <div key={template.id} style={{
                            padding: '16px 20px',
                            background: '#F7FAFC',
                            borderRadius: '12px',
                            border: '2px solid #E2E8F0'
                          }}>
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <span style={{ fontSize: '14px', fontWeight: '600', color: '#1A1A1A' }}>{template.name}</span>
                                <span style={{
                                  padding: '3px 10px',
                                  borderRadius: '20px',
                                  fontSize: '11px',
                                  fontWeight: '600',
                                  background: contextStyle.bg,
                                  color: contextStyle.color
                                }}>{contextStyle.label}</span>
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
                        );
                      })}
                    </div>
                  )}
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
                <div style={{ padding: '24px', borderBottom: '1px solid #E2E8F0' }}>
                  <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#1A1A1A', margin: '0 0 4px 0' }}>
                    Test & Preview
                  </h2>
                  <p style={{ fontSize: '14px', color: '#A0AEC0', margin: 0 }}>
                    Thử nghiệm AI với các câu hỏi mẫu
                  </p>
                </div>

                {/* Chat Area */}
                <div style={{ flex: 1, padding: '24px', overflowY: 'auto' }}>
                  {chatHistory.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '60px 20px', color: '#A0AEC0' }}>
                      <MessageSquare size={48} color="#E2E8F0" style={{ marginBottom: 16 }} />
                      <p>Nhập câu hỏi bên dưới để test AI</p>
                    </div>
                  ) : (
                    chatHistory.map((msg, idx) => (
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
                          background: msg.role === 'user' ? '#DBEAFE' : msg.role === 'system' ? '#FEF3C7' : 'linear-gradient(135deg, #FF751F, #FF9B5A)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0
                        }}>
                          {msg.role === 'user' ? (
                            <User size={18} color="#2563EB" />
                          ) : msg.role === 'system' ? (
                            <AlertTriangle size={18} color="#D97706" />
                          ) : (
                            <Bot size={18} color="#fff" />
                          )}
                        </div>
                        <div style={{
                          maxWidth: '70%',
                          padding: '14px 18px',
                          borderRadius: msg.role === 'user' ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                          background: msg.role === 'user' ? '#DBEAFE' : msg.role === 'system' ? '#FEF3C7' : '#F7FAFC',
                          fontSize: '14px',
                          color: '#1A1A1A',
                          lineHeight: '1.6',
                          whiteSpace: 'pre-line'
                        }}>
                          {msg.content}
                        </div>
                      </div>
                    ))
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
                      style={{
                        flex: 1,
                        padding: '14px 18px',
                        border: '2px solid #E2E8F0',
                        borderRadius: '12px',
                        fontSize: '14px',
                        outline: 'none',
                        background: '#fff'
                      }}
                    />
                    <button
                      onClick={handleSendTestMessage}
                      style={{
                        padding: '14px 24px',
                        background: '#FF751F',
                        border: 'none',
                        borderRadius: '12px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px'
                      }}>
                      <Send size={18} color="#fff" />
                    </button>
                  </div>
                  <div style={{ display: 'flex', gap: '8px', marginTop: '12px', flexWrap: 'wrap' }}>
                    {['Làm sao để đặt chỗ?', 'Giờ mở cửa thư viện?', 'Điểm uy tín là gì?'].map((suggestion, idx) => (
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
              <button onClick={() => setShowKnowledgeModal(false)} style={{
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
                  <option value="INFO">Thông tin chung</option>
                  <option value="RULES">Quy định</option>
                  <option value="GUIDE">Hướng dẫn</option>
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
                <button onClick={() => setShowKnowledgeModal(false)} style={{
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
              <button onClick={() => setShowPromptModal(false)} style={{
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
                  placeholder="VD: Hỗ trợ đặt chỗ"
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
                  <option value="GENERAL">Chung</option>
                  <option value="BOOKING">Đặt chỗ</option>
                  <option value="RULES">Quy định</option>
                </select>
              </div>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', color: '#1A1A1A', marginBottom: '8px' }}>
                  Nội dung Prompt
                </label>
                <textarea
                  placeholder="Nhập hướng dẫn cho AI..."
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
                <button onClick={() => setShowPromptModal(false)} style={{
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
        .animate-spin {
          animation: spin 1s linear infinite;
        }
      `}</style>
    </>
  );
};

export default AIConfig;

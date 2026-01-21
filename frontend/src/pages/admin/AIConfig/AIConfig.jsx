import React, { useState } from 'react';
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

// Mock Data
const KNOWLEDGE_BASE = [
  { id: 1, title: 'Quy định sử dụng thư viện', content: 'Sinh viên cần giữ im lặng, không ăn uống...', type: 'rules', updatedAt: '2025-01-15' },
  { id: 2, title: 'Hướng dẫn đặt chỗ', content: 'Để đặt chỗ, sinh viên mở app SLIB...', type: 'guide', updatedAt: '2025-01-14' },
  { id: 3, title: 'Giờ hoạt động', content: 'Thư viện mở cửa từ 7:00 đến 22:00...', type: 'info', updatedAt: '2025-01-12' },
  { id: 4, title: 'Điểm uy tín', content: 'Mỗi sinh viên có 100 điểm uy tín ban đầu...', type: 'rules', updatedAt: '2025-01-10' },
  { id: 5, title: 'Khu vực thư viện', content: 'Thư viện có 3 khu vực: Khu A yên tĩnh...', type: 'info', updatedAt: '2025-01-08' },
];

const PROMPT_TEMPLATES = [
  { id: 1, name: 'Trả lời câu hỏi chung', prompt: 'Bạn là trợ lý AI của hệ thống SLIB - Smart Library. Hãy trả lời thân thiện, ngắn gọn và chính xác.', isActive: true },
  { id: 2, name: 'Hỗ trợ đặt chỗ', prompt: 'Hướng dẫn sinh viên các bước đặt chỗ trong thư viện một cách chi tiết và dễ hiểu.', isActive: true },
  { id: 3, name: 'Giải đáp quy định', prompt: 'Giải thích các quy định của thư viện một cách rõ ràng, thân thiện.', isActive: true },
];

const CHAT_HISTORY = [
  { role: 'user', content: 'Làm sao để đặt chỗ trong thư viện?' },
  { role: 'assistant', content: 'Để đặt chỗ trong thư viện, bạn có thể làm theo các bước sau:\n\n1. Mở ứng dụng SLIB trên điện thoại\n2. Đăng nhập bằng tài khoản sinh viên\n3. Chọn "Đặt chỗ" trên màn hình chính\n4. Chọn khu vực và ghế mong muốn\n5. Chọn thời gian bắt đầu và kết thúc\n6. Xác nhận đặt chỗ\n\nNhớ check-in trong vòng 15 phút sau thời gian đặt nhé!' },
  { role: 'user', content: 'Nếu tôi không đến thì sao?' },
  { role: 'assistant', content: 'Nếu bạn không đến check-in trong vòng 15 phút (thời gian ân hạn), hệ thống sẽ:\n\n• Tự động hủy đặt chỗ của bạn\n• Trừ 15 điểm uy tín (vi phạm No-show)\n\nĐể tránh bị trừ điểm, bạn nên hủy đặt chỗ trước ít nhất 30 phút nếu có việc đột xuất nhé!' },
];

const AIConfig = () => {
  const [activeTab, setActiveTab] = useState('settings');
  const [showApiKey, setShowApiKey] = useState(false);
  const [apiKey, setApiKey] = useState('AIzaSyD•••••••••••••••••••••••••••••');
  const [isTestingApi, setIsTestingApi] = useState(false);
  const [apiStatus, setApiStatus] = useState('connected'); // connected, error, unknown
  const [showKnowledgeModal, setShowKnowledgeModal] = useState(false);
  const [showPromptModal, setShowPromptModal] = useState(false);
  const [testMessage, setTestMessage] = useState('');
  const [editingKnowledge, setEditingKnowledge] = useState(null);

  // AI Settings State
  const [aiSettings, setAiSettings] = useState({
    model: 'gemini-1.5-pro',
    temperature: 0.7,
    maxTokens: 1024,
    enableContext: true,
    enableHistory: true,
    responseLanguage: 'vi',
    autoSuggest: true,
  });

  const tabs = [
    { id: 'settings', label: 'Cài đặt API', icon: Key },
    { id: 'knowledge', label: 'Knowledge Base', icon: BookOpen },
    { id: 'prompts', label: 'Prompt Templates', icon: Wand2 },
    { id: 'testing', label: 'Test & Preview', icon: MessageSquare },
  ];

  const handleTestApi = () => {
    setIsTestingApi(true);
    setTimeout(() => {
      setIsTestingApi(false);
      setApiStatus('connected');
    }, 2000);
  };

  const getTypeStyle = (type) => {
    switch(type) {
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
            <button style={{
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
            <button style={{
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
              boxShadow: '0 4px 14px rgba(255, 117, 31, 0.25)'
            }}>
              <Save size={18} />
              Lưu cấu hình
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
                Gemini 1.5 Pro
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
                          value={apiKey}
                          onChange={(e) => setApiKey(e.target.value)}
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
                        {isTestingApi ? <RefreshCw size={18} className="spin" /> : <Zap size={18} />}
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
                          onChange={(e) => setAiSettings({...aiSettings, model: e.target.value})}
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
                          <option value="gemini-1.5-pro">Gemini 1.5 Pro</option>
                          <option value="gemini-1.5-flash">Gemini 1.5 Flash</option>
                          <option value="gemini-1.0-pro">Gemini 1.0 Pro</option>
                        </select>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#4A5568', marginBottom: '8px' }}>
                          Ngôn ngữ phản hồi
                        </label>
                        <select 
                          value={aiSettings.responseLanguage}
                          onChange={(e) => setAiSettings({...aiSettings, responseLanguage: e.target.value})}
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
                          onChange={(e) => setAiSettings({...aiSettings, temperature: parseFloat(e.target.value)})}
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
                          onChange={(e) => setAiSettings({...aiSettings, maxTokens: parseInt(e.target.value)})}
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
                            onChange={(e) => setAiSettings({...aiSettings, [feature.key]: e.target.checked})}
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
                    {KNOWLEDGE_BASE.map((item) => {
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
                              {item.content.length > 80 ? item.content.substring(0, 80) + '...' : item.content}
                            </p>
                            <div style={{ fontSize: '12px', color: '#A0AEC0', marginTop: '8px' }}>
                              Cập nhật: {item.updatedAt}
                            </div>
                          </div>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button 
                              onClick={() => { setEditingKnowledge(item); setShowKnowledgeModal(true); }}
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
                            <button style={{
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
                      defaultValue="Bạn là SLIB AI Assistant - trợ lý thông minh của hệ thống Thư viện thông minh SLIB. Nhiệm vụ của bạn là hỗ trợ sinh viên về mọi vấn đề liên quan đến thư viện: hướng dẫn đặt chỗ, giải đáp quy định, thông tin khu vực, điểm uy tín, v.v. Hãy trả lời ngắn gọn, thân thiện và chính xác bằng tiếng Việt."
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
                    {PROMPT_TEMPLATES.map((template) => (
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
                          </div>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button style={{
                              padding: '6px',
                              background: '#fff',
                              border: '1px solid #E2E8F0',
                              borderRadius: '6px',
                              cursor: 'pointer'
                            }}>
                              <Edit size={14} color="#4A5568" />
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
                  {CHAT_HISTORY.map((msg, idx) => (
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
                        background: msg.role === 'user' ? '#DBEAFE' : 'linear-gradient(135deg, #FF751F, #FF9B5A)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0
                      }}>
                        {msg.role === 'user' ? (
                          <User size={18} color="#2563EB" />
                        ) : (
                          <Bot size={18} color="#fff" />
                        )}
                      </div>
                      <div style={{
                        maxWidth: '70%',
                        padding: '14px 18px',
                        borderRadius: msg.role === 'user' ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                        background: msg.role === 'user' ? '#DBEAFE' : '#F7FAFC',
                        fontSize: '14px',
                        color: '#1A1A1A',
                        lineHeight: '1.6',
                        whiteSpace: 'pre-line'
                      }}>
                        {msg.content}
                      </div>
                    </div>
                  ))}
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
                    <button style={{
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
                  defaultValue={editingKnowledge?.title || ''}
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
                  defaultValue={editingKnowledge?.type || 'info'}
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
                  defaultValue={editingKnowledge?.content || ''}
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
                }}>Lưu kiến thức</button>
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
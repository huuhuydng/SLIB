import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useToast } from '../../../components/common/ToastProvider';
import { useSearchParams } from 'react-router-dom';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import '../../../styles/librarian/librarian-shared.css';
import '../../../styles/librarian/ChatManage.css';

import { handleLogout } from "../../../utils/auth";
import { useLibrarianNotification } from "../../../context/LibrarianNotificationContext";
import {
  Image as ImageIcon,
  Send,
  MessageCircle,
  Clock,
  CheckCircle,
  XCircle,
  Bot,
  Search,
  AlertTriangle,
} from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL || '';

const ChatManage = () => {
  const toast = useToast();
  const [searchParams] = useSearchParams();
  const urlConversationId = searchParams.get('conversationId');
  const [conversations, setConversations] = useState([]);
  const [selectedConversationId, setSelectedConversationId] = useState(urlConversationId || null);
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [wsConnected, setWsConnected] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [showEndConfirm, setShowEndConfirm] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [imagePreview, setImagePreview] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);

  const messagesEndRef = useRef(null);
  const stompClientRef = useRef(null);
  const subscriptionRef = useRef(null);
  const fileInputRef = useRef(null);
  const selectedConversationIdRef = useRef(selectedConversationId);

  // Notification context for badge updates & chat toast
  const { refreshUnreadChatCount, chatToast, setChatToast } = useLibrarianNotification();
  // Keep ref in sync with state
  useEffect(() => {
    selectedConversationIdRef.current = selectedConversationId;
  }, [selectedConversationId]);

  // Fetch all conversations (waiting + active)
  const fetchConversations = useCallback(async () => {
    try {
      const token = localStorage.getItem('librarian_token');
      const response = await fetch(`${API_BASE}/slib/chat/conversations/all`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setConversations(data);
        // Only auto-select if no conversation is currently selected
        if (data.length > 0 && !selectedConversationIdRef.current && !urlConversationId) {
          setSelectedConversationId(data[0].id);
        }
      } else {
        console.error('Failed to fetch conversations:', response.status);
      }
    } catch (err) {
      console.error('Error fetching conversations:', err);
      setError('Không thể tải danh sách hội thoại');
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch messages for selected conversation
  const fetchMessages = useCallback(async (conversationId) => {
    if (!conversationId) return;

    try {
      const token = localStorage.getItem('librarian_token');
      const response = await fetch(`${API_BASE}/slib/chat/conversations/${conversationId}/messages`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        const serverMessages = data.map(msg => ({
          ...msg,
          isMine: msg.senderType === 'LIBRARIAN'
        }));
        setMessages(prev => {
          const pendingOptimistic = prev.filter(m =>
            m._optimistic && !serverMessages.some(sm =>
              sm.content === m.content && sm.senderType === m.senderType
            )
          );
          return [...serverMessages, ...pendingOptimistic];
        });
      }
    } catch (err) {
      console.error('Error fetching messages:', err);
    }
  }, []);

  // Take over conversation
  const handleTakeOver = async (conversationId) => {
    try {
      const token = localStorage.getItem('librarian_token');
      const response = await fetch(`${API_BASE}/slib/chat/conversations/${conversationId}/take-over`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        fetchConversations();
        setSelectedConversationId(conversationId);
      }
    } catch (err) {
      console.error('Error taking over conversation:', err);
    }
  };

  // Send message (text or with image)
  const handleSendMessage = async (e) => {
    e.preventDefault();
    const hasText = inputValue.trim();
    const hasImage = selectedFile;
    if ((!hasText && !hasImage) || !selectedConversationId) return;

    const messageContent = inputValue;
    setInputValue("");
    const currentFile = selectedFile;
    setSelectedFile(null);
    setImagePreview(null);

    const tempId = `optimistic_${Date.now()}`;
    const optimisticMsg = {
      id: tempId,
      content: currentFile ? (messageContent || 'Đang gửi ảnh...') : messageContent,
      senderType: 'LIBRARIAN',
      createdAt: new Date().toISOString(),
      isMine: true,
      _optimistic: true
    };
    setMessages(prev => [...prev, optimisticMsg]);

    try {
      const token = localStorage.getItem('librarian_token');

      if (currentFile) {
        // Upload ảnh kèm message
        setUploadingImage(true);
        const formData = new FormData();
        formData.append('file', currentFile);
        formData.append('content', messageContent);
        formData.append('senderType', 'LIBRARIAN');
        await fetch(`${API_BASE}/slib/chat/conversations/${selectedConversationId}/messages/with-image`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` },
          body: formData
        });
        setUploadingImage(false);
      } else {
        // Text message
        await fetch(`${API_BASE}/slib/chat/conversations/${selectedConversationId}/messages`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ content: messageContent, senderType: 'LIBRARIAN' })
        });
      }
    } catch (err) {
      console.error('Error sending message:', err);
      setUploadingImage(false);
    }
  };

  // Handle image selection
  const handleImageSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      toast.warning('File quá lớn. Tối đa: 5MB');
      return;
    }
    setSelectedFile(file);
    setImagePreview(URL.createObjectURL(file));
  };

  // Parse [IMAGES] from message content
  const parseMessageContent = (content) => {
    if (!content || !content.includes('[IMAGES]')) return { text: content || '', imageUrls: [] };
    const parts = content.split('[IMAGES]');
    const text = parts[0].trim();
    const imageUrls = parts[1]?.trim().split('\n').filter(url => url.trim().startsWith('http')) || [];
    return { text, imageUrls };
  };

  // End chat with confirmation
  const handleEndChat = async (conversationId) => {
    try {
      const token = localStorage.getItem('librarian_token');
      const response = await fetch(`${API_BASE}/slib/chat/conversations/${conversationId}/resolve`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        fetchConversations();
        setSelectedConversationId(null);
        setMessages([]);
        setShowEndConfirm(false);
      }
    } catch (err) {
      console.error('Error ending chat:', err);
    }
  };

  // Auto scroll to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    fetchConversations();
    const interval = setInterval(fetchConversations, 10000);
    return () => clearInterval(interval);
  }, [fetchConversations]);

  useEffect(() => {
    if (selectedConversationId) {
      fetchMessages(selectedConversationId);
      const interval = setInterval(() => {
        fetchMessages(selectedConversationId);
      }, 10000);
      return () => clearInterval(interval);
    }
  }, [selectedConversationId, fetchMessages]);

  // WebSocket connection
  useEffect(() => {
    const backendUrl = API_BASE || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const wsEndpoint = backendUrl + '/ws';

    const client = new Client({
      webSocketFactory: () => new SockJS(wsEndpoint),
      reconnectDelay: 5000,
      debug: () => { },
      onConnect: () => {
        stompClientRef.current = client;
        setWsConnected(true);

        client.subscribe('/topic/escalate', (message) => {
          const data = JSON.parse(message.body);
          if (data.type === 'QUEUE_CANCELLED') {
            setConversations(prev => prev.filter(c => c.id !== data.conversationId));
            setSelectedConversationId(prev => prev === data.conversationId ? null : prev);
          } else if (data.type === 'CONVERSATION_ACCEPTED' || data.type === 'CONVERSATION_RESOLVED') {
            fetchConversations();
          } else if (data.id) {
            setConversations(prev => {
              if (prev.some(c => c.id === data.id)) return prev;
              return [data, ...prev];
            });
          }
        });

        // Subscribe for new message -> refresh messages in current conversation
        client.subscribe('/topic/librarian-notifications', (message) => {
          const data = JSON.parse(message.body);
          if (data.type === 'CHAT_NEW_MESSAGE') {
            // Refresh messages if viewing this conversation (no toast here — context handles it)
            if (data.conversationId === selectedConversationIdRef.current) {
              fetchMessages(data.conversationId);
            }
            // Refresh conversation list (but don't auto-switch)
            fetchConversations();
          }
        });
      },
      onStompError: (frame) => {
        console.error('[WS] STOMP error:', frame);
      },
      onWebSocketClose: () => {
        stompClientRef.current = null;
        setWsConnected(false);
      }
    });

    client.activate();
    return () => { if (client) client.deactivate(); };
  }, []);

  // Subscribe to conversation topic
  const subscribeToConversation = useCallback((conversationId) => {
    if (!stompClientRef.current?.connected || !conversationId) return;

    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    subscriptionRef.current = stompClientRef.current.subscribe(
      `/topic/conversation/${conversationId}`,
      (message) => {
        const newMessage = JSON.parse(message.body);

        if (newMessage.type === 'TYPING' || newMessage.type === 'MESSAGES_READ') return;

        newMessage.isMine = newMessage.senderType === 'LIBRARIAN';
        if (!newMessage.createdAt) {
          newMessage.createdAt = new Date().toISOString();
        }

        setMessages(prev => {
          if (prev.some(m => m.id === newMessage.id && !m._optimistic)) return prev;
          const hasOptimistic = prev.some(m =>
            m._optimistic && m.content === newMessage.content && m.senderType === newMessage.senderType
          );
          if (hasOptimistic) {
            let replaced = false;
            return prev.map(m => {
              if (!replaced && m._optimistic && m.content === newMessage.content && m.senderType === newMessage.senderType) {
                replaced = true;
                return { ...newMessage, createdAt: newMessage.createdAt || m.createdAt, _optimistic: false };
              }
              return m;
            });
          }
          return [...prev, newMessage];
        });
      }
    );
  }, []);

  useEffect(() => {
    if (selectedConversationId && wsConnected) {
      subscribeToConversation(selectedConversationId);
    }
    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
    };
  }, [selectedConversationId, wsConnected, subscribeToConversation]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const currentConversation = conversations.find(c => c.id === selectedConversationId);

  const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    if (isNaN(date.getTime())) return '';
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  const getWaitDuration = (escalatedAt) => {
    if (!escalatedAt) return null;
    const now = new Date();
    const escalated = new Date(escalatedAt);
    const diffMs = now - escalated;
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return 'Vừa xong';
    if (diffMin < 60) return `${diffMin} phút`;
    const diffHour = Math.floor(diffMin / 60);
    return `${diffHour}h ${diffMin % 60}p`;
  };

  const getInitial = (name) => {
    if (!name) return '?';
    return name.charAt(0).toUpperCase();
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'QUEUE_WAITING':
        return <span className="cm-status-badge waiting">Chờ xử lý</span>;
      case 'HUMAN_CHATTING':
        return <span className="cm-status-badge active">Đang chat</span>;
      case 'RESOLVED':
      case 'AI_HANDLING':
        return <span className="cm-status-badge resolved">Đã kết thúc</span>;
      default:
        return null;
    }
  };

  // Separate and filter conversations
  const waitingConvs = conversations.filter(c => c.status === 'QUEUE_WAITING');
  const activeConvs = conversations.filter(c => c.status === 'HUMAN_CHATTING');
  const otherConvs = conversations.filter(c => c.status !== 'QUEUE_WAITING' && c.status !== 'HUMAN_CHATTING');

  const filteredWaiting = useMemo(() => {
    if (!searchTerm) return waitingConvs;
    const q = searchTerm.toLowerCase();
    return waitingConvs.filter(c =>
      (c.studentName || '').toLowerCase().includes(q) ||
      (c.studentCode || '').toLowerCase().includes(q)
    );
  }, [waitingConvs, searchTerm]);

  const filteredActive = useMemo(() => {
    if (!searchTerm) return activeConvs;
    const q = searchTerm.toLowerCase();
    return activeConvs.filter(c =>
      (c.studentName || '').toLowerCase().includes(q) ||
      (c.studentCode || '').toLowerCase().includes(q)
    );
  }, [activeConvs, searchTerm]);

  const filteredOther = useMemo(() => {
    if (!searchTerm) return otherConvs;
    const q = searchTerm.toLowerCase();
    return otherConvs.filter(c =>
      (c.studentName || '').toLowerCase().includes(q) ||
      (c.studentCode || '').toLowerCase().includes(q)
    );
  }, [otherConvs, searchTerm]);

  const renderConvItem = (conv) => (
    <div
      key={conv.id}
      onClick={() => setSelectedConversationId(conv.id)}
      className={`cm-conv-item ${selectedConversationId === conv.id ? 'active' : ''}`}
    >
      <div className="cm-conv-avatar">
        {getInitial(conv.studentName)}
      </div>
      <div className="cm-conv-info">
        <div className="cm-conv-name-row">
          <span className="cm-conv-name">{conv.studentName || 'Sinh viên'}</span>
          <span className="cm-conv-time">
            {formatTime(conv.lastMessage?.createdAt || conv.updatedAt || conv.createdAt)}
          </span>
        </div>
        {conv.studentCode && (
          <span className="cm-conv-code">{conv.studentCode}</span>
        )}
        {conv.escalationReason && (
          <span className="cm-conv-reason">{conv.escalationReason}</span>
        )}
        <div className="cm-conv-bottom">
          {conv.status === 'QUEUE_WAITING' && conv.escalatedAt && (
            <span className="cm-conv-wait-time">
              <Clock size={11} />
              {getWaitDuration(conv.escalatedAt)}
            </span>
          )}
          {getStatusBadge(conv.status)}
        </div>
      </div>
    </div>
  );

  return (
    <>

      <div className="lib-container">
        <div className="cm-header-row">
          <h1 className="cm-page-title">Hỗ trợ sinh viên</h1>
          {waitingConvs.length > 0 && (
            <span className="cm-badge-count">
              {waitingConvs.length} đang chờ
            </span>
          )}
          <div className="cm-ws-indicator">
            <span className={`cm-ws-dot ${wsConnected ? 'connected' : ''}`} />
            {wsConnected ? 'Đang kết nối' : 'Mất kết nối'}
          </div>
        </div>

        {loading ? (
          <div className="cm-loading">
            <div className="cm-loading-spinner" />
            <p>Đang tải dữ liệu...</p>
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--lib-red)' }}>
            <XCircle size={48} style={{ margin: '0 auto 12px', display: 'block' }} />
            <p>{error}</p>
          </div>
        ) : conversations.length === 0 ? (
          <div className="lib-panel cm-empty-state-big">
            <MessageCircle size={52} color="#d1d5db" />
            <p>Không có cuộc hội thoại nào</p>
          </div>
        ) : (
          <div className="cm-chat-container">
            {/* Conversation List */}
            <div className="cm-conversation-list">
              <div className="cm-sidebar-header">
                <div className="cm-sidebar-search">
                  <Search size={15} className="cm-sidebar-search-icon" />
                  <input
                    type="text"
                    placeholder="Tìm theo tên hoặc mã SV..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </div>
              </div>

              <div className="cm-sidebar-body">
                {filteredWaiting.length > 0 && (
                  <>
                    <div className="cm-section-header">
                      <span className="cm-section-dot waiting" />
                      Chờ xử lý ({filteredWaiting.length})
                    </div>
                    {filteredWaiting.map(renderConvItem)}
                  </>
                )}

                {filteredActive.length > 0 && (
                  <>
                    <div className="cm-section-header">
                      <span className="cm-section-dot active" />
                      Đang hỗ trợ ({filteredActive.length})
                    </div>
                    {filteredActive.map(renderConvItem)}
                  </>
                )}

                {filteredOther.length > 0 && (
                  <>
                    <div className="cm-section-header">
                      <span className="cm-section-dot other" />
                      Khác ({filteredOther.length})
                    </div>
                    {filteredOther.map(renderConvItem)}
                  </>
                )}

                {filteredWaiting.length === 0 && filteredActive.length === 0 && filteredOther.length === 0 && (
                  <div className="cm-conv-empty">
                    <div className="cm-conv-empty-icon">
                      <MessageCircle size={36} />
                    </div>
                    <p>{searchTerm ? 'Không tìm thấy kết quả' : 'Chưa có hội thoại mới'}</p>
                  </div>
                )}
              </div>
            </div>

            {/* Chat Window */}
            <div className="cm-chat-window">
              {currentConversation ? (
                <>
                  {/* Header */}
                  <div className="cm-chat-header">
                    <div className="cm-chat-header-left">
                      <div className="cm-chat-header-avatar">
                        {getInitial(currentConversation.studentName)}
                        {currentConversation.status === 'HUMAN_CHATTING' && (
                          <span className="cm-online-dot" />
                        )}
                      </div>
                      <div className="cm-chat-header-info">
                        <span className="cm-chat-header-name">
                          {currentConversation.studentName || 'Sinh viên'}
                          {currentConversation.studentCode && (
                            <span className="cm-chat-header-code">
                              ({currentConversation.studentCode})
                            </span>
                          )}
                        </span>
                        <span className="cm-chat-header-sub">
                          {currentConversation.status === 'HUMAN_CHATTING'
                            ? 'Đang trò chuyện'
                            : currentConversation.status === 'QUEUE_WAITING'
                              ? 'Đang chờ hỗ trợ'
                              : 'Hội thoại đã kết thúc'}
                        </span>
                      </div>
                    </div>

                    <div className="cm-chat-header-actions">
                      {currentConversation.status === 'QUEUE_WAITING' && (
                        <button
                          className="cm-btn-accept"
                          onClick={() => handleTakeOver(currentConversation.id)}
                        >
                          <CheckCircle size={16} />
                          Nhận hỗ trợ
                        </button>
                      )}
                      {currentConversation.status === 'HUMAN_CHATTING' && (
                        <button
                          className="cm-btn-end"
                          onClick={() => setShowEndConfirm(true)}
                        >
                          <XCircle size={16} />
                          Kết thúc
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Escalation reason banner */}
                  {currentConversation.escalationReason && currentConversation.status !== 'RESOLVED' && (
                    <div className="cm-escalation-banner">
                      <AlertTriangle size={14} />
                      <span>Lý do: <strong>{currentConversation.escalationReason}</strong></span>
                    </div>
                  )}

                  {/* Messages */}
                  <div className="cm-messages-area">
                    {messages.map((msg, index) => {
                      const isAI = msg.senderType === 'AI';
                      const isLibrarian = msg.senderType === 'LIBRARIAN';
                      const isStudent = !isAI && !isLibrarian;

                      // System messages
                      if (msg.senderType === 'SYSTEM') {
                        const isSupportRequest = msg.content?.startsWith('[YÊU CẦU HỖ TRỢ]');

                        let textContent = msg.content;
                        let imageUrls = [];
                        if (isSupportRequest && msg.content?.includes('[IMAGES]')) {
                          const parts = msg.content.split('[IMAGES]');
                          textContent = parts[0].trim();
                          imageUrls = parts[1].trim().split('\n').filter(url => url.trim().startsWith('http'));
                        }

                        return (
                          <div key={index} className={`cm-system-message ${isSupportRequest ? 'cm-support-context' : ''}`}>
                            {isSupportRequest ? (
                              <div className="cm-support-card">
                                <div className="cm-support-card-header">
                                  <span className="cm-support-card-icon">&#9888;</span>
                                  <span>Yêu cầu hỗ trợ từ sinh viên</span>
                                </div>
                                <div className="cm-support-card-body">
                                  {textContent.replace('[YÊU CẦU HỖ TRỢ]\n', '')}
                                  {imageUrls.length > 0 && (
                                    <div className="cm-support-card-images">
                                      {imageUrls.map((url, i) => (
                                        <a key={i} href={url} target="_blank" rel="noopener noreferrer">
                                          <img src={url} alt={`Ảnh ${i + 1}`} className="cm-support-card-img" />
                                        </a>
                                      ))}
                                    </div>
                                  )}
                                </div>
                              </div>
                            ) : msg.content}
                          </div>
                        );
                      }

                      const parsed = parseMessageContent(msg.content);
                      return (
                        <div
                          key={msg.id || index}
                          className={`cm-message-row ${isLibrarian ? 'mine' : 'theirs'}`}
                        >
                          {!isLibrarian && (
                            <div className={`cm-msg-avatar ${isAI ? 'ai' : 'student'}`}>
                              {isAI ? <Bot size={14} /> : getInitial(currentConversation.studentName)}
                            </div>
                          )}

                          <div className={`cm-bubble ${isAI ? 'ai-bubble' : isStudent ? 'student-bubble' : ''}`}>
                            {isAI && (
                              <div className="cm-ai-label">
                                <Bot size={11} />
                                SLIB AI
                              </div>
                            )}
                            {parsed.text && <span>{parsed.text}</span>}
                            {parsed.imageUrls.length > 0 && (
                              <div className="cm-msg-images">
                                {parsed.imageUrls.map((url, i) => (
                                  <a key={i} href={url} target="_blank" rel="noopener noreferrer">
                                    <img src={url} alt={`Ảnh ${i + 1}`} className="cm-msg-img" />
                                  </a>
                                ))}
                              </div>
                            )}
                            <div className="cm-msg-time">
                              {formatTime(msg.createdAt)}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                    <div ref={messagesEndRef} />
                  </div>

                  {/* Composer */}
                  {currentConversation.status === 'HUMAN_CHATTING' && (
                    <div className="cm-composer">
                      {imagePreview && (
                        <div className="cm-image-preview">
                          <img src={imagePreview} alt="Preview" />
                          <button onClick={() => { setSelectedFile(null); setImagePreview(null); }} className="cm-preview-remove">&times;</button>
                        </div>
                      )}
                      <div className="cm-composer-row">
                        <input
                          type="file"
                          ref={fileInputRef}
                          accept="image/*"
                          style={{ display: 'none' }}
                          onChange={handleImageSelect}
                        />
                        <button className="cm-btn-icon" onClick={() => fileInputRef.current?.click()} disabled={uploadingImage}>
                          <ImageIcon size={18} />
                        </button>
                        <form onSubmit={handleSendMessage} className="cm-composer-form">
                          <input
                            type="text"
                            placeholder="Nhập tin nhắn..."
                            value={inputValue}
                            onChange={(e) => setInputValue(e.target.value)}
                          />
                          <button
                            type="submit"
                            className="cm-btn-send"
                            disabled={(!inputValue.trim() && !selectedFile) || uploadingImage}
                          >
                            {uploadingImage ? '...' : <Send size={18} />}
                          </button>
                        </form>
                      </div>
                    </div>
                  )}
                </>
              ) : (
                <div className="cm-empty-state">
                  <MessageCircle size={48} className="cm-empty-state-icon" />
                  <p>Chọn một cuộc hội thoại để bắt đầu</p>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Confirm End Chat Dialog */}
      {showEndConfirm && currentConversation && (
        <div className="cm-confirm-overlay" onClick={() => setShowEndConfirm(false)}>
          <div className="cm-confirm-dialog" onClick={(e) => e.stopPropagation()}>
            <h3>Kết thúc hội thoại</h3>
            <p>
              Bạn có chắc chắn muốn kết thúc cuộc trò chuyện với{' '}
              <strong>{currentConversation.studentName}</strong>?
            </p>
            <div className="cm-confirm-actions">
              <button
                className="cm-confirm-cancel"
                onClick={() => setShowEndConfirm(false)}
              >
                Hủy
              </button>
              <button
                className="cm-confirm-yes"
                onClick={() => handleEndChat(currentConversation.id)}
              >
                Kết thúc
              </button>
            </div>
          </div>
        </div>
      )}
      {/* Chat Toast Notification - suppressed on chat page, messages already visible */}
    </>
  );
};

export default ChatManage;
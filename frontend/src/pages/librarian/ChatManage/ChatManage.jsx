import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import '../../../styles/librarian/ChatManage.css';
import Header from "../../../components/shared/Header";
import { handleLogout } from "../../../utils/auth";
import {
  Image as ImageIcon,
  Send,
  MessageCircle,
  User,
  Clock,
  CheckCircle,
  XCircle,
  Bot
} from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL || '';

const ChatManage = () => {
  const [searchParams] = useSearchParams();
  const urlConversationId = searchParams.get('conversationId');
  const [conversations, setConversations] = useState([]);
  const [selectedConversationId, setSelectedConversationId] = useState(urlConversationId || null);
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [wsConnected, setWsConnected] = useState(false);

  const messagesEndRef = useRef(null);
  const stompClientRef = useRef(null);
  const subscriptionRef = useRef(null);

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
        if (data.length > 0 && !selectedConversationId && !urlConversationId) {
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
  }, [selectedConversationId]);

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
          // Giữ lại optimistic messages chưa được server confirm
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

  // Send message
  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputValue.trim() || !selectedConversationId) return;

    const messageContent = inputValue;
    setInputValue("");

    // Optimistic UI update with temporary ID
    const tempId = `optimistic_${Date.now()}`;
    const optimisticMsg = {
      id: tempId,
      content: messageContent,
      senderType: 'LIBRARIAN',
      createdAt: new Date().toISOString(),
      isMine: true,
      _optimistic: true
    };
    setMessages(prev => [...prev, optimisticMsg]);

    try {
      const token = localStorage.getItem('librarian_token');
      await fetch(`${API_BASE}/slib/chat/conversations/${selectedConversationId}/messages`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ content: messageContent, senderType: 'LIBRARIAN' })
      });
    } catch (err) {
      console.error('Error sending message:', err);
    }
  };

  // End chat - resolve conversation
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
      // Fallback polling chậm (10s) phòng khi WebSocket miss
      const interval = setInterval(() => {
        fetchMessages(selectedConversationId);
      }, 10000);
      return () => clearInterval(interval);
    }
  }, [selectedConversationId, fetchMessages]);

  // WebSocket connection
  useEffect(() => {
    const backendUrl = API_BASE || 'http://localhost:8080';
    const wsEndpoint = backendUrl + '/ws';

    const client = new Client({
      webSocketFactory: () => new SockJS(wsEndpoint),
      reconnectDelay: 5000,
      debug: () => { },
      onConnect: () => {
        stompClientRef.current = client;
        setWsConnected(true);
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
        newMessage.isMine = newMessage.senderType === 'LIBRARIAN';
        // Đảm bảo luôn có createdAt cho mọi message
        if (!newMessage.createdAt) {
          newMessage.createdAt = new Date().toISOString();
        }
        setMessages(prev => {
          // Skip if already exists (by server id)
          if (prev.some(m => m.id === newMessage.id && !m._optimistic)) return prev;
          // Replace optimistic message with server message
          const hasOptimistic = prev.some(m =>
            m._optimistic && m.content === newMessage.content && m.senderType === newMessage.senderType
          );
          if (hasOptimistic) {
            let replaced = false;
            return prev.map(m => {
              if (!replaced && m._optimistic && m.content === newMessage.content && m.senderType === newMessage.senderType) {
                replaced = true;
                // Giữ createdAt từ optimistic nếu server không gửi
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
    // Backend dùng LocalDateTime (giờ local server, UTC+7)
    // Không thêm 'Z' vì sẽ bị lệch timezone
    const date = new Date(timeStr);
    if (isNaN(date.getTime())) return '';
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
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

  // Separate conversations by status
  const waitingConvs = conversations.filter(c => c.status === 'QUEUE_WAITING');
  const activeConvs = conversations.filter(c => c.status === 'HUMAN_CHATTING');
  const otherConvs = conversations.filter(c => c.status !== 'QUEUE_WAITING' && c.status !== 'HUMAN_CHATTING');

  return (
    <>
      <Header searchPlaceholder="Tìm kiếm..." onLogout={handleLogout} />

      <div style={{
        padding: '1.5rem 2rem',
        maxWidth: '1500px',
        margin: '0 auto',
        backgroundColor: '#f9fafb',
        minHeight: 'calc(100vh - 80px)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '1.2rem' }}>
          <h2 className="cm-page-title">Hỗ trợ sinh viên</h2>
          {waitingConvs.length > 0 && (
            <span className="cm-badge-count">
              {waitingConvs.length} đang chờ
            </span>
          )}
        </div>

        {loading ? (
          <div className="cm-loading">
            <div className="cm-loading-spinner"></div>
            <p>Đang tải dữ liệu...</p>
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#ef4444' }}>
            <XCircle size={48} style={{ margin: '0 auto 12px', display: 'block' }} />
            <p>{error}</p>
          </div>
        ) : conversations.length === 0 ? (
          <div className="cm-empty-state-big">
            <MessageCircle size={56} color="#d1d5db" />
            <p>Không có cuộc hội thoại nào</p>
          </div>
        ) : (
          <div className="cm-chat-container">
            {/* Conversation List */}
            <div className="cm-conversation-list">
              {waitingConvs.length > 0 && (
                <div className="cm-section-header">Chờ xử lý ({waitingConvs.length})</div>
              )}
              {waitingConvs.map((conv) => (
                <div
                  key={conv.id}
                  onClick={() => {
                    handleTakeOver(conv.id);
                  }}
                  className={`cm-conv-item ${selectedConversationId === conv.id ? 'active' : ''}`}
                >
                  <div className="cm-conv-avatar">
                    {getInitial(conv.studentName)}
                  </div>
                  <div className="cm-conv-info">
                    <span className="cm-conv-name">{conv.studentName || 'Sinh viên'}</span>
                    <span className="cm-conv-meta">
                      <Clock size={11} />
                      {formatTime(conv.lastMessage?.createdAt || conv.updatedAt || conv.createdAt)}
                    </span>
                  </div>
                  {getStatusBadge(conv.status)}
                </div>
              ))}

              {activeConvs.length > 0 && (
                <div className="cm-section-header">Đang hỗ trợ ({activeConvs.length})</div>
              )}
              {activeConvs.map((conv) => (
                <div
                  key={conv.id}
                  onClick={() => setSelectedConversationId(conv.id)}
                  className={`cm-conv-item ${selectedConversationId === conv.id ? 'active' : ''}`}
                >
                  <div className="cm-conv-avatar">
                    {getInitial(conv.studentName)}
                  </div>
                  <div className="cm-conv-info">
                    <span className="cm-conv-name">{conv.studentName || 'Sinh viên'}</span>
                    <span className="cm-conv-meta">
                      <Clock size={11} />
                      {formatTime(conv.lastMessage?.createdAt || conv.updatedAt || conv.createdAt)}
                    </span>
                  </div>
                  {getStatusBadge(conv.status)}
                </div>
              ))}

              {otherConvs.length > 0 && (
                <div className="cm-section-header">Khác</div>
              )}
              {otherConvs.map((conv) => (
                <div
                  key={conv.id}
                  onClick={() => setSelectedConversationId(conv.id)}
                  className={`cm-conv-item ${selectedConversationId === conv.id ? 'active' : ''}`}
                >
                  <div className="cm-conv-avatar">
                    {getInitial(conv.studentName)}
                  </div>
                  <div className="cm-conv-info">
                    <span className="cm-conv-name">{conv.studentName || 'Sinh viên'}</span>
                    <span className="cm-conv-meta">
                      <Clock size={11} />
                      {formatTime(conv.lastMessage?.createdAt || conv.updatedAt || conv.createdAt)}
                    </span>
                  </div>
                  {getStatusBadge(conv.status)}
                </div>
              ))}

              {conversations.length === 0 && (
                <div className="cm-conv-empty">
                  <div className="cm-conv-empty-icon">
                    <MessageCircle size={40} />
                  </div>
                  <p>Chưa có hội thoại mới</p>
                </div>
              )}
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
                          <span className="cm-online-dot"></span>
                        )}
                      </div>
                      <div className="cm-chat-header-info">
                        <span className="cm-chat-header-name">
                          {currentConversation.studentName || 'Sinh viên'}
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
                          onClick={() => handleEndChat(currentConversation.id)}
                        >
                          <XCircle size={16} />
                          Kết thúc
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Messages */}
                  <div className="cm-messages-area">
                    {messages.map((msg, index) => {
                      const isAI = msg.senderType === 'AI';
                      const isLibrarian = msg.senderType === 'LIBRARIAN';
                      const isStudent = !isAI && !isLibrarian;

                      // System messages - context card cho yêu cầu hỗ trợ
                      if (msg.senderType === 'SYSTEM') {
                        const isSupportRequest = msg.content?.startsWith('[YÊU CẦU HỖ TRỢ]');

                        // Parse images from message if present
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

                      return (
                        <div
                          key={msg.id || index}
                          className={`cm-message-row ${isLibrarian ? 'mine' : 'theirs'}`}
                        >
                          {/* Avatar for non-librarian */}
                          {!isLibrarian && (
                            <div className={`cm-msg-avatar ${isAI ? 'ai' : 'student'}`}>
                              {isAI ? <Bot size={16} /> : getInitial(currentConversation.studentName)}
                            </div>
                          )}

                          {/* Bubble */}
                          <div className={`cm-bubble ${isAI ? 'ai-bubble' : isStudent ? 'student-bubble' : ''}`}>
                            {isAI && (
                              <div className="cm-ai-label">
                                <Bot size={12} />
                                SLIB AI
                              </div>
                            )}
                            {msg.content}
                            <div className="cm-msg-time">
                              {formatTime(msg.createdAt)}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                    <div ref={messagesEndRef} />
                  </div>

                  {/* Composer - only show for active conversations */}
                  {currentConversation.status === 'HUMAN_CHATTING' && (
                    <div className="cm-composer">
                      <button className="cm-btn-icon">
                        <ImageIcon size={20} />
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
                          disabled={!inputValue.trim()}
                        >
                          <Send size={20} />
                        </button>
                      </form>
                    </div>
                  )}
                </>
              ) : (
                <div className="cm-empty-state">
                  <MessageCircle size={52} className="cm-empty-state-icon" />
                  <p>Chọn một cuộc hội thoại để bắt đầu</p>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default ChatManage;
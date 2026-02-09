import React, { useState, useEffect, useRef, useCallback } from 'react';
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
  CheckCircle
} from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL || '';

const ChatManage = () => {
  const [conversations, setConversations] = useState([]);
  const [selectedConversationId, setSelectedConversationId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const messagesEndRef = useRef(null);
  const stompClientRef = useRef(null);
  const subscriptionRef = useRef(null);

  // Fetch all conversations (waiting + active)
  const fetchConversations = useCallback(async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE}/slib/chat/conversations/all`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setConversations(data);
        if (data.length > 0 && !selectedConversationId) {
          setSelectedConversationId(data[0].id);
        }
      } else {
        console.error('Failed to fetch conversations:', response.status);
      }
    } catch (err) {
      console.error('Error fetching conversations:', err);
      setError('Khong the tai danh sach hoi thoai');
    } finally {
      setLoading(false);
    }
  }, [selectedConversationId]);

  // Fetch messages for selected conversation
  const fetchMessages = useCallback(async (conversationId) => {
    if (!conversationId) return;

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE}/slib/chat/conversations/${conversationId}/messages`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('[CHAT] Fetched messages:', data.length, 'messages');
        setMessages(data.map(msg => ({
          ...msg,
          isMine: msg.senderType === 'LIBRARIAN'
        })));
      } else {
        console.error('[CHAT] Failed to fetch messages:', response.status);
      }
    } catch (err) {
      console.error('Error fetching messages:', err);
    }
  }, []);

  // Take over conversation
  const handleTakeOver = async (conversationId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE}/slib/chat/conversations/${conversationId}/take-over`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        // Refresh conversations list
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

    // Optimistic UI update
    const optimisticMsg = {
      content: messageContent,
      senderType: 'LIBRARIAN',
      time: new Date().toISOString(),
      isMine: true
    };
    setMessages(prev => [...prev, optimisticMsg]);

    try {
      const token = localStorage.getItem('token');
      await fetch(`${API_BASE}/slib/chat/conversations/${selectedConversationId}/messages`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ content: messageContent })
      });
    } catch (err) {
      console.error('Error sending message:', err);
    }
  };

  // End chat - resolve conversation
  const handleEndChat = async (conversationId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE}/slib/chat/conversations/${conversationId}/resolve`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        // Refresh conversations list
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
    // Poll every 10 seconds
    const interval = setInterval(fetchConversations, 10000);
    return () => clearInterval(interval);
  }, [fetchConversations]);

  useEffect(() => {
    if (selectedConversationId) {
      fetchMessages(selectedConversationId);
      // Poll messages every 3 seconds for real-time chat
      const interval = setInterval(() => {
        fetchMessages(selectedConversationId);
      }, 3000);
      return () => clearInterval(interval);
    }
  }, [selectedConversationId, fetchMessages]);

  // WebSocket connection for real-time messages - connect once on mount
  useEffect(() => {
    // Determine WebSocket URL - use backend URL directly
    const backendUrl = API_BASE || 'http://localhost:8080';
    const wsEndpoint = backendUrl + '/ws';

    console.log('[WS] Connecting to:', wsEndpoint);

    const client = new Client({
      webSocketFactory: () => new SockJS(wsEndpoint),
      reconnectDelay: 5000,
      debug: (str) => console.log('[WS Debug]', str),
      onConnect: () => {
        console.log('[WS] Connected to WebSocket successfully!');
        stompClientRef.current = client;
      },
      onStompError: (frame) => {
        console.error('[WS] STOMP error:', frame);
      },
      onWebSocketClose: () => {
        console.log('[WS] WebSocket closed');
        stompClientRef.current = null;
      }
    });

    client.activate();

    return () => {
      if (client) {
        client.deactivate();
      }
    };
  }, []); // Empty dependency - connect once on mount

  // Function to subscribe to a conversation's WebSocket topic
  const subscribeToConversation = useCallback((conversationId) => {
    if (!stompClientRef.current?.connected || !conversationId) {
      console.log('[WS] Cannot subscribe - not connected or no conversationId');
      return;
    }

    // Unsubscribe from previous
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    // Subscribe to new conversation
    subscriptionRef.current = stompClientRef.current.subscribe(
      `/topic/conversation/${conversationId}`,
      (message) => {
        const newMessage = JSON.parse(message.body);
        console.log('[WS] Received message:', newMessage);
        setMessages(prev => {
          // Check duplicate
          if (prev.some(m => m.id === newMessage.id)) {
            return prev;
          }
          return [...prev, newMessage];
        });
      }
    );

    console.log('[WS] Subscribed to conversation:', conversationId);
  }, []);

  // Subscribe to conversation topic when selected
  useEffect(() => {
    if (selectedConversationId && stompClientRef.current?.connected) {
      subscribeToConversation(selectedConversationId);
    }

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
    };
  }, [selectedConversationId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSelectConversation = (id) => {
    setSelectedConversationId(id);
  };

  const currentConversation = conversations.find(c => c.id === selectedConversationId);

  const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <>
      <Header searchPlaceholder="Tim kiem..." onLogout={handleLogout} />

      <div style={{
        padding: '2rem',
        maxWidth: '1400px',
        margin: '0 auto',
        backgroundColor: '#f9fafb',
        minHeight: 'calc(100vh - 80px)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '1.5rem' }}>
          <h2 className="cm-page-title" style={{ margin: 0 }}>Ho tro sinh vien</h2>
          {conversations.length > 0 && (
            <span style={{
              backgroundColor: '#ef4444',
              color: 'white',
              padding: '4px 12px',
              borderRadius: '999px',
              fontSize: '14px',
              fontWeight: '600'
            }}>
              {conversations.length} dang cho
            </span>
          )}
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <p>Dang tai...</p>
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#ef4444' }}>
            <p>{error}</p>
          </div>
        ) : conversations.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '3rem',
            backgroundColor: 'white',
            borderRadius: '12px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
          }}>
            <MessageCircle size={48} color="#9ca3af" />
            <p style={{ color: '#6b7280', marginTop: '1rem' }}>
              Khong co cuoc hoi thoai nao dang cho ho tro
            </p>
          </div>
        ) : (
          <div className="cm-chat-container">
            {/* Left Column: Conversation List */}
            <div className="cm-conversation-list">
              {conversations.map((conv) => (
                <div
                  key={conv.id}
                  onClick={() => handleSelectConversation(conv.id)}
                  className={`cm-conv-item ${selectedConversationId === conv.id ? 'active' : ''}`}
                >
                  <div className="cm-conv-avatar" style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '50%',
                    backgroundColor: '#e5e7eb',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}>
                    <User size={20} color="#6b7280" />
                  </div>
                  <div className="cm-conv-info">
                    <span className="cm-conv-name">{conv.studentName || 'Sinh vien'}</span>
                    <span className="cm-conv-id" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Clock size={12} />
                      {formatTime(conv.createdAt)}
                    </span>
                  </div>
                  {conv.status === 'WAITING_LIBRARIAN' && (
                    <span style={{
                      backgroundColor: '#fef3c7',
                      color: '#92400e',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      fontSize: '11px',
                      fontWeight: '500'
                    }}>
                      Cho
                    </span>
                  )}
                </div>
              ))}
            </div>

            {/* Right Column: Chat Window */}
            <div className="cm-chat-window">
              {currentConversation ? (
                <>
                  {/* Chat Header */}
                  <div style={{
                    padding: '12px 16px',
                    borderBottom: '1px solid #e5e7eb',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between'
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div style={{
                        width: '36px',
                        height: '36px',
                        borderRadius: '50%',
                        backgroundColor: '#e5e7eb',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}>
                        <User size={18} color="#6b7280" />
                      </div>
                      <div>
                        <div style={{ fontWeight: '600' }}>{currentConversation.studentName || 'Sinh vien'}</div>
                        <div style={{ fontSize: '12px', color: '#6b7280' }}>
                          {currentConversation.studentId || 'ID: N/A'}
                        </div>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      {currentConversation.status === 'QUEUE_WAITING' && (
                        <button
                          onClick={() => handleTakeOver(currentConversation.id)}
                          style={{
                            backgroundColor: '#10b981',
                            color: 'white',
                            border: 'none',
                            padding: '8px 16px',
                            borderRadius: '8px',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '6px',
                            fontWeight: '500'
                          }}
                        >
                          <CheckCircle size={16} />
                          Nhan ho tro
                        </button>
                      )}
                      {currentConversation.status === 'HUMAN_CHATTING' && (
                        <button
                          onClick={() => handleEndChat(currentConversation.id)}
                          style={{
                            backgroundColor: '#ef4444',
                            color: 'white',
                            border: 'none',
                            padding: '8px 16px',
                            borderRadius: '8px',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '6px',
                            fontWeight: '500'
                          }}
                        >
                          Ket thuc ho tro
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Messages Area */}
                  <div className="cm-messages-area">
                    {messages.map((msg, index) => {
                      const isAI = msg.senderType === 'AI';
                      const isLibrarian = msg.senderType === 'LIBRARIAN';
                      const isStudent = msg.senderType === 'STUDENT' || (!isAI && !isLibrarian);

                      return (
                        <div
                          key={index}
                          className={`cm-message-row ${isLibrarian ? 'mine' : 'theirs'}`}
                        >
                          {!isLibrarian && (
                            <div style={{
                              width: '32px',
                              height: '32px',
                              borderRadius: '50%',
                              backgroundColor: isAI ? '#8b5cf6' : '#e5e7eb',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              flexShrink: 0
                            }}>
                              {isAI ? (
                                <span style={{ fontSize: '14px' }}>🤖</span>
                              ) : (
                                <User size={16} color="#6b7280" />
                              )}
                            </div>
                          )}
                          <div className="cm-bubble" style={{
                            backgroundColor: isLibrarian ? '#3b82f6' : (isAI ? '#f3e8ff' : '#f3f4f6'),
                            color: isLibrarian ? 'white' : '#1f2937'
                          }}>
                            {isAI && (
                              <div style={{
                                fontSize: '10px',
                                color: '#8b5cf6',
                                fontWeight: '600',
                                marginBottom: '4px'
                              }}>
                                SLIB AI
                              </div>
                            )}
                            {msg.content}
                            <div style={{
                              fontSize: '10px',
                              color: isLibrarian ? 'rgba(255,255,255,0.7)' : '#9ca3af',
                              marginTop: '4px',
                              textAlign: 'right'
                            }}>
                              {formatTime(msg.createdAt || msg.time)}
                            </div>
                          </div>
                        </div>
                      );
                    })}

                    <div ref={messagesEndRef} />
                  </div>

                  {/* Composer */}
                  <div className="cm-composer">
                    <button className="cm-btn-icon">
                      <ImageIcon size={20} />
                    </button>
                    <form onSubmit={handleSendMessage} className="cm-composer-form">
                      <input
                        type="text"
                        placeholder="Nhap tin nhan..."
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
                </>
              ) : (
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  height: '100%',
                  color: '#9ca3af'
                }}>
                  <p>Chon mot cuoc hoi thoai de bat dau</p>
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
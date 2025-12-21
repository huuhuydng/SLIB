import React, { useState, useEffect, useRef } from 'react';
import {
  Search,
  ChevronDown,
  Image as ImageIcon,
  Send,
  LayoutGrid,
  ArrowRightLeft,
  Thermometer,
  Armchair,
  Users,
  AlertTriangle,
  MessageCircle,
  BarChart3,
  Bell,
  HelpCircle
} from 'lucide-react';
import '../../styles/ChatManage.css';

// link từ BE lên ở đây
const WS_URL = "ws://localhost:8080/ws/chat";

// Mock Data
const MOCK_CONVERSATIONS = Array.from({ length: 10 }, (_, i) => ({
  id: `DE17070${i}`,
  name: `PhucNH ${i + 1}`,
  studentId: `DE17070${i}`,
  avatar: `https://picsum.photos/seed/${i}/40/40`,
}));

const INITIAL_MESSAGES = [
  {
    type: "message",
    conversationId: "DE170700",
    senderId: "DE170700",
    senderName: "PhucNH 1",
    content: "Trong này ngủ ngon kinh khủng chị ơi",
    time: "2025-12-15T12:25:00",
    isMine: false
  },
  {
    type: "message",
    conversationId: "DE170700",
    senderId: "admin",
    senderName: "Librarian",
    content: "Đừng ngủ trong thư viện em ơi",
    time: "2025-12-15T12:26:00",
    isMine: true
  }
];

// Status websocket: CONNECTING, OPEN, CLOSED
const ChatManage = () => {
  const [conversations] = useState(MOCK_CONVERSATIONS);
  const [selectedConversationId, setSelectedConversationId] = useState(MOCK_CONVERSATIONS[0].id);
  const [messages, setMessages] = useState(INITIAL_MESSAGES);
  const [inputValue, setInputValue] = useState("");
  const [wsStatus, setWsStatus] = useState("DISCONNECTED"); // CONNECTING, OPEN, CLOSED
  
  const ws = useRef(null);
  const messagesEndRef = useRef(null);

  // 1. WebSocket Connection
  useEffect(() => {
    console.log("Connecting to WebSocket...");
    setWsStatus("CONNECTING");
    
    ws.current = new WebSocket(WS_URL);

    ws.current.onopen = () => {
      console.log("WebSocket Connected");
      setWsStatus("OPEN");
    };

    ws.current.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        // Simple logic: if message belongs to current conversation (or generally append)
        // Check if message is mine to flag it (assuming senderId 'admin' is me)
        const newMsg = {
          ...message,
          isMine: message.senderId === 'admin'
        };
        setMessages((prev) => [...prev, newMsg]);
        console.log("Received:", newMsg);
      } catch (err) {
        console.error("Error parsing message:", err);
      }
    };

    ws.current.onclose = () => {
      console.log("WebSocket Disconnected");
      setWsStatus("CLOSED");
    };

    ws.current.onerror = (error) => {
      console.error("WebSocket Error:", error);
      // Optional: setWsStatus("CLOSED");
    };

    return () => {
      if (ws.current) {
        ws.current.close();
      }
    };
  }, []);

  // 2. Auto scroll to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 3. Handle Send Message
  const handleSendMessage = (e) => {
    if (e) e.preventDefault();
    if (!inputValue.trim()) return;

    const payload = {
      type: "message",
      conversationId: selectedConversationId,
      senderId: "admin",
      senderName: "PhucNH", // As per requirement
      content: inputValue,
      time: new Date().toISOString()
    };

    // Optimistic UI update
    const optimisticMsg = { ...payload, isMine: true };
    setMessages((prev) => [...prev, optimisticMsg]);

    // Send via WS
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify(payload));
    } else {
      console.warn("WebSocket not connected. Message not sent to server.");
    }

    setInputValue("");
  };

  const handleSelectConversation = (id) => {
    setSelectedConversationId(id);
    // In a real app, fetch history for this conversation here.
    // For this mock, we just keep the current message list or could reset it.
  };

  const currentConversation = conversations.find(c => c.id === selectedConversationId);

  return (
    <div className="cm-layout">
      {/* Sidebar (Static UI based on "Slib") */}
      <aside className="cm-sidebar">
        <div className="cm-brand">
          <h1>Slib<span className="cm-brand-icon">📚</span></h1>
        </div>
        <nav className="cm-nav">
          <a href="#" className="cm-nav-item"><LayoutGrid size={20} /> Tổng quan</a>
          <a href="#" className="cm-nav-item"><ArrowRightLeft size={20} /> Kiểm tra ra/vào</a>
          <a href="#" className="cm-nav-item"><Thermometer size={20} /> Bản đồ nhiệt</a>
          <a href="#" className="cm-nav-item"><Armchair size={20} /> Quản lý chỗ ngồi</a>
          <a href="#" className="cm-nav-item"><Users size={20} /> Sinh viên</a>
          <a href="#" className="cm-nav-item"><AlertTriangle size={20} /> Vi phạm</a>
          <a href="#" className="cm-nav-item active"><MessageCircle size={20} /> Trò chuyện</a>
          <a href="#" className="cm-nav-item"><BarChart3 size={20} /> Thống kê</a>
          <a href="#" className="cm-nav-item"><Bell size={20} /> Thông báo</a>
        </nav>
        <div className="cm-sidebar-footer">
          <div className="cm-help-icon"><HelpCircle size={24} /></div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="cm-main-content">
        {/* Topbar */}
        <header className="cm-topbar">
          <div className="cm-search-bar">
            <Search size={18} className="cm-search-icon" />
            <input type="text" placeholder="Search for anything..." />
          </div>
          <div className="cm-profile-pill">
            <img src="https://picsum.photos/id/64/40/40" alt="Admin" className="cm-avatar-sm" />
            <div className="cm-profile-info">
              <span className="cm-profile-name">PhucNH</span>
              <span className="cm-profile-role">Librarian</span>
            </div>
            <ChevronDown size={16} className="cm-chevron" />
          </div>
        </header>

        <h2 className="cm-page-title">Đoạn chat</h2>

        <div className="cm-chat-container">
          {/* Left Column: Conversation List */}
          <div className="cm-conversation-list">
            {conversations.map((conv) => (
              <div
                key={conv.id}
                className={`cm-conv-item ${selectedConversationId === conv.id ? 'active' : ''}`}
                onClick={() => handleSelectConversation(conv.id)}
              >
                <img src={conv.avatar} alt={conv.name} className="cm-conv-avatar" />
                <div className="cm-conv-info">
                  <span className="cm-conv-name">{conv.name}</span>
                  <span className="cm-conv-id">{conv.studentId}</span>
                </div>
              </div>
            ))}
          </div>

          {/* Right Column: Chat Window */}
          <div className="cm-chat-window">
            {/* Header within chat window (optional, implied by UI structure, keeping simple) */}
            
            {/* Messages Area */}
            <div className="cm-messages-area">
              {messages.map((msg, index) => (
                <div key={index} className={`cm-message-row ${msg.isMine ? 'mine' : 'theirs'}`}>
                  {!msg.isMine && currentConversation && (
                    <img src={currentConversation.avatar} alt="Sender" className="cm-msg-avatar" />
                  )}
                  <div className="cm-bubble">
                    {msg.content}
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            {/* Composer */}
            <div className="cm-composer">
               {wsStatus !== 'OPEN' && <span className="cm-connecting">Connecting...</span>}
              <button className="cm-btn-icon">
                <ImageIcon size={20} />
              </button>
              <form onSubmit={handleSendMessage} className="cm-composer-form">
                <input
                  type="text"
                  placeholder="Aa"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  disabled={wsStatus !== 'OPEN'}
                />
                <button type="submit" className="cm-btn-send" disabled={wsStatus !== 'OPEN'}>
                  <Send size={20} />
                </button>
              </form>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default ChatManage;

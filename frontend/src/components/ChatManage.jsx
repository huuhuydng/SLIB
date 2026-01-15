import React, { useState, useEffect, useRef } from 'react';
import '../styles/ChatManage.css';
import Header from './Header';
import {
  Image as ImageIcon,
  Send
} from 'lucide-react';

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
    senderId: "admin",
    senderName: "Librarian",
    content: "Đừng ngủ trong thư viện em ơi",
    time: "2025-12-15T12:26:00",
    isMine: true
  },
  {
    type: "message",
    conversationId: "DE170700",
    senderId: "DE170700",
    senderName: "PhucNH 1",
    content: "Trong này ngủ ngon kinh khủng chị ơi",
    time: "2025-12-15T12:25:00",
    isMine: false
  }
];


// Status websocket: CONNECTING, OPEN, CLOSED
const ChatManage = () => {
  const [conversations] = useState(MOCK_CONVERSATIONS);
  const [selectedConversationId, setSelectedConversationId] = useState(MOCK_CONVERSATIONS[0].id);
  const [messages, setMessages] = useState(INITIAL_MESSAGES);
  const [inputValue, setInputValue] = useState("");
  const [wsStatus, setWsStatus] = useState("DISCONNECTED"); // CONNECTING, OPEN, CLOSED
  

  useEffect(() => {
  setWsStatus("OPEN"); // 👈 giả lập đã kết nối
}, []);

  const ws = useRef(null);
  const messagesEndRef = useRef(null);

  // 1. WebSocket Connection
//   useEffect(() => {
//     console.log("Connecting to WebSocket...");
//     setWsStatus("CONNECTING");
    
//     ws.current = new WebSocket(WS_URL);

//     ws.current.onopen = () => {
//       console.log("WebSocket Connected");
//       setWsStatus("OPEN");
//     };

//     ws.current.onmessage = (event) => {
//       try {
//         const message = JSON.parse(event.data);
//         // Simple logic: if message belongs to current conversation (or generally append)
//         // Check if message is mine to flag it (assuming senderId 'admin' is me)
//         const newMsg = {
//           ...message,
//           isMine: message.senderId === 'admin'
//         };
//         setMessages((prev) => [...prev, newMsg]);
//         console.log("Received:", newMsg);
//       } catch (err) {
//         console.error("Error parsing message:", err);
//       }
//     };

//     ws.current.onclose = () => {
//       console.log("WebSocket Disconnected");
//       setWsStatus("CLOSED");
//     };

//     ws.current.onerror = (error) => {
//       console.error("WebSocket Error:", error);
//       // Optional: setWsStatus("CLOSED");
//     };

//     return () => {
//       if (ws.current) {
//         ws.current.close();
//       }
//     };
//   }, []);

  // 2. Auto scroll to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 3. Handle Send Message
  const handleSendMessage = (e) => {
 e.preventDefault();

  if (!inputValue.trim()) return;

  const myMsg = {
    type: "message",
    conversationId: selectedConversationId,
    senderId: "admin",
    senderName: "PhucNH",
    content: inputValue,
    time: new Date().toISOString(),
    isMine: true
  };

  // 1️⃣ Hiện ngay trên UI
  setMessages(prev => [...prev, myMsg]);

  // 2️⃣ Gửi qua WS (nếu có)
  if (ws.current && ws.current.readyState === WebSocket.OPEN) {
    ws.current.send(JSON.stringify(myMsg));
  }

  // 3️⃣ Clear input
  setInputValue("");

  // 4️⃣ Giả lập phản hồi sinh viên
  setTimeout(() => {
    setMessages(prev => [
      ...prev,
      {
        type: "message",
        conversationId: selectedConversationId,
        senderId: "student",
        senderName: "Student",
        content: "Dạ em xin lỗi ạ 😥",
        time: new Date().toISOString(),
        isMine: false
      }
    ]);
  }, 1000);
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
    <>
      <Header searchPlaceholder="Search for anything..." />

        <div style={{
          padding: '2rem',
          maxWidth: '1400px',
          margin: '0 auto',
          backgroundColor: '#f9fafb',
          minHeight: 'calc(100vh - 80px)'
        }}>
        <h2 className="cm-page-title">Đoạn chat</h2>

        <div className="cm-chat-container">
          {/* Left Column: Conversation List */}
          <div className="cm-conversation-list">
            {conversations.map((conv) => (
              <div
                key={conv.id}
                onClick={() => handleSelectConversation(conv.id)}
                className={`cm-conv-item ${selectedConversationId === conv.id ? 'active' : ''}`}
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
            {/* Messages Area */}
            <div className="cm-messages-area">
              {messages.map((msg, index) => (
                <div
                  key={index}
                  className={`cm-message-row ${msg.isMine ? 'mine' : 'theirs'}`}
                >
                  {!msg.isMine && currentConversation && (
                    <img
                      src={currentConversation.avatar}
                      alt="Sender"
                      className="cm-msg-avatar"
                    />
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
              {wsStatus !== 'OPEN' && (
                <span className="cm-connecting">Connecting...</span>
              )}
              <button className="cm-btn-icon">
                <ImageIcon size={20} />
              </button>
              <form onSubmit={handleSendMessage} className="cm-composer-form">
                <input
                  type="text"
                  placeholder="Aa"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  disabled={false}
                />
                <button
                  type="submit"
                  disabled={wsStatus !== 'OPEN'}
                  className="cm-btn-send"
                >
                  <Send size={20} />
                </button>
              </form>
            </div>
          </div>
        </div>
        </div>
    </>
  );
};

export default ChatManage;
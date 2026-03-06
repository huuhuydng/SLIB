# Module 11: Chat & Support

## FE-113: Chat with AI virtual assistant

### Function trigger

- **Navigation path:** Mobile: Bottom Tab -> Chat icon
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student
- **Purpose:** Chat with AI Assistant for library support
- **Onboarding:** "Nhận gợi ý giờ vàng học tập và giải đáp thắc mắc 24/7 cùng Chatbot AI thông minh"
- **Interface:**
    1. **Chat history:** Message history
    2. **Message input:** Message input field
    3. **Quick replies:** Common questions
    4. **Typing indicator:** Indicator when AI is responding
    5. **Feedback buttons:** Like/Dislike for responses
- **Data processing:**
    1. Student sends question
    2. AI Service uses RAG to find context
    3. Generate response with streaming
    4. Display response

### Function details

- **Data:** Messages, conversation history, context
- **Validation:** Rate limit 10 messages/minute
- **Business rules:**
    - AI answers about booking, rules, library
    - Escalate to Librarian when AI doesn't know
    - Conversations saved to improve AI
- **Normal case:** AI answers question successfully
- **Abnormal case:** AI doesn't understand: Suggest contacting Librarian

---

## FE-114: Chat with Librarian

- **Actors:** Student
- **Purpose:** Chat directly with Librarian when AI isn't sufficient ("Chat với quản trị viên")
- **Interface:** Chat interface with human indicator
- **Business rules:** Transfer from AI chat or initiate directly
- **Status:** Waiting, Connected, Ended

---

## FE-115: View history of chat

- **Actors:** Student
- **Purpose:** View conversation history
- **Interface:** Conversation list with summaries
- **Data:** Conversation list with date, AI/Human flag

---

## FE-116: View list of chats

- **Actors:** Librarian
- **Navigation path:** Sidebar -> Chat management
- **Purpose:** View all chats needing support
- **Interface:** Queue with priority (Waiting first)
- **Data:** All conversations with status

---

## FE-117: View chat details (can use AI for replying suggestion)

- **Actors:** Librarian
- **Purpose:** View chat details and get AI suggestions
- **Interface:** Chat panel with AI suggestions
- **Features:**
    - Full conversation history
    - AI suggested replies
    - Student context (bookings, violations)

---

## FE-118: Response to Student manually

- **Actors:** Librarian
- **Purpose:** Reply to student directly
- **Interface:** Message input with send button
- **Business rules:** Log responses

---

## FE-119: Response to Student with AI suggestion

- **Actors:** Librarian
- **Purpose:** Use AI suggestion for quick reply
- **Interface:** Click AI suggestion to insert
- **Business rules:** Librarian can edit before sending

---


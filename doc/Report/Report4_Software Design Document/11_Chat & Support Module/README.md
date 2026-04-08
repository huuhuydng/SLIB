# Module 11 - Chat & Support Module

This folder contains the Report 4 diagrams for Module 11 - Chat & Support Module.

## Included Artifacts

- Sequence diagrams for:
  - `FE-113` Chat with AI virtual assistant
  - `FE-114` Chat with Librarian
  - `FE-115` Send request for support
  - `FE-116` View list of support requests
  - `FE-117` View history of chat
  - `FE-118` View list of chats
  - `FE-119` View chat details
  - `FE-120` Response to user manually
  - `FE-121` Response to user with AI suggestion
- Class diagram for the Chat & Support module

## Actor Scope

- `FE-113`: Student
- `FE-114`: Student
- `FE-115`: Student
- `FE-116`: Librarian
- `FE-117`: Student
- `FE-118`: Librarian
- `FE-119`: Librarian
- `FE-120`: Librarian
- `FE-121`: Librarian

## Current Working Assumptions

- The current chat flow used by the mobile app and librarian web portal is the conversation-based implementation under `UserChatController`, `ConversationService`, `ChatScreen`, and `ChatManage.jsx`.
- `FE-113` follows the current student AI chat flow in `ChatScreen`, `chat_service.dart`, backend `/slib/ai/proxy-chat`, and AI service chat router with MongoDB session history.
- `FE-114` follows the current student-to-librarian escalation flow using `/slib/chat/conversations/request-librarian`, queue waiting, WebSocket updates, and librarian takeover in `ChatManage.jsx`.
- `FE-115` follows the current support request flow implemented in `SupportRequestScreen`, `SupportRequestHistoryScreen`, `SupportRequestController`, and `SupportRequestService`.
- `FE-116` follows the current librarian support request management page in `SupportRequestManage.jsx`, which loads all support requests and optionally filters by status.
- `FE-117` follows the current student chat history restoration flow in `ChatScreen`, which loads persisted conversation state and fetches paginated conversation messages from `/slib/chat/conversations/{conversationId}/messages`.
- `FE-118` follows the current librarian chat list flow in `ChatManage.jsx`, which loads `/slib/chat/conversations/all` and updates waiting and active conversations via WebSocket topics.
- `FE-119` follows the current librarian chat detail flow in `ChatManage.jsx`, which loads `/slib/chat/conversations/{conversationId}/messages` and marks the selected conversation as read.
- `FE-120` follows the current librarian manual response flow in `ChatManage.jsx`, where the librarian sends a message directly to `/slib/chat/conversations/{conversationId}/messages` or `/messages/with-image`.
- `FE-121` is mapped to the current AI-assisted context flow rather than a dedicated “generate suggestion” button. In the current system, AI chat history is preserved during escalation, bot messages are assigned into the human support session, and the librarian reviews that AI context before replying manually.
- The legacy endpoints under `backend/controller/ai/ChatController.java` are not used as the primary flow for the current mobile and librarian chat experience, so they are not treated as the main sequence source for this module.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.

# Module 11 - Chat & Support Module

This folder contains the Report 4 diagrams for Module 11 - Chat & Support Module.

## Included Artifacts

- Sequence diagrams for:
  - `FE-121` Chat with AI virtual assistant
  - `FE-122` Chat with Librarian
  - `FE-123` Send request for support
  - `FE-124` View list of support requests
  - `FE-125` View history of chat
  - `FE-126` View list of chats
  - `FE-127` View chat details
  - `FE-128` Response to user manually
- Class diagram for the Chat & Support module

## Actor Scope

- `FE-121`: Student
- `FE-122`: Student
- `FE-123`: Student
- `FE-124`: Librarian
- `FE-125`: Student
- `FE-126`: Librarian
- `FE-127`: Librarian
- `FE-128`: Librarian

## Current Working Assumptions

- The current chat flow used by the mobile app and librarian web portal is the conversation-based implementation under `UserChatController`, `ConversationService`, `ChatScreen`, and `ChatManage.jsx`.
- `FE-121` follows the current student AI chat flow in `ChatScreen`, `chat_service.dart`, backend `/slib/ai/proxy-chat`, and AI service chat router with MongoDB session history.
- `FE-122` follows the current student-to-librarian escalation flow using `/slib/chat/conversations/request-librarian`, queue waiting, WebSocket updates, and librarian takeover in `ChatManage.jsx`.
- `FE-123` follows the current support request flow implemented in `SupportRequestScreen`, `SupportRequestHistoryScreen`, `SupportRequestController`, and `SupportRequestService`.
- `FE-124` follows the current librarian support request management page in `SupportRequestManage.jsx`, which loads all support requests and optionally filters by status.
- `FE-125` follows the current student chat history restoration flow in `ChatScreen`, which loads persisted conversation state and fetches paginated conversation messages from `/slib/chat/conversations/{conversationId}/messages`.
- `FE-126` follows the current librarian chat list flow in `ChatManage.jsx`, which loads `/slib/chat/conversations/all` and updates waiting and active conversations via WebSocket topics.
- `FE-127` follows the current librarian chat detail flow in `ChatManage.jsx`, which loads `/slib/chat/conversations/{conversationId}/messages` and marks the selected conversation as read.
- `FE-128` follows the current librarian manual response flow in `ChatManage.jsx`, where the librarian sends a message directly to `/slib/chat/conversations/{conversationId}/messages` or `/messages/with-image`.
- The legacy endpoints under `backend/controller/ai/ChatController.java` are not used as the primary flow for the current mobile and librarian chat experience, so they are not treated as the main sequence source for this module.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.

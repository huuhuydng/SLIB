# AGENTS.md

Guidance for Codex and sub-agents working in the SLIB repository.

Source of truth for these notes:
- `CLAUDE.md`
- `.claude/agents/slib-backend-developer.md`
- `.claude/agents/frontend-slib-developer.md`
- `.claude/agents/slib-ai-service-expert.md`

When these files change, update this file too.

## Project Overview

SLIB (Smart Library) is a monorepo with:
- `backend/`: Spring Boot 3.4, Java 21, REST API, STOMP WebSocket
- `frontend/`: React 19 + Vite 7 web portal for librarian/admin
- `mobile/`: Flutter app for students
- `ai-service/`: FastAPI Python service for RAG chat and escalation

Production domains:
- Frontend: `slibsystem.site`
- Backend: `api.slibsystem.site`

## Architecture Notes

- Main API base path is `/slib/` and not `/slib/api/v1/`.
- WebSocket endpoint is `/ws`.
- STOMP broker prefix is `/topic/*`.
- STOMP app prefix is `/app/*`.
- PostgreSQL is the main relational database.
- MongoDB stores AI chat history.
- Qdrant stores vector embeddings.
- Redis is used for cache and realtime support pieces.

## Working Rules

- Use Vietnamese with full diacritics for all user-facing text.
- Do not add emoji in code or documentation, except intentional chatbot output.
- Read existing files before editing to match local patterns.
- Stay within the module you are changing unless the task clearly requires cross-module work.
- For cross-module work, implement in this order when possible:
  1. `backend/`
  2. `ai-service/`
  3. `frontend/` or `mobile/`

## Module Routing

Use this mapping when delegating or structuring work:

- `backend/`
  - Scope: Spring Boot, Java, SQL, Flyway, JPA, auth, WebSocket server, Cloudinary, Firebase push.
  - Typical tasks: entities, repositories, services, controllers, migrations, permissions, realtime send logic.

- `frontend/`
  - Scope: React web app for librarian/admin only.
  - Typical tasks: pages, components, CSS, axios services, STOMP subscriptions, routing.

- `mobile/`
  - Scope: Flutter student app.
  - Typical tasks: screens, widgets, chat UI, service integration, notification UX.

- `ai-service/`
  - Scope: FastAPI, RAG, MongoDB chat history, Qdrant retrieval, escalation logic.
  - Typical tasks: prompt/response flow, retrieval thresholds, ingestion, AI endpoints.

## Read First Files

Before changing each area, read these first when relevant.

### Backend

- `backend/src/main/java/slib/com/example/config/SecurityConfig.java`
- `backend/src/main/java/slib/com/example/config/WebSocketConfig.java`
- `backend/src/main/resources/application.properties`

Backend conventions:
- Package root: `slib.com.example`
- Use `ResponseEntity<?>` from controllers
- Use Lombok for entities and DTOs where the codebase already does
- Prefer `FetchType.LAZY` for relationships
- Use `@Transactional` for write operations
- Use Flyway migrations in `backend/src/main/resources/db/migration/`
- Throw project exceptions instead of ad hoc generic failures when practical

### Frontend

- `frontend/src/App.jsx`
- `frontend/src/styles/librarian/librarian-shared.css`
- `frontend/src/services/librarianService.jsx`
- `frontend/vite.config.js`

Frontend conventions:
- Use React hooks and existing service patterns
- Use `lucide-react` for icons
- Do not introduce Tailwind or CSS-in-JS
- Follow CSS prefixes already used in the repo such as `lib-*`, `cio-*`, `sm-*`, `booking-*`, `chat-*`
- Keep page titles in the established style used by nearby pages
- Use Vietnamese text with full accents in UI

### Mobile

- Read the target screen and its related service files first
- Follow existing Flutter state and service patterns in the touched feature
- Keep new text in Vietnamese with full accents
- Preserve local message persistence and conversation restoration behavior when changing chat flows

### AI Service

- `ai-service/app/main.py`
- `ai-service/app/services/chat_service.py`
- `ai-service/app/services/escalation_service.py`
- `ai-service/app/config/settings.py`

AI service conventions:
- Main prefix is `/api/ai/`
- Legacy chat endpoint compatibility may still matter
- Use logging, not raw `print`, for new code
- Respect RAG fallback and escalation patterns already present
- Keep response tone aligned with the current friendly Vietnamese chatbot persona

## Collaboration Patterns

Use these when a task spans multiple modules.

### Full-stack feature

1. Backend API/data shape
2. AI service if needed
3. Frontend/mobile integration

### Realtime feature

1. Backend STOMP/topic publishing
2. Frontend/mobile subscription and rendering

### Chat escalation feature

1. AI service escalation rules if applicable
2. Backend conversation and notification flow
3. Frontend/mobile chat presentation

## Important Domain Conventions

- Roles: `STUDENT`, `LIBRARIAN`, `ADMIN`
- Google OAuth is restricted to FPT email domain `@fpt.edu.vn`
- Token keys in web flows commonly include `librarian_token` and `librarian_user`

Status examples already present in the system:
- Reservation: `PROCESSING`, `BOOKED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `EXPIRED`
- Seat: `AVAILABLE`, `HOLDING`, `BOOKED`, `OCCUPIED`, `MAINTENANCE`

## Naming Conventions

- Java entity: PascalCase singular
- Java controller/service/repository: standard `*Controller`, `*Service`, `*Repository`
- DTOs: `*Request`, `*Response`
- React and Flutter components/widgets: PascalCase
- Database tables: snake_case plural

## Common Commands

### Backend

```bash
cd backend
./mvnw spring-boot:run
./mvnw clean package -DskipTests
./mvnw test
```

### Frontend

```bash
cd frontend
npm install
npm run dev
npm run build
npm run lint
npm test
```

### Mobile

```bash
cd mobile
flutter pub get
flutter run
flutter build apk
flutter build ios --release
```

### AI Service

```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

### Infra

```bash
docker-compose up -d
docker-compose down
docker-compose logs -f
```

## Notes For Codex

- Prefer local repo conventions over generic framework defaults.
- If `.claude` contains more specific guidance for a touched module, use it as supporting context.
- Do not copy `.claude` instructions blindly when they conflict with the current Codex environment; adapt them.

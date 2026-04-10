# Database Table Descriptions

This document describes the current PostgreSQL tables used by the SLIB system in the same style as the database description template.

| No | Table | Description |
|---|---|---|
| 01 | `users` | Stores basic information of all system users, including Admin, Librarian, Student, and Teacher.<br><br>Primary keys: `id` |
| 02 | `user_settings` | Stores personal settings such as HCE, AI recommendation, booking reminder, theme, and language preferences.<br><br>Primary keys: `user_id`<br>Foreign keys: `users.id` |
| 03 | `student_profiles` | Stores student-specific profile statistics such as reputation score, study hours, and violation count.<br><br>Primary keys: `user_id`<br>Foreign keys: `users.id` |
| 04 | `refresh_tokens` | Stores JWT refresh tokens for login sessions and device-based authentication tracking.<br><br>Primary keys: `id`<br>Foreign keys: `users.id` |
| 05 | `otp_tokens` | Stores OTP and reset token records for password recovery and verification flows.<br><br>Primary keys: `id` |
| 06 | `import_jobs` | Stores metadata of bulk user import jobs, including file name, status, and processing summary.<br><br>Primary keys: `id` |
| 07 | `user_import_staging` | Temporarily stores imported rows before they are validated and written into official user records.<br><br>Primary keys: `id` |
| 08 | `areas` | Stores top-level library layout areas used for map design and physical space management.<br><br>Primary keys: `area_id` |
| 09 | `area_factories` | Stores reusable layout objects or factory blocks associated with an area in the layout editor.<br><br>Primary keys: `factory_id`<br>Foreign keys: `areas.area_id` |
| 10 | `zones` | Stores zones inside each area, including coordinates, dimensions, and lock status.<br><br>Primary keys: `zone_id`<br>Foreign keys: `areas.area_id` |
| 11 | `zone_amenities` | Stores amenities or attributes attached to a specific zone.<br><br>Primary keys: `amenity_id`<br>Foreign keys: `zones.zone_id` |
| 12 | `seats` | Stores seat definitions, positions, booking status, holding status, and NFC tag binding data.<br><br>Primary keys: `seat_id`<br>Foreign keys: `zones.zone_id`, `users.id` |
| 13 | `library_settings` | Stores global library configuration such as operating hours, booking rules, reputation threshold, and notification toggles.<br><br>Primary keys: `id` |
| 14 | `hce_devices` | Stores HCE scan station information, heartbeat data, location, and current device state.<br><br>Primary keys: `id`<br>Foreign keys: `areas.area_id` |
| 15 | `reservations` | Stores seat booking records, booking time window, confirmation time, actual end time, and reservation status.<br><br>Primary keys: `reservation_id`<br>Foreign keys: `users.id`, `seats.seat_id` |
| 16 | `access_logs` | Stores library check-in and check-out logs produced by HCE or QR access flows.<br><br>Primary keys: `log_id`<br>Foreign keys: `users.id`, `reservations.reservation_id`, `seats.seat_id` |
| 17 | `activity_logs` | Stores activity history records shown in the user activity screen and used for traceability.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `reservations.reservation_id` |
| 18 | `reputation_rules` | Stores reputation deduction or reward rules used by the library discipline system.<br><br>Primary keys: `id` |
| 19 | `point_transactions` | Stores reputation score changes for users, including related rule, activity, and resulting balance.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `activity_logs.id`, `reputation_rules.id` |
| 20 | `news_categories` | Stores categories used to classify news and announcement posts.<br><br>Primary keys: `id` |
| 21 | `news` | Stores news and announcement content, publication state, pinning flag, and view count.<br><br>Primary keys: `id`<br>Foreign keys: `news_categories.id` |
| 22 | `new_books` | Stores information about newly introduced books displayed in the librarian portal and user-facing news screens.<br><br>Primary keys: `id`<br>Foreign keys: `users.id` |
| 23 | `notifications` | Stores in-app notification records for users, including type, read status, and optional reference target.<br><br>Primary keys: `id`<br>Foreign keys: `users.id` |
| 24 | `feedbacks` | Stores feedback submitted after check-out, including rating, content, classification, and review result.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `reservations.reservation_id`, `users.id` |
| 25 | `seat_status_reports` | Stores reports about seat condition issues such as maintenance or damage and their verification lifecycle.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `seats.seat_id`, `users.id` |
| 26 | `seat_violation_reports` | Stores reports about seat misuse or violation cases, related reservation, evidence, and verification result.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `users.id`, `seats.seat_id`, `reservations.reservation_id`, `users.id` |
| 27 | `complaints` | Stores complaints submitted by users to dispute deductions or violation decisions and their resolution details.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `point_transactions.id`, `seat_violation_reports.id`, `users.id` |
| 28 | `conversations` | Stores human support conversation sessions between students and librarians, including escalation and resolution state.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `users.id` |
| 29 | `messages` | Stores chat messages exchanged in support conversations, including sender, receiver, attachment, and read state.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `users.id`, `conversations.id` |
| 30 | `support_requests` | Stores support requests submitted by students and the manual response or resolution from staff.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `users.id` |
| 31 | `kiosk_images` | Stores slideshow images displayed on kiosk screens, including order, active state, and duration.<br><br>Primary keys: `id` |
| 32 | `library_maps` | Stores uploaded library map images used in kiosk and visual layout display features.<br><br>Primary keys: `id` |
| 33 | `zone_maps` | Stores interactive zone rectangles drawn on top of a library map image.<br><br>Primary keys: `id`<br>Foreign keys: `library_maps.id` |
| 34 | `kiosk_configs` | Stores kiosk device configuration, activation state, token information, and issuance metadata.<br><br>Primary keys: `id`<br>Foreign keys: `users.id` |
| 35 | `kiosk_qr_sessions` | Stores temporary QR sessions used when students interact with kiosk devices for access operations.<br><br>Primary keys: `id`<br>Foreign keys: `kiosk_configs.id`, `users.id`, `access_logs.log_id` |
| 36 | `kiosk_activation_codes` | Stores activation codes generated for kiosk device registration and secure activation.<br><br>Primary keys: `id`<br>Foreign keys: `kiosk_configs.id` |
| 37 | `ai_config` | Stores global AI-related configuration key-value pairs used by the system.<br><br>Primary keys: `id` |
| 38 | `ai_knowledge_stores` | Stores AI knowledge store definitions and synchronization metadata.<br><br>Primary keys: `id` |
| 39 | `ai_materials` | Stores AI materials grouped under a knowledge store.<br><br>Primary keys: `id`<br>Foreign keys: `ai_knowledge_stores.id` |
| 40 | `ai_material_items` | Stores individual material items such as text or files belonging to an AI material.<br><br>Primary keys: `id`<br>Foreign keys: `ai_materials.id` |
| 41 | `ai_knowledge_store_items` | Stores the mapping between knowledge stores and material items for indexing and retrieval.<br><br>Foreign keys: `ai_knowledge_stores.id`, `ai_material_items.id` |
| 42 | `chat_sessions` | Stores AI chat sessions owned by a user in the assistant subsystem.<br><br>Primary keys: `id`<br>Foreign keys: `users.id` |
| 43 | `chat_messages` | Stores messages generated inside an AI chat session, including role and token usage.<br><br>Primary keys: `id`<br>Foreign keys: `chat_sessions.id` |
| 44 | `knowledge_base` | Stores structured knowledge base content used by the AI service for retrieval or prompt augmentation.<br><br>Primary keys: `id` |
| 45 | `prompt_templates` | Stores reusable prompt templates for AI interactions and assistant contexts.<br><br>Primary keys: `id` |
| 46 | `student_behaviors` | Stores AI or analytics-derived behavior records linked to students, bookings, seats, and zones.<br><br>Primary keys: `id`<br>Foreign keys: `users.id`, `reservations.reservation_id`, `seats.seat_id`, `zones.zone_id` |
| 47 | `backup_schedules` | Stores automatic backup schedule configuration, including frequency, time, and retention period.<br><br>Primary keys: `id` |
| 48 | `backup_history` | Stores execution history of manual or scheduled backup operations.<br><br>Primary keys: `id`<br>Foreign keys: `backup_schedules.id`, `users.id` |
| 49 | `system_logs` | Stores system log entries for monitoring, auditing, and operational tracing.<br><br>Primary keys: `id`<br>Foreign keys: `users.id` |
| 50 | `seed_records` | Stores records of seeded data to avoid duplicate initialization across environments.<br><br>Primary keys: `id` |
| 51 | `layout_drafts` | Stores unpublished layout draft snapshots created in the layout administration module.<br><br>Primary keys: `draft_id`<br>Foreign keys: `users.id` |
| 52 | `layout_history` | Stores published layout history snapshots and metadata about who published them.<br><br>Primary keys: `history_id`<br>Foreign keys: `users.id` |

## Notes

- The descriptions are based on the latest PostgreSQL schema currently used by the project.
- A few tables contain more than one foreign key pointing to the same table, especially `users`, because they represent different business roles such as reporter, verifier, reviewer, or resolver.
- Soft references such as `notifications.reference_id` and `feedbacks.conversation_id` are intentionally not listed as strict foreign keys here because they are handled by business logic rather than a hard database constraint.

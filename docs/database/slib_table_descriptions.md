# SLIB Current Database Table Descriptions

Source: PostgreSQL schema currently running on the VM.

01
`users`
Stores basic information of all system actors, including Admin, Librarian, Student, and Teacher accounts.
Primary keys: `id`

02
`student_profiles`
Stores student-specific reputation statistics, total study hours, and violation counters.
Primary keys: `user_id`
Foreign keys: `users.id`

03
`user_settings`
Stores user preferences for language, theme, booking reminders, AI recommendations, and HCE usage.
Primary keys: `user_id`
Foreign keys: `users.id`

04
`refresh_tokens`
Stores hashed refresh tokens used to maintain authenticated user sessions.
Primary keys: `id`
Foreign keys: `users.id`

05
`otp_tokens`
Stores OTP tokens for password reset and related email-based verification flows.
Primary keys: `id`

06
`import_jobs`
Stores metadata and progress summary for bulk user import jobs.
Primary keys: `id`

07
`user_import_staging`
Temporarily stores imported user rows before validation and official user creation.
Primary keys: `id`

08
`areas`
Stores top-level physical library areas used in digital layout configuration.
Primary keys: `area_id`

09
`area_factories`
Stores reusable layout objects or factory blocks placed inside library areas.
Primary keys: `factory_id`
Foreign keys: `areas.area_id`

10
`zones`
Stores zones inside each area, including coordinates, dimensions, and movement lock status.
Primary keys: `zone_id`
Foreign keys: `areas.area_id`

11
`zone_amenities`
Stores amenities or attributes assigned to specific zones.
Primary keys: `amenity_id`
Foreign keys: `zones.zone_id`

12
`seats`
Stores seat definitions, zone placement, active/visible state, and NFC tag binding data.
Primary keys: `seat_id`
Foreign keys: `zones.zone_id`

13
`library_settings`
Stores global library configuration such as operating hours, booking rules, notification toggles, reputation thresholds, and temporary closure settings.
Primary keys: `id`

14
`hce_devices`
Stores HCE scan station information, heartbeat data, location, type, and current device state.
Primary keys: `id`
Foreign keys: `areas.area_id`

15
`reservations`
Stores seat booking records, booking time windows, confirmation time, actual end time, cancellation data, and layout-change warning metadata.
Primary keys: `reservation_id`
Foreign keys: `users.id`, `seats.seat_id`

16
`access_logs`
Stores user library check-in/check-out records, including device, reservation, and seat references.
Primary keys: `log_id`
Foreign keys: `users.id`, `reservations.reservation_id`, `seats.seat_id`

17
`activity_logs`
Stores user activity records related to booking, check-in, seat usage, and reputation events.
Primary keys: `id`
Foreign keys: `users.id`, `reservations.reservation_id`

18
`reputation_rules`
Stores rule definitions used to add or deduct reputation points.
Primary keys: `id`

19
`point_transactions`
Stores reputation point changes for users, including rule references and balance after transaction.
Primary keys: `id`
Foreign keys: `users.id`, `reputation_rules.id`, `activity_logs.id`

20
`student_behaviors`
Stores behavior analytics records used for AI-based user behavior analysis.
Primary keys: `id`

21
`complaints`
Stores complaints submitted by users and resolution information handled by staff.
Primary keys: `id`
Foreign keys: `users.id`

22
`feedbacks`
Stores user feedback after library usage, AI categorization results, and review status.
Primary keys: `id`
Foreign keys: `users.id`

23
`seat_status_reports`
Stores reports about broken, dirty, or unavailable seats and their verification workflow.
Primary keys: `id`
Foreign keys: `users.id`, `seats.seat_id`

24
`seat_violation_reports`
Stores reports of seat usage violations, reporter/violator information, evidence, and verification results.
Primary keys: `id`
Foreign keys: `users.id`, `seats.seat_id`

25
`notifications`
Stores notifications sent to users, including reference type, reference ID, read state, and message content.
Primary keys: `id`
Foreign keys: `users.id`

26
`categories`
Stores legacy or shared category records used by news-related data.
Primary keys: `id`

27
`news_categories`
Stores news and announcement category definitions.
Primary keys: `id`

28
`news`
Stores news and announcement content, publishing state, pinning state, view count, author, category, and PDF attachment metadata.
Primary keys: `id`
Foreign keys: `users.id`, `categories.id`, `news_categories.id`

29
`new_books`
Stores newly added book information displayed in the library portal and mobile app.
Primary keys: `id`
Foreign keys: `users.id`

30
`kiosk_configs`
Stores kiosk device configuration, activation tokens, QR secret data, and device activity timestamps.
Primary keys: `id`

31
`kiosk_activation_codes`
Stores temporary activation codes used to register kiosk devices.
Primary keys: `id`
Foreign keys: `kiosk_configs.id`

32
`kiosk_qr_sessions`
Stores QR-code access sessions created by kiosks for student check-in/check-out flows.
Primary keys: `id`
Foreign keys: `kiosk_configs.id`, `users.id`

33
`kiosk_images`
Stores kiosk slideshow images, display order, duration, Cloudinary public ID, and active state.
Primary keys: `id`

34
`library_maps`
Stores uploaded library map images and metadata for kiosk or map display.
Primary keys: `id`

35
`zone_maps`
Stores visual zone overlays attached to uploaded library maps.
Primary keys: `id`
Foreign keys: `library_maps.id`

36
`layout_drafts`
Stores unpublished layout draft snapshots created by administrators.
Primary keys: `draft_id`

37
`layout_history`
Stores published layout history snapshots and audit summaries.
Primary keys: `history_id`

38
`layout_schedules`
Stores scheduled layout publishing jobs, retry counters, cancellation state, and execution metadata.
Primary keys: `schedule_id`

39
`ai_config`
Stores AI provider configuration, model settings, generation parameters, and system prompt.
Primary keys: `id`

40
`ai_materials`
Stores AI material collections created for knowledge management.
Primary keys: `id`

41
`ai_material_items`
Stores individual text or file items belonging to AI material collections.
Primary keys: `id`
Foreign keys: `ai_materials.id`

42
`ai_knowledge_stores`
Stores AI knowledge store definitions and synchronization status.
Primary keys: `id`

43
`ai_knowledge_store_items`
Maps AI knowledge stores to material items used for retrieval.
Primary keys: `knowledge_store_id`, `material_item_id`
Foreign keys: `ai_knowledge_stores.id`, `ai_material_items.id`

44
`knowledge_base`
Stores static AI knowledge base content such as rules, guides, and information articles.
Primary keys: `id`

45
`prompt_templates`
Stores reusable AI prompt templates for different chatbot contexts.
Primary keys: `id`

46
`chat_sessions`
Stores legacy AI chat sessions between users and the chatbot.
Primary keys: `id`
Foreign keys: `users.id`

47
`chat_messages`
Stores messages belonging to legacy AI chat sessions.
Primary keys: `id`
Foreign keys: `chat_sessions.id`

48
`conversations`
Stores student support conversations, including AI handling, queue state, librarian assignment, and resolution timestamps.
Primary keys: `id`
Foreign keys: `users.id`

49
`messages`
Stores student-librarian conversation messages, attachments, read state, sender/receiver, and human session ID.
Primary keys: `id`
Foreign keys: `conversations.id`, `users.id`

50
`support_requests`
Stores support requests submitted by students and resolution responses from librarians.
Primary keys: `id`
Foreign keys: `users.id`

51
`backup_schedules`
Stores automatic backup schedules, cron expressions, backup type, retention policy, and active state.
Primary keys: `id`

52
`backup_history`
Stores backup execution history, generated file path, file size, status, and error message.
Primary keys: `id`
Foreign keys: `backup_schedules.id`, `users.id`

53
`system_logs`
Stores system audit logs, error logs, source, category, actor, request metadata, and structured details.
Primary keys: `id`
Foreign keys: `users.id`

54
`seed_records`
Tracks seeded data records to prevent duplicate initialization across seed scopes.
Primary keys: `id`

55
`flyway_schema_history`
Stores Flyway database migration history and execution status.
Primary keys: `installed_rank`

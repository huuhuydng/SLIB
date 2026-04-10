# Current Database Design

This document captures the latest relational database design used by the current SLIB system in `dbdiagram.io` compatible `DBML` format.

## Scope

- Source of truth:
  - `backend/src/main/resources/db/migration`
  - `backend/src/main/java/slib/com/example/entity`
- This design focuses on the current PostgreSQL relational schema.
- MongoDB, Qdrant, and Redis are excluded because they are not part of the relational database model rendered in `dbdiagram.io`.

## DBML Code

```dbml
Table users {
  id uuid [pk]
  user_code varchar(20) [not null, unique]
  username varchar(50) [unique]
  password varchar(255)
  full_name varchar(255) [not null]
  email varchar(255) [not null, unique]
  dob date
  phone varchar(20)
  avt_url text
  role varchar(20) [not null]
  is_active boolean
  lock_reason text
  password_changed boolean
  noti_device varchar(255)
  notify_booking boolean
  notify_reminder boolean
  notify_news boolean
  reputation_score int
  created_at timestamp
  updated_at timestamp
}

Table user_settings {
  user_id uuid [pk]
  is_hce_enabled boolean
  is_ai_recommend_enabled boolean
  is_booking_remind_enabled boolean
  theme_mode varchar(20)
  language_code varchar(10)
  created_at timestamp
  updated_at timestamp
}

Table student_profiles {
  user_id uuid [pk]
  reputation_score int
  total_study_hours int
  violation_count int
  created_at timestamp
  updated_at timestamp
}

Table refresh_tokens {
  id uuid [pk]
  user_id uuid [not null]
  token_hash varchar(255) [not null, unique]
  expires_at timestamp
  revoked boolean
  device_info varchar(255)
  created_at timestamp
  updated_at timestamp
}

Table otp_tokens {
  id uuid [pk]
  email varchar(255) [not null]
  token varchar(255) [not null]
  type varchar(30) [not null]
  is_used boolean [not null]
  expires_at timestamp [not null]
  created_at timestamp
}

Table import_jobs {
  id uuid [pk]
  file_name varchar(255)
  status varchar(50)
  total_rows int
  success_rows int
  failed_rows int
  created_at timestamp
  completed_at timestamp
}

Table user_import_staging {
  id uuid [pk]
  batch_id uuid
  row_number int
  user_code varchar(50)
  email varchar(255)
  full_name varchar(255)
  phone varchar(20)
  dob date
  role varchar(20)
  avt_url text
  status varchar(50)
  error_message text
  created_at timestamp
  processed_at timestamp
}

Table areas {
  area_id bigint [pk]
  area_name varchar(255) [not null, unique]
  width int [not null]
  height int [not null]
  position_x int
  position_y int
  is_active boolean [not null]
  locked boolean [not null]
}

Table area_factories {
  factory_id bigint [pk]
  area_id bigint [not null]
  factory_name varchar(255) [not null]
  width int [not null]
  height int [not null]
  position_x int [not null]
  position_y int [not null]
  is_locked boolean [not null]
}

Table zones {
  zone_id int [pk]
  area_id bigint [not null]
  zone_name varchar(255) [not null]
  zone_des text
  position_x int [not null]
  position_y int [not null]
  width int [not null]
  height int [not null]
  is_locked boolean [not null]
}

Table zone_amenities {
  amenity_id int [pk]
  zone_id int [not null]
  amenity_name varchar(100) [not null]
}

Table seats {
  seat_id int [pk]
  zone_id int [not null]
  seat_code varchar(255) [not null]
  seat_status varchar(20)
  row_number int [not null]
  column_number int [not null]
  is_active boolean
  held_by_user uuid
  hold_expires_at timestamp
  nfc_tag_uid varchar(255) [unique]
  nfc_tag_uid_updated_at timestamp
}

Table library_settings {
  id int [pk]
  open_time varchar(10)
  close_time varchar(10)
  slot_duration int
  max_booking_days int
  working_days varchar(50)
  max_bookings_per_day int
  max_hours_per_day int
  auto_cancel_minutes int
  auto_cancel_on_leave_minutes int
  min_reputation int
  library_closed boolean
  closed_reason varchar(500)
  notify_booking_success boolean
  notify_checkin_reminder boolean
  notify_time_expiry boolean
  notify_violation boolean
  notify_weekly_report boolean
  notify_device_alert boolean
}

Table hce_devices {
  id int [pk]
  device_id varchar(50) [not null, unique]
  device_name varchar(100) [not null]
  location varchar(200)
  device_type varchar(20) [not null]
  status varchar(20) [not null]
  last_heartbeat timestamp
  area_id bigint
  created_at timestamp
  updated_at timestamp
}

Table reservations {
  reservation_id uuid [pk]
  user_id uuid [not null]
  seat_id int [not null]
  start_time timestamp [not null]
  end_time timestamp [not null]
  confirmed_at timestamp
  actual_end_time timestamp
  status varchar(50) [not null]
  created_at timestamp
}

Table access_logs {
  log_id uuid [pk]
  user_id uuid
  reservation_id uuid
  seat_id int
  device_id varchar(50) [not null]
  check_in_time timestamp [not null]
  check_out_time timestamp
}

Table activity_logs {
  id uuid [pk]
  user_id uuid [not null]
  activity_type varchar(50) [not null]
  description text
  reservation_id uuid
  seat_code varchar(20)
  zone_name varchar(100)
  duration_minutes int
  created_at timestamp
}

Table reputation_rules {
  id int [pk]
  rule_code varchar(50) [not null, unique]
  rule_name varchar(200) [not null]
  description text
  points int [not null]
  rule_type varchar(20) [not null]
  is_active boolean [not null]
  created_at timestamp
  updated_at timestamp
}

Table point_transactions {
  id uuid [pk]
  user_id uuid [not null]
  points int [not null]
  transaction_type varchar(50) [not null]
  description text
  balance_after int
  activity_log_id uuid
  rule_id int
  created_at timestamp
}

Table news_categories {
  id bigint [pk]
  name varchar(50) [not null, unique]
  color_code varchar(20)
  created_at timestamp
}

Table news {
  id bigint [pk]
  category_id bigint
  title varchar(255) [not null]
  summary text
  content text [not null]
  image_url text
  is_published boolean [not null]
  is_pinned boolean [not null]
  view_count int [not null]
  published_at timestamp
  created_at timestamp
  updated_at timestamp
}

Table new_books {
  id bigint [pk]
  title varchar(300) [not null]
  author varchar(200)
  isbn varchar(20)
  cover_url text
  description text
  category varchar(255)
  source_url text
  publisher varchar(255)
  publish_date date
  arrival_date date
  is_active boolean [not null]
  is_pinned boolean [not null]
  created_by uuid
  created_at timestamp
  updated_at timestamp
}

Table notifications {
  id uuid [pk]
  user_id uuid [not null]
  title varchar(200) [not null]
  content text [not null]
  notification_type varchar(50) [not null]
  reference_type varchar(50)
  reference_id uuid
  is_read boolean [not null]
  created_at timestamp
}

Table feedbacks {
  id uuid [pk]
  user_id uuid [not null]
  reservation_id uuid
  conversation_id varchar(255)
  rating int
  content text
  category varchar(50)
  ai_category_confidence decimal(3,2)
  status varchar(20) [not null]
  reviewed_by uuid
  created_at timestamp
  reviewed_at timestamp
}

Table seat_status_reports {
  id uuid [pk]
  user_id uuid [not null]
  seat_id int [not null]
  issue_type varchar(50) [not null]
  description text
  image_url text
  status varchar(20) [not null]
  verified_by uuid
  created_at timestamp
  verified_at timestamp
  resolved_at timestamp
}

Table seat_violation_reports {
  id uuid [pk]
  reporter_id uuid [not null]
  violator_id uuid
  seat_id int [not null]
  reservation_id uuid
  violation_type varchar(50) [not null]
  description text
  evidence_url text
  status varchar(20) [not null]
  verified_by uuid
  point_deducted int
  created_at timestamp
  verified_at timestamp
}

Table complaints {
  id uuid [pk]
  user_id uuid [not null]
  point_transaction_id uuid
  violation_report_id uuid
  subject varchar(200) [not null]
  content text [not null]
  evidence_url text
  status varchar(20) [not null]
  resolution_note text
  resolved_by uuid
  created_at timestamp
  resolved_at timestamp
}

Table conversations {
  id uuid [pk]
  student_id uuid [not null]
  librarian_id uuid
  status varchar(30) [not null]
  escalation_reason text
  created_at timestamp
  updated_at timestamp
  escalated_at timestamp
  resolved_at timestamp
  student_cleared_at timestamp
  current_human_session int
  ai_session_id varchar(255)
}

Table messages {
  id uuid [pk]
  sender_id uuid [not null]
  receiver_id uuid [not null]
  conversation_id uuid
  content text
  attachment_url text
  message_type varchar(20) [not null]
  sender_type varchar(20)
  human_session_id int
  is_read boolean [not null]
  created_at timestamp
}

Table support_requests {
  id uuid [pk]
  student_id uuid [not null]
  description text [not null]
  image_urls text[]
  status varchar(20) [not null]
  admin_response text
  resolved_by uuid
  created_at timestamp
  updated_at timestamp
  resolved_at timestamp
}

Table kiosk_images {
  id int [pk]
  image_name varchar(200) [not null]
  image_url text [not null]
  public_id varchar(255)
  display_order int [not null]
  is_active boolean [not null]
  duration_seconds int [not null]
  created_at timestamp
  updated_at timestamp
}

Table library_maps {
  id int [pk]
  map_name varchar(200) [not null]
  map_image_url text [not null]
  public_id varchar(255)
  description text
  is_active boolean [not null]
  created_at timestamp
  updated_at timestamp
}

Table zone_maps {
  id int [pk]
  library_map_id int [not null]
  zone_name varchar(150) [not null]
  zone_type varchar(100) [not null]
  x_position int [not null]
  y_position int [not null]
  width int [not null]
  height int [not null]
  color_code varchar(20)
  is_interactive boolean [not null]
  created_at timestamp
  updated_at timestamp
}

Table kiosk_configs {
  id int [pk]
  kiosk_code varchar(50) [not null, unique]
  kiosk_name varchar(200) [not null]
  kiosk_type varchar(50) [not null]
  location varchar(255)
  is_active boolean [not null]
  qr_secret_key varchar(255)
  device_token text
  device_token_issued_at timestamp
  device_token_expires_at timestamp
  device_token_issued_by uuid
  last_active_at timestamp
  created_at timestamp
  updated_at timestamp
}

Table kiosk_qr_sessions {
  id int [pk]
  kiosk_id int [not null]
  student_id uuid
  access_log_id uuid
  session_token varchar(255) [not null, unique]
  qr_payload text [not null]
  qr_expires_at timestamp [not null]
  status varchar(20) [not null]
  created_at timestamp
  updated_at timestamp
}

Table kiosk_activation_codes {
  id int [pk]
  kiosk_id int [not null]
  code varchar(20) [not null, unique]
  device_token text [not null]
  expires_at timestamp [not null]
  used boolean [not null]
  created_at timestamp
}

Table ai_config {
  id int [pk]
  config_key varchar(100) [not null, unique]
  config_value text [not null]
  description varchar(500)
  created_at timestamp
  updated_at timestamp
}

Table ai_knowledge_stores {
  id bigint [pk]
  name varchar(255) [not null]
  description text
  created_by varchar(100)
  store_type varchar(20)
  status varchar(20)
  active boolean
  is_active boolean
  document_count int
  last_synced_at timestamp
  created_at timestamp
  updated_at timestamp
}

Table ai_materials {
  id bigint [pk]
  knowledge_store_id bigint
  name varchar(255) [not null]
  description text
  created_by varchar(100)
  active boolean
  is_active boolean
  created_at timestamp
  updated_at timestamp
}

Table ai_material_items {
  id bigint [pk]
  material_id bigint [not null]
  name varchar(255) [not null]
  type varchar(20) [not null]
  content text
  file_name varchar(255)
  file_path varchar(500)
  file_size bigint
  created_at timestamp
}

Table ai_knowledge_store_items {
  knowledge_store_id bigint [not null]
  material_item_id bigint [not null]
}

Table chat_sessions {
  id uuid [pk]
  user_id uuid [not null]
  title varchar(255)
  is_active boolean [not null]
  created_at timestamp
  updated_at timestamp
}

Table chat_messages {
  id uuid [pk]
  session_id uuid [not null]
  role varchar(20) [not null]
  content text [not null]
  tokens_used int
  created_at timestamp
}

Table knowledge_base {
  id bigint [pk]
  title varchar(200)
  question text
  answer text
  content text
  category varchar(50)
  type varchar(20)
  priority int
  is_active boolean [not null]
  created_at timestamp
  updated_at timestamp
}

Table prompt_templates {
  id bigint [pk]
  name varchar(100) [not null]
  prompt text [not null]
  context varchar(20) [not null]
  is_active boolean [not null]
  created_at timestamp
  updated_at timestamp
}

Table student_behaviors {
  id int [pk]
  user_id uuid [not null]
  behavior_type varchar(50) [not null]
  description text
  related_booking_id uuid
  related_seat_id int
  related_zone_id int
  points_impact int
  metadata text
  created_at timestamp
  updated_at timestamp
}

Table backup_schedules {
  id int [pk]
  enabled boolean
  frequency varchar(20)
  backup_time varchar(10)
  retain_days int
  created_at timestamp
  updated_at timestamp
}

Table backup_history {
  id int [pk]
  schedule_id int
  file_name varchar(255)
  file_path text
  file_size bigint
  status varchar(50)
  error_message text
  created_by uuid
  created_at timestamp
  completed_at timestamp
}

Table system_logs {
  id uuid [pk]
  user_id uuid
  level varchar(20)
  service varchar(100)
  message text [not null]
  actor_email varchar(255)
  source varchar(20)
  reference_id varchar(255)
  action varchar(100)
  entity_type varchar(50)
  entity_id varchar(100)
  details jsonb
  ip_address varchar(45)
  user_agent text
  created_at timestamp
}

Table seed_records {
  id bigint [pk]
  entity_type varchar(60) [not null]
  entity_id varchar(100) [not null]
  seed_scope varchar(120) [not null]
  created_at timestamp [not null]
}

Table layout_drafts {
  draft_id bigint [pk]
  snapshot_json text [not null]
  based_on_published_version bigint
  updated_by_user_id uuid
  updated_by_name varchar(255)
  updated_at timestamp [not null]
}

Table layout_history {
  history_id bigint [pk]
  action_type varchar(32) [not null]
  summary text
  snapshot_json text [not null]
  published_version bigint
  created_by_user_id uuid
  created_by_name varchar(255)
  created_at timestamp [not null]
}

Ref: user_settings.user_id > users.id
Ref: student_profiles.user_id > users.id
Ref: refresh_tokens.user_id > users.id
Ref: area_factories.area_id > areas.area_id
Ref: zones.area_id > areas.area_id
Ref: zone_amenities.zone_id > zones.zone_id
Ref: seats.zone_id > zones.zone_id
Ref: seats.held_by_user > users.id
Ref: hce_devices.area_id > areas.area_id
Ref: reservations.user_id > users.id
Ref: reservations.seat_id > seats.seat_id
Ref: access_logs.user_id > users.id
Ref: access_logs.reservation_id > reservations.reservation_id
Ref: access_logs.seat_id > seats.seat_id
Ref: activity_logs.user_id > users.id
Ref: activity_logs.reservation_id > reservations.reservation_id
Ref: point_transactions.user_id > users.id
Ref: point_transactions.activity_log_id > activity_logs.id
Ref: point_transactions.rule_id > reputation_rules.id
Ref: news.category_id > news_categories.id
Ref: new_books.created_by > users.id
Ref: notifications.user_id > users.id
Ref: feedbacks.user_id > users.id
Ref: feedbacks.reservation_id > reservations.reservation_id
Ref: feedbacks.reviewed_by > users.id
Ref: seat_status_reports.user_id > users.id
Ref: seat_status_reports.seat_id > seats.seat_id
Ref: seat_status_reports.verified_by > users.id
Ref: seat_violation_reports.reporter_id > users.id
Ref: seat_violation_reports.violator_id > users.id
Ref: seat_violation_reports.seat_id > seats.seat_id
Ref: seat_violation_reports.reservation_id > reservations.reservation_id
Ref: seat_violation_reports.verified_by > users.id
Ref: complaints.user_id > users.id
Ref: complaints.point_transaction_id > point_transactions.id
Ref: complaints.violation_report_id > seat_violation_reports.id
Ref: complaints.resolved_by > users.id
Ref: conversations.student_id > users.id
Ref: conversations.librarian_id > users.id
Ref: messages.sender_id > users.id
Ref: messages.receiver_id > users.id
Ref: messages.conversation_id > conversations.id
Ref: support_requests.student_id > users.id
Ref: support_requests.resolved_by > users.id
Ref: zone_maps.library_map_id > library_maps.id
Ref: kiosk_qr_sessions.kiosk_id > kiosk_configs.id
Ref: kiosk_qr_sessions.student_id > users.id
Ref: kiosk_qr_sessions.access_log_id > access_logs.log_id
Ref: kiosk_activation_codes.kiosk_id > kiosk_configs.id
Ref: kiosk_configs.device_token_issued_by > users.id
Ref: ai_materials.knowledge_store_id > ai_knowledge_stores.id
Ref: ai_material_items.material_id > ai_materials.id
Ref: ai_knowledge_store_items.knowledge_store_id > ai_knowledge_stores.id
Ref: ai_knowledge_store_items.material_item_id > ai_material_items.id
Ref: chat_sessions.user_id > users.id
Ref: chat_messages.session_id > chat_sessions.id
Ref: student_behaviors.user_id > users.id
Ref: student_behaviors.related_booking_id > reservations.reservation_id
Ref: student_behaviors.related_seat_id > seats.seat_id
Ref: student_behaviors.related_zone_id > zones.zone_id
Ref: backup_history.schedule_id > backup_schedules.id
Ref: backup_history.created_by > users.id
Ref: system_logs.user_id > users.id
Ref: layout_drafts.updated_by_user_id > users.id
Ref: layout_history.created_by_user_id > users.id
```

## Notes

- Some tables have evolved across multiple migrations, so there are a few legacy-compatible fields still present in the schema.
- `notifications.reference_id`, `feedbacks.conversation_id`, and some analytics fields act as soft references in business logic.
- Human support chat and AI chat are modeled separately with `conversations/messages` and `chat_sessions/chat_messages`.

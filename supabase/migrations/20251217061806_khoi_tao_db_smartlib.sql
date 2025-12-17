create sequence "public"."feedbacks_feedback_id_seq";

create sequence "public"."reputation_history_history_id_seq";

create sequence "public"."seats_seat_id_seq";

create sequence "public"."student_schedules_schedule_id_seq";

create sequence "public"."zones_zone_id_seq";


  create table "public"."access_logs" (
    "log_id" uuid not null,
    "user_id" uuid not null,
    "reservation_id" uuid,
    "check_in_time" timestamp without time zone not null,
    "check_out_time" timestamp without time zone not null,
    "device_id" character varying(255) not null
      );


alter table "public"."access_logs" enable row level security;


  create table "public"."chat_logs" (
    "message_id" uuid not null,
    "user_id" uuid not null,
    "sender" character varying(255) not null,
    "message_content" text not null,
    "timestamp" timestamp without time zone not null
      );


alter table "public"."chat_logs" enable row level security;


  create table "public"."feedbacks" (
    "feedback_id" integer not null default nextval('public.feedbacks_feedback_id_seq'::regclass),
    "user_id" uuid not null,
    "content" text not null,
    "sentiment_score" numeric not null,
    "category" character varying(255) not null,
    "status" character varying(255) not null,
    "created_at" timestamp without time zone not null
      );


alter table "public"."feedbacks" enable row level security;


  create table "public"."notifications" (
    "notification_id" uuid not null,
    "user_id" uuid not null,
    "title" character varying(255) not null,
    "message" text not null,
    "is_read" boolean not null,
    "type" character varying(255) not null,
    "created_at" timestamp without time zone not null
      );


alter table "public"."notifications" enable row level security;


  create table "public"."reputation_history" (
    "history_id" integer not null default nextval('public.reputation_history_history_id_seq'::regclass),
    "user_id" uuid not null,
    "change_amount" integer not null,
    "reason" character varying(255) not null,
    "created_at" timestamp without time zone not null
      );


alter table "public"."reputation_history" enable row level security;


  create table "public"."reservations" (
    "reservation_id" uuid not null,
    "user_id" uuid not null,
    "seat_id" integer not null,
    "start_time" timestamp without time zone not null,
    "end_time" timestamp without time zone not null,
    "status" character varying(20) not null,
    "created_at" timestamp without time zone not null
      );


alter table "public"."reservations" enable row level security;


  create table "public"."seats" (
    "seat_id" integer not null default nextval('public.seats_seat_id_seq'::regclass),
    "zone_id" integer not null,
    "seat_code" character varying(255) not null,
    "is_active" boolean not null,
    "position_x" integer not null,
    "position_y" integer not null
      );


alter table "public"."seats" enable row level security;


  create table "public"."student_schedules" (
    "schedule_id" integer not null default nextval('public.student_schedules_schedule_id_seq'::regclass),
    "user_id" uuid not null,
    "day_of_week" integer not null,
    "start_time" time without time zone not null,
    "end_time" time without time zone not null
      );


alter table "public"."student_schedules" enable row level security;


  create table "public"."users" (
    "user_id" uuid not null,
    "student_code" character varying(10),
    "full_name" character varying(255) not null,
    "email" character varying(255) not null,
    "password" character varying(255) not null,
    "role" character varying(255) not null,
    "reputation_score" integer,
    "noti_device" character varying(255),
    "created_at" timestamp without time zone not null
      );


alter table "public"."users" enable row level security;


  create table "public"."zones" (
    "zone_id" integer not null default nextval('public.zones_zone_id_seq'::regclass),
    "zone_name" character varying(255) not null,
    "zone_des" text,
    "has_power_outlet" boolean not null
      );


alter table "public"."zones" enable row level security;

alter sequence "public"."feedbacks_feedback_id_seq" owned by "public"."feedbacks"."feedback_id";

alter sequence "public"."reputation_history_history_id_seq" owned by "public"."reputation_history"."history_id";

alter sequence "public"."seats_seat_id_seq" owned by "public"."seats"."seat_id";

alter sequence "public"."student_schedules_schedule_id_seq" owned by "public"."student_schedules"."schedule_id";

alter sequence "public"."zones_zone_id_seq" owned by "public"."zones"."zone_id";

CREATE UNIQUE INDEX access_logs_pkey ON public.access_logs USING btree (log_id);

CREATE UNIQUE INDEX chat_logs_pkey ON public.chat_logs USING btree (message_id);

CREATE UNIQUE INDEX feedbacks_pkey ON public.feedbacks USING btree (feedback_id);

CREATE UNIQUE INDEX notifications_pkey ON public.notifications USING btree (notification_id);

CREATE UNIQUE INDEX reputation_history_pkey ON public.reputation_history USING btree (history_id);

CREATE UNIQUE INDEX reservations_pkey ON public.reservations USING btree (reservation_id);

CREATE UNIQUE INDEX seats_pkey ON public.seats USING btree (seat_id);

CREATE UNIQUE INDEX student_schedules_pkey ON public.student_schedules USING btree (schedule_id);

CREATE UNIQUE INDEX users_noti_device_key ON public.users USING btree (noti_device);

CREATE UNIQUE INDEX users_pkey ON public.users USING btree (user_id);

CREATE UNIQUE INDEX users_student_code_key ON public.users USING btree (student_code);

CREATE UNIQUE INDEX zones_pkey ON public.zones USING btree (zone_id);

alter table "public"."access_logs" add constraint "access_logs_pkey" PRIMARY KEY using index "access_logs_pkey";

alter table "public"."chat_logs" add constraint "chat_logs_pkey" PRIMARY KEY using index "chat_logs_pkey";

alter table "public"."feedbacks" add constraint "feedbacks_pkey" PRIMARY KEY using index "feedbacks_pkey";

alter table "public"."notifications" add constraint "notifications_pkey" PRIMARY KEY using index "notifications_pkey";

alter table "public"."reputation_history" add constraint "reputation_history_pkey" PRIMARY KEY using index "reputation_history_pkey";

alter table "public"."reservations" add constraint "reservations_pkey" PRIMARY KEY using index "reservations_pkey";

alter table "public"."seats" add constraint "seats_pkey" PRIMARY KEY using index "seats_pkey";

alter table "public"."student_schedules" add constraint "student_schedules_pkey" PRIMARY KEY using index "student_schedules_pkey";

alter table "public"."users" add constraint "users_pkey" PRIMARY KEY using index "users_pkey";

alter table "public"."zones" add constraint "zones_pkey" PRIMARY KEY using index "zones_pkey";

alter table "public"."access_logs" add constraint "access_logs_reservation_id_fkey" FOREIGN KEY (reservation_id) REFERENCES public.reservations(reservation_id) not valid;

alter table "public"."access_logs" validate constraint "access_logs_reservation_id_fkey";

alter table "public"."access_logs" add constraint "access_logs_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(user_id) not valid;

alter table "public"."access_logs" validate constraint "access_logs_user_id_fkey";

alter table "public"."chat_logs" add constraint "chat_logs_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(user_id) not valid;

alter table "public"."chat_logs" validate constraint "chat_logs_user_id_fkey";

alter table "public"."feedbacks" add constraint "feedbacks_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(user_id) not valid;

alter table "public"."feedbacks" validate constraint "feedbacks_user_id_fkey";

alter table "public"."notifications" add constraint "notifications_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(user_id) not valid;

alter table "public"."notifications" validate constraint "notifications_user_id_fkey";

alter table "public"."reputation_history" add constraint "reputation_history_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(user_id) not valid;

alter table "public"."reputation_history" validate constraint "reputation_history_user_id_fkey";

alter table "public"."reservations" add constraint "reservations_seat_id_fkey" FOREIGN KEY (seat_id) REFERENCES public.seats(seat_id) not valid;

alter table "public"."reservations" validate constraint "reservations_seat_id_fkey";

alter table "public"."reservations" add constraint "reservations_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(user_id) not valid;

alter table "public"."reservations" validate constraint "reservations_user_id_fkey";

alter table "public"."seats" add constraint "seats_zone_id_fkey" FOREIGN KEY (zone_id) REFERENCES public.zones(zone_id) not valid;

alter table "public"."seats" validate constraint "seats_zone_id_fkey";

alter table "public"."student_schedules" add constraint "student_schedules_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(user_id) not valid;

alter table "public"."student_schedules" validate constraint "student_schedules_user_id_fkey";

alter table "public"."users" add constraint "users_noti_device_key" UNIQUE using index "users_noti_device_key";

alter table "public"."users" add constraint "users_student_code_key" UNIQUE using index "users_student_code_key";

grant delete on table "public"."access_logs" to "anon";

grant insert on table "public"."access_logs" to "anon";

grant references on table "public"."access_logs" to "anon";

grant select on table "public"."access_logs" to "anon";

grant trigger on table "public"."access_logs" to "anon";

grant truncate on table "public"."access_logs" to "anon";

grant update on table "public"."access_logs" to "anon";

grant delete on table "public"."access_logs" to "authenticated";

grant insert on table "public"."access_logs" to "authenticated";

grant references on table "public"."access_logs" to "authenticated";

grant select on table "public"."access_logs" to "authenticated";

grant trigger on table "public"."access_logs" to "authenticated";

grant truncate on table "public"."access_logs" to "authenticated";

grant update on table "public"."access_logs" to "authenticated";

grant delete on table "public"."access_logs" to "postgres";

grant insert on table "public"."access_logs" to "postgres";

grant references on table "public"."access_logs" to "postgres";

grant select on table "public"."access_logs" to "postgres";

grant trigger on table "public"."access_logs" to "postgres";

grant truncate on table "public"."access_logs" to "postgres";

grant update on table "public"."access_logs" to "postgres";

grant delete on table "public"."access_logs" to "service_role";

grant insert on table "public"."access_logs" to "service_role";

grant references on table "public"."access_logs" to "service_role";

grant select on table "public"."access_logs" to "service_role";

grant trigger on table "public"."access_logs" to "service_role";

grant truncate on table "public"."access_logs" to "service_role";

grant update on table "public"."access_logs" to "service_role";

grant delete on table "public"."chat_logs" to "anon";

grant insert on table "public"."chat_logs" to "anon";

grant references on table "public"."chat_logs" to "anon";

grant select on table "public"."chat_logs" to "anon";

grant trigger on table "public"."chat_logs" to "anon";

grant truncate on table "public"."chat_logs" to "anon";

grant update on table "public"."chat_logs" to "anon";

grant delete on table "public"."chat_logs" to "authenticated";

grant insert on table "public"."chat_logs" to "authenticated";

grant references on table "public"."chat_logs" to "authenticated";

grant select on table "public"."chat_logs" to "authenticated";

grant trigger on table "public"."chat_logs" to "authenticated";

grant truncate on table "public"."chat_logs" to "authenticated";

grant update on table "public"."chat_logs" to "authenticated";

grant delete on table "public"."chat_logs" to "postgres";

grant insert on table "public"."chat_logs" to "postgres";

grant references on table "public"."chat_logs" to "postgres";

grant select on table "public"."chat_logs" to "postgres";

grant trigger on table "public"."chat_logs" to "postgres";

grant truncate on table "public"."chat_logs" to "postgres";

grant update on table "public"."chat_logs" to "postgres";

grant delete on table "public"."chat_logs" to "service_role";

grant insert on table "public"."chat_logs" to "service_role";

grant references on table "public"."chat_logs" to "service_role";

grant select on table "public"."chat_logs" to "service_role";

grant trigger on table "public"."chat_logs" to "service_role";

grant truncate on table "public"."chat_logs" to "service_role";

grant update on table "public"."chat_logs" to "service_role";

grant delete on table "public"."feedbacks" to "anon";

grant insert on table "public"."feedbacks" to "anon";

grant references on table "public"."feedbacks" to "anon";

grant select on table "public"."feedbacks" to "anon";

grant trigger on table "public"."feedbacks" to "anon";

grant truncate on table "public"."feedbacks" to "anon";

grant update on table "public"."feedbacks" to "anon";

grant delete on table "public"."feedbacks" to "authenticated";

grant insert on table "public"."feedbacks" to "authenticated";

grant references on table "public"."feedbacks" to "authenticated";

grant select on table "public"."feedbacks" to "authenticated";

grant trigger on table "public"."feedbacks" to "authenticated";

grant truncate on table "public"."feedbacks" to "authenticated";

grant update on table "public"."feedbacks" to "authenticated";

grant delete on table "public"."feedbacks" to "postgres";

grant insert on table "public"."feedbacks" to "postgres";

grant references on table "public"."feedbacks" to "postgres";

grant select on table "public"."feedbacks" to "postgres";

grant trigger on table "public"."feedbacks" to "postgres";

grant truncate on table "public"."feedbacks" to "postgres";

grant update on table "public"."feedbacks" to "postgres";

grant delete on table "public"."feedbacks" to "service_role";

grant insert on table "public"."feedbacks" to "service_role";

grant references on table "public"."feedbacks" to "service_role";

grant select on table "public"."feedbacks" to "service_role";

grant trigger on table "public"."feedbacks" to "service_role";

grant truncate on table "public"."feedbacks" to "service_role";

grant update on table "public"."feedbacks" to "service_role";

grant delete on table "public"."notifications" to "anon";

grant insert on table "public"."notifications" to "anon";

grant references on table "public"."notifications" to "anon";

grant select on table "public"."notifications" to "anon";

grant trigger on table "public"."notifications" to "anon";

grant truncate on table "public"."notifications" to "anon";

grant update on table "public"."notifications" to "anon";

grant delete on table "public"."notifications" to "authenticated";

grant insert on table "public"."notifications" to "authenticated";

grant references on table "public"."notifications" to "authenticated";

grant select on table "public"."notifications" to "authenticated";

grant trigger on table "public"."notifications" to "authenticated";

grant truncate on table "public"."notifications" to "authenticated";

grant update on table "public"."notifications" to "authenticated";

grant delete on table "public"."notifications" to "postgres";

grant insert on table "public"."notifications" to "postgres";

grant references on table "public"."notifications" to "postgres";

grant select on table "public"."notifications" to "postgres";

grant trigger on table "public"."notifications" to "postgres";

grant truncate on table "public"."notifications" to "postgres";

grant update on table "public"."notifications" to "postgres";

grant delete on table "public"."notifications" to "service_role";

grant insert on table "public"."notifications" to "service_role";

grant references on table "public"."notifications" to "service_role";

grant select on table "public"."notifications" to "service_role";

grant trigger on table "public"."notifications" to "service_role";

grant truncate on table "public"."notifications" to "service_role";

grant update on table "public"."notifications" to "service_role";

grant delete on table "public"."reputation_history" to "anon";

grant insert on table "public"."reputation_history" to "anon";

grant references on table "public"."reputation_history" to "anon";

grant select on table "public"."reputation_history" to "anon";

grant trigger on table "public"."reputation_history" to "anon";

grant truncate on table "public"."reputation_history" to "anon";

grant update on table "public"."reputation_history" to "anon";

grant delete on table "public"."reputation_history" to "authenticated";

grant insert on table "public"."reputation_history" to "authenticated";

grant references on table "public"."reputation_history" to "authenticated";

grant select on table "public"."reputation_history" to "authenticated";

grant trigger on table "public"."reputation_history" to "authenticated";

grant truncate on table "public"."reputation_history" to "authenticated";

grant update on table "public"."reputation_history" to "authenticated";

grant delete on table "public"."reputation_history" to "postgres";

grant insert on table "public"."reputation_history" to "postgres";

grant references on table "public"."reputation_history" to "postgres";

grant select on table "public"."reputation_history" to "postgres";

grant trigger on table "public"."reputation_history" to "postgres";

grant truncate on table "public"."reputation_history" to "postgres";

grant update on table "public"."reputation_history" to "postgres";

grant delete on table "public"."reputation_history" to "service_role";

grant insert on table "public"."reputation_history" to "service_role";

grant references on table "public"."reputation_history" to "service_role";

grant select on table "public"."reputation_history" to "service_role";

grant trigger on table "public"."reputation_history" to "service_role";

grant truncate on table "public"."reputation_history" to "service_role";

grant update on table "public"."reputation_history" to "service_role";

grant delete on table "public"."reservations" to "anon";

grant insert on table "public"."reservations" to "anon";

grant references on table "public"."reservations" to "anon";

grant select on table "public"."reservations" to "anon";

grant trigger on table "public"."reservations" to "anon";

grant truncate on table "public"."reservations" to "anon";

grant update on table "public"."reservations" to "anon";

grant delete on table "public"."reservations" to "authenticated";

grant insert on table "public"."reservations" to "authenticated";

grant references on table "public"."reservations" to "authenticated";

grant select on table "public"."reservations" to "authenticated";

grant trigger on table "public"."reservations" to "authenticated";

grant truncate on table "public"."reservations" to "authenticated";

grant update on table "public"."reservations" to "authenticated";

grant delete on table "public"."reservations" to "postgres";

grant insert on table "public"."reservations" to "postgres";

grant references on table "public"."reservations" to "postgres";

grant select on table "public"."reservations" to "postgres";

grant trigger on table "public"."reservations" to "postgres";

grant truncate on table "public"."reservations" to "postgres";

grant update on table "public"."reservations" to "postgres";

grant delete on table "public"."reservations" to "service_role";

grant insert on table "public"."reservations" to "service_role";

grant references on table "public"."reservations" to "service_role";

grant select on table "public"."reservations" to "service_role";

grant trigger on table "public"."reservations" to "service_role";

grant truncate on table "public"."reservations" to "service_role";

grant update on table "public"."reservations" to "service_role";

grant delete on table "public"."seats" to "anon";

grant insert on table "public"."seats" to "anon";

grant references on table "public"."seats" to "anon";

grant select on table "public"."seats" to "anon";

grant trigger on table "public"."seats" to "anon";

grant truncate on table "public"."seats" to "anon";

grant update on table "public"."seats" to "anon";

grant delete on table "public"."seats" to "authenticated";

grant insert on table "public"."seats" to "authenticated";

grant references on table "public"."seats" to "authenticated";

grant select on table "public"."seats" to "authenticated";

grant trigger on table "public"."seats" to "authenticated";

grant truncate on table "public"."seats" to "authenticated";

grant update on table "public"."seats" to "authenticated";

grant delete on table "public"."seats" to "postgres";

grant insert on table "public"."seats" to "postgres";

grant references on table "public"."seats" to "postgres";

grant select on table "public"."seats" to "postgres";

grant trigger on table "public"."seats" to "postgres";

grant truncate on table "public"."seats" to "postgres";

grant update on table "public"."seats" to "postgres";

grant delete on table "public"."seats" to "service_role";

grant insert on table "public"."seats" to "service_role";

grant references on table "public"."seats" to "service_role";

grant select on table "public"."seats" to "service_role";

grant trigger on table "public"."seats" to "service_role";

grant truncate on table "public"."seats" to "service_role";

grant update on table "public"."seats" to "service_role";

grant delete on table "public"."student_schedules" to "anon";

grant insert on table "public"."student_schedules" to "anon";

grant references on table "public"."student_schedules" to "anon";

grant select on table "public"."student_schedules" to "anon";

grant trigger on table "public"."student_schedules" to "anon";

grant truncate on table "public"."student_schedules" to "anon";

grant update on table "public"."student_schedules" to "anon";

grant delete on table "public"."student_schedules" to "authenticated";

grant insert on table "public"."student_schedules" to "authenticated";

grant references on table "public"."student_schedules" to "authenticated";

grant select on table "public"."student_schedules" to "authenticated";

grant trigger on table "public"."student_schedules" to "authenticated";

grant truncate on table "public"."student_schedules" to "authenticated";

grant update on table "public"."student_schedules" to "authenticated";

grant delete on table "public"."student_schedules" to "postgres";

grant insert on table "public"."student_schedules" to "postgres";

grant references on table "public"."student_schedules" to "postgres";

grant select on table "public"."student_schedules" to "postgres";

grant trigger on table "public"."student_schedules" to "postgres";

grant truncate on table "public"."student_schedules" to "postgres";

grant update on table "public"."student_schedules" to "postgres";

grant delete on table "public"."student_schedules" to "service_role";

grant insert on table "public"."student_schedules" to "service_role";

grant references on table "public"."student_schedules" to "service_role";

grant select on table "public"."student_schedules" to "service_role";

grant trigger on table "public"."student_schedules" to "service_role";

grant truncate on table "public"."student_schedules" to "service_role";

grant update on table "public"."student_schedules" to "service_role";

grant delete on table "public"."users" to "anon";

grant insert on table "public"."users" to "anon";

grant references on table "public"."users" to "anon";

grant select on table "public"."users" to "anon";

grant trigger on table "public"."users" to "anon";

grant truncate on table "public"."users" to "anon";

grant update on table "public"."users" to "anon";

grant delete on table "public"."users" to "authenticated";

grant insert on table "public"."users" to "authenticated";

grant references on table "public"."users" to "authenticated";

grant select on table "public"."users" to "authenticated";

grant trigger on table "public"."users" to "authenticated";

grant truncate on table "public"."users" to "authenticated";

grant update on table "public"."users" to "authenticated";

grant delete on table "public"."users" to "postgres";

grant insert on table "public"."users" to "postgres";

grant references on table "public"."users" to "postgres";

grant select on table "public"."users" to "postgres";

grant trigger on table "public"."users" to "postgres";

grant truncate on table "public"."users" to "postgres";

grant update on table "public"."users" to "postgres";

grant delete on table "public"."users" to "service_role";

grant insert on table "public"."users" to "service_role";

grant references on table "public"."users" to "service_role";

grant select on table "public"."users" to "service_role";

grant trigger on table "public"."users" to "service_role";

grant truncate on table "public"."users" to "service_role";

grant update on table "public"."users" to "service_role";

grant delete on table "public"."zones" to "anon";

grant insert on table "public"."zones" to "anon";

grant references on table "public"."zones" to "anon";

grant select on table "public"."zones" to "anon";

grant trigger on table "public"."zones" to "anon";

grant truncate on table "public"."zones" to "anon";

grant update on table "public"."zones" to "anon";

grant delete on table "public"."zones" to "authenticated";

grant insert on table "public"."zones" to "authenticated";

grant references on table "public"."zones" to "authenticated";

grant select on table "public"."zones" to "authenticated";

grant trigger on table "public"."zones" to "authenticated";

grant truncate on table "public"."zones" to "authenticated";

grant update on table "public"."zones" to "authenticated";

grant delete on table "public"."zones" to "postgres";

grant insert on table "public"."zones" to "postgres";

grant references on table "public"."zones" to "postgres";

grant select on table "public"."zones" to "postgres";

grant trigger on table "public"."zones" to "postgres";

grant truncate on table "public"."zones" to "postgres";

grant update on table "public"."zones" to "postgres";

grant delete on table "public"."zones" to "service_role";

grant insert on table "public"."zones" to "service_role";

grant references on table "public"."zones" to "service_role";

grant select on table "public"."zones" to "service_role";

grant trigger on table "public"."zones" to "service_role";

grant truncate on table "public"."zones" to "service_role";

grant update on table "public"."zones" to "service_role";



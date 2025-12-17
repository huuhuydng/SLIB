


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;


COMMENT ON SCHEMA "public" IS 'standard public schema';



CREATE EXTENSION IF NOT EXISTS "pg_graphql" WITH SCHEMA "graphql";






CREATE EXTENSION IF NOT EXISTS "pg_stat_statements" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "supabase_vault" WITH SCHEMA "vault";






CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA "extensions";





SET default_tablespace = '';

SET default_table_access_method = "heap";


CREATE TABLE IF NOT EXISTS "public"."access_logs" (
    "log_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "reservation_id" "uuid",
    "check_in_time" timestamp without time zone NOT NULL,
    "check_out_time" timestamp without time zone NOT NULL,
    "device_id" character varying(255) NOT NULL
);


ALTER TABLE "public"."access_logs" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."chat_logs" (
    "message_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "sender" character varying(255) NOT NULL,
    "message_content" "text" NOT NULL,
    "timestamp" timestamp without time zone NOT NULL
);


ALTER TABLE "public"."chat_logs" OWNER TO "postgres";


COMMENT ON COLUMN "public"."chat_logs"."sender" IS '''USER'', ''BOT'', ''LIBRARIAN''';



CREATE TABLE IF NOT EXISTS "public"."feedbacks" (
    "feedback_id" integer NOT NULL,
    "user_id" "uuid" NOT NULL,
    "content" "text" NOT NULL,
    "sentiment_score" numeric NOT NULL,
    "category" character varying(255) NOT NULL,
    "status" character varying(255) NOT NULL,
    "created_at" timestamp without time zone NOT NULL
);


ALTER TABLE "public"."feedbacks" OWNER TO "postgres";


COMMENT ON COLUMN "public"."feedbacks"."sentiment_score" IS 'Điểm cảm xúc từ AI (VD: 0.9 là tích cực, -0.8 là tiêu cực)';



COMMENT ON COLUMN "public"."feedbacks"."category" IS 'Chủ đề do AI phân loại (Wifi, Điều hòa, Ồn ào...)';



COMMENT ON COLUMN "public"."feedbacks"."status" IS '''NEW'', ''PROCESSING'', ''RESOLVED''';



CREATE SEQUENCE IF NOT EXISTS "public"."feedbacks_feedback_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE "public"."feedbacks_feedback_id_seq" OWNER TO "postgres";


ALTER SEQUENCE "public"."feedbacks_feedback_id_seq" OWNED BY "public"."feedbacks"."feedback_id";



CREATE TABLE IF NOT EXISTS "public"."notifications" (
    "notification_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "title" character varying(255) NOT NULL,
    "message" "text" NOT NULL,
    "is_read" boolean NOT NULL,
    "type" character varying(255) NOT NULL,
    "created_at" timestamp without time zone NOT NULL
);


ALTER TABLE "public"."notifications" OWNER TO "postgres";


COMMENT ON COLUMN "public"."notifications"."type" IS 'REMINDER, WARNING, NEWS';



CREATE TABLE IF NOT EXISTS "public"."reputation_history" (
    "history_id" integer NOT NULL,
    "user_id" "uuid" NOT NULL,
    "change_amount" integer NOT NULL,
    "reason" character varying(255) NOT NULL,
    "created_at" timestamp without time zone NOT NULL
);


ALTER TABLE "public"."reputation_history" OWNER TO "postgres";


COMMENT ON COLUMN "public"."reputation_history"."change_amount" IS 'Số điểm thay đổi (VD: -5, +10)';



CREATE SEQUENCE IF NOT EXISTS "public"."reputation_history_history_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE "public"."reputation_history_history_id_seq" OWNER TO "postgres";


ALTER SEQUENCE "public"."reputation_history_history_id_seq" OWNED BY "public"."reputation_history"."history_id";



CREATE TABLE IF NOT EXISTS "public"."reservations" (
    "reservation_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "seat_id" integer NOT NULL,
    "start_time" timestamp without time zone NOT NULL,
    "end_time" timestamp without time zone NOT NULL,
    "status" character varying(20) NOT NULL,
    "created_at" timestamp without time zone NOT NULL
);


ALTER TABLE "public"."reservations" OWNER TO "postgres";


COMMENT ON COLUMN "public"."reservations"."status" IS 'PENDING, CHECKED_IN, COMPLETED, CANCELLED, MISSED';



CREATE TABLE IF NOT EXISTS "public"."seats" (
    "seat_id" integer NOT NULL,
    "zone_id" integer NOT NULL,
    "seat_code" character varying(255) NOT NULL,
    "is_active" boolean NOT NULL,
    "position_x" integer NOT NULL,
    "position_y" integer NOT NULL
);


ALTER TABLE "public"."seats" OWNER TO "postgres";


COMMENT ON COLUMN "public"."seats"."seat_code" IS 'Mã ghế để hiện thị trên UI';



CREATE SEQUENCE IF NOT EXISTS "public"."seats_seat_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE "public"."seats_seat_id_seq" OWNER TO "postgres";


ALTER SEQUENCE "public"."seats_seat_id_seq" OWNED BY "public"."seats"."seat_id";



CREATE TABLE IF NOT EXISTS "public"."student_schedules" (
    "schedule_id" integer NOT NULL,
    "user_id" "uuid" NOT NULL,
    "day_of_week" integer NOT NULL,
    "start_time" time without time zone NOT NULL,
    "end_time" time without time zone NOT NULL
);


ALTER TABLE "public"."student_schedules" OWNER TO "postgres";


COMMENT ON COLUMN "public"."student_schedules"."day_of_week" IS '2-8 (thứ 2 đến CN)';



CREATE SEQUENCE IF NOT EXISTS "public"."student_schedules_schedule_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE "public"."student_schedules_schedule_id_seq" OWNER TO "postgres";


ALTER SEQUENCE "public"."student_schedules_schedule_id_seq" OWNED BY "public"."student_schedules"."schedule_id";



CREATE TABLE IF NOT EXISTS "public"."users" (
    "user_id" "uuid" NOT NULL,
    "student_code" character varying(255),
    "full_name" character varying(255) NOT NULL,
    "email" character varying(255) NOT NULL,
    "password" character varying(255) NOT NULL,
    "role" character varying(255) NOT NULL,
    "reputation_score" integer,
    "noti_device" character varying(255),
    "created_at" timestamp without time zone NOT NULL
);


ALTER TABLE "public"."users" OWNER TO "postgres";


COMMENT ON COLUMN "public"."users"."noti_device" IS 'Token máy điện thoại';



CREATE TABLE IF NOT EXISTS "public"."zones" (
    "zone_id" integer NOT NULL,
    "zone_name" character varying(255) NOT NULL,
    "zone_des" "text",
    "has_power_outlet" boolean NOT NULL
);


ALTER TABLE "public"."zones" OWNER TO "postgres";


CREATE SEQUENCE IF NOT EXISTS "public"."zones_zone_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE "public"."zones_zone_id_seq" OWNER TO "postgres";


ALTER SEQUENCE "public"."zones_zone_id_seq" OWNED BY "public"."zones"."zone_id";



ALTER TABLE ONLY "public"."feedbacks" ALTER COLUMN "feedback_id" SET DEFAULT "nextval"('"public"."feedbacks_feedback_id_seq"'::"regclass");



ALTER TABLE ONLY "public"."reputation_history" ALTER COLUMN "history_id" SET DEFAULT "nextval"('"public"."reputation_history_history_id_seq"'::"regclass");



ALTER TABLE ONLY "public"."seats" ALTER COLUMN "seat_id" SET DEFAULT "nextval"('"public"."seats_seat_id_seq"'::"regclass");



ALTER TABLE ONLY "public"."student_schedules" ALTER COLUMN "schedule_id" SET DEFAULT "nextval"('"public"."student_schedules_schedule_id_seq"'::"regclass");



ALTER TABLE ONLY "public"."zones" ALTER COLUMN "zone_id" SET DEFAULT "nextval"('"public"."zones_zone_id_seq"'::"regclass");



ALTER TABLE ONLY "public"."access_logs"
    ADD CONSTRAINT "access_logs_pkey" PRIMARY KEY ("log_id");



ALTER TABLE ONLY "public"."chat_logs"
    ADD CONSTRAINT "chat_logs_pkey" PRIMARY KEY ("message_id");



ALTER TABLE ONLY "public"."feedbacks"
    ADD CONSTRAINT "feedbacks_pkey" PRIMARY KEY ("feedback_id");



ALTER TABLE ONLY "public"."notifications"
    ADD CONSTRAINT "notifications_pkey" PRIMARY KEY ("notification_id");



ALTER TABLE ONLY "public"."reputation_history"
    ADD CONSTRAINT "reputation_history_pkey" PRIMARY KEY ("history_id");



ALTER TABLE ONLY "public"."reservations"
    ADD CONSTRAINT "reservations_pkey" PRIMARY KEY ("reservation_id");



ALTER TABLE ONLY "public"."seats"
    ADD CONSTRAINT "seats_pkey" PRIMARY KEY ("seat_id");



ALTER TABLE ONLY "public"."student_schedules"
    ADD CONSTRAINT "student_schedules_pkey" PRIMARY KEY ("schedule_id");



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_noti_device_key" UNIQUE ("noti_device");



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_pkey" PRIMARY KEY ("user_id");



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_student_code_key" UNIQUE ("student_code");



ALTER TABLE ONLY "public"."zones"
    ADD CONSTRAINT "zones_pkey" PRIMARY KEY ("zone_id");



ALTER TABLE ONLY "public"."access_logs"
    ADD CONSTRAINT "access_logs_reservation_id_fkey" FOREIGN KEY ("reservation_id") REFERENCES "public"."reservations"("reservation_id");



ALTER TABLE ONLY "public"."access_logs"
    ADD CONSTRAINT "access_logs_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");



ALTER TABLE ONLY "public"."chat_logs"
    ADD CONSTRAINT "chat_logs_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");



ALTER TABLE ONLY "public"."feedbacks"
    ADD CONSTRAINT "feedbacks_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");



ALTER TABLE ONLY "public"."notifications"
    ADD CONSTRAINT "notifications_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");



ALTER TABLE ONLY "public"."reputation_history"
    ADD CONSTRAINT "reputation_history_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");



ALTER TABLE ONLY "public"."reservations"
    ADD CONSTRAINT "reservations_seat_id_fkey" FOREIGN KEY ("seat_id") REFERENCES "public"."seats"("seat_id");



ALTER TABLE ONLY "public"."reservations"
    ADD CONSTRAINT "reservations_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");



ALTER TABLE ONLY "public"."seats"
    ADD CONSTRAINT "seats_zone_id_fkey" FOREIGN KEY ("zone_id") REFERENCES "public"."zones"("zone_id");



ALTER TABLE ONLY "public"."student_schedules"
    ADD CONSTRAINT "student_schedules_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");





ALTER PUBLICATION "supabase_realtime" OWNER TO "postgres";


ALTER PUBLICATION "supabase_realtime" ADD TABLE ONLY "public"."users";



GRANT USAGE ON SCHEMA "public" TO "postgres";
GRANT USAGE ON SCHEMA "public" TO "anon";
GRANT USAGE ON SCHEMA "public" TO "authenticated";
GRANT USAGE ON SCHEMA "public" TO "service_role";








































































































































































GRANT ALL ON TABLE "public"."access_logs" TO "anon";
GRANT ALL ON TABLE "public"."access_logs" TO "authenticated";
GRANT ALL ON TABLE "public"."access_logs" TO "service_role";



GRANT ALL ON TABLE "public"."chat_logs" TO "anon";
GRANT ALL ON TABLE "public"."chat_logs" TO "authenticated";
GRANT ALL ON TABLE "public"."chat_logs" TO "service_role";



GRANT ALL ON TABLE "public"."feedbacks" TO "anon";
GRANT ALL ON TABLE "public"."feedbacks" TO "authenticated";
GRANT ALL ON TABLE "public"."feedbacks" TO "service_role";



GRANT ALL ON SEQUENCE "public"."feedbacks_feedback_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."feedbacks_feedback_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."feedbacks_feedback_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."notifications" TO "anon";
GRANT ALL ON TABLE "public"."notifications" TO "authenticated";
GRANT ALL ON TABLE "public"."notifications" TO "service_role";



GRANT ALL ON TABLE "public"."reputation_history" TO "anon";
GRANT ALL ON TABLE "public"."reputation_history" TO "authenticated";
GRANT ALL ON TABLE "public"."reputation_history" TO "service_role";



GRANT ALL ON SEQUENCE "public"."reputation_history_history_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."reputation_history_history_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."reputation_history_history_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."reservations" TO "anon";
GRANT ALL ON TABLE "public"."reservations" TO "authenticated";
GRANT ALL ON TABLE "public"."reservations" TO "service_role";



GRANT ALL ON TABLE "public"."seats" TO "anon";
GRANT ALL ON TABLE "public"."seats" TO "authenticated";
GRANT ALL ON TABLE "public"."seats" TO "service_role";



GRANT ALL ON SEQUENCE "public"."seats_seat_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."seats_seat_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."seats_seat_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."student_schedules" TO "anon";
GRANT ALL ON TABLE "public"."student_schedules" TO "authenticated";
GRANT ALL ON TABLE "public"."student_schedules" TO "service_role";



GRANT ALL ON SEQUENCE "public"."student_schedules_schedule_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."student_schedules_schedule_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."student_schedules_schedule_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."users" TO "anon";
GRANT ALL ON TABLE "public"."users" TO "authenticated";
GRANT ALL ON TABLE "public"."users" TO "service_role";



GRANT ALL ON TABLE "public"."zones" TO "anon";
GRANT ALL ON TABLE "public"."zones" TO "authenticated";
GRANT ALL ON TABLE "public"."zones" TO "service_role";



GRANT ALL ON SEQUENCE "public"."zones_zone_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."zones_zone_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."zones_zone_id_seq" TO "service_role";









ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "service_role";































drop extension if exists "pg_net";



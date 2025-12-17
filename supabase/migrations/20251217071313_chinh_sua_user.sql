alter table "public"."access_logs" drop constraint "access_logs_user_id_fkey";

alter table "public"."chat_logs" drop constraint "chat_logs_user_id_fkey";

alter table "public"."feedbacks" drop constraint "feedbacks_user_id_fkey";

alter table "public"."notifications" drop constraint "notifications_user_id_fkey";

alter table "public"."reputation_history" drop constraint "reputation_history_user_id_fkey";

alter table "public"."reservations" drop constraint "reservations_user_id_fkey";

alter table "public"."student_schedules" drop constraint "student_schedules_user_id_fkey";

alter table "public"."users" drop constraint "users_noti_device_key";

drop index if exists "public"."users_noti_device_key";

alter table "public"."users" drop column "password";

alter table "public"."users" add column "dob" date;

alter table "public"."users" alter column "created_at" set default now();

alter table "public"."users" alter column "created_at" drop not null;

alter table "public"."users" alter column "created_at" set data type timestamp with time zone using "created_at"::timestamp with time zone;

alter table "public"."users" alter column "full_name" drop not null;

alter table "public"."users" alter column "reputation_score" set default 100;

alter table "public"."users" alter column "role" set default 'STUDENT'::character varying;

alter table "public"."users" alter column "role" drop not null;

alter table "public"."users" alter column "role" set data type character varying(20) using "role"::character varying(20);

alter table "public"."users" add constraint "users_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE not valid;

alter table "public"."users" validate constraint "users_user_id_fkey";

set check_function_bodies = off;

CREATE OR REPLACE FUNCTION public.handle_new_user()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
AS $function$
BEGIN
  INSERT INTO public.users (user_id, email, full_name, role, student_code, dob) -- Thêm dob vào đây
  VALUES (
    new.id,
    new.email,
    new.raw_user_meta_data->>'full_name',
    'STUDENT',
    new.raw_user_meta_data->>'student_code',
    (new.raw_user_meta_data->>'dob')::DATE -- Lấy dob và ép kiểu sang Date
  );
  RETURN new;
END;
$function$
;

grant delete on table "public"."access_logs" to "postgres";

grant insert on table "public"."access_logs" to "postgres";

grant references on table "public"."access_logs" to "postgres";

grant select on table "public"."access_logs" to "postgres";

grant trigger on table "public"."access_logs" to "postgres";

grant truncate on table "public"."access_logs" to "postgres";

grant update on table "public"."access_logs" to "postgres";

grant delete on table "public"."chat_logs" to "postgres";

grant insert on table "public"."chat_logs" to "postgres";

grant references on table "public"."chat_logs" to "postgres";

grant select on table "public"."chat_logs" to "postgres";

grant trigger on table "public"."chat_logs" to "postgres";

grant truncate on table "public"."chat_logs" to "postgres";

grant update on table "public"."chat_logs" to "postgres";

grant delete on table "public"."feedbacks" to "postgres";

grant insert on table "public"."feedbacks" to "postgres";

grant references on table "public"."feedbacks" to "postgres";

grant select on table "public"."feedbacks" to "postgres";

grant trigger on table "public"."feedbacks" to "postgres";

grant truncate on table "public"."feedbacks" to "postgres";

grant update on table "public"."feedbacks" to "postgres";

grant delete on table "public"."notifications" to "postgres";

grant insert on table "public"."notifications" to "postgres";

grant references on table "public"."notifications" to "postgres";

grant select on table "public"."notifications" to "postgres";

grant trigger on table "public"."notifications" to "postgres";

grant truncate on table "public"."notifications" to "postgres";

grant update on table "public"."notifications" to "postgres";

grant delete on table "public"."reputation_history" to "postgres";

grant insert on table "public"."reputation_history" to "postgres";

grant references on table "public"."reputation_history" to "postgres";

grant select on table "public"."reputation_history" to "postgres";

grant trigger on table "public"."reputation_history" to "postgres";

grant truncate on table "public"."reputation_history" to "postgres";

grant update on table "public"."reputation_history" to "postgres";

grant delete on table "public"."reservations" to "postgres";

grant insert on table "public"."reservations" to "postgres";

grant references on table "public"."reservations" to "postgres";

grant select on table "public"."reservations" to "postgres";

grant trigger on table "public"."reservations" to "postgres";

grant truncate on table "public"."reservations" to "postgres";

grant update on table "public"."reservations" to "postgres";

grant delete on table "public"."seats" to "postgres";

grant insert on table "public"."seats" to "postgres";

grant references on table "public"."seats" to "postgres";

grant select on table "public"."seats" to "postgres";

grant trigger on table "public"."seats" to "postgres";

grant truncate on table "public"."seats" to "postgres";

grant update on table "public"."seats" to "postgres";

grant delete on table "public"."student_schedules" to "postgres";

grant insert on table "public"."student_schedules" to "postgres";

grant references on table "public"."student_schedules" to "postgres";

grant select on table "public"."student_schedules" to "postgres";

grant trigger on table "public"."student_schedules" to "postgres";

grant truncate on table "public"."student_schedules" to "postgres";

grant update on table "public"."student_schedules" to "postgres";

grant delete on table "public"."users" to "postgres";

grant insert on table "public"."users" to "postgres";

grant references on table "public"."users" to "postgres";

grant select on table "public"."users" to "postgres";

grant trigger on table "public"."users" to "postgres";

grant truncate on table "public"."users" to "postgres";

grant update on table "public"."users" to "postgres";

grant delete on table "public"."zones" to "postgres";

grant insert on table "public"."zones" to "postgres";

grant references on table "public"."zones" to "postgres";

grant select on table "public"."zones" to "postgres";

grant trigger on table "public"."zones" to "postgres";

grant truncate on table "public"."zones" to "postgres";

grant update on table "public"."zones" to "postgres";

CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();



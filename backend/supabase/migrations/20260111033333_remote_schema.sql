create sequence "public"."categories_id_seq";

create sequence "public"."news_id_seq";

drop policy "Users can view own access logs" on "public"."access_logs";

drop policy "Users can send chat messages" on "public"."chat_logs";

drop policy "Users can view own chats" on "public"."chat_logs";

drop policy "Users can submit feedbacks" on "public"."feedbacks";

drop policy "Users can view own feedbacks" on "public"."feedbacks";

drop policy "Users can update own notifications" on "public"."notifications";

drop policy "Users can view own notifications" on "public"."notifications";

drop policy "Users can view own reputation" on "public"."reputation_history";

drop policy "Users can create reservations" on "public"."reservations";

drop policy "Users can update own reservations" on "public"."reservations";

drop policy "Users can view own reservations" on "public"."reservations";

drop policy "Authenticated users can view seats" on "public"."seats";

drop policy "Users can view own schedule" on "public"."student_schedules";

drop policy "Service Role full access" on "public"."users";

drop policy "Authenticated users can view zones" on "public"."zones";

drop policy "Users can view own profile" on "public"."users";

revoke delete on table "public"."access_logs" from "anon";

revoke insert on table "public"."access_logs" from "anon";

revoke references on table "public"."access_logs" from "anon";

revoke select on table "public"."access_logs" from "anon";

revoke trigger on table "public"."access_logs" from "anon";

revoke truncate on table "public"."access_logs" from "anon";

revoke update on table "public"."access_logs" from "anon";

revoke delete on table "public"."access_logs" from "authenticated";

revoke insert on table "public"."access_logs" from "authenticated";

revoke references on table "public"."access_logs" from "authenticated";

revoke select on table "public"."access_logs" from "authenticated";

revoke trigger on table "public"."access_logs" from "authenticated";

revoke truncate on table "public"."access_logs" from "authenticated";

revoke update on table "public"."access_logs" from "authenticated";

revoke delete on table "public"."access_logs" from "service_role";

revoke insert on table "public"."access_logs" from "service_role";

revoke references on table "public"."access_logs" from "service_role";

revoke select on table "public"."access_logs" from "service_role";

revoke trigger on table "public"."access_logs" from "service_role";

revoke truncate on table "public"."access_logs" from "service_role";

revoke update on table "public"."access_logs" from "service_role";

revoke delete on table "public"."chat_logs" from "anon";

revoke insert on table "public"."chat_logs" from "anon";

revoke references on table "public"."chat_logs" from "anon";

revoke select on table "public"."chat_logs" from "anon";

revoke trigger on table "public"."chat_logs" from "anon";

revoke truncate on table "public"."chat_logs" from "anon";

revoke update on table "public"."chat_logs" from "anon";

revoke delete on table "public"."chat_logs" from "authenticated";

revoke insert on table "public"."chat_logs" from "authenticated";

revoke references on table "public"."chat_logs" from "authenticated";

revoke select on table "public"."chat_logs" from "authenticated";

revoke trigger on table "public"."chat_logs" from "authenticated";

revoke truncate on table "public"."chat_logs" from "authenticated";

revoke update on table "public"."chat_logs" from "authenticated";

revoke delete on table "public"."chat_logs" from "service_role";

revoke insert on table "public"."chat_logs" from "service_role";

revoke references on table "public"."chat_logs" from "service_role";

revoke select on table "public"."chat_logs" from "service_role";

revoke trigger on table "public"."chat_logs" from "service_role";

revoke truncate on table "public"."chat_logs" from "service_role";

revoke update on table "public"."chat_logs" from "service_role";

revoke delete on table "public"."feedbacks" from "anon";

revoke insert on table "public"."feedbacks" from "anon";

revoke references on table "public"."feedbacks" from "anon";

revoke select on table "public"."feedbacks" from "anon";

revoke trigger on table "public"."feedbacks" from "anon";

revoke truncate on table "public"."feedbacks" from "anon";

revoke update on table "public"."feedbacks" from "anon";

revoke delete on table "public"."feedbacks" from "authenticated";

revoke insert on table "public"."feedbacks" from "authenticated";

revoke references on table "public"."feedbacks" from "authenticated";

revoke select on table "public"."feedbacks" from "authenticated";

revoke trigger on table "public"."feedbacks" from "authenticated";

revoke truncate on table "public"."feedbacks" from "authenticated";

revoke update on table "public"."feedbacks" from "authenticated";

revoke delete on table "public"."feedbacks" from "service_role";

revoke insert on table "public"."feedbacks" from "service_role";

revoke references on table "public"."feedbacks" from "service_role";

revoke select on table "public"."feedbacks" from "service_role";

revoke trigger on table "public"."feedbacks" from "service_role";

revoke truncate on table "public"."feedbacks" from "service_role";

revoke update on table "public"."feedbacks" from "service_role";

revoke delete on table "public"."notifications" from "anon";

revoke insert on table "public"."notifications" from "anon";

revoke references on table "public"."notifications" from "anon";

revoke select on table "public"."notifications" from "anon";

revoke trigger on table "public"."notifications" from "anon";

revoke truncate on table "public"."notifications" from "anon";

revoke update on table "public"."notifications" from "anon";

revoke delete on table "public"."notifications" from "authenticated";

revoke insert on table "public"."notifications" from "authenticated";

revoke references on table "public"."notifications" from "authenticated";

revoke select on table "public"."notifications" from "authenticated";

revoke trigger on table "public"."notifications" from "authenticated";

revoke truncate on table "public"."notifications" from "authenticated";

revoke update on table "public"."notifications" from "authenticated";

revoke delete on table "public"."notifications" from "service_role";

revoke insert on table "public"."notifications" from "service_role";

revoke references on table "public"."notifications" from "service_role";

revoke select on table "public"."notifications" from "service_role";

revoke trigger on table "public"."notifications" from "service_role";

revoke truncate on table "public"."notifications" from "service_role";

revoke update on table "public"."notifications" from "service_role";

revoke delete on table "public"."reputation_history" from "anon";

revoke insert on table "public"."reputation_history" from "anon";

revoke references on table "public"."reputation_history" from "anon";

revoke select on table "public"."reputation_history" from "anon";

revoke trigger on table "public"."reputation_history" from "anon";

revoke truncate on table "public"."reputation_history" from "anon";

revoke update on table "public"."reputation_history" from "anon";

revoke delete on table "public"."reputation_history" from "authenticated";

revoke insert on table "public"."reputation_history" from "authenticated";

revoke references on table "public"."reputation_history" from "authenticated";

revoke select on table "public"."reputation_history" from "authenticated";

revoke trigger on table "public"."reputation_history" from "authenticated";

revoke truncate on table "public"."reputation_history" from "authenticated";

revoke update on table "public"."reputation_history" from "authenticated";

revoke delete on table "public"."reputation_history" from "service_role";

revoke insert on table "public"."reputation_history" from "service_role";

revoke references on table "public"."reputation_history" from "service_role";

revoke select on table "public"."reputation_history" from "service_role";

revoke trigger on table "public"."reputation_history" from "service_role";

revoke truncate on table "public"."reputation_history" from "service_role";

revoke update on table "public"."reputation_history" from "service_role";

revoke delete on table "public"."reservations" from "anon";

revoke insert on table "public"."reservations" from "anon";

revoke references on table "public"."reservations" from "anon";

revoke select on table "public"."reservations" from "anon";

revoke trigger on table "public"."reservations" from "anon";

revoke truncate on table "public"."reservations" from "anon";

revoke update on table "public"."reservations" from "anon";

revoke delete on table "public"."reservations" from "authenticated";

revoke insert on table "public"."reservations" from "authenticated";

revoke references on table "public"."reservations" from "authenticated";

revoke select on table "public"."reservations" from "authenticated";

revoke trigger on table "public"."reservations" from "authenticated";

revoke truncate on table "public"."reservations" from "authenticated";

revoke update on table "public"."reservations" from "authenticated";

revoke delete on table "public"."reservations" from "service_role";

revoke insert on table "public"."reservations" from "service_role";

revoke references on table "public"."reservations" from "service_role";

revoke select on table "public"."reservations" from "service_role";

revoke trigger on table "public"."reservations" from "service_role";

revoke truncate on table "public"."reservations" from "service_role";

revoke update on table "public"."reservations" from "service_role";

revoke delete on table "public"."seats" from "anon";

revoke insert on table "public"."seats" from "anon";

revoke references on table "public"."seats" from "anon";

revoke select on table "public"."seats" from "anon";

revoke trigger on table "public"."seats" from "anon";

revoke truncate on table "public"."seats" from "anon";

revoke update on table "public"."seats" from "anon";

revoke delete on table "public"."seats" from "authenticated";

revoke insert on table "public"."seats" from "authenticated";

revoke references on table "public"."seats" from "authenticated";

revoke select on table "public"."seats" from "authenticated";

revoke trigger on table "public"."seats" from "authenticated";

revoke truncate on table "public"."seats" from "authenticated";

revoke update on table "public"."seats" from "authenticated";

revoke delete on table "public"."seats" from "service_role";

revoke insert on table "public"."seats" from "service_role";

revoke references on table "public"."seats" from "service_role";

revoke select on table "public"."seats" from "service_role";

revoke trigger on table "public"."seats" from "service_role";

revoke truncate on table "public"."seats" from "service_role";

revoke update on table "public"."seats" from "service_role";

revoke delete on table "public"."student_schedules" from "anon";

revoke insert on table "public"."student_schedules" from "anon";

revoke references on table "public"."student_schedules" from "anon";

revoke select on table "public"."student_schedules" from "anon";

revoke trigger on table "public"."student_schedules" from "anon";

revoke truncate on table "public"."student_schedules" from "anon";

revoke update on table "public"."student_schedules" from "anon";

revoke delete on table "public"."student_schedules" from "authenticated";

revoke insert on table "public"."student_schedules" from "authenticated";

revoke references on table "public"."student_schedules" from "authenticated";

revoke select on table "public"."student_schedules" from "authenticated";

revoke trigger on table "public"."student_schedules" from "authenticated";

revoke truncate on table "public"."student_schedules" from "authenticated";

revoke update on table "public"."student_schedules" from "authenticated";

revoke delete on table "public"."student_schedules" from "service_role";

revoke insert on table "public"."student_schedules" from "service_role";

revoke references on table "public"."student_schedules" from "service_role";

revoke select on table "public"."student_schedules" from "service_role";

revoke trigger on table "public"."student_schedules" from "service_role";

revoke truncate on table "public"."student_schedules" from "service_role";

revoke update on table "public"."student_schedules" from "service_role";

revoke delete on table "public"."users" from "anon";

revoke insert on table "public"."users" from "anon";

revoke references on table "public"."users" from "anon";

revoke select on table "public"."users" from "anon";

revoke trigger on table "public"."users" from "anon";

revoke truncate on table "public"."users" from "anon";

revoke update on table "public"."users" from "anon";

revoke delete on table "public"."users" from "authenticated";

revoke insert on table "public"."users" from "authenticated";

revoke references on table "public"."users" from "authenticated";

revoke select on table "public"."users" from "authenticated";

revoke trigger on table "public"."users" from "authenticated";

revoke truncate on table "public"."users" from "authenticated";

revoke update on table "public"."users" from "authenticated";

revoke delete on table "public"."users" from "service_role";

revoke insert on table "public"."users" from "service_role";

revoke references on table "public"."users" from "service_role";

revoke select on table "public"."users" from "service_role";

revoke trigger on table "public"."users" from "service_role";

revoke truncate on table "public"."users" from "service_role";

revoke update on table "public"."users" from "service_role";

revoke delete on table "public"."zones" from "anon";

revoke insert on table "public"."zones" from "anon";

revoke references on table "public"."zones" from "anon";

revoke select on table "public"."zones" from "anon";

revoke trigger on table "public"."zones" from "anon";

revoke truncate on table "public"."zones" from "anon";

revoke update on table "public"."zones" from "anon";

revoke delete on table "public"."zones" from "authenticated";

revoke insert on table "public"."zones" from "authenticated";

revoke references on table "public"."zones" from "authenticated";

revoke select on table "public"."zones" from "authenticated";

revoke trigger on table "public"."zones" from "authenticated";

revoke truncate on table "public"."zones" from "authenticated";

revoke update on table "public"."zones" from "authenticated";

revoke delete on table "public"."zones" from "service_role";

revoke insert on table "public"."zones" from "service_role";

revoke references on table "public"."zones" from "service_role";

revoke select on table "public"."zones" from "service_role";

revoke trigger on table "public"."zones" from "service_role";

revoke truncate on table "public"."zones" from "service_role";

revoke update on table "public"."zones" from "service_role";

alter table "public"."access_logs" drop constraint "fkg5hsjqn8c6r2lieprhno3yp2m";

alter table "public"."reservations" drop constraint "fkb5g9io5h54iwl2inkno50ppln";

alter table "public"."users" alter column "role" drop default;

alter type "public"."user_role" rename to "user_role__old_version_to_be_dropped";

create type "public"."user_role" as enum ('STUDENT', 'LIBRARIAN', 'ADMIN');


  create table "public"."categories" (
    "id" bigint not null default nextval('public.categories_id_seq'::regclass),
    "name" character varying(50) not null,
    "color_code" character varying(20) default '#F37021'::character varying,
    "created_at" timestamp with time zone default now()
      );


alter table "public"."categories" enable row level security;


  create table "public"."news" (
    "id" bigint not null default nextval('public.news_id_seq'::regclass),
    "title" character varying(255) not null,
    "summary" text,
    "content" text not null,
    "image_url" character varying(255),
    "category_id" bigint,
    "author_id" bigint,
    "user_id" uuid,
    "is_published" boolean default false,
    "is_pinned" boolean default false,
    "view_count" integer default 0,
    "published_at" timestamp with time zone,
    "created_at" timestamp with time zone default now(),
    "updated_at" timestamp with time zone default now()
      );


alter table "public"."news" enable row level security;


  create table "public"."user_settings" (
    "setting_id" uuid not null default gen_random_uuid(),
    "user_id" uuid not null,
    "is_hce_enabled" boolean default false,
    "is_ai_recommend_enabled" boolean default true,
    "is_booking_remind_enabled" boolean default true,
    "theme_mode" character varying(20) default 'light'::character varying,
    "language_code" character varying(10) default 'vi'::character varying,
    "created_at" timestamp with time zone default now(),
    "updated_at" timestamp with time zone default now()
      );


alter table "public"."user_settings" enable row level security;

alter table "public"."users" alter column role type "public"."user_role" using role::text::"public"."user_role";

alter table "public"."users" alter column "role" set default 'student'::public.user_role;

drop type "public"."user_role__old_version_to_be_dropped";

alter table "public"."access_logs" alter column "check_in_time" set default now();

alter table "public"."access_logs" alter column "check_out_time" drop not null;

alter table "public"."access_logs" alter column "device_id" set data type character varying(50) using "device_id"::character varying(50);

alter table "public"."access_logs" alter column "log_id" set default gen_random_uuid();

alter table "public"."chat_logs" alter column "message_id" set default gen_random_uuid();

alter table "public"."chat_logs" alter column "timestamp" set default now();

alter table "public"."chat_logs" alter column "timestamp" drop not null;

alter table "public"."feedbacks" alter column "created_at" set default now();

alter table "public"."feedbacks" alter column "created_at" drop not null;

alter table "public"."feedbacks" alter column "sentiment_score" set default 0;

alter table "public"."feedbacks" alter column "status" set default 'PENDING'::character varying;

alter table "public"."notifications" alter column "created_at" set default now();

alter table "public"."notifications" alter column "created_at" drop not null;

alter table "public"."notifications" alter column "is_read" set default false;

alter table "public"."notifications" alter column "notification_id" set default gen_random_uuid();

alter table "public"."reputation_history" alter column "created_at" set default now();

alter table "public"."reputation_history" alter column "created_at" drop not null;

alter table "public"."reservations" alter column "created_at" set default now();

alter table "public"."reservations" alter column "created_at" drop not null;

alter table "public"."reservations" alter column "reservation_id" set default gen_random_uuid();

alter table "public"."seats" alter column "is_active" set default true;

alter table "public"."seats" alter column "position_x" set default 0;

alter table "public"."seats" alter column "position_y" set default 0;

alter table "public"."users" drop column "dob";

alter table "public"."users" alter column "role" set default 'STUDENT'::public.user_role;

alter table "public"."zones" alter column "has_power_outlet" set default false;

alter sequence "public"."categories_id_seq" owned by "public"."categories"."id";

alter sequence "public"."news_id_seq" owned by "public"."news"."id";

CREATE UNIQUE INDEX categories_name_key ON public.categories USING btree (name);

CREATE UNIQUE INDEX categories_pkey ON public.categories USING btree (id);

CREATE INDEX idx_news_published ON public.news USING btree (is_published, published_at DESC);

CREATE UNIQUE INDEX news_pkey ON public.news USING btree (id);

CREATE UNIQUE INDEX user_settings_pkey ON public.user_settings USING btree (setting_id);

CREATE UNIQUE INDEX user_settings_user_id_key ON public.user_settings USING btree (user_id);

alter table "public"."categories" add constraint "categories_pkey" PRIMARY KEY using index "categories_pkey";

alter table "public"."news" add constraint "news_pkey" PRIMARY KEY using index "news_pkey";

alter table "public"."user_settings" add constraint "user_settings_pkey" PRIMARY KEY using index "user_settings_pkey";

alter table "public"."access_logs" add constraint "access_logs_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."access_logs" validate constraint "access_logs_user_id_fkey";

alter table "public"."categories" add constraint "categories_name_key" UNIQUE using index "categories_name_key";

alter table "public"."chat_logs" add constraint "chat_logs_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."chat_logs" validate constraint "chat_logs_user_id_fkey";

alter table "public"."feedbacks" add constraint "feedbacks_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."feedbacks" validate constraint "feedbacks_user_id_fkey";

alter table "public"."news" add constraint "news_category_id_fkey" FOREIGN KEY (category_id) REFERENCES public.categories(id) ON DELETE SET NULL not valid;

alter table "public"."news" validate constraint "news_category_id_fkey";

alter table "public"."news" add constraint "news_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."news" validate constraint "news_user_id_fkey";

alter table "public"."notifications" add constraint "notifications_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."notifications" validate constraint "notifications_user_id_fkey";

alter table "public"."reputation_history" add constraint "reputation_history_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."reputation_history" validate constraint "reputation_history_user_id_fkey";

alter table "public"."reservations" add constraint "reservations_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."reservations" validate constraint "reservations_user_id_fkey";

alter table "public"."student_schedules" add constraint "student_schedules_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."student_schedules" validate constraint "student_schedules_user_id_fkey";

alter table "public"."user_settings" add constraint "user_settings_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE not valid;

alter table "public"."user_settings" validate constraint "user_settings_user_id_fkey";

alter table "public"."user_settings" add constraint "user_settings_user_id_key" UNIQUE using index "user_settings_user_id_key";

set check_function_bodies = off;

CREATE OR REPLACE FUNCTION public.create_user_settings_automatically()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    INSERT INTO public.user_settings (user_id) VALUES (NEW.id);
    RETURN NEW;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.get_current_user_id()
 RETURNS uuid
 LANGUAGE plpgsql
 SECURITY DEFINER
AS $function$
BEGIN
  RETURN (SELECT id FROM public.users WHERE supabase_uid = auth.uid());
END;
$function$
;

CREATE OR REPLACE FUNCTION public.is_staff()
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
AS $function$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.users 
    WHERE supabase_uid = auth.uid() 
    AND role IN ('ADMIN', 'LIBRARIAN')
  );
END;
$function$
;

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$function$
;


  create policy "Staff manage logs"
  on "public"."access_logs"
  as permissive
  for all
  to public
using (public.is_staff());



  create policy "User view own logs"
  on "public"."access_logs"
  as permissive
  for select
  to public
using ((user_id = public.get_current_user_id()));



  create policy "Public read categories"
  on "public"."categories"
  as permissive
  for select
  to public
using (true);



  create policy "Staff modify categories"
  on "public"."categories"
  as permissive
  for all
  to public
using (public.is_staff());



  create policy "Staff view all chat"
  on "public"."chat_logs"
  as permissive
  for select
  to public
using (public.is_staff());



  create policy "User send chat"
  on "public"."chat_logs"
  as permissive
  for insert
  to public
with check ((user_id = public.get_current_user_id()));



  create policy "User view own chat"
  on "public"."chat_logs"
  as permissive
  for select
  to public
using ((user_id = public.get_current_user_id()));



  create policy "Staff view all feedbacks"
  on "public"."feedbacks"
  as permissive
  for select
  to public
using (public.is_staff());



  create policy "User manage own feedbacks"
  on "public"."feedbacks"
  as permissive
  for all
  to public
using ((user_id = public.get_current_user_id()));



  create policy "Public read news"
  on "public"."news"
  as permissive
  for select
  to public
using (true);



  create policy "Staff modify news"
  on "public"."news"
  as permissive
  for all
  to public
using (public.is_staff());



  create policy "User view own noti"
  on "public"."notifications"
  as permissive
  for select
  to public
using ((user_id = public.get_current_user_id()));



  create policy "Staff manage reputation"
  on "public"."reputation_history"
  as permissive
  for all
  to public
using (public.is_staff());



  create policy "User view own reputation"
  on "public"."reputation_history"
  as permissive
  for select
  to public
using ((user_id = public.get_current_user_id()));



  create policy "Staff modify reservations"
  on "public"."reservations"
  as permissive
  for update
  to public
using (public.is_staff());



  create policy "Staff view all reservations"
  on "public"."reservations"
  as permissive
  for select
  to public
using (public.is_staff());



  create policy "User create reservations"
  on "public"."reservations"
  as permissive
  for insert
  to public
with check ((user_id = public.get_current_user_id()));



  create policy "User view own reservations"
  on "public"."reservations"
  as permissive
  for select
  to public
using ((user_id = public.get_current_user_id()));



  create policy "Public read seats"
  on "public"."seats"
  as permissive
  for select
  to public
using (true);



  create policy "Staff modify seats"
  on "public"."seats"
  as permissive
  for all
  to public
using (public.is_staff());



  create policy "User manage own schedule"
  on "public"."student_schedules"
  as permissive
  for all
  to public
using ((user_id = public.get_current_user_id()));



  create policy "Service Role Settings"
  on "public"."user_settings"
  as permissive
  for all
  to service_role
using (true)
with check (true);



  create policy "Users update own settings"
  on "public"."user_settings"
  as permissive
  for update
  to public
using ((user_id = public.get_current_user_id()));



  create policy "Users view own settings"
  on "public"."user_settings"
  as permissive
  for select
  to public
using ((user_id = public.get_current_user_id()));



  create policy "Service Role Users"
  on "public"."users"
  as permissive
  for all
  to service_role
using (true)
with check (true);



  create policy "Public read zones"
  on "public"."zones"
  as permissive
  for select
  to public
using (true);



  create policy "Staff modify zones"
  on "public"."zones"
  as permissive
  for all
  to public
using (public.is_staff());



  create policy "Users can view own profile"
  on "public"."users"
  as permissive
  for select
  to public
using ((auth.uid() = supabase_uid));


CREATE TRIGGER update_news_updated_at BEFORE UPDATE ON public.news FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_user_settings_updated_at BEFORE UPDATE ON public.user_settings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER on_user_created_insert_settings AFTER INSERT ON public.users FOR EACH ROW EXECUTE FUNCTION public.create_user_settings_automatically();



drop extension if exists "pg_net";

create type "public"."user_role" as enum ('student', 'librarian', 'admin');

drop policy "Users can update own profile" on "public"."users";

drop policy "Users can view own profile" on "public"."users";

alter table "public"."users" drop constraint "users_user_id_fkey";

alter table "public"."users" drop constraint "users_pkey";

drop index if exists "public"."users_pkey";

alter table "public"."reservations" alter column "status" set data type character varying(255) using "status"::character varying(255);

alter table "public"."users" drop column "user_id";

alter table "public"."users" add column "id" uuid not null default gen_random_uuid();

alter table "public"."users" add column "is_active" boolean default true;

alter table "public"."users" add column "supabase_uid" uuid;

alter table "public"."users" add column "updated_at" timestamp with time zone default now();

alter table "public"."users" alter column "email" drop not null;

alter table "public"."users" alter column "full_name" set not null;

alter table "public"."users" alter column "role" set default 'student'::public.user_role;

alter table "public"."users" alter column "role" set data type public.user_role using "role"::public.user_role;

alter table "public"."users" alter column "student_code" set not null;

alter table "public"."users" alter column "student_code" set data type character varying(20) using "student_code"::character varying(20);

CREATE UNIQUE INDEX users_supabase_uid_key ON public.users USING btree (supabase_uid);

CREATE UNIQUE INDEX users_pkey ON public.users USING btree (id);

alter table "public"."users" add constraint "users_pkey" PRIMARY KEY using index "users_pkey";

alter table "public"."users" add constraint "users_supabase_uid_fkey" FOREIGN KEY (supabase_uid) REFERENCES auth.users(id) ON DELETE SET NULL not valid;

alter table "public"."users" validate constraint "users_supabase_uid_fkey";

alter table "public"."users" add constraint "users_supabase_uid_key" UNIQUE using index "users_supabase_uid_key";

set check_function_bodies = off;

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


  create policy "Service Role full access"
  on "public"."users"
  as permissive
  for all
  to service_role
using (true)
with check (true);



  create policy "Users can view own profile"
  on "public"."users"
  as permissive
  for select
  to public
using (((auth.uid() = supabase_uid) OR (EXISTS ( SELECT 1
   FROM public.users users_1
  WHERE ((users_1.supabase_uid = auth.uid()) AND (users_1.role = ANY (ARRAY['admin'::public.user_role, 'librarian'::public.user_role])))))));


CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();



drop function if exists "public"."handle_new_user"() cascade;

drop trigger if exists "on_auth_user_created" on "auth"."users";



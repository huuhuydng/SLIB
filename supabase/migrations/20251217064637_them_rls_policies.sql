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


  create policy "Users can view own access logs"
  on "public"."access_logs"
  as permissive
  for select
  to authenticated
using ((auth.uid() = user_id));



  create policy "Users can send chat messages"
  on "public"."chat_logs"
  as permissive
  for insert
  to authenticated
with check ((auth.uid() = user_id));



  create policy "Users can view own chats"
  on "public"."chat_logs"
  as permissive
  for select
  to authenticated
using ((auth.uid() = user_id));



  create policy "Users can submit feedbacks"
  on "public"."feedbacks"
  as permissive
  for insert
  to authenticated
with check ((auth.uid() = user_id));



  create policy "Users can view own feedbacks"
  on "public"."feedbacks"
  as permissive
  for select
  to authenticated
using ((auth.uid() = user_id));



  create policy "Users can update own notifications"
  on "public"."notifications"
  as permissive
  for update
  to authenticated
using ((auth.uid() = user_id));



  create policy "Users can view own notifications"
  on "public"."notifications"
  as permissive
  for select
  to authenticated
using ((auth.uid() = user_id));



  create policy "Users can view own reputation"
  on "public"."reputation_history"
  as permissive
  for select
  to authenticated
using ((auth.uid() = user_id));



  create policy "Users can create reservations"
  on "public"."reservations"
  as permissive
  for insert
  to authenticated
with check ((auth.uid() = user_id));



  create policy "Users can update own reservations"
  on "public"."reservations"
  as permissive
  for update
  to authenticated
using ((auth.uid() = user_id));



  create policy "Users can view own reservations"
  on "public"."reservations"
  as permissive
  for select
  to authenticated
using ((auth.uid() = user_id));



  create policy "Authenticated users can view seats"
  on "public"."seats"
  as permissive
  for select
  to authenticated
using (true);



  create policy "Users can view own schedule"
  on "public"."student_schedules"
  as permissive
  for select
  to authenticated
using ((auth.uid() = user_id));



  create policy "Users can update own profile"
  on "public"."users"
  as permissive
  for update
  to authenticated
using ((auth.uid() = user_id));



  create policy "Users can view own profile"
  on "public"."users"
  as permissive
  for select
  to authenticated
using ((auth.uid() = user_id));



  create policy "Authenticated users can view zones"
  on "public"."zones"
  as permissive
  for select
  to authenticated
using (true);




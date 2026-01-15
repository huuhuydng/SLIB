alter table "public"."zones" alter column "zone_des" set data type character varying(255) using "zone_des"::character varying(255);

alter table "public"."access_logs" add constraint "fkg5hsjqn8c6r2lieprhno3yp2m" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."access_logs" validate constraint "fkg5hsjqn8c6r2lieprhno3yp2m";

alter table "public"."reservations" add constraint "fkb5g9io5h54iwl2inkno50ppln" FOREIGN KEY (user_id) REFERENCES public.users(id) not valid;

alter table "public"."reservations" validate constraint "fkb5g9io5h54iwl2inkno50ppln";



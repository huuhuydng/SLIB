CREATE OR REPLACE FUNCTION public.handle_new_student_profile()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.role IN ('STUDENT', 'TEACHER') THEN
    INSERT INTO public.student_profiles (user_id, reputation_score, total_study_hours, violation_count)
    VALUES (NEW.id, 100, 0, 0)
    ON CONFLICT (user_id) DO NOTHING;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

INSERT INTO public.student_profiles (user_id, reputation_score, total_study_hours, violation_count)
SELECT u.id, 100, 0, 0
FROM public.users u
LEFT JOIN public.student_profiles sp ON sp.user_id = u.id
WHERE u.role IN ('STUDENT', 'TEACHER')
  AND sp.user_id IS NULL;

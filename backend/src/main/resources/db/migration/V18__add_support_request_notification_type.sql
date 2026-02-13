-- Add SUPPORT_REQUEST to the notification_type check constraint
-- Hibernate auto-generated a check constraint that only allows: BOOKING, REMINDER, VIOLATION, SYSTEM, NEWS
-- We need to drop it and recreate with SUPPORT_REQUEST included

ALTER TABLE notifications
DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

ALTER TABLE notifications
ADD CONSTRAINT notifications_notification_type_check CHECK (
    notification_type::text = ANY (
        ARRAY[
            'BOOKING'::text,
            'REMINDER'::text,
            'VIOLATION'::text,
            'SYSTEM'::text,
            'NEWS'::text,
            'SUPPORT_REQUEST'::text
        ]
    )
);
-- Ensure notifications check constraint matches all enum values used in code.

ALTER TABLE notifications
DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

ALTER TABLE notifications
ADD CONSTRAINT notifications_notification_type_check CHECK (
    notification_type IN (
        'BOOKING',
        'REMINDER',
        'VIOLATION',
        'VIOLATION_REPORT',
        'REPUTATION',
        'SYSTEM',
        'NEWS',
        'SUPPORT_REQUEST',
        'COMPLAINT',
        'SEAT_STATUS_REPORT',
        'CHAT_MESSAGE'
    )
);

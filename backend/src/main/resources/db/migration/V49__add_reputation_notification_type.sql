-- Add REPUTATION to notifications notification_type constraint

ALTER TABLE notifications
DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

ALTER TABLE notifications
ADD CONSTRAINT notifications_notification_type_check CHECK (
    notification_type IN (
        'BOOKING',
        'REMINDER',
        'VIOLATION',
        'REPUTATION',
        'SYSTEM',
        'NEWS',
        'SUPPORT_REQUEST',
        'VIOLATION_REPORT',
        'CHAT_MESSAGE'
    )
);

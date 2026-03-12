-- V32: Add CHAT_MESSAGE to notifications notification_type constraint
-- Fix: Push notifications for chat messages were failing due to missing type in CHECK constraint

ALTER TABLE notifications
DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

ALTER TABLE notifications
ADD CONSTRAINT notifications_notification_type_check CHECK (
    notification_type IN (
        'BOOKING',
        'REMINDER',
        'VIOLATION',
        'SYSTEM',
        'NEWS',
        'SUPPORT_REQUEST',
        'VIOLATION_REPORT',
        'CHAT_MESSAGE'
    )
);
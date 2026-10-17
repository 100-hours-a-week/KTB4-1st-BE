ALTER TABLE chat_messages
    ADD COLUMN system_event_type VARCHAR(40) NULL;

UPDATE chat_messages
SET system_event_type = 'EXCHANGE_REQUEST_CREATED'
WHERE message_type = 'SYSTEM';

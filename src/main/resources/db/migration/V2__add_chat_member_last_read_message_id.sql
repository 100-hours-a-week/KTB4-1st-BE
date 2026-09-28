ALTER TABLE chat_members
    ADD COLUMN last_read_message_id BIGINT NULL;

UPDATE chat_members cm
JOIN (
    SELECT chat_room_id, MAX(message_id) AS message_id
    FROM chat_messages
    GROUP BY chat_room_id
) latest_message ON latest_message.chat_room_id = cm.chat_room_id
SET cm.last_read_message_id = latest_message.message_id;

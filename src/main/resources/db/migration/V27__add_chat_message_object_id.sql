ALTER TABLE chat_messages
    ADD COLUMN message_object_id VARCHAR(24) NULL,
    ADD KEY ix_chat_messages_message_object_id (message_object_id);

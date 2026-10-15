CREATE TABLE chatbot_messages (
    chatbot_message_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    message_role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (chatbot_message_id),
    KEY idx_chatbot_messages_user_id_id (user_id, chatbot_message_id),
    CONSTRAINT ck_chatbot_messages_role CHECK (message_role IN ('USER', 'ASSISTANT')),
    CONSTRAINT fk_chatbot_messages_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

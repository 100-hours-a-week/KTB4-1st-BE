ALTER TABLE chat_rooms
    DROP FOREIGN KEY fk_chat_room_exchange_request;

ALTER TABLE chat_members
    DROP FOREIGN KEY fk_chat_member_room,
    DROP FOREIGN KEY fk_chat_member_user;

ALTER TABLE chat_messages
    DROP FOREIGN KEY fk_chat_message_room,
    DROP FOREIGN KEY fk_chat_message_user;

ALTER TABLE item_likes
    ADD CONSTRAINT uk_item_likes_item_user UNIQUE (item_id, user_id);

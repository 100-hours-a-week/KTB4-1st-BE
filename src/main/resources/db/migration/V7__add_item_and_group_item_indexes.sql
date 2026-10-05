ALTER TABLE items
    ADD INDEX idx_items_user_deleted (user_id, deleted_at);

ALTER TABLE group_items
    ADD INDEX idx_group_item_item_deleted_group (item_id, deleted_at, group_id);

ALTER TABLE group_members
    ADD INDEX idx_group_member_group_status (group_id, user_status);

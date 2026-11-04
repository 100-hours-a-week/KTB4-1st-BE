ALTER TABLE item_view_interest_events
    DROP INDEX idx_item_view_interest_item_time,
    ADD INDEX idx_item_view_interest_time_item (viewed_at, item_id),
    ADD INDEX idx_item_view_interest_item_time_user (item_id, viewed_at, user_id);

ALTER TABLE exchange_requests
    ADD COLUMN group_id BIGINT NULL;

ALTER TABLE exchange_requests
    ADD CONSTRAINT fk_exchange_request_group
        FOREIGN KEY (group_id) REFERENCES `groups` (group_id);

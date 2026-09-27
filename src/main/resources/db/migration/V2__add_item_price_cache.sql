ALTER TABLE items
    ADD COLUMN unit_price BIGINT NOT NULL DEFAULT 0;

ALTER TABLE moderation_checks
    ADD COLUMN keyword VARCHAR(255) NULL;

CREATE TABLE item_cash (
    item_cash_id BIGINT NOT NULL AUTO_INCREMENT,
    keyword VARCHAR(255) NOT NULL,
    unit_price BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (item_cash_id),
    UNIQUE KEY uk_item_cash_keyword (keyword)
) ENGINE=InnoDB;

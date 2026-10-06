ALTER TABLE items
    ADD COLUMN thumbnail_image_id BIGINT NULL;

UPDATE items AS i
JOIN (
    SELECT item_id, MIN(image_id) AS image_id
    FROM images
    WHERE item_id IS NOT NULL
    GROUP BY item_id
) AS first_image ON first_image.item_id = i.item_id
SET i.thumbnail_image_id = first_image.image_id;

ALTER TABLE items
    ADD INDEX idx_items_thumbnail_image (thumbnail_image_id),
    ADD CONSTRAINT fk_items_thumbnail_image
        FOREIGN KEY (thumbnail_image_id) REFERENCES images (image_id);

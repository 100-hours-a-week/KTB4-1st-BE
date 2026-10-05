ALTER TABLE `groups`
    ADD COLUMN active_group_name_generated VARCHAR(30)
        GENERATED ALWAYS AS (
            CASE WHEN deleted_at IS NULL THEN group_name ELSE NULL END
        ) VIRTUAL;

ALTER TABLE `groups`
    ADD UNIQUE KEY uk_groups_active_group_name_generated (active_group_name_generated);

ALTER TABLE `groups`
    DROP INDEX uk_groups_active_group_name,
    DROP COLUMN active_group_name;

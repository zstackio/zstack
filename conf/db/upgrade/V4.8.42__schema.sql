-- ZSTAC-75952: backfill one default zone for deployments affected by zone creation without isDefault.
DROP PROCEDURE IF EXISTS backfill_default_zone_if_absent;
DELIMITER $$
CREATE PROCEDURE backfill_default_zone_if_absent()
BEGIN
    DECLARE default_zone_count BIGINT DEFAULT 0;
    DECLARE first_zone_uuid VARCHAR(32) DEFAULT NULL;

    SELECT COUNT(*) INTO default_zone_count
    FROM `zstack`.`ZoneVO`
    WHERE `isDefault` = 1;

    IF default_zone_count = 0 THEN
        SET first_zone_uuid = (
            SELECT `uuid`
            FROM `zstack`.`ZoneVO`
            ORDER BY `createDate` ASC, `uuid` ASC
            LIMIT 1
        );

        IF first_zone_uuid IS NOT NULL THEN
            UPDATE `zstack`.`ZoneEO`
            SET `isDefault` = 1
            WHERE `uuid` = first_zone_uuid;
        END IF;
    END IF;
END $$
DELIMITER ;

CALL backfill_default_zone_if_absent();
DROP PROCEDURE IF EXISTS backfill_default_zone_if_absent;

ALTER TABLE VolumeSnapshotTreeEO ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT "Completed";
DROP VIEW IF EXISTS `zstack`.`VolumeSnapshotTreeVO`;
CREATE VIEW `zstack`.`VolumeSnapshotTreeVO` AS SELECT uuid, volumeUuid, current, status, createDate, lastOpDate FROM `zstack`.`VolumeSnapshotTreeEO` WHERE deleted IS NULL;

ALTER TABLE `IAM2OrganizationVO` ADD COLUMN `rootOrganizationUuid` VARCHAR(32) NOT NULL;

DROP PROCEDURE IF EXISTS upgradeChild;
DROP PROCEDURE IF EXISTS upgradeOrganization;

DELIMITER $$
CREATE PROCEDURE upgradeChild(IN root_organization_uuid VARCHAR(32), IN current_organization_uuid VARCHAR(32))
    BEGIN
        DECLARE next_organization_uuid varchar(32);
        DECLARE done INT DEFAULT FALSE;
        DEClARE cur CURSOR FOR SELECT uuid FROM IAM2OrganizationVO WHERE parentUuid = current_organization_uuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        open cur;
        upgrade_child_loop: LOOP
            FETCH cur INTO next_organization_uuid;
            SELECT next_organization_uuid;
            IF done THEN
                LEAVE upgrade_child_loop;
            END IF;

            UPDATE IAM2OrganizationVO SET rootOrganizationUuid = root_organization_uuid WHERE uuid = next_organization_uuid;
            CALL upgradeChild(root_organization_uuid, next_organization_uuid);
        END LOOP;
        close cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE upgradeOrganization()
    upgrade_procedure: BEGIN
        DECLARE root_organization_uuid VARCHAR(32);
        DECLARE null_root_organization_uuid_exists INT DEFAULT 0;
        DECLARE done INT DEFAULT FALSE;
        DEClARE cur CURSOR FOR SELECT uuid FROM IAM2OrganizationVO WHERE parentUuid is NULL;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        SELECT COUNT(uuid) INTO null_root_organization_uuid_exists FROM IAM2OrganizationVO where rootOrganizationUuid is NULL or rootOrganizationUuid = '';

        IF (null_root_organization_uuid_exists = 0) THEN
            LEAVE upgrade_procedure;
        END IF;

        OPEN cur;
        root_organization_loop: LOOP
            FETCH cur INTO root_organization_uuid;
            IF done THEN
                LEAVE root_organization_loop;
            END IF;

            UPDATE IAM2OrganizationVO SET rootOrganizationUuid = root_organization_uuid WHERE (rootOrganizationUuid is NULL or rootOrganizationUuid = '') and uuid = root_organization_uuid;
            CALL upgradeChild(root_organization_uuid, root_organization_uuid);
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

SET max_sp_recursion_depth=512;
call upgradeOrganization();
SET max_sp_recursion_depth=0;
DROP PROCEDURE IF EXISTS upgradeChild;
DROP PROCEDURE IF EXISTS upgradeOrganization;

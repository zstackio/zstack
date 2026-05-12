-- ZSTAC-84025: Add pipelineTag to ModelVO for inference template auto-matching
CALL ADD_COLUMN('ModelVO', 'pipelineTag', 'VARCHAR(64)', 1, NULL);

-- ZSTAC-84025: Add isDefault to ModelServiceRefVO to mark the default inference template per model
ALTER TABLE `zstack`.`ModelServiceRefVO` ADD COLUMN `isDefault` TINYINT(1) NOT NULL DEFAULT 0;

-- ZSTAC-84025-F2: Add manifestJson to ModelVO so Step 1 (file format) of the auto-match Matcher can
-- parse file_types/file_extensions from the manifest returned by the aios agent.
CALL ADD_COLUMN('ModelVO', 'manifestJson', 'TEXT', 1, NULL);

-- ZSTAC-84025: Add createDate/lastOpDate to ModelServiceRefVO so the auto-match Matcher can
-- pick the earliest isDefault=true row when DB has the rare 2+ defaults anomaly (Q5).
ALTER TABLE `zstack`.`ModelServiceRefVO` ADD COLUMN `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE `zstack`.`ModelServiceRefVO` ADD COLUMN `createDate` TIMESTAMP NULL DEFAULT NULL;

DROP PROCEDURE IF EXISTS backfill_model_service_ref_create_date;
DELIMITER $$
CREATE PROCEDURE backfill_model_service_ref_create_date()
BEGIN
    UPDATE `zstack`.`ModelServiceRefVO`
    SET `createDate` = CURRENT_TIMESTAMP
    WHERE `createDate` IS NULL OR `createDate` = '0000-00-00 00:00:00';
END $$
DELIMITER ;
CALL backfill_model_service_ref_create_date();
DROP PROCEDURE IF EXISTS backfill_model_service_ref_create_date;

-- Older MySQL/MariaDB versions allow only one TIMESTAMP column with CURRENT_TIMESTAMP
-- in DEFAULT or ON UPDATE. lastOpDate already uses it, so keep createDate non-zero
-- and let ModelServiceRefVO.@PrePersist populate the real creation time for new rows.
ALTER TABLE `zstack`.`ModelServiceRefVO` MODIFY COLUMN `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00';

CALL ADD_COLUMN('ModelServiceVO', 'hasNewVersion', 'tinyint(1)', 1, NULL); 
CALL ADD_COLUMN('ModelCenterCapacityVO', 'availableCapacity', 'bigint', 1, NULL);
CALL ADD_COLUMN('ModelCenterCapacityVO', 'totalCapacity', 'bigint', 1, NULL);

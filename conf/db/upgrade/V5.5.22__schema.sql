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

CREATE TABLE IF NOT EXISTS `zstack`.`CdnModelServiceTemplateVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `version` varchar(255) DEFAULT NULL,
    `platform` varchar(255) DEFAULT NULL,
    `size` bigint DEFAULT NULL,
    `projectId` varchar(255) DEFAULT NULL,
    `projectName` varchar(255) DEFAULT NULL,
    `downloadUrl` varchar(2048) DEFAULT NULL,
    `installed` tinyint(1) NOT NULL DEFAULT 0,
    `modelServiceUuid` varchar(32) DEFAULT NULL,
    `usingServiceCount` bigint NOT NULL DEFAULT 0,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukCdnModelServiceTemplateVOName` (`name`),
    KEY `idxCdnModelServiceTemplateVOModelServiceUuid` (`modelServiceUuid`),
    CONSTRAINT `fkCdnModelServiceTemplateVOModelServiceVO` FOREIGN KEY (`modelServiceUuid`)
        REFERENCES `zstack`.`ModelServiceVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- dGPU billing support tables

CREATE TABLE IF NOT EXISTS `zstack`.`PriceDGpuGpuSpecRefVO` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `priceUuid`   VARCHAR(32)     NOT NULL,
    `gpuSpecUuid` VARCHAR(32)     NOT NULL,
    `createDate`  TIMESTAMP       NULL DEFAULT NULL,
    `lastOpDate`  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_price_dgpu_spec_price`
        FOREIGN KEY (`priceUuid`) REFERENCES `zstack`.`PriceVO`(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_price_dgpu_spec_gpu_spec`
        FOREIGN KEY (`gpuSpecUuid`) REFERENCES `zstack`.`GpuDeviceSpecVO`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`DGpuUsageVO` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `dgpuDeviceUuid`  VARCHAR(32)     NOT NULL,
    `gpuSpecUuid`     VARCHAR(32)     NOT NULL,
    `allocatedMemory` BIGINT UNSIGNED NOT NULL,
    `dgpuName`        VARCHAR(255)    DEFAULT NULL,
    `vmUuid`          VARCHAR(32)     DEFAULT NULL,
    `vmName`          VARCHAR(255)    DEFAULT NULL,
    `status`          VARCHAR(64)     NOT NULL,
    `accountUuid`     VARCHAR(32)     NOT NULL,
    `dateInLong`      BIGINT UNSIGNED NOT NULL,
    `inventory`       TEXT            DEFAULT NULL,
    `createDate`      TIMESTAMP       NULL DEFAULT NULL,
    `lastOpDate`      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dgpu_usage_account_date` (`accountUuid`, `dateInLong`),
    KEY `idx_dgpu_usage_device` (`accountUuid`, `dateInLong`, `dgpuDeviceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`DGpuUsageHistoryVO` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `dgpuDeviceUuid`  VARCHAR(32)     NOT NULL,
    `gpuSpecUuid`     VARCHAR(32)     NOT NULL,
    `allocatedMemory` BIGINT UNSIGNED NOT NULL,
    `dgpuName`        VARCHAR(255)    DEFAULT NULL,
    `vmUuid`          VARCHAR(32)     DEFAULT NULL,
    `vmName`          VARCHAR(255)    DEFAULT NULL,
    `status`          VARCHAR(64)     NOT NULL,
    `accountUuid`     VARCHAR(32)     NOT NULL,
    `dateInLong`      BIGINT UNSIGNED NOT NULL,
    `inventory`       TEXT            DEFAULT NULL,
    `createDate`      TIMESTAMP       NULL DEFAULT NULL,
    `lastOpDate`      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dgpu_usage_history_account_date` (`accountUuid`, `dateInLong`),
    KEY `idx_dgpu_usage_history_device` (`accountUuid`, `dateInLong`, `dgpuDeviceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`DGpuBillingVO` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `vmName`          VARCHAR(255)    DEFAULT NULL,
    `allocatedMemory` BIGINT UNSIGNED DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

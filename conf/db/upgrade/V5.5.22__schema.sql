-- ZSTAC-84025: Add pipelineTag to ModelVO for inference template auto-matching
CALL ADD_COLUMN('ModelVO', 'pipelineTag', 'VARCHAR(64)', 1, NULL);

-- ZSTAC-84246: Persist virtiofs restore success epoch for VmModelMountVO.
CALL ADD_COLUMN('VmModelMountVO', 'lastAttachedEpoch', 'BIGINT', 1, NULL);

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

CREATE TABLE IF NOT EXISTS `zstack`.`HaNetworkGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(128) NOT NULL,
    `minAvailableCount` INT(10) NOT NULL DEFAULT 1,
    `state` VARCHAR(32) NOT NULL DEFAULT 'Enabled',
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HaNetworkGroupL3NetworkRefVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `haNetworkGroupUuid` VARCHAR(32) NOT NULL,
    `l3NetworkUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    INDEX `idxHaNetworkGroupL3NetworkRefVOhaNetworkGroupUuid` (`haNetworkGroupUuid`),
    UNIQUE INDEX `ukHaNetworkGroupL3NetworkRefVOl3NetworkUuid` (`l3NetworkUuid`),
    CONSTRAINT `fkHaNetworkGroupL3NetworkRefVOHaNetworkGroupVO` FOREIGN KEY (`haNetworkGroupUuid`) REFERENCES `zstack`.`HaNetworkGroupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHaNetworkGroupL3NetworkRefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostHaNetworkGroupStatusVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `hostUuid` VARCHAR(32) NOT NULL,
    `networkGroupUuid` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'Unknown',
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE INDEX `ukHostHaNetworkGroupStatusVOHostUuidNetworkGroupUuid` (`hostUuid`, `networkGroupUuid`),
    INDEX `idxHostHaNetworkGroupStatusVOhostUuid` (`hostUuid`),
    INDEX `idxHostHaNetworkGroupStatusVOnetworkGroupUuid` (`networkGroupUuid`),
    CONSTRAINT `fkHostHaNetworkGroupStatusVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostHaNetworkGroupStatusVOHaNetworkGroupVO` FOREIGN KEY (`networkGroupUuid`) REFERENCES `zstack`.`HaNetworkGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HaNetworkGroupGlobalConfigVersionVO` (
    `name` VARCHAR(64) NOT NULL,
    `version` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `zstack`.`HaNetworkGroupGlobalConfigVersionVO` (`name`, `version`)
VALUES ('ha-network-group', 0);

-- ZNS SDN Controller support

CREATE TABLE IF NOT EXISTS `ZnsControllerVO` (
    `uuid` varchar(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkZnsControllerVOSdnControllerVO` FOREIGN KEY (`uuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `L2GeneveNetworkVO` (
    `uuid` varchar(32) NOT NULL,
    `geneveId` int(10) unsigned NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkL2GeneveNetworkVOL2NetworkEO` FOREIGN KEY (`uuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `ZnsTransportZoneVO` (
    `uuid` varchar(32) NOT NULL,
    `znsResourceUuid` varchar(64) NOT NULL,
    `name` varchar(255) DEFAULT NULL,
    `description` text DEFAULT NULL,
    `type` varchar(64) DEFAULT NULL,
    `physicalNetwork` varchar(255) DEFAULT NULL,
    `status` varchar(64) DEFAULT NULL,
    `isDefault` tinyint(1) NOT NULL DEFAULT 0,
    `tags` text DEFAULT NULL,
    `znsSdnControllerUuid` varchar(32) NOT NULL,
    `createDate`         timestamp       NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`         timestamp       NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_zns_tz_resource` (`znsSdnControllerUuid`, `znsResourceUuid`),
    CONSTRAINT `fkZnsTransportZoneVOSdnControllerVO` FOREIGN KEY (`znsSdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ZNS Wave 2: Tenant/TenantRouter resource modeling (ZCF-2133)

CREATE TABLE IF NOT EXISTS `ZnsTenantVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `sdnControllerUuid` VARCHAR(32) NOT NULL,
  `znsResourceUuid` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_zns_tenant_resource` (`sdnControllerUuid`, `znsResourceUuid`),
  CONSTRAINT `fk_zns_tenant_sdn` FOREIGN KEY (`sdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `ZnsTenantRouterVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `sdnControllerUuid` VARCHAR(32) NOT NULL,
  `tenantUuid` VARCHAR(32) DEFAULT NULL,
  `znsResourceUuid` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `state` VARCHAR(32) DEFAULT NULL COMMENT 'Active / Inactive',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_zns_tr_resource` (`sdnControllerUuid`, `znsResourceUuid`),
  KEY `idx_zns_tr_tenant` (`tenantUuid`),
  CONSTRAINT `fk_zns_tr_tenant` FOREIGN KEY (`tenantUuid`) REFERENCES `ZnsTenantVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

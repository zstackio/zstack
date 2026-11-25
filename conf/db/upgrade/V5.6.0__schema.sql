CREATE TABLE IF NOT EXISTS `zstack`.`FineTuneRunVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `modelServiceInstanceGroupUuid` VARCHAR(32) NOT NULL,
    `generation` INT NOT NULL,
    `runId` VARCHAR(64) NOT NULL,
    `modelUuid` VARCHAR(32) DEFAULT NULL,
    `modelVersion` VARCHAR(255) DEFAULT NULL,
    `datasetUuids` TEXT,
    `state` VARCHAR(16) NOT NULL,
    `phase` VARCHAR(32) DEFAULT NULL,
    `acceptedAt` TIMESTAMP NULL DEFAULT NULL,
    `finishedAt` TIMESTAMP NULL DEFAULT NULL,
    `lastReportedAt` TIMESTAMP(3) NULL DEFAULT NULL,
    `exitCode` INT DEFAULT NULL,
    `error` TEXT,
    `nodes` MEDIUMTEXT,
    `createDate` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_fine_tune_run_group_generation_id` (`modelServiceInstanceGroupUuid`, `generation`, `runId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL ADD_COLUMN('DatasetVO', 'version', 'INT', 0, 1);
CALL ADD_COLUMN('FineTuneRunVO', 'datasetVersions', 'TEXT', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`FineTuneRunDeletionVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `createDate` TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL ADD_COLUMN('FineTuneRunVO', 'trainingParameters', 'TEXT', 1, NULL);

CALL ADD_COLUMN('ModelVO', 'introductionFilePath', 'VARCHAR(2048)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsControllerApiCompatibilityVO` (
    `uuid` varchar(32) NOT NULL,
    `controllerUuid` varchar(32) NOT NULL,
    `token` varchar(64) NOT NULL,
    `status` varchar(32) NOT NULL,
    `expectedVersion` varchar(32) DEFAULT NULL,
    `actualVersion` varchar(32) DEFAULT NULL,
    `source` varchar(32) NOT NULL,
    `differenceDetails` text DEFAULT NULL,
    `impact` text DEFAULT NULL,
    `fallbackSolution` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukControllerToken` (`controllerUuid`, `token`),
    INDEX `idxControllerUuid` (`controllerUuid`),
    CONSTRAINT `fkZnsControllerApiCompatibilityVOSdnControllerVO`
        FOREIGN KEY (`controllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CALL ADD_COLUMN('ZnsSegmentRefVO', 'managedBy', 'VARCHAR(32)', 1, NULL);
UPDATE `zstack`.`ZnsSegmentRefVO` SET `managedBy` = 'ZNS' WHERE `managedBy` IS NULL;

-- ZCF-6749: shared durable inbox for sync-resource-v2.
DROP PROCEDURE IF EXISTS `ENSURE_ZNS_SYNC_RESOURCE_V2_COLUMNS`;
DELIMITER $$
CREATE PROCEDURE `ENSURE_ZNS_SYNC_RESOURCE_V2_COLUMNS`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsNetworkNotificationVO'
                     AND column_name = 'protocolVersion') THEN
        ALTER TABLE `zstack`.`ZnsNetworkNotificationVO`
            ADD COLUMN `protocolVersion` int NOT NULL DEFAULT 1;
    END IF;
    ALTER TABLE `zstack`.`ZnsNetworkNotificationVO` ALTER COLUMN `protocolVersion` SET DEFAULT 1;
    ALTER TABLE `zstack`.`ZnsResourceEventReceiptVO` MODIFY COLUMN `eventCursor` bigint NULL;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsResourceEventReceiptVO'
                     AND column_name = 'deliveryKey') THEN
        ALTER TABLE `zstack`.`ZnsResourceEventReceiptVO`
            ADD COLUMN `deliveryKey` varchar(255) CHARACTER SET ascii COLLATE ascii_bin NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsResourceEventReceiptVO'
                     AND index_name = 'ukZnsResourceEventReceiptDelivery') THEN
        ALTER TABLE `zstack`.`ZnsResourceEventReceiptVO`
            ADD UNIQUE KEY `ukZnsResourceEventReceiptDelivery` (`controllerUuid`, `taskUuid`, `deliveryKey`);
    END IF;
END $$
DELIMITER ;
CALL `ENSURE_ZNS_SYNC_RESOURCE_V2_COLUMNS`();
DROP PROCEDURE `ENSURE_ZNS_SYNC_RESOURCE_V2_COLUMNS`;

ALTER TABLE `zstack`.`ZnsSegmentInventoryVO` MODIFY COLUMN `transportZoneUuid` varchar(36) NULL;
CALL ADD_COLUMN('ZnsSegmentRefVO', 'leaseUntil', 'TIMESTAMP NULL', 1, NULL);
CALL ADD_COLUMN('ZnsSegmentRefVO', 'fenceToken', 'varchar(32)', 1, NULL);
CALL ADD_COLUMN('ZnsSegmentRefVO', 'fenceUntil', 'TIMESTAMP NULL', 1, NULL);

DROP TABLE IF EXISTS `zstack`.`ZnsControllerTransitionVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsControllerCapabilityVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsCloudOfferSnapshotVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsPeerOfferVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsCloudProtocolIdentityVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsSegmentProjectionDeleteVO`;

CALL DELETE_INDEX('ZnsSegmentRefVO', 'idxZnsSegmentRefVOOperationUuid');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationCapabilityName');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationCapabilityContract');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationCapabilityEpoch');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationProviderIdentity');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationProfileIdentity');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationAdapterIdentity');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationUuid');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationStep');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationDigest');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'leaseUntil');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'currentConfigVersion');

CALL ADD_COLUMN('ZnsSegmentRefVO', 'consistencyStatus', 'VARCHAR(32)', 0, 'Unknown');
CALL ADD_COLUMN('ZnsSegmentRefVO', 'consistencyDetails', 'TEXT', 1, NULL);

ALTER TABLE `ZnsSegmentRefVO`
    MODIFY COLUMN `zoneUuid` VARCHAR(32) DEFAULT NULL,
    MODIFY COLUMN `l2NetworkUuid` VARCHAR(32) DEFAULT NULL,
    MODIFY COLUMN `accountUuid` VARCHAR(32) DEFAULT NULL;

UPDATE `zstack`.`SystemTagVO`
SET `tag` = CONCAT('flatNetwork::Dhcp6Server::',
                   SUBSTRING(`tag`, CHAR_LENGTH('flatNetwork::DhcpServer::') + 1))
WHERE `resourceType` = 'L3NetworkVO'
  AND `tag` LIKE 'flatNetwork::DhcpServer::%::ipUuid::%'
  AND LOCATE(':', REPLACE(SUBSTRING_INDEX(
      SUBSTRING(`tag`, CHAR_LENGTH('flatNetwork::DhcpServer::') + 1),
      '::ipUuid::', 1), '--', '::')) > 0;

-- Generic runtime metadata for multi-vendor AI VM image selection.

CALL ADD_COLUMN('ModelServiceTemplateVO', 'runtimeName', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'runtimeVersionPattern', 'VARCHAR(2048)', 1, NULL);

-- Begin ZSTAC-88163: create the three zsdataset tables.
-- They were registered in premium's conf/persistence.xml without any DDL here, so the
-- management node threw "Table 'zstack.ZsDatasetSpaceRefVO' doesn't exist" on the first
-- query and almost every integration case collapsed with it.
-- Shaped after DatasetVO in the same module: it also extends ResourceVO and likewise
-- declares no foreign key to ResourceVO. Timestamp defaults follow the AI tables added
-- earlier in this file rather than a zero date, which strict SQL mode rejects.
CREATE TABLE IF NOT EXISTS `zstack`.`ZsDatasetSpaceRefVO` (
    `uuid`            varchar(32)  NOT NULL UNIQUE,
    `spaceId`         varchar(255) NOT NULL,
    `appInstanceUuid` varchar(36)  NOT NULL,
    `lastOpDate`      TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`      TIMESTAMP    NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZsDatasetServiceKeyVO` (
    `uuid`            varchar(32)  NOT NULL UNIQUE,
    `appInstanceUuid` varchar(36)  NOT NULL,
    `keyId`           varchar(255) NOT NULL,
    `secret`          varchar(255) NOT NULL,
    `lastOpDate`      TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`      TIMESTAMP    NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- downloadToken holds the VM's publication download token, an HMAC-SHA256 digest encoded as
-- base64url, so it is always 43 characters; varchar(2048) leaves ample room for a later
-- scheme without reserving an off-page text column. errorSummary is a failure summary widened
-- past the JPA default of 255 so a long message is stored rather than truncated.
CREATE TABLE IF NOT EXISTS `zstack`.`ZsDatasetPublicationVO` (
    `uuid`             varchar(32)   NOT NULL UNIQUE,
    `appInstanceUuid`  varchar(36)   NOT NULL,
    `spaceId`          varchar(255)  NOT NULL,
    `publishRequestId` varchar(255)  NOT NULL,
    `modelCenterUuid`  varchar(32)   DEFAULT NULL,
    `downloadToken`    varchar(2048) DEFAULT NULL,
    `datasetUuid`      varchar(32)   DEFAULT NULL,
    `errorSummary`     varchar(2048) DEFAULT NULL,
    `state`            varchar(32)   NOT NULL,
    `lastOpDate`       TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`       TIMESTAMP     NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- End ZSTAC-88163.

-- The original V5.5.38 schema created these columns as varchar(32) on some
-- installations. Editing that already-applied migration does not widen them.
-- Marketplace application instance UUIDs contain hyphens and require 36 characters.

ALTER TABLE `zstack`.`ZsDatasetSpaceRefVO`
    MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL;

ALTER TABLE `zstack`.`ZsDatasetServiceKeyVO`
    MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL;

ALTER TABLE `zstack`.`ZsDatasetPublicationVO`
    MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL;

-- Store 4-byte UTF-8 characters (emoji) in API request and result records.
--
-- These columns hold the serialized API message and event. With the 3-byte utf8 charset an API
-- whose request or result contains an emoji fails to persist its async record: the webhook is
-- never sent and the caller waits until its own timeout, although the API itself succeeded.
-- The JDBC connection already negotiates utf8mb4, so converting the columns is sufficient.
-- Same approach as GuestVmScriptExecutedRecordDetailVO in V5.5.6.

ALTER TABLE `zstack`.`AsyncRestVO`
    MODIFY `requestData` LONGTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci` DEFAULT NULL,
    MODIFY `result` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci` DEFAULT NULL;

ALTER TABLE `zstack`.`AuditsVO`
    MODIFY `requestDump` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`,
    MODIFY `responseDump` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

ALTER TABLE `ScsiLunVmInstanceRefVO` ADD COLUMN `diskMode` VARCHAR(32) NOT NULL DEFAULT 'scsi-block';

UPDATE `zstack`.`AffinityGroupVO` AS ag
SET ag.zoneUuid = COALESCE(
    (SELECT MIN(vm.zoneUuid)
     FROM `zstack`.`AffinityGroupUsageVO` AS ref
     JOIN `zstack`.`VmInstanceVO` AS vm ON vm.uuid = ref.resourceUuid
     WHERE ref.affinityGroupUuid = ag.uuid
     HAVING COUNT(DISTINCT vm.zoneUuid) = 1),
    (SELECT MIN(uuid) FROM `zstack`.`ZoneVO` HAVING COUNT(*) = 1))
WHERE ag.zoneUuid IS NULL;

UPDATE `zstack`.`VmSchedulingRuleGroupVO` AS ruleGroup
JOIN `zstack`.`AffinityGroupVO` AS ag ON ag.uuid = ruleGroup.srcUuid
SET ruleGroup.zoneUuid = ag.zoneUuid
WHERE ruleGroup.zoneUuid IS NULL AND ag.zoneUuid IS NOT NULL;

DROP PROCEDURE IF EXISTS mergeDuplicateScsiLuns;
DELIMITER $$
CREATE PROCEDURE mergeDuplicateScsiLuns()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS ScsiLunUpgradeGroup, ScsiLunUpgradeMap;
        RESIGNAL;
    END;

    START TRANSACTION;
    CREATE TEMPORARY TABLE ScsiLunUpgradeGroup AS
    SELECT l.source, COALESCE(i.iscsiTargetUuid, f.fiberChannelStorageUuid) AS scopeUuid,
           BINARY l.wwid AS wwid,
           COALESCE(MIN(CASE WHEN v.scsiLunUuid IS NOT NULL THEN l.uuid END), MIN(l.uuid)) AS keepUuid
    FROM LunVO l
    LEFT JOIN IscsiLunVO i ON i.uuid = l.uuid AND l.source = 'iSCSI'
    LEFT JOIN FiberChannelLunVO f ON f.uuid = l.uuid AND l.source = 'fiberChannel'
    LEFT JOIN (SELECT DISTINCT scsiLunUuid FROM ScsiLunVmInstanceRefVO) v ON v.scsiLunUuid = l.uuid
    WHERE l.source IN ('iSCSI', 'fiberChannel') AND l.wwid <> ''
      AND COALESCE(i.iscsiTargetUuid, f.fiberChannelStorageUuid) IS NOT NULL
    GROUP BY l.source, scopeUuid, BINARY l.wwid HAVING COUNT(*) > 1;

    CREATE TEMPORARY TABLE ScsiLunUpgradeMap AS
    SELECT l.uuid AS oldUuid, g.keepUuid
    FROM LunVO l
    LEFT JOIN IscsiLunVO i ON i.uuid = l.uuid AND l.source = 'iSCSI'
    LEFT JOIN FiberChannelLunVO f ON f.uuid = l.uuid AND l.source = 'fiberChannel'
    JOIN ScsiLunUpgradeGroup g ON g.source = l.source AND g.wwid = BINARY l.wwid
      AND g.scopeUuid = COALESCE(i.iscsiTargetUuid, f.fiberChannelStorageUuid)
    WHERE l.uuid <> g.keepUuid;

    DELETE r FROM ScsiLunHostRefVO r
    JOIN ScsiLunUpgradeMap m ON m.oldUuid = r.scsiLunUuid
    JOIN ScsiLunHostRefVO k ON k.scsiLunUuid = m.keepUuid AND k.hostUuid = r.hostUuid;
    UPDATE ScsiLunHostRefVO r JOIN ScsiLunUpgradeMap m ON m.oldUuid = r.scsiLunUuid
    SET r.scsiLunUuid = m.keepUuid;
    DELETE r FROM ScsiLunHostRefVO r
    JOIN ScsiLunHostRefVO k ON k.scsiLunUuid = r.scsiLunUuid AND k.hostUuid = r.hostUuid AND k.id < r.id
    JOIN ScsiLunUpgradeGroup g ON g.keepUuid = r.scsiLunUuid;

    UPDATE ScsiLunVmInstanceRefVO r JOIN ScsiLunUpgradeMap m ON m.oldUuid = r.scsiLunUuid
    SET r.scsiLunUuid = m.keepUuid, r.lastOpDate = r.lastOpDate;

    DELETE l FROM IscsiLunVO l JOIN ScsiLunUpgradeMap m ON m.oldUuid = l.uuid;
    DELETE l FROM FiberChannelLunVO l JOIN ScsiLunUpgradeMap m ON m.oldUuid = l.uuid;
    DELETE l FROM LunVO l JOIN ScsiLunUpgradeMap m ON m.oldUuid = l.uuid;
    DELETE r FROM ResourceVO r JOIN ScsiLunUpgradeMap m ON m.oldUuid = r.uuid;
    DROP TEMPORARY TABLE ScsiLunUpgradeMap, ScsiLunUpgradeGroup;
    COMMIT;
END$$
DELIMITER ;
CALL mergeDuplicateScsiLuns();
DROP PROCEDURE mergeDuplicateScsiLuns;

CREATE TABLE IF NOT EXISTS `zstack`.`PhysicalServerVO` (
    `uuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) DEFAULT NULL,
    `serialNumber` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukPhysicalServerSerialNumber` (`serialNumber`),
    CONSTRAINT `fkPhysicalServerVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `zstack`.`ZoneEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PhysicalServerResourceAssignmentVO` (
    `uuid` varchar(32) NOT NULL,
    `serverUuid` varchar(32) NOT NULL,
    `roleType` varchar(64) NOT NULL,
    `cpuSet` varchar(4096) NOT NULL DEFAULT '',
    `memory` bigint unsigned DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukPhysicalServerResourceAssignment` (`serverUuid`, `roleType`),
    CONSTRAINT `fkPhysicalServerResourceAssignmentServerUuid`
        FOREIGN KEY (`serverUuid`) REFERENCES `zstack`.`PhysicalServerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('HostEO', 'serverUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ManagementNodeVO', 'serverUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ImageStoreBackupStorageVO', 'serverUuid', 'VARCHAR(32)', 1, NULL);

UPDATE `zstack`.`HostEO`
SET `serverUuid` = NULL
WHERE `deleted` IS NOT NULL AND `serverUuid` IS NOT NULL;

DROP PROCEDURE IF EXISTS addPhysicalServerIdentityUniqueKeys;
DELIMITER $$
CREATE PROCEDURE addPhysicalServerIdentityUniqueKeys()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = 'zstack'
          AND table_name = 'HostEO'
          AND index_name = 'ukHostEOServerUuid'
    ) THEN
        ALTER TABLE `zstack`.`HostEO`
            ADD UNIQUE KEY `ukHostEOServerUuid` (`serverUuid`);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = 'zstack'
          AND table_name = 'ManagementNodeVO'
          AND index_name = 'ukManagementNodeVOServerUuid'
    ) THEN
        ALTER TABLE `zstack`.`ManagementNodeVO`
            ADD UNIQUE KEY `ukManagementNodeVOServerUuid` (`serverUuid`);
    END IF;
END $$
DELIMITER ;
CALL addPhysicalServerIdentityUniqueKeys();
DROP PROCEDURE IF EXISTS addPhysicalServerIdentityUniqueKeys;

CALL ADD_CONSTRAINT(
    'HostEO',
    'fkHostEOServerUuid',
    'serverUuid',
    'PhysicalServerVO',
    'uuid',
    'SET NULL'
);

DROP VIEW IF EXISTS `zstack`.`HostVO`;
CREATE VIEW `zstack`.`HostVO` AS
    SELECT uuid, zoneUuid, clusterUuid, name, description, managementIp, hypervisorType,
           state, status, createDate, lastOpDate, architecture, serverUuid
    FROM `zstack`.`HostEO`
    WHERE deleted IS NULL;

CALL ADD_CONSTRAINT(
    'ManagementNodeVO',
    'fkManagementNodeVOServerUuid',
    'serverUuid',
    'PhysicalServerVO',
    'uuid',
    'SET NULL'
);

INSERT INTO `zstack`.`GlobalConfigVO`
    (`name`, `description`, `category`, `defaultValue`, `value`)
SELECT
    'resourceAssignment.enabled',
    'Enable physical server resource assignment enforcement',
    'physicalServer',
    'false',
    'false'
FROM DUAL
WHERE EXISTS (SELECT 1 FROM `zstack`.`AccountVO` LIMIT 1)
  AND NOT EXISTS (
      SELECT 1 FROM `zstack`.`GlobalConfigVO`
      WHERE `category` = 'physicalServer'
        AND `name` = 'resourceAssignment.enabled'
  );

CALL ADD_COLUMN('CephOsdGroupVO', 'snapshotsTotalSize', 'BIGINT', 1, NULL);
CALL ADD_COLUMN('CephPrimaryStorageVO', 'snapshotsTotalSize', 'BIGINT', 1, NULL);

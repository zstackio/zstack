-- Do not filter by architecture here. The upgrade preserves previous Windows VM behavior across all architectures;
-- current kvmagent consumption is still gated by host CPU architecture at start time.
INSERT INTO `zstack`.`ResourceConfigVO` (`uuid`, `name`, `description`, `category`, `value`, `resourceUuid`, `resourceType`, `lastOpDate`, `createDate`)
SELECT REPLACE(UUID(), '-', ''), 'vm.cpu.hardwareVirtualization', 'enable or disable hardware virtualization feature in Windows guest cpuid',
       'kvm', 'true', vm.`uuid`, 'VmInstanceVO', NOW(), NOW()
FROM `zstack`.`VmInstanceVO` vm
WHERE (vm.`platform` IN ('Windows', 'WindowsVirtio')
    OR LOWER(IFNULL(vm.`guestOsType`, '')) LIKE '%windows%')
  AND NOT EXISTS (
      SELECT 1
      FROM `zstack`.`ResourceConfigVO` rc
      WHERE rc.`resourceUuid` = vm.`uuid`
        AND rc.`category` = 'kvm'
        AND rc.`name` = 'vm.cpu.hardwareVirtualization'
  );

ALTER TABLE `zstack`.`DRSVmMigrationActivityVO` MODIFY COLUMN `result` TEXT DEFAULT NULL;

UPDATE `zstack`.`PciDeviceMdevSpecRefVO` keepRef
JOIN (
    SELECT `pciDeviceUuid`, `mdevSpecUuid`, MAX(`id`) AS `keepId`, MAX(`effective`) AS `effective`
    FROM `zstack`.`PciDeviceMdevSpecRefVO`
    GROUP BY `pciDeviceUuid`, `mdevSpecUuid`
) groupedRef ON keepRef.`id` = groupedRef.`keepId`
SET keepRef.`effective` = groupedRef.`effective`;

DELETE duplicateRef FROM `zstack`.`PciDeviceMdevSpecRefVO` duplicateRef
JOIN `zstack`.`PciDeviceMdevSpecRefVO` keepRef
  ON duplicateRef.`pciDeviceUuid` = keepRef.`pciDeviceUuid`
 AND duplicateRef.`mdevSpecUuid` = keepRef.`mdevSpecUuid`
 AND duplicateRef.`id` < keepRef.`id`;

UPDATE `zstack`.`PciDeviceMdevSpecRefVO` ref
JOIN (
    SELECT activeRef.`id`
    FROM `zstack`.`PciDeviceMdevSpecRefVO` activeRef
    JOIN (
        SELECT `pciDeviceUuid`
        FROM `zstack`.`PciDeviceMdevSpecRefVO`
        WHERE `effective` = 1
        GROUP BY `pciDeviceUuid`
        HAVING COUNT(*) > 1
    ) duplicatedPci ON activeRef.`pciDeviceUuid` = duplicatedPci.`pciDeviceUuid`
    WHERE activeRef.`effective` = 1
      AND NOT EXISTS (
          SELECT 1
          FROM `zstack`.`MdevDeviceVO` mdev
          WHERE mdev.`parentUuid` = activeRef.`pciDeviceUuid`
            AND mdev.`mdevSpecUuid` = activeRef.`mdevSpecUuid`
      )
) staleRef ON ref.`id` = staleRef.`id`
SET ref.`effective` = 0;

UPDATE `zstack`.`PciDeviceMdevSpecRefVO` oldRef
JOIN `zstack`.`PciDeviceMdevSpecRefVO` newRef
  ON oldRef.`pciDeviceUuid` = newRef.`pciDeviceUuid`
 AND oldRef.`effective` = 1
 AND newRef.`effective` = 1
 AND oldRef.`id` < newRef.`id`
SET oldRef.`effective` = 0;

DROP PROCEDURE IF EXISTS addPciDeviceMdevSpecRefUniqueKey;
DELIMITER $$
CREATE PROCEDURE addPciDeviceMdevSpecRefUniqueKey()
BEGIN
    DECLARE index_count INT DEFAULT 0;

    SELECT COUNT(*) INTO index_count
    FROM information_schema.statistics
    WHERE table_schema = 'zstack'
      AND table_name = 'PciDeviceMdevSpecRefVO'
      AND index_name = 'ukPciDeviceMdevSpecRefVOPciUuidMdevSpecUuid';

    IF index_count < 1 THEN
        ALTER TABLE `zstack`.`PciDeviceMdevSpecRefVO`
            ADD UNIQUE KEY `ukPciDeviceMdevSpecRefVOPciUuidMdevSpecUuid` (`pciDeviceUuid`, `mdevSpecUuid`);
    END IF;

    SELECT CURTIME();
END $$
DELIMITER ;
CALL addPciDeviceMdevSpecRefUniqueKey();
DROP PROCEDURE IF EXISTS addPciDeviceMdevSpecRefUniqueKey;

-- ZCF-4158: Store SCIM event application state.
CREATE TABLE IF NOT EXISTS `zstack`.`ScimEventVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `clientId` varchar(128) NOT NULL DEFAULT 'default',
    `eventId` varchar(255) NOT NULL,
    `resourceType` varchar(64) NOT NULL,
    `resourceId` varchar(255) NOT NULL,
    `operation` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `payloadHash` varchar(128) DEFAULT NULL,
    `errorMessage` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukScimEventVOClientEvent` (`clientId`, `eventId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `HostCacheStoreVO` (
    `uuid`              VARCHAR(32)    NOT NULL,
    `hostUuid`          VARCHAR(32)    NOT NULL,
    `name`              VARCHAR(255)   DEFAULT NULL,
    `description`       VARCHAR(2048)  DEFAULT NULL,
    `mountPoint`        VARCHAR(255)   DEFAULT NULL,
    `devices`           TEXT,
    `state`             VARCHAR(32)    NOT NULL,
    `status`            VARCHAR(32)    NOT NULL,
    `createDate`        TIMESTAMP      NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostCacheStoreVOHostEO`
    FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`)
    ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `HostCacheStoreCapacityVO` (
    `uuid`              VARCHAR(32)    NOT NULL,
    `totalCapacity`     BIGINT         NOT NULL DEFAULT 0,
    `availableCapacity` BIGINT         NOT NULL DEFAULT 0,
    `totalPhysicalCapacity`     BIGINT NOT NULL DEFAULT 0,
    `availablePhysicalCapacity` BIGINT NOT NULL DEFAULT 0,
    `systemUsedCapacity`        BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostCacheStoreCapacityVOHostCacheStoreVO`
    FOREIGN KEY (`uuid`) REFERENCES `HostCacheStoreVO` (`uuid`)
    ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `VolumeCacheVO` (
    `uuid`        VARCHAR(32)   NOT NULL,
    `volumeUuid`  VARCHAR(32)   NOT NULL,
    `poolUuid`    VARCHAR(32)   DEFAULT NULL,
    `installPath` VARCHAR(2048) DEFAULT NULL,
    `cacheMode`   VARCHAR(32)   NOT NULL,
    `status`      VARCHAR(32)   NOT NULL,
    `createDate`  TIMESTAMP     NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uniVolumeCacheVOVolumeUuid` (`volumeUuid`),
    CONSTRAINT `fkVolumeCacheVOVolumeEO`
    FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`)
    ON DELETE CASCADE,
    CONSTRAINT `fkVolumeCacheVOPoolUuid`
    FOREIGN KEY (`poolUuid`) REFERENCES `HostCacheStoreVO` (`uuid`)
    ON DELETE SET NULL
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

-- ZCF-4419: Track resources synchronized from external identity sources.
CREATE TABLE IF NOT EXISTS `zstack`.`ResourceSourceRefVO` (
    `uuid` varchar(32) NOT NULL COMMENT 'uuid',
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(64) NOT NULL,
    `sourceType` varchar(64) NOT NULL,
    `sourceName` varchar(128) DEFAULT NULL,
    `externalUuid` varchar(32) DEFAULT NULL,
    `externalType` varchar(255) DEFAULT NULL,
    `syncType` varchar(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukResourceSourceRefVOResourceSourceSync` (`resourceUuid`, `resourceType`, `sourceType`, `syncType`),
    KEY `idxResourceSourceRefVOSourceSyncResourceType` (`sourceType`, `syncType`, `resourceType`),
    KEY `idxResourceSourceRefVOResourceSync` (`resourceType`, `resourceUuid`, `syncType`),
    KEY `idxResourceSourceRefVOSyncType` (`syncType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS upgrade_vtep_ip_not_null;
DELIMITER $$
CREATE PROCEDURE upgrade_vtep_ip_not_null()
BEGIN
    IF EXISTS (SELECT 1 FROM `zstack`.`VtepVO` WHERE `vtepIp` IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VtepVO.vtepIp contains NULL values';
    END IF;

    IF EXISTS (SELECT 1 FROM `zstack`.`RemoteVtepVO` WHERE `vtepIp` IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RemoteVtepVO.vtepIp contains NULL values';
    END IF;
END $$
DELIMITER ;

CALL upgrade_vtep_ip_not_null();
DROP PROCEDURE IF EXISTS upgrade_vtep_ip_not_null;

ALTER TABLE `zstack`.`VtepVO` MODIFY COLUMN `vtepIp` varchar(128) NOT NULL;
ALTER TABLE `zstack`.`RemoteVtepVO` MODIFY COLUMN `vtepIp` varchar(128) NOT NULL;

INSERT INTO `zstack`.`SystemTagVO` (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(), '-', ''), z.uuid, 'ZoneVO', 0, 'System', 'managementNetwork::ipVersion::ipv4', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
FROM `zstack`.`ZoneVO` z
WHERE NOT EXISTS (
    SELECT 1
    FROM `zstack`.`SystemTagVO` st
    WHERE st.resourceUuid = z.uuid
      AND st.resourceType = 'ZoneVO'
      AND st.type = 'System'
      AND st.tag LIKE 'managementNetwork::ipVersion::%'
);

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

ALTER TABLE `zstack`.`ConsoleProxyAgentVO` ADD COLUMN `consoleProxyOverriddenIpv4` varchar(255) DEFAULT NULL;
ALTER TABLE `zstack`.`ConsoleProxyAgentVO` ADD COLUMN `consoleProxyOverriddenIpv6` varchar(255) DEFAULT NULL;

ALTER TABLE `zstack`.`AlarmVO` ADD COLUMN `recoveryDuration` int unsigned DEFAULT NULL;
ALTER TABLE `zstack`.`AlarmVO` ADD COLUMN `recoveryThreshold` int unsigned DEFAULT NULL;

DROP PROCEDURE IF EXISTS addAlarmLabelLookupIndex;
DELIMITER $$
CREATE PROCEDURE addAlarmLabelLookupIndex()
BEGIN
    DECLARE index_count INT DEFAULT 0;

    SELECT COUNT(*) INTO index_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'AlarmLabelVO'
      AND index_name = 'idxAlarmLabelVOAlarmUuidKey';

    IF index_count < 1 THEN
        ALTER TABLE `zstack`.`AlarmLabelVO`
            ADD INDEX `idxAlarmLabelVOAlarmUuidKey` (`alarmUuid`, `key`);
    END IF;
END $$
DELIMITER ;

CALL addAlarmLabelLookupIndex();
DROP PROCEDURE IF EXISTS addAlarmLabelLookupIndex;
CALL DELETE_INDEX('AlarmLabelVO', 'alarmUuid');

CREATE TABLE IF NOT EXISTS `zstack`.`AlarmResourceStateVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `alarmUuid` varchar(32) NOT NULL,
    `identifyLabel` varchar(191) NOT NULL,
    `resourceUuid` varchar(32) DEFAULT NULL,
    `resourceType` varchar(256) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `lastStatusChangeTime` bigint(20) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukAlarmUuidIdentifyLabel` (`alarmUuid`, `identifyLabel`, `resourceUuid`),
    KEY `idxAlarmResourceStateVOresourceUuid` (`resourceUuid`),
    CONSTRAINT `fkAlarmResourceStateVOAlarmVO` FOREIGN KEY (`alarmUuid`) REFERENCES `AlarmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ZSTAC-75429: scope AI ModelCenter-derived resources by zone.
CALL ADD_COLUMN('ModelVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('DatasetVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceVO', 'launchCommand', 'MEDIUMTEXT', 1, NULL);
CALL ADD_COLUMN('ZdfsVO', 'metaServerPort', 'INT', 0, 6379);

UPDATE `zstack`.`ModelCenterVO` mc
INNER JOIN (
    SELECT ms.`modelCenterUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceVO` ms
    INNER JOIN `zstack`.`ModelServiceInstanceGroupVO` g ON g.`modelServiceUuid` = ms.`uuid`
    INNER JOIN `zstack`.`ModelServiceInstanceVO` i ON i.`modelServiceGroupUuid` = g.`uuid`
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
      AND ms.`modelCenterUuid` IS NOT NULL
    GROUP BY ms.`modelCenterUuid`
) inferred ON inferred.`modelCenterUuid` = mc.`uuid`
SET mc.`zoneUuid` = inferred.`zoneUuid`
WHERE mc.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

UPDATE `zstack`.`ModelVO` m
INNER JOIN `zstack`.`ModelCenterVO` mc ON m.`modelCenterUuid` = mc.`uuid`
SET m.`zoneUuid` = mc.`zoneUuid`
WHERE m.`zoneUuid` IS NULL;

UPDATE `zstack`.`ModelServiceVO` ms
INNER JOIN `zstack`.`ModelCenterVO` mc ON ms.`modelCenterUuid` = mc.`uuid`
SET ms.`zoneUuid` = mc.`zoneUuid`
WHERE ms.`zoneUuid` IS NULL;

UPDATE `zstack`.`DatasetVO` d
INNER JOIN `zstack`.`ModelCenterVO` mc ON d.`modelCenterUuid` = mc.`uuid`
SET d.`zoneUuid` = mc.`zoneUuid`
WHERE d.`zoneUuid` IS NULL;

UPDATE `zstack`.`ModelServiceVO` ms
INNER JOIN (
    SELECT g.`modelServiceUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceInstanceGroupVO` g
    INNER JOIN `zstack`.`ModelServiceInstanceVO` i ON i.`modelServiceGroupUuid` = g.`uuid`
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
      AND g.`modelServiceUuid` IS NOT NULL
    GROUP BY g.`modelServiceUuid`
) inferred ON inferred.`modelServiceUuid` = ms.`uuid`
SET ms.`zoneUuid` = inferred.`zoneUuid`
WHERE ms.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

UPDATE `zstack`.`ModelVO` m
INNER JOIN (
    SELECT g.`modelUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceInstanceGroupVO` g
    INNER JOIN `zstack`.`ModelServiceInstanceVO` i ON i.`modelServiceGroupUuid` = g.`uuid`
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
      AND g.`modelUuid` IS NOT NULL
    GROUP BY g.`modelUuid`
) inferred ON inferred.`modelUuid` = m.`uuid`
SET m.`zoneUuid` = inferred.`zoneUuid`
WHERE m.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

UPDATE `zstack`.`ModelServiceInstanceGroupVO` g
LEFT JOIN `zstack`.`ModelServiceVO` ms ON g.`modelServiceUuid` = ms.`uuid`
LEFT JOIN `zstack`.`ModelVO` m ON g.`modelUuid` = m.`uuid`
SET g.`zoneUuid` = COALESCE(ms.`zoneUuid`, m.`zoneUuid`)
WHERE g.`zoneUuid` IS NULL;

UPDATE `zstack`.`ModelServiceInstanceGroupVO` g
INNER JOIN (
    SELECT i.`modelServiceGroupUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceInstanceVO` i
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
    GROUP BY i.`modelServiceGroupUuid`
) inferred ON inferred.`modelServiceGroupUuid` = g.`uuid`
SET g.`zoneUuid` = inferred.`zoneUuid`
WHERE g.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

CALL ADD_CONSTRAINT('ModelVO', 'fkModelVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');
CALL ADD_CONSTRAINT('ModelServiceVO', 'fkModelServiceVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');
CALL ADD_CONSTRAINT('DatasetVO', 'fkDatasetVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');
CALL ADD_CONSTRAINT('ModelServiceInstanceGroupVO', 'fkModelServiceInstanceGroupVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');

-- ZSTAC-84111: Persist Zaku health state on NativeClusterVO for query and manual recovery.
CALL ADD_COLUMN('NativeClusterVO', 'zakuHealthStatus', 'VARCHAR(32)', 1, 'Unknown');
UPDATE `zstack`.`NativeClusterVO` SET `zakuHealthStatus` = 'Unknown' WHERE `zakuHealthStatus` IS NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`ExternalPrimaryStorageHostProtocolRefVO` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `protocol` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukExternalPrimaryStorageHostProtocolRefVO` (`primaryStorageUuid`, `hostUuid`, `protocol`),
    CONSTRAINT `fkExternalPrimaryStorageHostProtocolRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkExternalPrimaryStorageHostProtocolRefVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `zstack`.`PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CALL DROP_COLUMN('ExternalPrimaryStorageHostRefVO', 'protocol');

-- ZCF-4875: persist pending SCIM relation intents for out-of-order role binding materialization.
CREATE TABLE IF NOT EXISTS `zstack`.`IAM2ScimRelationIntentVO` (
    `uuid` varchar(32) NOT NULL,
    `sourceType` varchar(64) NOT NULL,
    `syncType` varchar(64) NOT NULL,
    `externalUuid` varchar(32) NOT NULL,
    `relationType` varchar(64) NOT NULL,
    `subjectType` varchar(32) NOT NULL,
    `subjectUuid` varchar(32) NOT NULL,
    `objectType` varchar(64) NOT NULL,
    `objectUuid` varchar(32) NOT NULL,
    `scopeType` varchar(64) DEFAULT NULL,
    `scopeUuid` varchar(32) DEFAULT NULL,
    `enabled` tinyint(1) NOT NULL DEFAULT 1,
    `attributesJson` varchar(2048) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukIAM2ScimRelationIntentSource` (`sourceType`, `syncType`, `relationType`, `externalUuid`),
    KEY `idxIAM2ScimRelationIntentSubject` (`relationType`, `subjectType`, `subjectUuid`),
    KEY `idxIAM2ScimRelationIntentObject` (`relationType`, `objectType`, `objectUuid`),
    KEY `idxIAM2ScimRelationIntentScope` (`relationType`, `scopeType`, `scopeUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

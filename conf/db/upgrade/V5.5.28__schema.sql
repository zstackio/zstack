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
    `resourceVersion` bigint NOT NULL,
    `operation` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `payloadHash` varchar(128) DEFAULT NULL,
    `errorMessage` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukScimEventVOClientEvent` (`clientId`, `eventId`),
    KEY `idxScimEventVOResourceVersion` (`clientId`, `resourceType`, `resourceId`, `resourceVersion`)
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
    `allocated`         BIGINT         NOT NULL DEFAULT 0,
    `dirty`             BIGINT         NOT NULL DEFAULT 0,
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

ALTER TABLE `zstack`.`ConsoleProxyAgentVO` ADD COLUMN `consoleProxyOverriddenIpv4` varchar(255) DEFAULT NULL;
ALTER TABLE `zstack`.`ConsoleProxyAgentVO` ADD COLUMN `consoleProxyOverriddenIpv6` varchar(255) DEFAULT NULL;

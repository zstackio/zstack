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

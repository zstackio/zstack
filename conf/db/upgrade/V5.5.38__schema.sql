CALL ADD_COLUMN('SNSSnmpPlatformVO', 'version', 'VARCHAR(32)', 0, 'v1');
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'community', 'TEXT', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'userName', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'authEnabled', 'tinyint(1)', 0, 0);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'authAlgorithm', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'authPassword', 'TEXT', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'privacyEnabled', 'tinyint(1)', 0, 0);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'privacyAlgorithm', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'privacyPassword', 'TEXT', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'configRevision', 'BIGINT UNSIGNED', 0, 0);

CREATE TABLE IF NOT EXISTS `zstack`.`SnmpEngineVO` (
    `uuid` varchar(32) NOT NULL,
    `engineId` varchar(64) NOT NULL UNIQUE,
    `engineBoots` int unsigned NOT NULL DEFAULT 1,
    `engineStartTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `ownerManagementNodeUuid` varchar(32) DEFAULT NULL,
    `ownerEpochUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PhysicalServerVO` (
    `uuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) DEFAULT NULL,
    `serialNumber` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukPhysicalServerSerialNumber` (`serialNumber`)
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

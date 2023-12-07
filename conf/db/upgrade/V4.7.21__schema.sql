CREATE TABLE `NvmeServerVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `name` VARCHAR(256) NOT NULL,
  `ip` VARCHAR(64) NOT NULL,
  `port` int unsigned DEFAULT 4420,
  `transport` VARCHAR(32) NOT NULL,
  `state` VARCHAR(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NvmeServerClusterRefVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `clusterUuid` VARCHAR(32) NOT NULL,
  `nvmeServerUuid` VARCHAR(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  CONSTRAINT `fkNvmeServerClusterRefVONvmeServerVO` FOREIGN KEY (`nvmeServerUuid`) REFERENCES NvmeServerVO (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNvmeServerClusterRefVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES ClusterEO (`uuid`) ON DELETE CASCADE
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`NvmeTargetVO` ADD COLUMN `nvmeServerUuid` varchar(32);
ALTER TABLE `zstack`.`NvmeTargetVO` ADD CONSTRAINT `fkNvmeTargetVONvmeServerVO` FOREIGN KEY (nvmeServerUuid) REFERENCES NvmeServerVO(uuid) ON DELETE SET NULL;

ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `userinfoUrl` varchar(256) DEFAULT NULL;
ALTER TABLE `zstack`.`SSOClientVO` ADD COLUMN `redirectUrl` varchar(256) DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `logoutUrl` varchar(256) DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2TokenVO` MODIFY COLUMN `accessToken` text DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2TokenVO` MODIFY COLUMN `idToken` text DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2TokenVO` MODIFY COLUMN `refreshToken` text DEFAULT NULL;
CALL CREATE_INDEX('SSOTokenVO', 'idxSSOTokenVOUserUuid', 'userUuid');

CREATE TABLE IF NOT EXISTS `zstack`.SshKeyPairVO (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `publicKey` varchar(4096) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.SshKeyPairRefVO (
    `id` int(11) NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceUuid` varchar(32) NOT NULL,
    `sshKeyPairUuid` varchar(32) NOT NULL,
    `resourceType` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkSshKeyPairRefVOVmInstanceEO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkSshKeyPairRefVOSshKey` FOREIGN KEY (`sshKeyPairUuid`) REFERENCES `SshKeyPairVO` (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SnmpAgentVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `version` varchar(32) NOT NULL COMMENT 'snmp authentication version',
    `readCommunity` varchar(32) DEFAULT NULL,
    `userName` varchar(32) DEFAULT NULL,
    `authAlgorithm` varchar(32) DEFAULT NULL,
    `authPassword` varchar(32) DEFAULT NULL,
    `privacyAlgorithm` varchar(32) DEFAULT NULL,
    `privacyPassword` varchar(32) DEFAULT NULL,
    `status` varchar(32) NOT NULL COMMENT 'SNMP agent status. status is enable which means start snmp with mn start.',
    `port` int(10) DEFAULT NULL,
    `securityLevel` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSSnmpPlatformVO`
(
    `uuid`        varchar(32)       NOT NULL,
    `snmpAddress` varchar(128)      NOT NULL,
    `snmpPort`    smallint unsigned NOT NULL,
    `createDate`  timestamp         NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp         NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `ukipAddrPort` (`snmpAddress`, `snmpPort`) USING BTREE,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

DELETE FROM `CpuFeaturesHistoryVO`;

CREATE TABLE IF NOT EXISTS `zstack`.`RemoteVtepVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `vtepIp` varchar(32) NOT NULL,
  `port` int NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `poolUuid` varchar(32) NOT NULL,
        `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
        `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY  (`uuid`),
        CONSTRAINT fkRemoteVtepVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE CASCADE,
        UNIQUE KEY `ukRemoteVtepIpPoolUuidClusterUuid` (`vtepIp`,`poolUuid`,`clusterUuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `zstack`.`EncryptEntityMetadataVO` (`entityName`, `columnName`, `state`, `lastOpDate`, `createDate`)
                VALUES ('IAM2ProjectAttributeVO', 'value', 'NeedDecrypt', NOW(), NOW());

CREATE TABLE IF NOT EXISTS `zstack`.`PrimaryStorageHistoricalUsageBaseVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `resourceUuid` varchar(32),
    `resourceType` varchar(32) NOT NULL,
    `totalPhysicalCapacity` bigint unsigned DEFAULT 0,
    `usedPhysicalCapacity` bigint unsigned DEFAULT 0,
    `recordDate` timestamp DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkUsageVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES PrimaryStorageEO (`uuid`) ON DELETE CASCADE,
    INDEX (primaryStorageUuid),
    INDEX (resourceUuid),
    INDEX (resourceType),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `zstack`.PrimaryStorageHistoricalUsageVO;
CREATE VIEW `zstack`.`PrimaryStorageHistoricalUsageVO` AS SELECT
id, primaryStorageUuid, resourceType, totalPhysicalCapacity, usedPhysicalCapacity, recordDate, createDate, lastOpDate
FROM `zstack`.`PrimaryStorageHistoricalUsageBaseVO` WHERE resourceType = 'PrimaryStorageVO';

DROP VIEW IF EXISTS `zstack`.CephOsdGroupHistoricalUsageVO;
CREATE VIEW `zstack`.`CephOsdGroupHistoricalUsageVO` AS SELECT
id, primaryStorageUuid, resourceUuid as osdGroupUuid, resourceType, totalPhysicalCapacity, usedPhysicalCapacity, recordDate, createDate, lastOpDate
FROM `zstack`.`PrimaryStorageHistoricalUsageBaseVO` WHERE resourceType = 'CephOsdGroupVO';

DROP VIEW IF EXISTS `zstack`.LocalStorageHostHistoricalUsageVO;
CREATE VIEW `zstack`.`LocalStorageHostHistoricalUsageVO` AS SELECT
id, primaryStorageUuid, resourceUuid as hostUuid, resourceType, totalPhysicalCapacity, usedPhysicalCapacity, recordDate, createDate, lastOpDate
FROM `zstack`.`PrimaryStorageHistoricalUsageBaseVO` WHERE resourceType = 'LocalStorageHostRefVO';

ALTER TABLE `zstack`.`InstanceOfferingEO` ADD COLUMN `reservedMemorySize` bigint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN `reservedMemorySize` bigint unsigned DEFAULT 0;

DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId, lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type, hypervisorType, cpuNum, cpuSpeed, memorySize, reservedMemorySize, platform, guestOsType, allocatorStrategy, createDate, lastOpDate, state, architecture FROM `zstack`.`VmInstanceEO` WHERE deleted IS NULL;
DROP VIEW IF EXISTS `zstack`.`InstanceOfferingVO`;
CREATE VIEW `zstack`.`InstanceOfferingVO` AS SELECT uuid, name, description, cpuNum, cpuSpeed, memorySize, reservedMemorySize, allocatorStrategy, sortKey, state, createDate, lastOpDate, type, duration FROM `zstack`.`InstanceOfferingEO` WHERE deleted IS NULL;

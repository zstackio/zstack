ALTER TABLE `zstack`.`SNSTopicVO` ADD COLUMN `locale` varchar(32);

ALTER TABLE `zstack`.`HostNumaNodeVO` MODIFY COLUMN `nodeCPUs` TEXT NOT NULL;
ALTER TABLE `zstack`.`VmInstanceNumaNodeVO` MODIFY COLUMN `vNodeCPUs` TEXT NOT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`HostOsCategoryVO` (
    `uuid` char(32) NOT NULL UNIQUE COMMENT 'uuid',
    `architecture` varchar(32) NOT NULL,
    `osReleaseVersion` varchar(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`KvmHostHypervisorMetadataVO` (
    `uuid` char(32) NOT NULL UNIQUE COMMENT 'uuid',
    `categoryUuid` char(32) NOT NULL,
    `managementNodeUuid` char(32) NOT NULL,
    `hypervisor` varchar(32) NOT NULL,
    `version` varchar(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `KvmHostHypervisorMetadataVOHostOsCategoryVO` FOREIGN KEY (`categoryUuid`) REFERENCES `zstack`.`HostOsCategoryVO` (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`KvmHypervisorInfoVO` (
    `uuid` char(32) NOT NULL UNIQUE COMMENT 'uuid',
    `hypervisor` varchar(32) NOT NULL,
    `version` varchar(64) NOT NULL,
    `matchState` char(10) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `KvmHypervisorInfoVOResourceVO` FOREIGN KEY (`uuid`) REFERENCES `zstack`.`ResourceVO` (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`UsedIpVO` MODIFY COLUMN `ipRangeUuid` varchar(32) DEFAULT NULL;

ALTER TABLE `zstack`.`L3NetworkEO` ADD COLUMN `enableIPAM` boolean NOT NULL DEFAULT TRUE;
DROP VIEW IF EXISTS `zstack`.`L3NetworkVO`;
CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, description, state, type, zoneUuid, l2NetworkUuid, system, dnsDomain, createDate, lastOpDate, category, ipVersion, enableIPAM FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;
ALTER TABLE `zstack`.`UsedIpVO` DROP FOREIGN KEY fkUsedIpVOVmNicVO;
ALTER TABLE `zstack`.`UsedIpVO` ADD CONSTRAINT fkUsedIpVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE CASCADE;

INSERT INTO SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(),'-',''), vm.uuid, 'VmInstanceVO', 0, 'System', 'vRingBufferSize::256::256', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
FROM VmInstanceVO vm LEFT JOIN SystemTagVO st ON st.resourceUuid = vm.uuid AND st.tag LIKE 'vRingBufferSize::%' WHERE vm.state = 'running' AND st.uuid IS NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`SSOTokenVO`(
    `uuid` varchar(32) not null unique,
    `clientUuid` varchar(32) DEFAULT NULL,
    `userUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSSOTokenVOClientVO` FOREIGN KEY (`clientUuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`OAuth2TokenVO`(
    `uuid` varchar(32) not null unique,
    `accessToken` text not null,
    `idToken` text not null,
    `refreshToken` text not null,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkOAuth2TokenVOSSOTokenVO` FOREIGN KEY (`uuid`) REFERENCES `SSOTokenVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`GuestToolsStateVO` ADD COLUMN `zwatchState` varchar(32) NOT NULL DEFAULT 'NotInstalled';
ALTER TABLE `zstack`.`GuestToolsStateVO` CHANGE `state` `qgaState` varchar(32) NOT NULL DEFAULT 'NotInstalled';

CREATE TABLE IF NOT EXISTS `NvmeTargetVO` (
    `name` VARCHAR(256) DEFAULT NULL,
    `uuid` VARCHAR(32) NOT NULL,
    `nqn` VARCHAR(256) NOT NULL,
    `state` VARCHAR(64) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NvmeLunVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `nvmeTargetUuid` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkNvmeLunVONvmeTargetVO` FOREIGN KEY (`nvmeTargetUuid`) REFERENCES NvmeTargetVO (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NvmeLunHostRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `nvmeLunUuid` varchar(32) NOT NULL,
    `hctl` VARCHAR(64) DEFAULT NULL,
    `path` VARCHAR(128) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkNvmeLunHostRefVONvmeLunVO` FOREIGN KEY (`nvmeLunUuid`) REFERENCES `NvmeLunVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkNvmeLunHostRefVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `ScsiLunVO` RENAME AS LunVO;

# for compatibility
CREATE VIEW `ScsiLunVO` AS SELECT uuid, name, wwid, vendor, model, wwn, serial, type, hctl, path, size, state, source, multipathDeviceUuid, createDate, lastOpDate FROM `LunVO` WHERE source IN ('iSCSI', 'fiberChannel');
INSERT INTO `NvmeLunHostRefVO` (hostUuid, nvmeLunUuid, hctl, path, lastOpDate, createDate) SELECT hostUuid, scsiLunUuid, hctl, path, lastOpDate, createDate FROM `ScsiLunHostRefVO` WHERE scsiLunUuid NOT IN (SELECT uuid FROM `ScsiLunVO`);
DELETE FROM `ScsiLunHostRefVO` WHERE scsiLunUuid not in (SELECT uuid FROM `ScsiLunVO`);


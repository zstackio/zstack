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
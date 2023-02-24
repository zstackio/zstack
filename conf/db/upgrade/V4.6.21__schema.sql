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
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `KvmHypervisorInfoVOResourceVO` FOREIGN KEY (`uuid`) REFERENCES `zstack`.`ResourceVO` (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

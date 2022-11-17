alter table LicenseHistoryVO modify COLUMN `userName` varchar(64) NOT NULL;

ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `type` char(32) DEFAULT 'unknown';

CREATE TABLE IF NOT EXISTS `zstack`.`VmVdpaNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `pciDeviceUuid` varchar(32) DEFAULT NULL,
    `lastPciDeviceUuid` varchar(32) DEFAULT NULL,
    `srcPath` varchar(128) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVmVdpaNicVOPciDeviceVO` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`VipNetworkServicesRefVO` DROP INDEX `uuid`;
ALTER TABLE `zstack`.`VipNetworkServicesRefVO` DROP PRIMARY KEY, ADD PRIMARY KEY(`uuid`,`serviceType`,`vipUuid`);

CREATE TABLE IF NOT EXISTS `zstack`.`VmQgaVO` (
    `vmInstanceUuid` varchar(32) NOT NULL UNIQUE,
    `state` varchar(32) NOT NULL DEFAULT 'Unknown',
    `version` varchar(32),
    `platform` varchar(32),
    `osType` varchar(32),
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`vmInstanceUuid`),
    CONSTRAINT `fkVmQgaVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmConfigSyncResultVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `success` boolean NOT NULL,
    `errCode` varchar(64),
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVmConfigSyncResultVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

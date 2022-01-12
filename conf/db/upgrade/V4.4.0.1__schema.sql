ALTER TABLE `zstack`.`HostNetworkBondingVO` ADD COLUMN `type` char(32) DEFAULT 'unknown';

CREATE TABLE IF NOT EXISTS `zstack`.`VmVdpaNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `pciDeviceUuid` varchar(32) DEFAULT NULL,
    `lastPciDeviceUuid` varchar(32) DEFAULT NULL,
    `srcPath` varchar(128) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVmVdpaNicVOPciDeviceVO` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE IF NOT EXISTS `zstack`.`VdpaPortVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `hostUuid` varchar(32) NOT NULL,
    `pciDeviceUuid` varchar(32) DEFAULT NULL,
    `parentPciDeviceUuid` varchar(32) DEFAULT NULL,
    `pciDeviceAddress` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkVdpaPortVOPciDeviceVO FOREIGN KEY (`pciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE VmVdpaNicVO DROP FOREIGN KEY fkVmVdpaNicVOPciDeviceVO;
ALTER TABLE VmVdpaNicVO DROP COLUMN `pciDeviceUuid`;
ALTER TABLE VmVdpaNicVO DROP COLUMN `lastPciDeviceUuid`;
ALTER TABLE VmVdpaNicVO ADD COLUMN `vdpaPortUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE VmVdpaNicVO ADD COLUMN `lastVdpaPortUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`VmVdpaNicVO` ADD CONSTRAINT fkVmVdpaNicVOVdpaPortVO FOREIGN KEY (`vdpaPortUuid`) REFERENCES `zstack`.`VdpaPortVO` (`uuid`) ON DELETE SET NULL;
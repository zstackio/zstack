ALTER TABLE JsonLabelVO MODIFY COLUMN labelValue MEDIUMTEXT;

ALTER TABLE `zstack`.`VmNicVO` ADD COLUMN `type` varchar(32) DEFAULT 'VNIC';

CREATE TABLE IF NOT EXISTS `zstack`.`VmVfNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `pciDeviceUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVmVfNicVOPciDeviceVO` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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

ALTER TABLE `zstack`.`VmNicVO` ADD COLUMN `state` varchar(255) NOT NULL DEFAULT "enable";

UPDATE `zstack`.`GlobalConfigVO` SET value="enable", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="true";
UPDATE `zstack`.`GlobalConfigVO` SET value="ignore", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="false";

CREATE TABLE IF NOT EXISTS `zstack`.`LicenseAppIdRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `licenseId` varchar(32) NOT NULL,
    `appId` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `interfaceModel` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `vendorId` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `deviceId` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `subvendorId` VARCHAR(64)  DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `subdeviceId` VARCHAR(64)  DEFAULT NULL;
ALTER TABLE `zstack`.`MonitorGroupTemplateRefVO`
    ADD COLUMN `isApplied` boolean not null DEFAULT TRUE;

ALTER TABLE VmNicVO DROP INDEX `ukVmNicVO`;

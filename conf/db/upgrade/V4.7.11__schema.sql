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

CALL DELETE_INDEX('VmNicVO', 'ukVmNicVO');

CALL DELETE_INDEX('QuotaVO', 'idxIdentityUuid');
ALTER TABLE `QuotaVO` ADD INDEX `idxIdentityUuid` (`identityUuid`);

CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkInterfaceServiceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `interfaceUuid` varchar(32) NOT NULL,
    `vlanId` int(32) NOT NULL DEFAULT 0,
    `serviceType` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkHostNetworkInterfaceServiceRefVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES HostNetworkInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkBondingServiceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `bondingUuid` varchar(32) NOT NULL,
    `vlanId` int(32) NOT NULL DEFAULT 0,
    `serviceType` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkHostNetworkBodnuingServiceRefVOHostNetworkBondingVO` FOREIGN KEY (`bondingUuid`) REFERENCES HostNetworkBondingVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`VtepVO` ADD COLUMN `physicalInterface` VARCHAR(32) DEFAULT NULL AFTER `poolUuid`;
UPDATE AlarmVO
SET NAME = 'Host Memory Used Capacity Per Host alarm'
WHERE uuid = 'ue0x30t7wfyuba87nwk6ywu3ub5svtwk';

CALL CREATE_INDEX('SchedulerJobHistoryVO', 'idxSchedulerJobHistoryVOExecuteTime', 'executeTime');

ALTER TABLE `zstack`.`AlarmRecordsVO`
    MODIFY COLUMN `resourceUuid` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL AFTER `resourceType`;

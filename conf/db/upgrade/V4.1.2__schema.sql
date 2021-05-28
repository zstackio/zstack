ALTER TABLE `zstack`.`PciDeviceVO` ADD `chooser` varchar(32) DEFAULT 'None';
UPDATE PciDeviceVO SET chooser='None' WHERE vmInstanceUuid IS NULL;
UPDATE PciDeviceVO SET chooser='Device' WHERE vmInstanceUuid IS NOT NULL;

ALTER TABLE `zstack`.`MdevDeviceVO` ADD `chooser` varchar(32) DEFAULT 'None';
UPDATE MdevDeviceVO SET chooser='None' WHERE vmInstanceUuid IS NULL;
UPDATE MdevDeviceVO SET chooser='Device' WHERE vmInstanceUuid IS NOT NULL;

UPDATE VmInstancePciSpecDeviceRefVO AS ref LEFT JOIN PciDeviceVO AS pci
    ON ref.pciDeviceUuid = pci.uuid
    SET chooser='Spec'
    WHERE ref.vmInstanceUuid = pci.vmInstanceUuid;
UPDATE VmInstanceMdevSpecDeviceRefVO AS ref LEFT JOIN MdevDeviceVO AS mdev
    ON ref.mdevDeviceUuid = mdev.uuid
    SET chooser='Spec'
    WHERE ref.vmInstanceUuid = mdev.vmInstanceUuid;
DROP TABLE IF EXISTS VmInstancePciSpecDeviceRefVO;
DROP TABLE IF EXISTS VmInstanceMdevSpecDeviceRefVO;

DELETE FROM SystemTagVO WHERE tag LIKE 'pciDevice::%';
DELETE FROM SystemTagVO WHERE tag LIKE 'mdevDevice::%';

ALTER TABLE `zstack`.`LoadBalancerListenerVO` ADD COLUMN `securityPolicyType` varchar(255);

DROP PROCEDURE IF EXISTS updateLoadBalancerListenerVO;
DELIMITER $$
CREATE PROCEDURE updateLoadBalancerListenerVO()
BEGIN
    DECLARE uuid VARCHAR(32);
    DECLARE protocol VARCHAR(64);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT lbl.uuid,lbl.protocol FROM `zstack`.`LoadBalancerListenerVO` lbl;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    update_loop: LOOP
        FETCH cur INTO uuid,protocol;
        IF done THEN
            LEAVE update_loop;
        END IF;

        IF protocol = "https" THEN UPDATE `zstack`.`LoadBalancerListenerVO` lbl SET lbl.securityPolicyType = "tls_cipher_policy_default" WHERE lbl.uuid = uuid;
        END IF;
    END LOOP;
    CLOSE cur;

END $$
DELIMITER ;

CALL updateLoadBalancerListenerVO();
DROP PROCEDURE IF EXISTS updateLoadBalancerListenerVO;

ALTER TABLE `zstack`.`HostEO` ADD COLUMN architecture varchar(32) DEFAULT NULL;

UPDATE HostEO h
INNER JOIN (
SELECT
substring_index(tag,"::",-1) arch,
resourceUuid ru
FROM
SystemTagVO
WHERE
tag like "cpuArchitecture%"
) tags on tags.ru = h.uuid
SET h.architecture = tags.arch;

UPDATE HostEO set architecture = "x86_64" WHERE architecture IS NULL;
DROP VIEW IF EXISTS `zstack`.`HostVO`;
CREATE VIEW `zstack`.`HostVO` AS SELECT uuid, zoneUuid, clusterUuid, name, description, managementIp, hypervisorType, state, status, createDate, lastOpDate, architecture FROM `zstack`.`HostEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN architecture varchar(32) DEFAULT NULL;

UPDATE VmInstanceEO v
INNER JOIN HostEO h
ON (v.hostUuid = h.uuid OR v.lastHostUuid = h.uuid)
set v.architecture = h.architecture;

DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId, lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type, hypervisorType, cpuNum, cpuSpeed, memorySize, platform, allocatorStrategy, createDate, lastOpDate, state, architecture FROM `zstack`.`VmInstanceEO` WHERE deleted IS NULL;


ALTER TABLE `zstack`.`ImageEO` ADD COLUMN `virtio` boolean DEFAULT TRUE;
UPDATE `zstack`.`ImageEO` SET virtio = FALSE, guestOsType = "Windows" WHERE platform="Windows";
UPDATE `zstack`.`ImageEO` SET platform = "Windows", virtio = TRUE, guestOsType = "Windows" WHERE platform = "WindowsVirtio";
UPDATE `zstack`.`ImageEO` SET virtio = TRUE, guestOsType = "Linux" WHERE platform = "Linux";
UPDATE `zstack`.`ImageEO` SET virtio = FALSE, guestOsType = "Other" WHERE platform = "Other";
UPDATE `zstack`.`ImageEO` SET platform = "Other", virtio = TRUE, guestOsType = "Other" WHERE platform = "Paravirtualization";

DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, `system`, mediaType, guestOsType, architecture, virtio, createDate, lastOpDate FROM `zstack`.`ImageEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN `guestOsType` varchar(255) DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId, lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type, hypervisorType, cpuNum, cpuSpeed, memorySize, platform, guestOsType, allocatorStrategy, createDate, lastOpDate, state, architecture FROM `zstack`.`VmInstanceEO` WHERE deleted IS NULL;

CREATE TABLE IF NOT EXISTS `GuestOsCategoryVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `platform` VARCHAR(32) NOT NULL,
    `name` VARCHAR(32) NOT NULL,
    `version` VARCHAR(32),
    `osRelease` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(),'-',''), t.uuid, 'VmInstanceVO', 0, 'System', 'driver::virtio', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM VmInstanceEO t WHERE t.platform IN ('Paravirtualization', 'WindowsVirtio', 'Linux');
UPDATE `zstack`.`VmInstanceEO` SET guestOsType = "Windows" WHERE platform="Windows";
UPDATE `zstack`.`VmInstanceEO` SET platform = "Windows", guestOsType = "Windows" WHERE platform = "WindowsVirtio";
UPDATE `zstack`.`VmInstanceEO` SET guestOsType = "Linux" WHERE platform = "Linux";
UPDATE `zstack`.`VmInstanceEO` SET guestOsType = "Other" WHERE platform = "Other";
UPDATE `zstack`.`VmInstanceEO` SET platform = "Other", guestOsType = "Other" WHERE platform = "Paravirtualization";

UPDATE `zstack`.`VtepVO` SET port=8472 WHERE port=4789;

CREATE TABLE IF NOT EXISTS `zstack`.`EventRecordsVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `createTime` bigint  NOT NULL,
    `accountUuid` varchar(32) ,
    `dataUuid` varchar(32) ,
    `emergencyLevel` varchar(64) DEFAULT NULL,
    `name` varchar(256) DEFAULT NULL,
    `error` text DEFAULT NULL,
    `labels` text DEFAULT NULL,
    `namespace` varchar(256) DEFAULT NULL,
    `readStatus` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `resourceId` varchar(32) DEFAULT NULL,
    `resourceName` varchar(256) DEFAULT NULL,
    `subscriptionUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX `idxEventRecordsVOcreateTimeid` ON EventRecordsVO (`createTime`,`id`);
CREATE INDEX `idxEventRecordsVOaccountUuid` ON EventRecordsVO (`accountUuid`);
CREATE INDEX `idxEventRecordsVOemergencyLevel` ON EventRecordsVO (`emergencyLevel`);
CREATE INDEX `idxEventRecordsVOname` ON EventRecordsVO (`name`);
CREATE INDEX `idxEventRecordsVOsubscriptionUuid` ON EventRecordsVO (`subscriptionUuid`);

CREATE TABLE IF NOT EXISTS `zstack`.`AlarmRecordsVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `createTime` bigint  NOT NULL,
    `accountUuid` varchar(32),
    `alarmName` varchar(255) NOT NULL,
    `alarmStatus` varchar(64) DEFAULT NULL,
    `alarmUuid` varchar(32) ,
    `comparisonOperator` varchar(128) DEFAULT NULL,
    `context`  text DEFAULT NULL,
    `dataUuid` varchar(32) ,
    `emergencyLevel` varchar(64) DEFAULT NULL,
    `labels` text DEFAULT NULL,
    `metricName` varchar(256) DEFAULT NULL,
    `metricValue` double DEFAULT NULL,
    `namespace` varchar(256) DEFAULT NULL,
    `period` int unsigned NOT NULL,
    `readStatus` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `resourceType` VARCHAR(256) NOT NULL,
    `resourceUuid` varchar(256) ,
    `threshold` double NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX `idxAlarmRecordsVOcreateTimeid` ON AlarmRecordsVO (`createTime`,`id`);
CREATE INDEX `idxAlarmRecordsVOaccountUuid` ON AlarmRecordsVO (`accountUuid`);
CREATE INDEX `idxAlarmRecordsVOalarmUuid` ON AlarmRecordsVO (`alarmUuid`);
CREATE INDEX `idxAlarmRecordsVOemergencyLevel` ON AlarmRecordsVO (`emergencyLevel`);

CREATE TABLE IF NOT EXISTS `zstack`.`AuditsVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `createTime` bigint  NOT NULL,
    `apiName` varchar(2048) NOT NULL,
    `clientBrowser` VARCHAR(64) NOT NULL,
    `clientIp` VARCHAR(64) NOT NULL,
    `duration` int unsigned NOT NULL,
    `error` text DEFAULT NULL,
    `operator` varchar(256) DEFAULT NULL,
    `requestDump` text DEFAULT NULL,
    `resourceType` VARCHAR(256) NOT NULL,
    `resourceUuid` varchar(32),
    `requestUuid` varchar(32),
    `responseDump`  text DEFAULT NULL,
    `success` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'api call success or failed',
    PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
CREATE INDEX `idxAuditsVOcreateTimeid` ON AuditsVO (`createTime`,`id`);
CREATE INDEX `idxAuditsVOresourceUuid` ON AuditsVO (`resourceUuid`);
CREATE INDEX `idxAuditsVOsuccess` ON AuditsVO (`success`);

alter table AlarmRecordsVO add column hour int(10);
alter table AlarmRecordsVO add index idxAccountUuidHourEmergencyLevel(`accountUuid`,`hour`,`emergencyLevel`);
alter table AlarmRecordsVO add index idxCreateTimeReadStatusEmergencyLevel (`createTime`, `emergencyLevel`, `readStatus`, `accountUuid`);
alter table AlarmRecordsVO add index idxDataUuid (`dataUuid`);

alter table EventRecordsVO add column hour int(10);
alter table EventRecordsVO add index idxAccountUuidHourEmergencyLevel(`accountUuid`,`hour`,`emergencyLevel`);
alter table EventRecordsVO add index idxCreateTimeReadStatusEmergencyLevel (`createTime`, `emergencyLevel`, `readStatus`, `accountUuid`);
alter table EventRecordsVO add index idxDataUuid (`dataUuid`);

alter table AuditsVO add column operatorAccountUuid varchar(32);
alter table AuditsVO add index idxOperatorAccountUuid (`operatorAccountUuid`);
alter table AuditsVO add index idxRequestUuid (`requestUuid`);
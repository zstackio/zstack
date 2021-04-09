DROP PROCEDURE IF EXISTS upgradeProjectOperatorSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeProjectOperatorSystemTags()
BEGIN
    DECLARE projectOperatorTag VARCHAR(62);
    DECLARE targetProjectUuid VARCHAR(32);
    DECLARE iameTargetAccountUuid VARCHAR(32);
    DECLARE iam2VirtualIDUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag where systemTag.tag like 'projectOperatorOfProjectUuid::%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO projectOperatorTag, iam2VirtualIDUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET targetProjectUuid = SUBSTRING_INDEX(projectOperatorTag, '::', -1);
        SELECT `accountUuid` into iameTargetAccountUuid FROM `IAM2ProjectAccountRefVO` WHERE `projectUuid` = targetProjectUuid LIMIT 1;
        IF (select count(*) from IAM2VirtualIDRoleRefVO where virtualIDUuid = iam2VirtualIDUuid and roleUuid = 'f2f474c60e7340c0a1d44080d5bde3a9' and targetAccountUuid = iameTargetAccountUuid) < 1 THEN
        begin
            INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `targetAccountUuid`, createDate, lastOpDate) VALUES (iam2VirtualIDUuid, 'f2f474c60e7340c0a1d44080d5bde3a9', iameTargetAccountUuid, NOW(), NOW());
        end;
        END IF;
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeProjectOperatorSystemTags();

DROP PROCEDURE IF EXISTS upgradeProjectAdminSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeProjectAdminSystemTags()
BEGIN
    DECLARE projectAdminTag VARCHAR(59);
    DECLARE targetProjectUuid VARCHAR(32);
    DECLARE iameTargetAccountUuid VARCHAR(32);
    DECLARE iam2VirtualIDUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag where systemTag.tag like 'projectAdminOfProjectUuid::%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO projectAdminTag, iam2VirtualIDUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET targetProjectUuid = SUBSTRING_INDEX(projectAdminTag, '::', -1);
        SELECT `accountUuid` into iameTargetAccountUuid FROM `IAM2ProjectAccountRefVO` WHERE `projectUuid` = targetProjectUuid LIMIT 1;
        IF (select count(*) from IAM2VirtualIDRoleRefVO where virtualIDUuid = iam2VirtualIDUuid and roleUuid = '55553cefbbfb42468873897c95408a43' and targetAccountUuid = iameTargetAccountUuid) < 1 THEN
        begin
            INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `targetAccountUuid`, createDate, lastOpDate) VALUES (iam2VirtualIDUuid, '55553cefbbfb42468873897c95408a43', iameTargetAccountUuid, NOW(), NOW());
        end;
        END IF;
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeProjectAdminSystemTags();

ALTER TABLE `zstack`.`SNSEmailPlatformVO` modify COLUMN `password` VARCHAR(255) NULL;
ALTER TABLE `zstack`.`SNSEmailPlatformVO` modify COLUMN `username` VARCHAR(255) NULL;
alter table `ConsoleProxyAgentVO` add `consoleProxyPort` int NOT NULL;

alter table GarbageCollectorVO add index idxName (`name`(255));
alter table GarbageCollectorVO add index idxStatus (`status`);

CREATE TABLE IF NOT EXISTS `zstack`.`LicenseHistoryVO`
(
    `id`          bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid`        varchar(32) NOT NULL,
    `cpuNum`      int(10) NOT NULL,
    `hostNum`     int(10) NOT NULL,
    `vmNum`       int(10) NOT NULL,
    `expiredDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `issuedDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `uploadDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `licenseType` varchar(32) NOT NULL,
    `userName`    varchar(32) NOT NULL,
    `prodInfo`    varchar(32) DEFAULT NULL,
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE INDEX idxLicenseHistoryVOUploadDate ON LicenseHistoryVO (uploadDate);
drop table ElaborationVO;
drop table ResourceUsageVO;

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
UPDATE `zstack`.`ImageEO` SET virtio = FALSE, guestOsType = "other" WHERE platform = "Other";
UPDATE `zstack`.`ImageEO` SET platform = "Other", virtio = TRUE, guestOsType = "other" WHERE platform = "Paravirtualization";

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
SELECT REPLACE(UUID(),'-',''), t.uuid, 'VmInstanceVO', 0, 'System', 'driver::virtio', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM VmInstanceVO t WHERE t.platform IN ('Paravirtualization', 'WindowsVirtio', 'Linux');
UPDATE `zstack`.`VmInstanceVO` SET guestOsType = "Windows" WHERE platform="Windows";
UPDATE `zstack`.`VmInstanceVO` SET platform = "Windows", guestOsType = "Windows" WHERE platform = "WindowsVirtio";
UPDATE `zstack`.`VmInstanceVO` SET guestOsType = "Linux" WHERE platform = "Linux";
UPDATE `zstack`.`VmInstanceVO` SET guestOsType = "other" WHERE platform = "Other";
UPDATE `zstack`.`VmInstanceVO` SET platform = "Other", guestOsType = "other" WHERE platform = "Paravirtualization";
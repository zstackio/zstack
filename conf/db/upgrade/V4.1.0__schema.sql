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
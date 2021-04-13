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

alter table BillingVO drop index idxBillingVOaccountUuid;
alter table BillingVO add index idxAccountUuidEndTime (`accountUuid`,`endTime`);
alter table BillingVO add index idxAccountUuidStartTime (`accountUuid`,`startTime`);
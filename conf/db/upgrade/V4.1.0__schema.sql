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
    `expiredDate` bigint unsigned NOT NULL DEFAULT 0,
    `issuedDate`  bigint unsigned NOT NULL DEFAULT 0,
    `uploadDate`  bigint unsigned NOT NULL DEFAULT 0,
    `licenseType` varchar(32) NOT NULL,
    `userName`    varchar(32) NOT NULL,
    `prodInfo`    varchar(32) DEFAULT NULL,
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ApiVO` (
  `id`          bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid`        varchar(32) NOT NULL,
  `name`        varchar(255) DEFAULT NULL,
  `apiId`       varchar(255) DEFAULT NULL,
  `lastUpdate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ApiLogVO` (
  `id`          bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid`        varchar(32) NOT NULL,
  `name`        varchar(255) DEFAULT NULL,
  `apiId`       varchar(255) DEFAULT NULL,
  `originApiId` varchar(255) DEFAULT NULL,
  `isAnalyzed`  int(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`RStepVO` (
  `id`           bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid`         varchar(32) NOT NULL,
  `fromStepId`   varchar(255) DEFAULT NULL,
  `toStepId`     varchar(255) DEFAULT NULL,
  `apiId`        varchar(255) DEFAULT NULL,
  `weight`       decimal(7,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`StepVO` (
  `id`        bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid`      varchar(32) NOT NULL,
  `stepId`    varchar(255) DEFAULT NULL,
  `name`      varchar(255) DEFAULT NULL,
  `meanWait`  decimal(7,2) DEFAULT NULL,
  `apiId`     varchar(255) DEFAULT NULL,
  `logCount`  int(10) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`StepLogVO` (
  `id`         bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid`       varchar(32) NOT NULL,
  `stepId`     varchar(255) NOT NULL,
  `startTime`  bigint DEFAULT NULL,
  `endTime`    bigint DEFAULT NULL,
  `wait`       decimal(7,2) DEFAULT NULL,
  `name`       varchar(255) DEFAULT NULL,
  `apiLogId`   bigint unsigned DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MsgLogVO` (
    `id`          bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid`        varchar(32) NOT NULL,
    `msgId`       varchar(255) DEFAULT NULL,
    `msgName`     varchar(255) DEFAULT NULL,
    `taskName`    varchar(255) DEFAULT NULL,
    `apiId`       varchar(255) NOT NULL,
    `startTime`   bigint DEFAULT NULL,
    `replyTime`   bigint DEFAULT NULL,
    `wait`        decimal(7,2) DEFAULT NULL,
    `status`      int(1) NOT NULL,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX idxLicenseHistoryVOUploadDate ON LicenseHistoryVO (uploadDate);
drop table ElaborationVO;
drop table ResourceUsageVO;
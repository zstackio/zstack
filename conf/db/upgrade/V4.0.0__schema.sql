ALTER TABLE `zstack`.`TicketStatusHistoryVO` ADD COLUMN `sequence` INT;
ALTER TABLE `zstack`.`ArchiveTicketStatusHistoryVO` ADD COLUMN `sequence` INT;

DROP PROCEDURE IF EXISTS updateTicketStatusHistoryVO;

DELIMITER $$
CREATE PROCEDURE updateTicketStatusHistoryVO()
BEGIN
    DECLARE sequence INT;
    DECLARE uuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE loopCount INT DEFAULT 1;
    DECLARE cur CURSOR FOR SELECT history.sequence,history.uuid FROM `zstack`.`TicketStatusHistoryVO` history WHERE history.fromStatus != 'FinalApproved' ORDER BY history.createDate,history.operationContextType;
    DECLARE extra_cur CURSOR FOR SELECT history.sequence,history.uuid FROM `zstack`.`TicketStatusHistoryVO` history WHERE history.fromStatus = 'FinalApproved' ORDER BY history.createDate,history.operationContextType;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    update_loop: LOOP
        FETCH cur INTO sequence,uuid;
        IF done THEN
            LEAVE update_loop;
        END IF;

        UPDATE `zstack`.`TicketStatusHistoryVO` history SET history.sequence = loopCount WHERE history.uuid = uuid;
        SET loopCount = loopCount + 1;
    END LOOP;
    CLOSE cur;

    SET done = FALSE;
    OPEN extra_cur;
    extra_loop: LOOP
        FETCH extra_cur INTO sequence,uuid;
        IF done THEN
            LEAVE extra_loop;
        END IF;

        UPDATE `zstack`.`TicketStatusHistoryVO` history SET history.sequence = loopCount WHERE history.uuid = uuid;
        SET loopCount = loopCount + 1;
    END LOOP;
    CLOSE extra_cur;

END $$
DELIMITER ;

DROP PROCEDURE IF EXISTS updateArchiveTicketStatusHistoryVO;

DELIMITER $$
CREATE PROCEDURE updateArchiveTicketStatusHistoryVO()
BEGIN
    DECLARE sequence INT;
    DECLARE uuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE loopCount INT DEFAULT 1;
    DECLARE cur CURSOR FOR SELECT history.sequence,history.uuid FROM `zstack`.`ArchiveTicketStatusHistoryVO` history WHERE history.fromStatus != 'FinalApproved' ORDER BY history.createDate,history.operationContextType;
    DECLARE extra_cur CURSOR FOR SELECT history.sequence,history.uuid FROM `zstack`.`ArchiveTicketStatusHistoryVO` history WHERE history.fromStatus = 'FinalApproved' ORDER BY history.createDate,history.operationContextType;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    update_loop: LOOP
        FETCH cur INTO sequence,uuid;
        IF done THEN
            LEAVE update_loop;
        END IF;

        UPDATE `zstack`.`ArchiveTicketStatusHistoryVO` history SET history.sequence = loopCount WHERE history.uuid = uuid;
        SET loopCount = loopCount + 1;
    END LOOP;
    CLOSE cur;

    SET done = FALSE;
    OPEN extra_cur;
    extra_loop: LOOP
        FETCH extra_cur INTO sequence,uuid;
        IF done THEN
            LEAVE extra_loop;
        END IF;

        UPDATE `zstack`.`ArchiveTicketStatusHistoryVO` history SET history.sequence = loopCount WHERE history.uuid = uuid;
        SET loopCount = loopCount + 1;
    END LOOP;
    CLOSE extra_cur;

END $$
DELIMITER ;

call updateTicketStatusHistoryVO();
DROP PROCEDURE IF EXISTS updateTicketStatusHistoryVO;
call updateArchiveTicketStatusHistoryVO();
DROP PROCEDURE IF EXISTS updateArchiveTicketStatusHistoryVO;

ALTER TABLE `zstack`.`TicketStatusHistoryVO` CHANGE sequence sequence INT AUTO_INCREMENT UNIQUE;
ALTER TABLE `zstack`.`ArchiveTicketStatusHistoryVO` CHANGE sequence sequence INT AUTO_INCREMENT UNIQUE;

CREATE TABLE IF NOT EXISTS `zstack`.`VpcFirewallIpSetTemplateVO`
(
    `uuid`        varchar(32)  NOT NULL,
    `name`        varchar(255) NOT NULL,
    `sourceValue` varchar(2048)         DEFAULT NULL,
    `destValue`   varchar(2048)         DEFAULT NULL,
    `type`        varchar(255) NOT NULL,
    `createDate`  timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VpcFirewallRuleTemplateVO`
(
    `uuid`         varchar(32)  NOT NULL,
    `action`       varchar(255) NOT NULL,
    `name`         varchar(255) NOT NULL,
    `protocol`     varchar(255)          DEFAULT NULL,
    `sourcePort`   varchar(255)          DEFAULT NULL,
    `destPort`     varchar(255)          DEFAULT NULL,
    `sourceIp`     varchar(2048)         DEFAULT NULL,
    `destIp`       varchar(2048)         DEFAULT NULL,
    `ruleNumber`   int(10)      NOT NULL,
    `icmpTypeName` varchar(255)          DEFAULT NULL,
    `allowStates`  varchar(255)          DEFAULT NULL,
    `tcpFlag`      varchar(255)          DEFAULT NULL,
    `enableLog`    tinyint(1)   NOT NULL DEFAULT '0',
    `state`        varchar(32)  NOT NULL DEFAULT '0',
    `isDefault`    tinyint(1)   NOT NULL DEFAULT '0',
    `description`  varchar(2048)         DEFAULT NULL,
    `createDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` ADD COLUMN `isApplied` boolean NOT NULL DEFAULT TRUE;
ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` MODIFY `actionType` varchar(255) DEFAULT NULL;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` ADD COLUMN `isApplied` boolean NOT NULL DEFAULT TRUE;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` ADD COLUMN `expired` boolean NOT NULL DEFAULT FALSE;

ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` DROP FOREIGN KEY fkVpcFirewallRuleSetVOVpcFirewallVO;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP FOREIGN KEY fkVpcFirewallRuleVOVpcFirewallVO;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP FOREIGN KEY fkVpcFirewallRuleVOVpcFirewallRuleSetVO;
ALTER TABLE `VpcFirewallRuleSetVO` DROP INDEX `fkVpcFirewallRuleSetVOVpcFirewallVO`;
ALTER TABLE `VpcFirewallRuleVO` DROP INDEX `fkVpcFirewallRuleVOVpcFirewallVO`;
ALTER TABLE `VpcFirewallRuleVO` DROP INDEX `fkVpcFirewallRuleVOVpcFirewallRuleSetVO`;
ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` DROP COLUMN `vyosName`;
ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` DROP COLUMN `vpcFirewallUuid`;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP COLUMN `vpcFirewallUuid`;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP COLUMN `ruleSetName`;

DROP PROCEDURE IF EXISTS checkVirtualhostsExist;
DELIMITER $$
CREATE PROCEDURE checkVirtualhostsExist()
BEGIN
    if((SELECT count(*) from ApplianceVmVO where applianceVmType = "VirtualRouter") > 0) THEN
        SIGNAL SQLSTATE "45000"
            SET MESSAGE_TEXT = "VirtualRouter are not supported this version";
    END IF;
END$$
DELIMITER ;
CALL checkVirtualhostsExist();
DROP PROCEDURE IF EXISTS checkVirtualhostsExist;

ALTER TABLE `zstack`.`VpcRouterVmVO` ADD COLUMN `generalVersion` varchar(32) DEFAULT NULL;
CREATE TABLE `IAM2ProjectRoleVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `iam2ProjectRoleType` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE IAM2VirtualIDRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDRoleRefVOIAM2VirtualIDVO;
ALTER TABLE IAM2VirtualIDRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDRoleRefVORoleVO;
ALTER TABLE IAM2VirtualIDRoleRefVO ADD COLUMN `targetAccountUuid` varchar(32) NOT NULL;
ALTER TABLE IAM2VirtualIDRoleRefVO DROP PRIMARY KEY, ADD PRIMARY KEY(virtualIDUuid, roleUuid, targetAccountUuid);
ALTER TABLE IAM2VirtualIDRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDRoleRefVOIAM2VirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2VirtualIDRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDRoleRefVORoleVO FOREIGN KEY (roleUuid) REFERENCES RoleVO (uuid) ON DELETE CASCADE;
CREATE INDEX idxIAM2VirtualIDRoleRefVOTargetAccountUuid ON IAM2VirtualIDRoleRefVO (targetAccountUuid);

# upgrade PROJECT_OPERATOR_OF_PROJECT and PROJECT_ADMIN_OF_PROJECT to new data structure
DROP PROCEDURE IF EXISTS upgradeProjectOperatorSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeProjectOperatorSystemTags()
BEGIN
    DECLARE projectOperatorTag VARCHAR(62);
    DECLARE targetProjectUuid VARCHAR(32);
    DECLARE targetAccountUuid VARCHAR(32);
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
        SELECT `accountUuid` into targetAccountUuid FROM `IAM2ProjectAccountRefVO` WHERE `projectUuid` = targetProjectUuid LIMIT 1;
        delete from IAM2VirtualIDRoleRefVO where virtualIDUuid = iam2VirtualIDUuid and roleUuid = 'f2f474c60e7340c0a1d44080d5bde3a9';
        INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `targetAccountUuid`, createDate, lastOpDate) VALUES (iam2VirtualIDUuid, 'f2f474c60e7340c0a1d44080d5bde3a9', targetAccountUuid, NOW(), NOW());
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
    DECLARE targetAccountUuid VARCHAR(32);
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
        SELECT `accountUuid` into targetAccountUuid FROM `IAM2ProjectAccountRefVO` WHERE `projectUuid` = targetProjectUuid LIMIT 1;
        delete from IAM2VirtualIDRoleRefVO where virtualIDUuid = iam2VirtualIDUuid and roleUuid = 'f2f474c60e7340c0a1d44080d5bde3a9';
        INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `targetAccountUuid`, createDate, lastOpDate) VALUES (iam2VirtualIDUuid, 'f2f474c60e7340c0a1d44080d5bde3a9', targetAccountUuid, NOW(), NOW());
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeProjectAdminSystemTags();


DROP PROCEDURE IF EXISTS updateIAM2VirtualIDRoleRefCreateAccountUuid;
DElIMITER $$
CREATE PROCEDURE updateIAM2VirtualIDRoleRefCreateAccountUuid()
BEGIN
  DECLARE targetAccountUuid VARCHAR(32);
  DECLARE virtualIDUuid VARCHAR(32);
  DECLARE roleUuid VARCHAR(32);
  DECLARE done INT DEFAULT FALSE;
  DECLARE cur CURSOR FOR select ref.virtualIDUuid, ref.roleUuid from IAM2VirtualIDRoleRefVO ref where ref.targetAccountUuid='';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
  open cur;
  read_loop: LOOP
      FETCH cur INTO virtualIDUuid, roleUuid;
      IF done THEN
            LEAVE read_loop;
      END IF;

      select accountUuid into targetAccountUuid from AccountResourceRefVO where resourceUuid = roleUuid and resourceType = 'RoleVO';
      update IAM2VirtualIDRoleRefVO refvo set refvo.targetAccountUuid = targetAccountUuid where refvo.virtualIDUuid = virtualIDUuid and refvo.roleUuid = roleUuid;
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL updateIAM2VirtualIDRoleRefCreateAccountUuid();



DELIMITER $$
CREATE PROCEDURE insertDefaultIAM2Organization()
BEGIN
    DECLARE virtualIDUuid VARCHAR(32);
    DECLARE organizationUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid, '6e3d19dab98348d8bd67657378843f82' FROM zstack.IAM2VirtualIDVO where type = 'ZStack' and uuid not in (SELECT virtualIDUuid FROM zstack.IAM2VirtualIDOrganizationRefVO);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO virtualIDUuid, organizationUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO `zstack`.IAM2VirtualIDOrganizationRefVO (virtualIDUuid, organizationUuid, createDate, lastOpDate) VALUES (virtualIDUuid, organizationUuid, NOW(), NOW());

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL insertDefaultIAM2Organization();
DROP PROCEDURE IF EXISTS insertDefaultIAM2Organization;


CREATE TABLE `IAM2ProjectVirtualIDGroupRefVO` (
    `groupUuid` VARCHAR(32) NOT NULL,
    `projectUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`groupUuid`,`projectUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE insertIAM2ProjectVirtualIDGroupRef()
BEGIN
    DECLARE groupUuid VARCHAR(32);
    DECLARE projectUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT virtualIDGroup.uuid, virtualIDGroup.projectUuid FROM zstack.IAM2VirtualIDGroupVO virtualIDGroup;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO groupUuid, projectUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO `zstack`.IAM2ProjectVirtualIDGroupRefVO (groupUuid, projectUuid, createDate, lastOpDate) VALUES (groupUuid, projectUuid, NOW(), NOW());

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL insertIAM2ProjectVirtualIDGroupRef();
DROP PROCEDURE IF EXISTS insertIAM2ProjectVirtualIDGroupRef;

Alter table `zstack`.`IAM2VirtualIDGroupVO` modify projectUuid VARCHAR(32) NULL;

ALTER TABLE IAM2VirtualIDGroupRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDGroupRoleRefVOIAM2VirtualIDGroupVO;
ALTER TABLE IAM2VirtualIDGroupRoleRefVO DROP FOREIGN KEY fkIAM2VirtualIDGroupRoleRefVORoleVO;
ALTER TABLE IAM2VirtualIDGroupRoleRefVO ADD COLUMN `targetAccountUuid` varchar(32) NOT NULL;
ALTER TABLE IAM2VirtualIDGroupRoleRefVO DROP PRIMARY KEY, ADD PRIMARY KEY(groupUuid, roleUuid, targetAccountUuid);
ALTER TABLE IAM2VirtualIDGroupRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDGroupRoleRefVOIAM2VirtualIDGroupVO FOREIGN KEY (groupUuid) REFERENCES IAM2VirtualIDGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2VirtualIDGroupRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDGroupRoleRefVORoleVO FOREIGN KEY (roleUuid) REFERENCES RoleVO (uuid) ON DELETE CASCADE;
CREATE INDEX idxIAM2VirtualIDGroupRoleRefVOTargetAccountUuid ON IAM2VirtualIDGroupRoleRefVO (targetAccountUuid);

ALTER TABLE RolePolicyStatementVO ADD INDEX (`roleUuid`);

DELIMITER $$
CREATE PROCEDURE insertIAM2ProjectRoleVOForProjectSystemRoles()
BEGIN
    DECLARE project_system_role_exists INT DEFAULT 0;
    DECLARE iam2_project_role_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO iam2_project_role_exists FROM IAM2ProjectRoleVO WHERE uuid = '55553cefbbfb42468873897c95408a43';
    SELECT COUNT(*) INTO project_system_role_exists FROM RoleVO WHERE uuid = '55553cefbbfb42468873897c95408a43';
    IF iam2_project_role_exists = 0 and project_system_role_exists = 1 THEN
        INSERT INTO IAM2ProjectRoleVO (`uuid`, `iam2ProjectRoleType`) VALUES ('55553cefbbfb42468873897c95408a43', 'CreatedByAdmin');
    END IF;

    SET project_system_role_exists = 0;
    SET iam2_project_role_exists = 0;
    SELECT COUNT(*) INTO iam2_project_role_exists FROM IAM2ProjectRoleVO WHERE uuid = 'f2f474c60e7340c0a1d44080d5bde3a9';
    SELECT COUNT(*) INTO project_system_role_exists FROM RoleVO WHERE uuid = '55553cefbbfb42468873897c95408a43';
    IF iam2_project_role_exists = 0 and project_system_role_exists = 1 THEN
        INSERT INTO IAM2ProjectRoleVO (`uuid`, `iam2ProjectRoleType`) VALUES ('f2f474c60e7340c0a1d44080d5bde3a9', 'CreatedByAdmin');
    END IF;

    SELECT CURTIME();
END $$
DELIMITER ;

CALL insertIAM2ProjectRoleVOForProjectSystemRoles();
DROP PROCEDURE IF EXISTS insertIAM2ProjectRoleVOForProjectSystemRoles;

DELIMITER $$
CREATE PROCEDURE insertIAM2ProjectRole()
BEGIN
    DECLARE roleUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT resourceUuid FROM zstack.AccountResourceRefVO where resourceType = 'RoleVO' and resourceUuid in (select uuid from zstack.RoleVO) and  accountUuid in (SELECT accountUuid FROM zstack.IAM2ProjectAccountRefVO ) and accountUuid <> '36c27e8ff05c4780bf6d2fa65700f22e';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO roleUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO `zstack`.IAM2ProjectRoleVO (uuid, iam2ProjectRoleType) VALUES (roleUuid, 'CreateInProject');

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL insertIAM2ProjectRole();
DROP PROCEDURE IF EXISTS insertIAM2ProjectRole;

ALTER TABLE RolePolicyStatementVO ADD INDEX (`roleUuid`);

DELIMITER $$
CREATE PROCEDURE insertIntoRole()
    BEGIN
        IF (SELECT count(*) from RoleVO where type = 'System' and name like 'read-api-role-%' and uuid <> '86d67c89dfe64b3ba67ecffd34cee418') > 0 THEN
          insert into RoleVO(uuid, name, state, type, createDate, lastOpDate) values ('86d67c89dfe64b3ba67ecffd34cee418', 'read-api-role-default', 'Enabled', 'System', NOW(), NOW());
          INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`) VALUES ('86d67c89dfe64b3ba67ecffd34cee418', 'read-api-role-default', 'RoleVO');
          INSERT INTO AccountResourceRefVO (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`) values ('36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', '86d67c89dfe64b3ba67ecffd34cee418', 'RoleVO', 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'org.zstack.header.identity.role.RoleVO');
          insert into RolePolicyStatementVO(uuid, statement, roleUuid, createDate, lastOpDate) values ('7d800a63539b47e2cec86529cef3cd2d',  '', '86d67c89dfe64b3ba67ecffd34cee418', NOW(), NOW());
        END IF;
    END $$
DELIMITER ;

CALL insertIntoRole();
DROP PROCEDURE IF EXISTS insertIntoRole;


DELIMITER $$
CREATE PROCEDURE deleteRoleReadAPI()
BEGIN
    DECLARE roUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR select uuid from RoleVO where type = 'System' and name like 'read-api-role-%' and uuid <> '86d67c89dfe64b3ba67ecffd34cee418';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO roUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        update IAM2VirtualIDRoleRefVO refVO set refVO.roleUuid = '86d67c89dfe64b3ba67ecffd34cee418' where refVO.roleUuid = roUuid;
        delete from RolePolicyStatementVO where roleUuid = roUuid;
        delete from RoleVO where uuid = roUuid;
        delete from ResourceVO where uuid = roUuid;
        delete from AccountResourceRefVO where resourceUuid = roUuid and resourceType = 'RoleVO';

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL deleteRoleReadAPI();
DROP PROCEDURE IF EXISTS deleteRoleReadAPI;

CREATE TABLE IF NOT EXISTS `zstack`.`MonitorGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `actions` varchar(4096) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MonitorGroupInstanceVO` (
  `uuid` varchar(32) NOT NULL,
  `groupUuid` varchar(32) NOT NULL,
  `instanceResourceType` varchar(128) NOT NULL,
  `instanceUuid` varchar(32) NOT NULL,
  `status` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `groupUuid` (`groupUuid`),
  KEY `instanceUuid` (`instanceUuid`),
  UNIQUE KEY `groupUuidInstanceUuid` (`groupUuid`,`instanceUuid`),
  CONSTRAINT `fkMonitorGroupInstanceVOMonitorGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `zstack`.`MonitorGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MonitorTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MonitorGroupTemplateRefVO` (
  `uuid` varchar(32) NOT NULL,
  `templateUuid` varchar(32) NOT NULL,
  `groupUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `groupUuidTemplateUuid` (`groupUuid`,`templateUuid`),
  CONSTRAINT `fkMonitorGroupTemplateRefVOMonitorGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `zstack`.`MonitorGroupVO` (`uuid`),
  CONSTRAINT `fkMonitorGroupTemplateRefVOMonitorTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `zstack`.`MonitorTemplateVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MetricRuleTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `monitorTemplateUuid` varchar(32) NOT NULL,
  `comparisonOperator` varchar(128) NOT NULL,
  `period` int(10) unsigned NOT NULL,
  `repeatInterval` int(10) unsigned NOT NULL,
  `namespace` varchar(255) NOT NULL,
  `metricName` varchar(512) NOT NULL,
  `threshold` double NOT NULL,
  `repeatCount` int(11) DEFAULT NULL,
  `enableRecovery` tinyint(1) NOT NULL DEFAULT '0',
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `labels` varchar(4096) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `monitorTemplateUuid` (`monitorTemplateUuid`),
  CONSTRAINT `fkMetricRuleTemplateVOMonitorTemplateVO` FOREIGN KEY (`monitorTemplateUuid`) REFERENCES `zstack`.`MonitorTemplateVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`EventRuleTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `monitorTemplateUuid` varchar(32) NOT NULL,
  `namespace` varchar(255) NOT NULL,
  `eventName` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `labels` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  KEY `monitorTemplateUuid` (`monitorTemplateUuid`),
  CONSTRAINT `fkEventRuleTemplateVOMonitorTemplateVO` FOREIGN KEY (`monitorTemplateUuid`) REFERENCES `zstack`.`MonitorTemplateVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MonitorGroupEventSubscriptionVO` (
  `uuid` varchar(32) NOT NULL,
  `groupUuid` varchar(32) NOT NULL,
  `eventSubscriptionUuid` varchar(32) NOT NULL,
  `eventRuleTemplateUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `groupUuid` (`groupUuid`),
  CONSTRAINT `fkMonitorGroupEventSubscriptionVOMonitorGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `zstack`.`MonitorGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`MonitorGroupAlarmVO` (
  `uuid` varchar(32) NOT NULL,
  `groupUuid` varchar(32) NOT NULL,
  `alarmUuid` varchar(32) NOT NULL,
  `metricRuleTemplateUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `groupUuid` (`groupUuid`),
  CONSTRAINT `fkMonitorGroupAlarmVOMonitorGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `zstack`.`MonitorGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ActiveAlarmTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `alarmName` varchar(255) NOT NULL,
  `comparisonOperator` varchar(128) NOT NULL,
  `period` int(10) unsigned NOT NULL,
  `repeatInterval` int(10) unsigned NOT NULL,
  `namespace` varchar(255) NOT NULL,
  `metricName` varchar(512) NOT NULL,
  `threshold` double NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `repeatCount` int(11) DEFAULT NULL,
  `enableRecovery` tinyint(1) NOT NULL DEFAULT '0',
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `labels` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ActiveAlarmVO` (
  `uuid` varchar(32) NOT NULL,
  `templateUuid` varchar(32) NOT NULL,
  `alarmUuid` varchar(32) NOT NULL,
  `namespace` varchar(128) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `alarmUuid` (`alarmUuid`),
  CONSTRAINT `fkActiveAlarmVOActiveAlarmTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `zstack`.`ActiveAlarmTemplateVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AlertDataAckVO` (
  `alertDataUuid` varchar(32) NOT NULL,
  `alertType` varchar(255) NOT NULL,
  `ackPeriod` int(10) unsigned NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `ackDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `resumeAlert` tinyint(1) NOT NULL DEFAULT '0',
  `operatorAccountUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`alertDataUuid`),
  KEY `resourceUuid` (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`EventDataAckVO` (
  `alertDataUuid` varchar(32) NOT NULL,
  `eventSubscriptionUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`alertDataUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AlarmDataAckVO` (
  `alertDataUuid` varchar(32) NOT NULL,
  `alarmUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`alertDataUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('180dcd21d9c64e1190ac09c825023a3f','Host-MemoryUsedInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/Host','MemoryUsedInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('1c43bab11c9b454281827a0af3ccb02e','Host-DiskAllUsedCapacityInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/Host','DiskAllUsedCapacityInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('231b35bf21d5406d992286ba4c0bf749','Host-CPUAverageUsedUtilization','GreaterThanOrEqualTo',300,1800,'ZStack/Host','CPUAverageUsedUtilization',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('383d9dcd547d46c9ac5f1031905a9b54','VM-DiskAllUsedCapacityInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VM','DiskAllUsedCapacityInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('64ff18b8628443d58dbf66c9bbad37e6','VRouter-VRouterDiskAllUsedCapacityInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VRouter','VRouterDiskAllUsedCapacityInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('65c8af4f0a2342e8a5a79511b546f750','VRouter-MemoryUsedInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VRouter','VRouterMemoryUsedPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('69d2840e61fa49d280948ce8f7112e46','VM-OperatingSystemMemoryUsedPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VM','OperatingSystemMemoryUsedPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('94fcd41cac524a57b47452a78d14cfab','VM-MemoryUsedInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VM','MemoryUsedInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('c9e6cdca107140bea62b4ca919ff9e88','VRouter-CPUAverageUsedUtilization','GreaterThanOrEqualTo',300,1800,'ZStack/VRouter','VRouterCPUAverageUsedUtilization',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('ccc249938ad34e7f92d6a1cc7e123b38','VM-CPUAverageUsedUtilization','GreaterThanOrEqualTo',300,1800,'ZStack/VM','CPUAverageUsedUtilization',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('fa6ead4d89064002b1b96ed2abf6ecb5','VM-OperatingSystemCPUAverageUsedUtilization','GreaterThanOrEqualTo',300,1800,'ZStack/VM','OperatingSystemCPUAverageUsedUtilization',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);

UPDATE ResourceVO SET concreteResourceType = "org.zstack.header.affinitygroup.AffinityGroupVO"  WHERE resourceName = "zstack.affinity.group.for.virtual.router" and resourceType = "AffinityGroupVO";

CREATE TABLE IF NOT EXISTS  `zstack`.`SlbOfferingVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementNetworkUuid` varchar(32) NOT NULL,
    `imageUuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSlbOfferingVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES `zstack`.`L3NetworkEO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SlbGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `backendType` VARCHAR(255) NOT NULL,
    `deployType` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `slbOfferingUuid` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkSlbGroupVOSlbOfferingVO FOREIGN KEY (slbOfferingUuid) REFERENCES `zstack`.`SlbOfferingVO` (uuid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`SlbLoadBalancerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `slbGroupUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSlbLoadBalancerVOSlbGroupVO FOREIGN KEY (slbGroupUuid) REFERENCES `zstack`.`SlbGroupVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`SlbVmInstanceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `slbGroupUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSlbVmInstanceVOSlbGroupVO FOREIGN KEY (slbGroupUuid) REFERENCES `zstack`.`SlbGroupVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SlbGroupL3NetworkRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `slbGroupUuid` varchar(32) NOT NULL,
    `l3NetworkUuid` varchar(32) NOT NULL,
    `type` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT fkSlbGroupL3NetworkRefVOSlbGroupVO FOREIGN KEY (slbGroupUuid) REFERENCES `zstack`.`SlbGroupVO` (uuid) ON DELETE CASCADE,
    CONSTRAINT fkSlbGroupL3NetworkRefVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES `zstack`.`L3NetworkEO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LoadBalancerServerGroupVO`(
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `loadBalancerUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkLoadBalancerServerGroupVOLoadBalancerVO FOREIGN KEY (loadBalancerUuid) REFERENCES `zstack`.`LoadBalancerVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LoadBalancerListenerServerGroupRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `listenerUuid` varchar(32) NOT NULL,
    `serverGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT fkLoadBalancerListenerServerGroupRefVOLoadBalancerListenerVO FOREIGN KEY (listenerUuid) REFERENCES `zstack`.`LoadBalancerListenerVO` (uuid) ON DELETE CASCADE,
    CONSTRAINT fkLoadBalancerListenerServerGroupRefVOLoadBalancerServerGroupVO FOREIGN KEY (serverGroupUuid) REFERENCES `zstack`.`LoadBalancerServerGroupVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerServerGroupVmNicRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
	`serverGroupUuid` varchar(32) NOT NULL,
    `vmNicUuid` varchar(32) NOT NULL,
    `weight` bigint unsigned NOT NULL DEFAULT 100,
    `status` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT fkLoadBalancerServerGroupVmNicRefVOLoadBalancerServerGroupVO FOREIGN KEY (serverGroupUuid) REFERENCES `zstack`.`LoadBalancerServerGroupVO` (uuid) ON DELETE CASCADE,
    CONSTRAINT fkLoadBalancerServerGroupVmNicRefVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES `zstack`.`VmNicVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerServerGroupServerIpVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `serverGroupUuid` varchar(32) NOT NULL,
  	`ipAddress` varchar(128) NOT NULL,
    `weight` bigint unsigned NOT NULL DEFAULT 100,
  	`status` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT fkLoadBalancerServerGroupServerIpVOLoadBalancerServerGroupVO FOREIGN KEY (serverGroupUuid) REFERENCES `zstack`.`LoadBalancerServerGroupVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`LoadBalancerVO` ADD COLUMN `type` varchar(255) DEFAULT "Shared";
ALTER TABLE `zstack`.`LoadBalancerVO` ADD COLUMN `serverGroupUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`LoadBalancerVO` ADD CONSTRAINT fkLoadBalancerVOLoadBalancerServerGroupVO FOREIGN KEY (serverGroupUuid) REFERENCES LoadBalancerServerGroupVO (uuid) ON DELETE SET NULL;
ALTER TABLE `zstack`.`LoadBalancerListenerVO` ADD COLUMN `serverGroupUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`LoadBalancerListenerVO` ADD CONSTRAINT fkLoadBalancerListenerVOLoadBalancerServerGroupVO FOREIGN KEY (serverGroupUuid) REFERENCES LoadBalancerServerGroupVO (uuid) ON DELETE SET NULL;

UPDATE `VRouterRouteEntryVO` SET `type` = 'UserStatic' where `type` = '0';
UPDATE `VRouterRouteEntryVO` SET `type` = 'UserBlackHole' where `type` = '1';
UPDATE `VRouterRouteEntryVO` SET `type` = 'DirectConnect' where `type` = '2';
UPDATE `VRouterRouteEntryVO` SET `type` = 'ZStack' where `type` = '3';
UPDATE `VRouterRouteEntryVO` SET `type` = 'OSPF' where `type` = '4';
UPDATE `VRouterRouteEntryVO` SET `type` = 'Unknown' where `type` = '5';

ALTER TABLE `zstack`.`UsbDeviceVO` modify column iSerial varchar(1024) DEFAULT NULL;
ALTER TABLE `zstack`.`ZBoxVO` modify column `iSerial` varchar(1024) DEFAULT NULL;

ALTER TABLE `zstack`.`SNSEmailPlatformVO` CHANGE COLUMN `password` `password` VARCHAR(255) NULL

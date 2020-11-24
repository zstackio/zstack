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
DROP PROCEDURE IF EXISTS checkFlatLoadBalancerExist;
DELIMITER $$
CREATE PROCEDURE checkFlatLoadBalancerExist()
BEGIN
    if((select count(*) from LoadBalancerListenerVmNicRefVO ref, VmNicVO nic
    where ref.vmNicUuid=nic.uuid
    and nic.l3NetworkUuid not in (SELECT uuid FROM L3NetworkEO l3
    LEFT JOIN NetworkServiceL3NetworkRefVO ref on l3.uuid = ref.l3NetworkUuid WHERE ref.networkServiceType='SNAT')) > 0) THEN
        SIGNAL SQLSTATE "45000"
            SET MESSAGE_TEXT = "VirtualRouter are not supported this version";
    END IF;
END$$
DELIMITER ;
CALL checkFlatLoadBalancerExist();
DROP PROCEDURE IF EXISTS checkFlatLoadBalancerExist;

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

        INSERT INTO IAM2VirtualIDRoleRefVO (`virtualIDUuid`, `roleUuid`, `targetAccountUuid`, createDate, lastOpDate) VALUES (iam2VirtualIDUuid, 'f2f474c60e7340c0a1d44080d5bde3a9', targetAccountUuid, NOW(), NOW());
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeProjectAdminSystemTags();


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

insert into RoleVO(uuid, name, state, type, createDate, lastOpDate) values ('86d67c89dfe64b3ba67ecffd34cee418', 'read-api-role-default', 'Enabled', 'System', NOW(), NOW());
insert into RolePolicyStatementVO(uuid, statement, roleUuid, createDate, lastOpDate) values ('7d800a63539b47e2cec86529cef3cd2d',  '', '86d67c89dfe64b3ba67ecffd34cee418', NOW(), NOW());


DELIMITER $$
CREATE PROCEDURE deleteRoleReadAPI()
BEGIN
    DECLARE roUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR select uuid from RoleVO where type = 'System' and name like 'read-api-role-%';
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

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

CALL deleteRoleReadAPI();
DROP PROCEDURE IF EXISTS deleteRoleReadAPI;


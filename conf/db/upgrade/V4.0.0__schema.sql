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
        INSERT INTO `zstack`.IAM2VirtualIDOrganizationRefVO (virtualIDUuid, organizationUuid) VALUES (virtualIDUuid, organizationUuid);

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
    DECLARE cur CURSOR FOR SELECT uuid, projectUuid  FROM zstack.IAM2VirtualIDGroupVO;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO groupUuid, projectUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;
        INSERT INTO `zstack`.IAM2ProjectVirtualIDGroupRefVO (groupUuid, projectUuid) VALUES (groupUuid, projectUuid);

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

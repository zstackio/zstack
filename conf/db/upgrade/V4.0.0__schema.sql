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

CREATE TABLE IF NOT EXISTS  `zstack`.`SlbOfferingVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementNetworkUuid` varchar(32) NOT NULL,
    `imageUuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSlbOfferingVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES `zstack`.`L3NetworkEO` (uuid) ON DELETE CASCADE,
    CONSTRAINT fkSlbOfferingVOImageEO FOREIGN KEY (imageUuid) REFERENCES `zstack`.`ImageEO` (uuid) ON DELETE CASCADE
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

ALTER TABLE `zstack`.`LoadBalancerVO` ADD COLUMN `type` varchar(255) DEFAULT "Shared";

CREATE TABLE IF NOT EXISTS `zstack`.`LoadBalancerServerGroupVO`(
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `loadBalancerUuid` varchar(32) NOT NULL,
  	`weight` TINYINT unsigned NOT NULL DEFAULT 1,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkLoadBalancerServerGroupVOLoadBalancerVO FOREIGN KEY (loadBalancerUuid) REFERENCES `zstack`.`LoadBalancerVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LoadBalancerListenerServerGroupRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `listenerUuid` varchar(32) NOT NULL,
    `loadBalancerServerGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT fkLoadBalancerListenerServerGroupRefVOLoadBalancerListenerVO FOREIGN KEY (listenerUuid) REFERENCES `zstack`.`LoadBalancerListenerVO` (uuid) ON DELETE CASCADE,
    CONSTRAINT fkLoadBalancerListenerVmNicRefVOLoadBalancerServerGroupVO FOREIGN KEY (loadBalancerServerGroupUuid) REFERENCES `zstack`.`LoadBalancerServerGroupVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerServerGroupVmNicRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
	`loadBalancerServerGroupUuid` varchar(32) NOT NULL,
    `vmNicUuid` varchar(32) NOT NULL,
    `status` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT fkLoadBalancerServerGroupVmNicRefVOLoadBalancerServerGroupVO FOREIGN KEY (loadBalancerServerGroupUuid) REFERENCES `zstack`.`LoadBalancerServerGroupVO` (uuid) ON DELETE CASCADE,
    CONSTRAINT fkLoadBalancerServerGroupVmNicRefVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES `zstack`.`VmNicVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerServerGroupServerIpVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  	`description` varchar(2048) DEFAULT NULL,
  	`ipAddress` varchar(128) NOT NULL,
  	`status` varchar(64) NOT NULL,
	`loadBalancerServerGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT fkLoadBalancerServerGroupServerIpVOLoadBalancerServerGroupVO FOREIGN KEY (loadBalancerServerGroupUuid) REFERENCES `zstack`.`LoadBalancerServerGroupVO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

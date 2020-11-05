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



CREATE TABLE IF NOT EXISTS `zstack`.`LoadBalancerServerGroupVO`(
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
     `loadBalancerUuid` varchar(32) NOT NULL,
  	`weight` TINYINT unsigned NOT NULL DEFAULT 1,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
# Foreign keys for table LoadBalancerServerGroupVO
ALTER TABLE LoadBalancerServerGroupVO ADD CONSTRAINT fkLoadBalancerServerGroupVOLoadBalancerVO FOREIGN KEY (loadBalancerUuid) REFERENCES LoadBalancerVO (uuid) ON DELETE CASCADE;



CREATE TABLE IF NOT EXISTS `zstack`.`LoadBalancerListenerServerGroupRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `listenerUuid` varchar(32) NOT NULL,
    `loadBalancerServerGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
# Foreign keys for table LoadBalancerListenerServerGroupRefVO
ALTER TABLE LoadBalancerListenerServerGroupRefVO ADD CONSTRAINT fkLoadBalancerListenerServerGroupRefVOLoadBalancerListenerVO FOREIGN KEY (listenerUuid) REFERENCES LoadBalancerListenerVO (uuid) ON DELETE CASCADE;
ALTER TABLE LoadBalancerListenerServerGroupRefVO ADD CONSTRAINT fkLoadBalancerListenerVmNicRefVOLoadBalancerServerGroupVO FOREIGN KEY (loadBalancerServerGroupUuid) REFERENCES LoadBalancerServerGroupVO (uuid) ON DELETE CASCADE;



CREATE TABLE  `zstack`.`LoadBalancerServerGroupVmNicRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
		`loadBalancerServerGroupUuid` varchar(32) NOT NULL,
    `vmNicUuid` varchar(32) NOT NULL,
    `status` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
# Foreign keys for table LoadBalancerServerGroupVmNicRefVO
ALTER TABLE LoadBalancerServerGroupVmNicRefVO ADD CONSTRAINT fkLoadBalancerServerGroupVmNicRefVOLoadBalancerServerGroupVO FOREIGN KEY (loadBalancerServerGroupUuid) REFERENCES LoadBalancerServerGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE LoadBalancerServerGroupVmNicRefVO ADD CONSTRAINT fkLoadBalancerServerGroupVmNicRefVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE CASCADE;

CREATE TABLE  `zstack`.`LoadBalancerServerGroupServerIpVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  	`description` varchar(2048) DEFAULT NULL,
  	`ipAddress` varchar(128) NOT NULL,
  	`status` varchar(64) NOT NULL,
	`loadBalancerServerGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
# Foreign keys for table LoadBalancerServerGroupServerIpVO
ALTER TABLE LoadBalancerServerGroupServerIpVO ADD CONSTRAINT fkLoadBalancerServerGroupServerIpVOLoadBalancerServerGroupVO FOREIGN KEY (loadBalancerServerGroupUuid) REFERENCES LoadBalancerServerGroupVO (uuid) ON DELETE CASCADE;





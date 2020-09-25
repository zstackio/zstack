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

-- -----------------------------------
--  Table structures for Bare Metal 2
-- -----------------------------------
CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2ProvisionNetworkVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `dhcpInterface` varchar(128) NOT NULL,
    `dhcpRangeStartIp` varchar(32) NOT NULL,
    `dhcpRangeEndIp` varchar(32) NOT NULL,
    `dhcpRangeNetmask` varchar(32) NOT NULL,
    `dhcpRangeGateway` varchar(32) DEFAULT NULL,
    `state` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2ProvisionNetworkVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2ProvisionNetworkClusterRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `clusterUuid` varchar(32) NOT NULL,
    `networkUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `ukBareMetal2ProvisionNetworkClusterRefVO` UNIQUE KEY (`clusterUuid`, `networkUuid`),
    CONSTRAINT `fkBareMetal2ProvisionNetworkVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2ProvisionNetworkVONetworkVO` FOREIGN KEY (`networkUuid`) REFERENCES `BareMetal2ProvisionNetworkVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2GatewayVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2GatewayVOKVMHostVO` FOREIGN KEY (`uuid`) REFERENCES `KVMHostVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2GatewayClusterRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `clusterUuid` varchar(32) NOT NULL,
    `gatewayUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `ukBareMetal2GatewayClusterRefVO` UNIQUE KEY (`clusterUuid`, `gatewayUuid`),
    CONSTRAINT `fkBareMetal2GatewayVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2GatewayVOGatewayVO` FOREIGN KEY (`gatewayUuid`) REFERENCES `BareMetal2GatewayVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2GatewayProvisionNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `gatewayUuid` varchar(32) NOT NULL,
    `networkUuid` varchar(32) NOT NULL,
    `interfaceName` varchar(17) NOT NULL,
    `ip` varchar(128) NOT NULL,
    `netmask` varchar(128) NOT NULL,
    `gateway` varchar(128) DEFAULT NULL,
    `metaData` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2GatewayProvisionNicVOGatewayVO` FOREIGN KEY (`gatewayUuid`) REFERENCES `BareMetal2GatewayVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2GatewayProvisionNicVONetworkVO` FOREIGN KEY (`networkUuid`) REFERENCES `BareMetal2ProvisionNetworkVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `BareMetal2ChassisOfferingVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `architecture` varchar(32) NOT NULL,
    `cpuModelName` varchar(255) NOT NULL,
    `cpuNum` int(10) unsigned NOT NULL,
    `memorySize` bigint unsigned NOT NULL COMMENT 'memory size in bytes',
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `BareMetal2ChassisVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `clusterUuid` varchar(32) NOT NULL,
    `chassisOfferingUuid` varchar(32) DEFAULT NULL,
    `bootMode` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2ChassisVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2ChassisVOOfferingVO` FOREIGN KEY (`chassisOfferingUuid`) REFERENCES `BareMetal2ChassisOfferingVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `BareMetal2ChassisNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `chassisUuid` varchar(32) NOT NULL,
    `mac` varchar(32) NOT NULL UNIQUE,
    `speed` varchar(32) DEFAULT NULL,
    `isProvisionNic` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2ChassisNicVOChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `BareMetal2ChassisDiskVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `chassisUuid` varchar(32) NOT NULL,
    `type` varchar(32) DEFAULT "",
    `diskSize` bigint unsigned NOT NULL COMMENT 'disk size in bytes',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2ChassisDiskVOChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `BareMetal2IpmiChassisVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `ipmiAddress` varchar(32) NOT NULL,
    `ipmiPort` int unsigned NOT NULL,
    `ipmiUsername` varchar(255) NOT NULL,
    `ipmiPassword` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `ukBareMetal2IpmiChassisVO` UNIQUE (`ipmiAddress`, `ipmiPort`),
    CONSTRAINT `fkBareMetal2IpmiChassisVOChassisVO` FOREIGN KEY (`uuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2InstanceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `status` varchar(32) NOT NULL,
    `chassisUuid` varchar(32) DEFAULT NULL,
    `lastChassisUuid` varchar(32) DEFAULT NULL,
    `gatewayUuid` varchar(32) DEFAULT NULL,
    `lastGatewayUuid` varchar(32) DEFAULT NULL,
    `chassisOfferingUuid` varchar(32) DEFAULT NULL,
    `gatewayAllocatorStrategy` varchar(64) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2InstanceVOVmInstanceEO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2InstanceVOChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkBareMetal2InstanceVOChassisVO1` FOREIGN KEY (`lastChassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkBareMetal2InstanceVOGatewayVO` FOREIGN KEY (`gatewayUuid`) REFERENCES `BareMetal2GatewayVO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkBareMetal2InstanceVOGatewayVO1` FOREIGN KEY (`lastGatewayUuid`) REFERENCES `BareMetal2GatewayVO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkBareMetal2InstanceVOChassisOfferingVO` FOREIGN KEY (`chassisOfferingUuid`) REFERENCES `BareMetal2ChassisOfferingVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2InstanceProvisionNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `instanceUuid` varchar(32) NOT NULL,
    `networkUuid` varchar(32) NOT NULL,
    `mac` varchar(17) NOT NULL UNIQUE,
    `ip` varchar(128) NOT NULL,
    `netmask` varchar(128) NOT NULL,
    `gateway` varchar(128) DEFAULT NULL,
    `metaData` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2InstanceProvisionNicVOInstanceVO` FOREIGN KEY (`instanceUuid`) REFERENCES `BareMetal2InstanceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2InstanceProvisionNicVONetworkVO` FOREIGN KEY (`networkUuid`) REFERENCES `BareMetal2ProvisionNetworkVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
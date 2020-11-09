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
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `groupUuid` (`groupUuid`),
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

INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('180dcd21d9c64e1190ac09c825023a3f','test-active-alarm','GreaterThan',300,1800,'ZStack/Host','MemoryUsedInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('1c43bab11c9b454281827a0af3ccb02e','test-active-alarm','GreaterThan',300,1800,'ZStack/Host','DiskAllUsedCapacityInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('231b35bf21d5406d992286ba4c0bf749','test-active-alarm','GreaterThan',300,1800,'ZStack/Host','CPUAverageUsedUtilization',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('383d9dcd547d46c9ac5f1031905a9b54','test-active-alarm','GreaterThan',300,1800,'ZStack/VM','DiskAllUsedCapacityInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('64ff18b8628443d58dbf66c9bbad37e6','test-active-alarm','GreaterThan',300,1800,'ZStack/VRouter','VRouterDiskAllUsedCapacityInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('65c8af4f0a2342e8a5a79511b546f750','test-active-alarm','GreaterThan',300,1800,'ZStack/VRouter','MemoryUsedInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('69d2840e61fa49d280948ce8f7112e46','test-active-alarm','GreaterThan',300,1800,'ZStack/VM','OperatingSystemMemoryUsedPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('94fcd41cac524a57b47452a78d14cfab','test-active-alarm','GreaterThan',300,1800,'ZStack/VM','MemoryUsedInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('c9e6cdca107140bea62b4ca919ff9e88','test-active-alarm','GreaterThan',300,1800,'ZStack/VRouter','CPUAverageUsedUtilization',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('ccc249938ad34e7f92d6a1cc7e123b38','test-active-alarm','GreaterThan',300,1800,'ZStack/VM','CPUAverageUsedUtilization',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('fa6ead4d89064002b1b96ed2abf6ecb5','test-active-alarm','GreaterThan',300,1800,'ZStack/VM','OperatingSystemCPUAverageUsedUtilization',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
ALTER TABLE `zstack`.`AlarmVO` ADD COLUMN `recoveryDuration` int unsigned DEFAULT NULL;
ALTER TABLE `zstack`.`AlarmVO` ADD COLUMN `recoveryThreshold` int unsigned DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`AlarmResourceStateVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `alarmUuid` varchar(32) NOT NULL,
    `identifyLabel` varchar(200) NOT NULL,
    `resourceUuid` varchar(32) DEFAULT NULL,
    `resourceType` varchar(256) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `lastStatusChangeTime` bigint(20) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukAlarmUuidIdentifyLabel` (`alarmUuid`, `identifyLabel`),
    KEY `idxAlarmResourceStateVOresourceUuid` (`resourceUuid`),
    CONSTRAINT `fkAlarmResourceStateVOAlarmVO` FOREIGN KEY (`alarmUuid`) REFERENCES `AlarmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

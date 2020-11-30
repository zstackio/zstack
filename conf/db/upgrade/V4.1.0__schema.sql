CREATE TABLE IF NOT EXISTS `zstack`.`EventRecordsVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `createTime` bigint  NOT NULL,
    `accountUuid` varchar(32) ,
    `dataUuid` varchar(32) ,
    `emergencyLevel` varchar(64) DEFAULT NULL,
    `name` varchar(256) DEFAULT NULL,
    `error` text DEFAULT NULL,
    `labels` text DEFAULT NULL,
    `namespace` varchar(256) DEFAULT NULL,
    `readStatus` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `resourceId` varchar(32) DEFAULT NULL,
    `resourceName` varchar(256) DEFAULT NULL,
    `subscriptionUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX `idxEventRecordsVOcreateTimeid` ON EventRecordsVO (`createTime`,`id`);
CREATE INDEX `idxEventRecordsVOaccountUuid` ON EventRecordsVO (`accountUuid`);
CREATE INDEX `idxEventRecordsVOemergencyLevel` ON EventRecordsVO (`emergencyLevel`);
CREATE INDEX `idxEventRecordsVOname` ON EventRecordsVO (`name`);
CREATE INDEX `idxEventRecordsVOsubscriptionUuid` ON EventRecordsVO (`subscriptionUuid`);

CREATE TABLE IF NOT EXISTS `zstack`.`AlarmRecordsVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `createTime` bigint  NOT NULL,
    `accountUuid` varchar(32),
    `alarmName` varchar(255) NOT NULL,
    `alarmStatus` varchar(64) DEFAULT NULL,
    `alarmUuid` varchar(32) ,
    `comparisonOperator` varchar(128) DEFAULT NULL,
    `context`  text DEFAULT NULL,
    `dataUuid` varchar(32) ,
    `emergencyLevel` varchar(64) DEFAULT NULL,
    `labels` text DEFAULT NULL,
    `metricName` varchar(256) DEFAULT NULL,
    `metricValue` double DEFAULT NULL,
    `namespace` varchar(256) DEFAULT NULL,
    `period` int unsigned NOT NULL,
    `readStatus` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `resourceType` VARCHAR(256) NOT NULL,
    `resourceUuid` varchar(256) ,
    `threshold` double NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX `idxAlarmRecordsVOcreateTimeid` ON AlarmRecordsVO (`createTime`,`id`);
CREATE INDEX `idxAlarmRecordsVOaccountUuid` ON AlarmRecordsVO (`accountUuid`);
CREATE INDEX `idxAlarmRecordsVOalarmUuid` ON AlarmRecordsVO (`alarmUuid`);
CREATE INDEX `idxAlarmRecordsVOemergencyLevel` ON AlarmRecordsVO (`emergencyLevel`);

CREATE TABLE IF NOT EXISTS `zstack`.`AuditsVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `createTime` bigint  NOT NULL,
    `apiName` varchar(2048) NOT NULL,
    `clientBrowser` VARCHAR(64) NOT NULL,
    `clientIp` VARCHAR(64) NOT NULL,
    `duration` int unsigned NOT NULL,
    `error` text DEFAULT NULL,
    `operator` varchar(256) DEFAULT NULL,
    `requestDump` text DEFAULT NULL,
    `resourceType` VARCHAR(256) NOT NULL,
    `resourceUuid` varchar(32) ,
    `responseDump`  text DEFAULT NULL,
    `success` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'api call success or failed',
    PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
CREATE INDEX `idxAuditsVOcreateTimeid` ON AuditsVO (`createTime`,`id`);
CREATE INDEX `idxAuditsVOresourceUuid` ON AuditsVO (`resourceUuid`);
CREATE INDEX `idxAuditsVOsuccess` ON AuditsVO (`success`);
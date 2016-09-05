ALTER TABLE `zstack`.`AccountResourceRefVO` ADD UNIQUE INDEX(resourceUuid,resourceType);

ALTER TABLE `zstack`.`ConsoleProxyAgentVO` modify column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`ImageEO` modify column description varchar(2048) DEFAULT NULL COMMENT 'image description';

ALTER TABLE `zstack`.`ImageEO` add column exportUrl varchar(2048) DEFAULT NULL COMMENT 'exported image URL';
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType, exportUrl FROM `zstack`.`ImageEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`InstanceOfferingEO` modify column description varchar(2048) DEFAULT NULL COMMENT 'instance offering description';
ALTER TABLE `zstack`.`DiskOfferingEO` modify column description varchar(2048) DEFAULT NULL COMMENT 'disk offering description';
ALTER TABLE `zstack`.`VolumeEO` modify column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`VipVO` modify column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`GlobalConfigVO` modify column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`NetworkServiceProviderVO` modify column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`SecurityGroupVO` modify column description varchar(2048) DEFAULT NULL;

ALTER TABLE `zstack`.`PolicyVO` modify column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`person` modify column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`UserGroupVO` modify column description varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`SchedulerVO` add column schedulerJob varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`SchedulerVO` change column status state varchar(128) DEFAULT NULL;

 CREATE TABLE  `zstack`.`AlarmVO` (
     `uuid` varchar(32) NOT NULL UNIQUE,
     `name` varchar(255) NOT NULL,
     `description` varchar(2048) DEFAULT NULL,
     `conditionName` varchar(1024) NOT NULL,
     `conditionOperator` varchar(128) NOT NULL,
     `conditionValue` varchar(255) NOT NULL,
     `conditionDuration` int unsigned NOT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     PRIMARY KEY  (`uuid`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 
 CREATE TABLE  `zstack`.`AlarmLabelVO` (
     `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
     `alarmUuid` varchar(32) NOT NULL,
     `key` text NOT NULL,
     `value` text DEFAULT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     PRIMARY KEY  (`id`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 
 CREATE TABLE  `zstack`.`AlertVO` (
     `uuid` varchar(32) NOT NULL UNIQUE,
     `name` text DEFAULT NULL,
     `description` text DEFAULT NULL,
     `status` varchar(128) NOT NULL,
     `count` int unsigned NOT NULL,
     `opaque` text DEFAULT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     PRIMARY KEY  (`uuid`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 
 CREATE TABLE  `zstack`.`AlertLabelVO` (
     `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
     `alertUuid` varchar(32) NOT NULL,
     `key` text NOT NULL,
     `value` text DEFAULT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     PRIMARY KEY  (`id`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 
 CREATE TABLE  `zstack`.`AlertTimestampVO` (
     `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
     `alertUuid` varchar(32) NOT NULL,
     `time` timestamp,
     PRIMARY KEY  (`id`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 
 # Foreign keys for table AlarmLabelVO
 
 ALTER TABLE AlarmLabelVO ADD CONSTRAINT fkAlarmLabelVOAlertVO FOREIGN KEY (alarmUuid) REFERENCES AlertVO (uuid) ON DELETE CASCADE;
 
 # Foreign keys for table AlertLabelVO
 
 ALTER TABLE AlertLabelVO ADD CONSTRAINT fkAlertLabelVOAlertVO FOREIGN KEY (alertUuid) REFERENCES AlertVO (uuid) ON DELETE CASCADE;
 
 # Foreign keys for table AlertTimestampVO
 
 ALTER TABLE AlertTimestampVO ADD CONSTRAINT fkAlertTimestampVOAlertVO FOREIGN KEY (alertUuid) REFERENCES AlertVO (uuid) ON DELETE CASCADE;

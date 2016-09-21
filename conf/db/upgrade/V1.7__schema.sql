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
ALTER TABLE `zstack`.`SchedulerVO` modify column jobData text DEFAULT NULL;

ALTER TABLE `zstack`.`AccountResourceRefVO` modify column resourceUuid varchar(255) NOT NULL;

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
    `label` text NOT NULL,
    `value` text DEFAULT NULL,
    `type` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`AlertVO` (
    `uuid` varchar(255) NOT NULL UNIQUE,
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
    `alertUuid` varchar(255) NOT NULL,
    `label` text NOT NULL,
    `value` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`AlertTimestampVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `alertUuid` varchar(255) NOT NULL,
    `time` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table AlarmLabelVO

ALTER TABLE AlarmLabelVO ADD CONSTRAINT fkAlarmLabelVOAlarmVO FOREIGN KEY (alarmUuid) REFERENCES AlarmVO (uuid) ON DELETE CASCADE;

# Foreign keys for table AlertLabelVO

ALTER TABLE AlertLabelVO ADD CONSTRAINT fkAlertLabelVOAlertVO FOREIGN KEY (alertUuid) REFERENCES AlertVO (uuid) ON DELETE CASCADE;

# Foreign keys for table AlertTimestampVO

ALTER TABLE AlertTimestampVO ADD CONSTRAINT fkAlertTimestampVOAlertVO FOREIGN KEY (alertUuid) REFERENCES AlertVO (uuid) ON DELETE CASCADE;

CREATE TABLE  `zstack`.`LdapServerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `url` varchar(1024) NOT NULL,
    `base` varchar(1024) NOT NULL,
    `username` varchar(1024) NOT NULL,
    `password` varchar(1024) NOT NULL,
    `encryption` varchar(1024) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LdapAccountRefVO` (
     `uuid` varchar(32) NOT NULL UNIQUE,
     `ldapUid` varchar(255) NOT NULL,
     `ldapServerUuid` varchar(255) NOT NULL,
     `accountUuid`  varchar(255) NOT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`LdapAccountRefVO` ADD CONSTRAINT fkLdapAccountRefVOLdapServerVO FOREIGN KEY (ldapServerUuid) REFERENCES LdapServerVO (uuid) ON DELETE CASCADE;

ALTER TABLE `zstack`.`LdapAccountRefVO` ADD CONSTRAINT fkLdapAccountRefVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;

ALTER TABLE `zstack`.`LdapAccountRefVO` ADD UNIQUE INDEX(ldapUid,ldapServerUuid);

CREATE TABLE  `zstack`.`JsonLabelVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `labelKey` varchar(128) NOT NULL UNIQUE,
    `labelValue` text DEFAULT NULL,
    `resourceUuid` varchar(256) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`CephPrimaryStorageMonVO` add column monAddr varchar(255) DEFAULT NULL;
ALTER TABLE `zstack`.`CephBackupStorageMonVO` add column monAddr varchar(255) DEFAULT NULL;

CREATE TABLE  `zstack`.`VmUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmUuid` varchar(32) NOT NULL,
    `state` varchar(64) NOT NULL,
    `name` varchar(255) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `cpuNum` int unsigned NOT NULL,
    `memorySize` bigint unsigned NOT NULL,
    `rootVolumeSize` bigint unsigned NOT NULL,
    `dateInLong` bigint unsigned NOT NULL,
    `inventory` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`RootVolumeUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmUuid` varchar(32) NOT NULL,
    `volumeUuid` varchar(32) NOT NULL,
    `volumeStatus` varchar(64) NOT NULL,
    `volumeName` varchar(255) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `volumeSize` bigint unsigned NOT NULL,
    `dateInLong` bigint unsigned NOT NULL,
    `inventory` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`DataVolumeUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `volumeUuid` varchar(32) NOT NULL,
    `volumeStatus` varchar(64) NOT NULL,
    `volumeName` varchar(255) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `volumeSize` bigint unsigned NOT NULL,
    `dateInLong` bigint unsigned NOT NULL,
    `inventory` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PriceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `resourceName` varchar(255) NOT NULL,
    `timeUnit` varchar(255) NOT NULL,
    `resourceUnit` varchar(255) DEFAULT NULL,
    `price` float NOT NULL,
    `dateInLong` bigint unsigned NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


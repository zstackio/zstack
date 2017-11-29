# drop deprecated tables from 1.7
ALTER TABLE AlarmLabelVO DROP FOREIGN KEY fkAlarmLabelVOAlarmVO;
DROP TABLE IF EXISTS `zstack`.`AlarmVO`;
DROP TABLE IF EXISTS `zstack`.`AlarmLabelVO`;

CREATE TABLE  `zstack`.`SNSApplicationEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `type` varchar(128) NOT NULL,
    `platformUuid` varchar(32) NOT NULL,
    `state` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSEmailEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `email` varchar(1024) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSHttpEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `url` varchar(1024) NOT NULL,
    `username` varchar(512) DEFAULT NULL,
    `password` varchar(512) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSDingTalkEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `url` varchar(1024) NOT NULL,
    `atAll` int(1) unsigned NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSDingTalkAtPersonVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `phoneNumber` varchar(64) NOT NULL,
    `endpointUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSApplicationPlatformVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `type` varchar(128) NOT NULL,
    `state` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSEmailPlatformVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `smtpServer` varchar(255) NOT NULL,
    `smtpPort` int unsigned NOT NULL,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSSubscriberVO` (
    `topicUuid` varchar(32) NOT NULL,
    `endpointUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`topicUuid`, `endpointUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSTopicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `state` varchar(64) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSTextTemplateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `applicationPlatformType` varchar(128) NOT NULL,
    `template` text NOT NULL,
    `defaultTemplate` int(1) unsigned NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`AlarmActionVO` (
    `alarmUuid` varchar(32) NOT NULL,
    `actionUuid` varchar(32) NOT NULL,
    `actionType` varchar(128) NOT NULL,
    PRIMARY KEY  (`alarmUuid`, `actionUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`AlarmLabelVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `key` varchar(1024) NOT NULL,
    `value` text NOT NULL,
    `operator` varchar(128) NOT NULL,
    `alarmUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`AlarmVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `comparisonOperator` varchar(128) NOT NULL,
    `period` int unsigned NOT NULL,
    `repeatInterval` int unsigned NOT NULL,
    `namespace` varchar(255) NOT NULL,
    `metricName` varchar(512) NOT NULL,
    `threshold` double NOT NULL,
    `status` varchar(64) NOT NULL,
    `state` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`EventSubscriptionVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `namespace` varchar(255) NOT NULL,
    `eventName` varchar(255) NOT  NULL,
    `state` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`EventSubscriptionActionVO` (
    `subscriptionUuid` varchar(32) NOT NULL,
    `actionUuid` varchar(32) NOT NULL,
    `actionType` varchar(128) NOT NULL,
    PRIMARY KEY  (`subscriptionUuid`, `actionUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`EventSubscriptionLabelVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `key` varchar(1024) NOT NULL,
    `value` text NOT NULL,
    `operator` varchar(128) NOT NULL,
    `subscriptionUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE  `zstack`.`NotificationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(1024) NOT NULL,
    `content` text NOT NULL,
    `arguments` text DEFAULT NULL,
    `sender` varchar(1024) NOT NULL,
    `status` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `resoruceUuid` varchar(255) DEFAULT NULL,
    `resoruceType` varchar(255) DEFAULT NULL,
    `opaque` text DEFAULT NULL,
    `time` bigint unsigned DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`NotificationSubscriptionVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(1024) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `notificationName` varchar(1024) NOT NULL,
    `filter` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

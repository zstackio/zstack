CREATE TABLE IF NOT EXISTS `zstack`.`VolumeCbtBackupRecordVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `taskUuid` varchar(32) NOT NULL,
    `volumeUuid` varchar(32) NOT NULL,
    `mode` varchar(255) NOT NULL,
    `target` varchar(2048) NOT NULL,
    `scratchNodeName` varchar(255) NOT NULL,
    `bitmapName` varchar(255) NOT NULL,
    `lastBitmapName` varchar(255),
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`ImageGroupVO` (
     `uuid` VARCHAR(32) NOT NULL UNIQUE,
     `name` VARCHAR(255) NOT NULL,
     `status` varchar(32) NOT NULL,
     `description` VARCHAR(2048) DEFAULT NULL,
     `imageCount` int unsigned NOT NULL,
     `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE current_timestamp(),
     `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
     PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ImageGroupRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `imageUuid` VARCHAR(32) NOT NULL,
    `imageGroupUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE current_timestamp(),
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
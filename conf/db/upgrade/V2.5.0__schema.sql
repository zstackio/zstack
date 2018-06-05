CREATE TABLE IF NOT EXISTS `StackTemplateVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `version` VARCHAR(32) NOT NULL,
    `state` tinyint(1) unsigned DEFAULT 1,
    `content` text NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `name` (`name`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `ResourceStackVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `version` VARCHAR(32) NOT NULL,
    `templateContent` text NOT NULL,
    `paramContent` text DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(2048) DEFAULT NULL,
    `enableRollback` boolean NOT NULL DEFAULT TRUE,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `name` (`name`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `CloudFormationStackResourceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `stackUuid` VARCHAR(32) NOT NULL,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `resourceType` VARCHAR(255) NOT NULL,
    `reserve` boolean NOT NULL DEFAULT TRUE,
    `round` int(10) unsigned,
    CONSTRAINT `fkCloudFormationStackResourceRefVOResourceStackVO` FOREIGN KEY (`stackUuid`) REFERENCES ResourceStackVO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkCloudFormationStackResourceRefVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES ResourceVO (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `CloudFormationStackEventVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `stackUuid` VARCHAR(32) NOT NULL,
    `action` VARCHAR(64) NOT NULL,
    `resourceName` VARCHAR(128) NOT NULL,
    `description` VARCHAR(128) DEFAULT TRUE,
    `content` text NOT NULL,
    `actionStatus` VARCHAR(16) NOT NULL,
    `duration` VARCHAR(64) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkCloudFormationStackEventVOResourceStackVO` FOREIGN KEY (`stackUuid`) REFERENCES ResourceStackVO (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
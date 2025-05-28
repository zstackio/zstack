CREATE TABLE `SSOClientAttributeVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` TEXT NOT NULL,
    `value` TEXT DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `purpose` VARCHAR(32) NOT NULL,
    `ssoClientUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkSSOClientAttributeVOSSOClientVO` FOREIGN KEY (`ssoClientUuid`) REFERENCES SSOClientVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`L3NetworkSequenceNumberVO` (
    `id` int unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`L3NetworkEO` ADD COLUMN `internalId` INT(32) unsigned DEFAULT 0;
DROP VIEW IF EXISTS `zstack`.`L3NetworkVO`;
CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, internalId, description, state, type, zoneUuid, l2NetworkUuid, `system`, dnsDomain, createDate, lastOpDate, category, ipVersion, enableIPAM, isolated FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;

CREATE TABLE IF NOT EXISTS  `zstack`.`ImageGroupVO` (
     `uuid` VARCHAR(32) NOT NULL UNIQUE,
     `name` VARCHAR(255) NOT NULL,
     `status` varchar(32) NOT NULL,
     `description` VARCHAR(2048) DEFAULT NULL,
     `imageCount` int unsigned NOT NULL,
     `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
     PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ImageGroupRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `imageUuid` VARCHAR(32) NOT NULL,
    `imageGroupUuid` VARCHAR(32) NOT NULL,
     `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE SecurityGroupRuleVO MODIFY COLUMN `dstPortRange` varchar(1024) DEFAULT NULL;
ALTER TABLE SecurityGroupRuleVO MODIFY COLUMN `srcPortRange` varchar(1024) DEFAULT NULL;

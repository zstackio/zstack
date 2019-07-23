CREATE TABLE  `zstack`.`LdapResourceRefVO` (
     `uuid` varchar(32) NOT NULL UNIQUE,
     `ldapUid` varchar(255) NOT NULL,
     `ldapGlobalUuid` varchar(255),
     `ldapServerUuid` varchar(255) NOT NULL,
     `resourceUuid`  varchar(32) NOT NULL,
     `resourceType` varchar(255) NOT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
     PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`LdapResourceRefVO` ADD CONSTRAINT fkLdapResourceRefVOLdapServerVO FOREIGN KEY (ldapServerUuid) REFERENCES LdapServerVO (uuid) ON DELETE CASCADE;
ALTER TABLE `zstack`.`LdapResourceRefVO` ADD UNIQUE INDEX(ldapUid,ldapServerUuid,resourceUuid,resourceType);
ALTER TABLE `zstack`.`LdapServerVO` ADD COLUMN `scope` varchar(255) NOT NULL;
UPDATE `zstack`.`LdapServerVO` SET `scope` = "account" WHERE `scope` IS NULL;

ALTER TABLE `zstack`.`IAM2VirtualIDVO` ADD COLUMN `type` varchar(32) DEFAULT NULL;
UPDATE `zstack`.`IAM2VirtualIDVO` SET `type` = "ZStack" WHERE `type` IS NULL;

ALTER TABLE LoginAttemptsVO ADD COLUMN locked tinyint(1) unsigned NOT NULL;
ALTER TABLE LoginAttemptsVO ADD COLUMN forceChangePassword tinyint(1) unsigned NOT NULL;
ALTER TABLE LoginAttemptsVO ADD COLUMN unlockDate TIMESTAMP;

UPDATE LoginAttemptsVO SET locked = 1 WHERE locked = NULL;
UPDATE LoginAttemptsVO SET forceChangePassword = 1 WHERE forceChangePassword = NULL;
UPDATE LoginAttemptsVO SET unlockDate = CURRENT_TIMESTAMP() WHERE unlockDate = NULL;

ALTER TABLE LoginAttemptsVO ADD COLUMN successCount int(10) unsigned DEFAULT 0;

CREATE TABLE `zstack`.`AccessControlRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `strategy` varchar(64) NOT NULL,
    `rule` text NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`HistoricalPasswordVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` varchar(32) NOT NULL,
    `password` varchar(255) DEFAULT NULL,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- in version zsv_4.3.0
-- Feature: LDAP Enhance | ZSV-5531

CREATE TABLE `zstack`.`ThirdPartyAccountSourceVO` (
    `uuid` char(32) not null unique,
    `description` varchar(2048) default null,
    `type` varchar(32) not null,
    `createAccountStrategy` varchar(32) not null,
    `deleteAccountStrategy` varchar(32) not null,
    `lastOpDate` timestamp on update current_timestamp,
    `createDate` timestamp,
    primary key (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`AccountThirdPartyAccountSourceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `credentials` varchar(255) not null,
    `accountSourceUuid` char(32) not null,
    `accountUuid` char(32) not null,
    `lastOpDate` timestamp on update current_timestamp,
    `createDate` timestamp,
    primary key (`id`),
    UNIQUE KEY `credentialsAccountSourceUuid` (credentials,accountSourceUuid) USING BTREE,
    CONSTRAINT `fkAccountSourceRefVOThirdPartyAccountSourceVO` FOREIGN KEY (`accountSourceUuid`) REFERENCES ThirdPartyAccountSourceVO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAccountSourceRefVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES AccountVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`LdapServerVO`
    DROP COLUMN `scope`,
    DROP COLUMN `lastOpDate`,
    DROP COLUMN `createDate`,
    DROP COLUMN `description`,
    DROP COLUMN `name`;
ALTER TABLE `zstack`.`LdapServerVO` ADD COLUMN `serverType` varchar(32) NOT NULL default 'WindowsAD';
ALTER TABLE `zstack`.`LdapServerVO` ADD COLUMN `filter` varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`LdapServerVO` ADD COLUMN `usernameProperty` varchar(255) NOT NULL default 'cn';
DROP TABLE `zstack`.`LdapAccountRefVO`;
DROP TABLE `zstack`.`LdapResourceRefVO`;

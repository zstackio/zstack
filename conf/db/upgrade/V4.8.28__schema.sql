ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `scope` varchar(255) default 'openid';

ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `identityProvider` varchar(32) default 'default';

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





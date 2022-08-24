CREATE TABLE IF NOT EXISTS `zstack`.`SSOClientVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `clientType` varchar(255) NOT NULL,
    `loginType` varchar(255) NOT NULL,
    `loginMNUrl` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`OAuth2ClientVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `clientId` varchar(255) NOT NULL,
    `clientSecret` varchar(255),
    `authorizationUrl` varchar(255),
    `grantType` varchar(64) NOT NULL,
    `tokenUrl` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkOAuth2ClientVOSSOClientVO` FOREIGN KEY (`uuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CasClientVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `casServerLoginUrl` varchar(255) NOT NULL,
    `casServerUrlPrefix` varchar(255) NOT NULL,
    `serverName` varchar(255) NOT NULL,
    `state` varchar(128) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkCasClientVOSSOClientVO` FOREIGN KEY (`uuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ThirdClientAccountRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `clientUuid` varchar(255) NOT NULL,
    `resourceUuid`  varchar(255) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SSORedirectTemplateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `clientUuid` varchar(255) NOT NULL,
    `redirectTemplate`  varchar(2048) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSSORedirectTemplateClientVO` FOREIGN KEY (`clientUuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


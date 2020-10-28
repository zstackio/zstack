
CREATE TABLE IF NOT EXISTS `zstack`.`AuthClientDetailsVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `clientId` varchar(32),
    `clientSecret` varchar(32),
    `clientName` varchar(256),
    `redirectUri` varchar(1024) NOT NULL,
    `authorizeUri` varchar(1024) NOT NULL,
    `accessTokenUri` varchar(1024) NOT NULL,
    `lastOpDate` TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AuthScopeDetailsVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `scope` varchar(32) NOT NULL,
    `serviceUrl` varchar(1024) NOT NULL,
    `appId` varchar(32),
    `appKey` varchar(32),
    `appSecret` varchar(32),
    `authClientUuid` varchar(32),
    `lastOpDate` TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
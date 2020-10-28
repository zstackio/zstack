
CREATE TABLE IF NOT EXISTS `zstack`.`AuthInfoVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `clientId` varchar(32),
    `clientSecret` varchar(32),
    `redirectUri` varchar(1024) NOT NULL,
    `authorizeUri` varchar(1024) NOT NULL,
    `accessTokenUri` varchar(1024) NOT NULL,
    `getUserInfo` varchar(1024) NOT NULL,
    `scope` varchar(32),
    `appId` varchar(32),
    `appKey` varchar(32),
    `appSecret` varchar(32),
    `lastOpDate` TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
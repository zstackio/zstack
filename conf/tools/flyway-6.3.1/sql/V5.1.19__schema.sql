CREATE TABLE IF NOT EXISTS `zstack`.`SSOServerTokenVO`(
    `uuid` varchar(32) not null unique,
    `accessToken` text DEFAULT NULL,
    `idToken` text DEFAULT NULL,
    `refreshToken` text DEFAULT NULL,
    `userUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

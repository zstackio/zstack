insert into `zstack`.`LicenseHistoryVO` (`uuid`, `cpuNum`, `hostNum`, `vmNum`, `capacity`, `expiredDate`, `issuedDate`, `uploadDate`, `licenseType`, `userName`, `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`)
    select `uuid`, `cpuNum`, `hostNum`, `vmNum`, `capacity`, `expiredDate`, `issuedDate`, `uploadDate`, 'AddOn' as `licenseType`, `userName`, 'hybrid' as `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`
    from `zstack`.`LicenseHistoryVO`
    where `licenseType`='Hybrid';
update `zstack`.`LicenseHistoryVO` set `licenseType`='Paid' where `licenseType`='Hybrid';

CREATE TABLE IF NOT EXISTS `zstack`.`SSOTokenVO`(
    `uuid` varchar(32) not null unique,
    `clientUuid` varchar(32) DEFAULT NULL,
    `userUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSSOTokenVOClientVO` FOREIGN KEY (`clientUuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`OAuth2TokenVO`(
    `uuid` varchar(32) not null unique,
    `accessToken` varchar(2048) not null,
    `idToken` varchar(2048) not null,
    `refreshToken` varchar(2048) not null,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkOAuth2TokenVOSSOTokenVO` FOREIGN KEY (`uuid`) REFERENCES `SSOTokenVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CCSCertificateVO` (
    `uuid`        varchar(32) NOT NULL UNIQUE,
    `algorithm`   varchar(10) NOT NULL DEFAULT 'SM2',
    `format`      char(3) NOT NULL DEFAULT 'CER',
    `issuerDN`    varchar(64) NOT NULL,
    `subjectDN`   varchar(64) NOT NULL,
    `serNumber`   bigint unsigned NOT NULL,
    `effectiveTime`  bigint unsigned NOT NULL DEFAULT 0,
    `expirationTime` bigint unsigned NOT NULL DEFAULT 0,
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `subjectDNAndSerNumber` (`subjectDN`, `serNumber`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CCSCertificateUserRefVO` (
    `id`          bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `userUuid`    char(32) NOT NULL,
    `certificateUuid` char(32) NOT NULL,
    `state`       varchar(10) NOT NULL DEFAULT 'Disabled',
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkCCSCertificateUserRefVOCertificateUuid` FOREIGN KEY (`certificateUuid`) REFERENCES `zstack`.`CCSCertificateVO` (`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

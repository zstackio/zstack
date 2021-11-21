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

CREATE TABLE IF NOT EXISTS `zstack`.`SecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `zoneUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `state` varchar(32) NOT NULL,
    `model` varchar(32) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    INDEX `idxSecretResourcePoolVOUuid` (`uuid`),
    CONSTRAINT fkSecretResourcePoolVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SecurityMachineVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `zoneUuid` varchar(32) NOT NULL,
    `secretResourcePoolUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `state` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `model` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `managementIp` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    INDEX `idxSecurityMachineVOUuid` (`uuid`),
    INDEX `idxSecurityMachineVOSecretResourcePoolUuid` (`secretResourcePoolUuid`),
    CONSTRAINT fkSecurityMachineVOSecretResourcePoolVO FOREIGN KEY (secretResourcePoolUuid) REFERENCES SecretResourcePoolVO (uuid) ON DELETE RESTRICT,
    CONSTRAINT fkSecurityMachineVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`InfoSecSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `connectionMode` int unsigned NOT NULL,
    `activatedToken` varchar(32) DEFAULT NULL,
    `protectToken` varchar(32) DEFAULT NULL,
    `hmacToken` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkInfoSecSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`InfoSecSecurityMachineVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `port` int unsigned NOT NULL,
    `password` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkInfoSecSecurityMachineVOSecurityMachineVO FOREIGN KEY (uuid) REFERENCES SecurityMachineVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

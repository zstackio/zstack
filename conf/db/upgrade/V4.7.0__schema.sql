CREATE TABLE IF NOT EXISTS `HostIpmiVO`
(
    `uuid`            varchar(32)  NOT NULL UNIQUE,
    `ipmiAddress`     varchar(32),
    `ipmiPort`        int unsigned,
    `ipmiUsername`    varchar(255),
    `ipmiPassword`    varchar(255),
    `ipmiPowerStatus` varchar(255),
    PRIMARY KEY (`uuid`),
    CONSTRAINT `ukHostIpmiVO` UNIQUE (`ipmiAddress`, `ipmiPort`),
    CONSTRAINT `fkHostIpmiVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
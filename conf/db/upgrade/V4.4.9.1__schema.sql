CREATE TABLE IF NOT EXISTS `NvmeTargetVO` (
    `name` VARCHAR(256) DEFAULT NULL,
    `uuid` VARCHAR(32) NOT NULL,
    `nqn` VARCHAR(256) NOT NULL,
    `state` VARCHAR(64) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NvmeLunVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `nvmeTargetUuid` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkNvmeLunVONvmeTargetVO` FOREIGN KEY (`nvmeTargetUuid`) REFERENCES NvmeTargetVO (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

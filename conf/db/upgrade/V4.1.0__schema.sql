CREATE TABLE IF NOT EXISTS `zstack`.`LicenseHistoryVO`
(
    `id`          bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid`        varchar(32) NOT NULL,
    `cpuNum`      int(10) NOT NULL,
    `hostNum`     int(10) NOT NULL,
    `vmNum`       int(10) NOT NULL,
    `expiredDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `issuedDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `uploadDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `licenseType` varchar(32) NOT NULL,
    `userName`    varchar(32) NOT NULL,
    `addOnModule` varchar(32) DEFAULT NULL,
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE INDEX idxLicenseHistoryVOUploadDate ON LicenseHistoryVO (uploadDate);

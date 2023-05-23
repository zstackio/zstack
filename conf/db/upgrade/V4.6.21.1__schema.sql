CREATE TABLE IF NOT EXISTS `zstack`.`LicenseAppIdRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `licenseId` varchar(32) NOT NULL,
    `appId` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE IF NOT EXISTS `zstack`.`PrimaryStorageHistoricalUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `totalPhysicalCapacity` bigint unsigned DEFAULT 0,
    `usedPhysicalCapacity` bigint unsigned DEFAULT 0,
    `historicalForecast` bigint unsigned DEFAULT 0,
    `recordDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkUsageVOPrimaryStorageCapacityVO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES PrimaryStorageCapacityVO (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CephOsdGroupHistoricalUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `osdGroupUuid` varchar(32) NOT NULL,
    `totalPhysicalCapacity` bigint unsigned DEFAULT 0,
    `usedPhysicalCapacity` bigint unsigned DEFAULT 0,
    `historicalForecast` bigint unsigned DEFAULT 0,
    `recordDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkUsageVOCephOsdGroupVO` FOREIGN KEY (`osdGroupUuid`) REFERENCES CephOsdGroupVO (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LocalStorageHostHistoricalUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `totalPhysicalCapacity` bigint unsigned DEFAULT 0,
    `usedPhysicalCapacity` bigint unsigned DEFAULT 0,
    `historicalForecast` bigint unsigned DEFAULT 0,
    `recordDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT `fkUsageVOLocalStorageHostRefVO` FOREIGN KEY (`hostUuid`) REFERENCES LocalStorageHostRefVO (`hostUuid`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


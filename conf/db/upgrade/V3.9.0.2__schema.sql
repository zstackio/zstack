
CREATE TABLE IF NOT EXISTS `zstack`.`FaultToleranceVmGroupVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `primaryVmInstanceUuid` varchar(32),
    `secondaryVmInstanceUuid` varchar(32),
    `status` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CacheVolumeRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `volumeUuid` varchar(32) NOT NULL,
    `backingVolumeUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    KEY `volumeUuid` (`volumeUuid`),
    CONSTRAINT `fkCacheVolumeRefVOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES `zstack`.`VolumeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`FaultToleranceVmInstanceGroupHostPortRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `nbdServerPortId` bigint unsigned NOT NULL,
    `blockReplicationPortId` bigint unsigned NOT NULL,
    `primaryVmMonitorPortId` bigint unsigned NOT NULL,
    `secondaryVmMonitorPortId` bigint unsigned NOT NULL,
    `reservedVmMigrationPortId` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    KEY `vmInstanceUuid` (`vmInstanceUuid`),
    CONSTRAINT `fkShadowVmInstanceHostPortRefVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceVmNicRedirectPortRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `mirrorPortId` bigint unsigned NOT NULL,
    `primaryInPortId` bigint unsigned NOT NULL,
    `secondaryInPortId` bigint unsigned NOT NULL,
    `primaryOutPortId` bigint unsigned NOT NULL,
    `vmNicUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    KEY `vmInstanceUuid` (`vmInstanceUuid`),
    KEY `vmNicUuid` (`vmNicUuid`),
    CONSTRAINT `fkVmInstanceVmNicRedirectPortRefVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmInstanceVmNicRedirectPortRefVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostPortVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `port` int unsigned DEFAULT NULL,
    `portUsage` varchar(128) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    KEY `hostUuid` (`hostUuid`),
    CONSTRAINT `fkHostPortVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES HostEO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE ref1 FROM ImageBackupStorageRefVO ref1 INNER JOIN ImageBackupStorageRefVO ref2 WHERE ref1.imageUuid = ref2.imageUuid
 AND ref1.backupStorageUuid = ref2.backupStorageUuid AND ref1.id < ref2.id;
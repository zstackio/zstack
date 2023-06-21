CREATE TABLE IF NOT EXISTS `zstack`.`BlockVolumeVO` (
    `uuid` varchar(32) NOT NULL,
    `iscsiPath` varchar(1024) NOT NULL,
    `vendor` varchar(32) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`XskyBlockVolumeVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `blockVolumeUuid` varchar(32) NOT NULL,
    `accessPathId` int NOT NULL,
    `accessPathIqn` varchar(128) NOT NULL,
    `xskyStatus` varchar(32) NULL,
    `xskyBlockVolumeId` int NULL,
    `burstTotalBw` bigint NULL,
    `burstTotalIops` bigint NULL,
    `maxTotalBw` bigint NULL,
    `maxTotalIops` bigint NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkBlockVolumeVO` FOREIGN KEY (`blockVolumeUuid`) REFERENCES BlockVolumeVO(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`XskyVolumeSnapshotVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `volumeSnapshotUuid` varchar(32) NOT NULL,
    `xskyBlockVolumeId` int NOT NULL,
    `xskyBlockSnapshotId` int NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkVolumeSnapshotVO` FOREIGN KEY (`volumeSnapshotUuid`) REFERENCES VolumeSnapshotEO(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE IF NOT EXISTS `zstack`.`BlockVolumeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `iscsiPath` varchar(1024) NOT NULL,
    `vendor` varchar(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkBlockVolumeVOVolumeVO FOREIGN KEY (uuid) REFERENCES VolumeEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`XskyBlockVolumeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `accessPathId` int NOT NULL,
    `accessPathIqn` varchar(128) NOT NULL,
    `xskyStatus` varchar(32) NULL,
    `xskyBlockVolumeId` int NULL,
    `burstTotalBw` bigint NULL,
    `burstTotalIops` bigint NULL,
    `maxTotalBw` bigint NULL,
    `maxTotalIops` bigint NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkXskyBlockVolumeVOBlockVolumeVO FOREIGN KEY (uuid) REFERENCES BlockVolumeVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BareMetal2BondingNicRefVO` modify column nicUuid varchar(32) DEFAULT NULL;
CALL ADD_COLUMN('BareMetal2BondingNicRefVO', 'provisionNicUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_CONSTRAINT('BareMetal2BondingNicRefVO', 'fkBareMetal2BondingNicRefVOProvisionNicVO', 'provisionNicUuid', 'BareMetal2InstanceProvisionNicVO', 'uuid', 'CASCADE');
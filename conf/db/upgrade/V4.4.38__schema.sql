CREATE TABLE `zstack`.`CephOsdGroupVO` (
    `uuid` varchar(32) NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `osds` varchar(1024) NOT NULL,
    `availableCapacity` bigint(20) DEFAULT NULL,
    `availablePhysicalCapacity` bigint(20) unsigned NOT NULL DEFAULT 0,
    `totalPhysicalCapacity` bigint(20) unsigned NOT NULL DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    KEY `fkPrimaryStorageUuid` (`primaryStorageUuid`),
    CONSTRAINT `fkPrimaryStorageUuid` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE CephPrimaryStoragePoolVO ADD COLUMN `osdGroupUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`CephPrimaryStoragePoolVO` ADD CONSTRAINT fkCephPrimaryStoragePoolVOOsdGroupVO FOREIGN KEY (osdGroupUuid) REFERENCES CephOsdGroupVO (uuid) ON DELETE SET NULL;

# New feature: affinity group -- add 2 tables: AffinityGroupVO, AffinityGroupUsageVO
CREATE TABLE IF NOT EXISTS `AffinityGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `policy` VARCHAR(255) NOT NULL,
    `version` VARCHAR(255) NOT NULL,
    `type` VARCHAR(255) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AffinityGroupUsageVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `affinityGroupUuid` VARCHAR(32) NOT NULL,
    `resourceType` VARCHAR(255) NOT NULL,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkAffinityGroupUsageVOcreateAffinityGroupVO` FOREIGN KEY (`affinityGroupUuid`) REFERENCES `zstack`.`AffinityGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE VmInstanceEO ADD COLUMN `affinityGroupUuid` VARCHAR(32) DEFAULT NULL;
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE AliyunSnapshotVO DROP FOREIGN KEY fkAliyunSnapshotVOAliyunDiskVO;
ALTER TABLE AliyunSnapshotVO ADD CONSTRAINT fkAliyunSnapshotVOAliyunDiskVO FOREIGN KEY (diskUuid) REFERENCES AliyunDiskVO (uuid) ON DELETE SET NULL;
SET FOREIGN_KEY_CHECKS = 1;
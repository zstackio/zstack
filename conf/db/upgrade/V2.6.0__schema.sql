ALTER TABLE `SchedulerTriggerVO` ADD COLUMN `cron` varchar(32) DEFAULT NULL COMMENT 'interval in cron format';

CREATE TABLE IF NOT EXISTS `VolumeBackupVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `volumeUuid` VARCHAR(32) NOT NULL,
    `size` bigint unsigned NOT NULL,
    `state` varchar(64) NOT NULL,
    `status` varchar(64) NOT NULL,
    `metadata` text DEFAULT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table VolumeBackupVO
ALTER TABLE VolumeBackupVO ADD CONSTRAINT fkVolumeBackupVOResourceVO FOREIGN KEY (uuid) REFERENCES ResourceVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VolumeBackupVO ADD CONSTRAINT fkVolumeBackupVOVolumeEO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `VolumeBackupStorageRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `backupStorageUuid` varchar(32) NOT NULL,
    `volumeBackupUuid` varchar(32) NOT NULL,
    `status` varchar(64) NOT NULL,
    `installPath` VARCHAR(2048) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table VolumeBackupStorageRefVO
ALTER TABLE VolumeBackupStorageRefVO ADD CONSTRAINT fkVolumeBackupStorageRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE VolumeBackupStorageRefVO ADD CONSTRAINT fkVolumeBackupStorageRefVOVolumeBackupVO FOREIGN KEY (volumeBackupUuid) REFERENCES VolumeBackupVO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `VolumeBackupHistoryVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `bitmap` VARCHAR(32) NOT NULL,
    `lastBackupUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table VolumeBackupHistoryVO
ALTER TABLE VolumeBackupHistoryVO ADD CONSTRAINT fkVolumeBackupHistoryVOVolumeBackupVO FOREIGN KEY (lastBackupUuid) REFERENCES VolumeBackupVO (uuid) ON DELETE CASCADE;
ALTER TABLE VolumeBackupHistoryVO ADD CONSTRAINT fkVolumeBackupHistoryVOVolumeEO FOREIGN KEY (uuid) REFERENCES VolumeEO (uuid) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE AccountResourceRefVO ADD COLUMN `concreteResourceType` varchar(512) NOT NULL;
UPDATE AccountResourceRefVO set concreteResourceType = 'org.zstack.network.l2.vxlan.vxlanNetwork', resourceType = 'L2NetworkVO' WHERE resourceType = 'VxlanNetworkVO';
ALTER TABLE ResourceVO ADD COLUMN `concreteResourceType` varchar(512) NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `TwoFactorAuthenticationSecretVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `secret` VARCHAR(2048) NOT NULL,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `resourceType` VARCHAR(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

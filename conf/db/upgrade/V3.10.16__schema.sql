ALTER TABLE `zstack`.`SNSEmailPlatformVO` modify COLUMN `username` VARCHAR(255) NOT NULL;

ALTER TABLE `zstack`.`CephPrimaryStoragePoolVO`
    ADD COLUMN `securityPolicy` varchar(255) DEFAULT 'Copy',
    ADD COLUMN `diskUtilization` FLOAT;
UPDATE `zstack`.`CephPrimaryStoragePoolVO` SET `diskUtilization` = (SELECT format(1 / `replicatedSize`, 3));

ALTER TABLE `zstack`.`CephBackupStorageVO`
    ADD COLUMN `poolSecurityPolicy` varchar(255) DEFAULT 'Copy',
    ADD COLUMN `poolDiskUtilization` FLOAT;
UPDATE `zstack`.`CephBackupStorageVO` SET `poolDiskUtilization` = (SELECT format(1 / `poolReplicatedSize`, 3));
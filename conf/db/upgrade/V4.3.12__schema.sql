CREATE TABLE IF NOT EXISTS `zstack`.`EncryptionIntegrityVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `resourceUuid` varchar(32),
    `resourceType` varchar(64) ,
    `signedText` varchar(255),
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    UNIQUE KEY `resource` (`resourceUuid`,`resourceType`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`VolumeSnapshotVO` ADD COLUMN `md5sum` char(255) DEFAULT NULL;

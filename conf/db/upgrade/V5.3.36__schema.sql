CREATE TABLE IF NOT EXISTS `zstack`.`VolumeCbtBackupRecordVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `taskUuid` varchar(32) NOT NULL,
    `volumeUuid` varchar(32) NOT NULL,
    `mode` varchar(255) NOT NULL,
    `target` varchar(2048) NOT NULL,
    `scratchNodeName` varchar(255) NOT NULL,
    `bitmapName` varchar(255) NOT NULL,
    `lastBitmapName` varchar(255),
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE VIEW `zstack`.`GuestVmScriptVO` AS SELECT uuid, name, description, platform, encodingType, scriptContent, renderParams, scriptType, scriptTimeout, version, createDate, lastOpDate FROM `zstack`.`GuestVmScriptEO` WHERE deleted IS NULL;
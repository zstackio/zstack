CREATE TABLE IF NOT EXISTS `zstack`.`EncryptEntityMetadataVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `entityName` varchar(255) NOT NULL,
    `columnName` varchar(255) NOT NULL,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table LicenseHistoryVO modify COLUMN `userName` varchar(64) NOT NULL;

ALTER TABLE LicenseHistoryVO ADD COLUMN capacity int(10) NOT NULL;

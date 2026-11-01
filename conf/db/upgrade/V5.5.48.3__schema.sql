CREATE TABLE IF NOT EXISTS `zstack`.`FeaturePluginVO` (
    `id` varchar(64) NOT NULL,
    `version` varchar(64) NOT NULL,
    `sha256` char(64) NOT NULL,
    `packageName` varchar(255) NOT NULL,
    `descriptor` text NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

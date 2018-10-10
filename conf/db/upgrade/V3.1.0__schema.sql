ALTER TABLE `AlarmVO`  ADD COLUMN `type` varchar(32) NOT NULL;

CREATE TABLE IF NOT EXISTS `V2VConversionCacheVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `conversionHostUuid` varchar(32) NOT NULL,
    `srcVmUrl` varchar(255) NOT NULL,
    `installPath` varchar(255) NOT NULL,
    `deviceId` int unsigned NOT NULL,
    `virtualSize` bigint unsigned NOT NULL,
    `actualSize` bigint unsigned NOT NULL,
    `bootMode` varchar(64) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `zstack`.`VmCrashHistoryVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid` varchar(32) NOT NULL,
  `dateInLong` bigint unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `ElaborationVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `errorInfo` text NOT NULL,
  `md5sum` varchar(32) NOT NULL,
  `distance` double NOT NULL,
  `matched` boolean NOT NULL DEFAULT FALSE,
  `repeats` bigint(20) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX idxElaborationVOmd5sum ON ElaborationVO (md5sum);
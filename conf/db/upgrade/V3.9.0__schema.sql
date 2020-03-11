ALTER TABLE JsonLabelVO MODIFY COLUMN labelValue MEDIUMTEXT;
CREATE INDEX idxTaskProgressVOapiId ON TaskProgressVO(apiId);

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE DahoVllVbrRefVO;
DROP TABLE DahoCloudConnectionVO;
DROP TABLE DahoVllsVO;
DROP TABLE DahoConnectionVO;
DROP TABLE DahoDCAccessVO;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE ImageBackupStorageRefVO ADD COLUMN exportMd5Sum VARCHAR(255) DEFAULT NULL;
ALTER TABLE ImageBackupStorageRefVO ADD COLUMN exportUrl VARCHAR(2048) DEFAULT NULL;
UPDATE ImageBackupStorageRefVO ibs, ImageVO i SET ibs.exportMd5Sum = i.exportMd5Sum, ibs.exportUrl = i.exportUrl WHERE ibs.imageUuid = i.uuid;
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType FROM `zstack`.`ImageEO` WHERE deleted IS NULL;
ALTER TABLE ImageEO DROP exportMd5Sum, DROP exportUrl;
CREATE TABLE IF NOT EXISTS `zstack`.`AccessControlListVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `ipVersion` int(10) unsigned DEFAULT 4,
  `description` varchar(2048) DEFAULT NULL,
  `createDate` timestamp not null default '0000-00-00 00:00:00',
  `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AccessControlListEntryVO` (
  `entryId` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `aclUuid` varchar(32) NOT NULL,
  `ipEntries` varchar(2048) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkACLRuleVOAccessControlListVO` (`aclUuid`),
  CONSTRAINT `fkACLRuleVOAccessControlListVO` FOREIGN KEY (`aclUuid`) REFERENCES `AccessControlListVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LoadBalancerListenerACLRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `aclUuid` varchar(32) NOT NULL,
  `listenerUuid` varchar(32) NOT NULL,
  `aclType` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkLoadbalancerListenerACLRefVOLoadBalancerListenerVO` (`listenerUuid`) USING BTREE,
  KEY `fkLoadbalancerListenerACLRefVOAccessControlListVO` (`aclUuid`) USING BTREE,
  CONSTRAINT `fkLoadbalancerListenerACLRefVOLoadBalancerListenerVO` FOREIGN KEY (`listenerUuid`) REFERENCES `LoadBalancerListenerVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkLoadbalancerListenerACLRefVOAccessControlListVO` FOREIGN KEY (`aclUuid`) REFERENCES `AccessControlListVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;

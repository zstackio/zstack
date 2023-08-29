CREATE TABLE `NvmeServerVO` (
  `uuid` VARCHAR(32) NOT NULL,
  `name` VARCHAR(256) NOT NULL,
  `ip` VARCHAR(64) NOT NULL,
  `port` int unsigned DEFAULT 4420,
  `transport` VARCHAR(32) NOT NULL,
  `state` VARCHAR(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NvmeServerClusterRefVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `clusterUuid` VARCHAR(32) NOT NULL,
  `nvmeServerUuid` VARCHAR(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  CONSTRAINT `fkNvmeServerClusterRefVONvmeServerVO` FOREIGN KEY (`nvmeServerUuid`) REFERENCES NvmeServerVO (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNvmeServerClusterRefVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES ClusterEO (`uuid`) ON DELETE CASCADE
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`NvmeTargetVO` ADD COLUMN `nvmeServerUuid` varchar(32);
ALTER TABLE `zstack`.`NvmeTargetVO` ADD CONSTRAINT `fkNvmeTargetVONvmeServerVO` FOREIGN KEY (nvmeServerUuid) REFERENCES NvmeServerVO(uuid) ON DELETE SET NULL;
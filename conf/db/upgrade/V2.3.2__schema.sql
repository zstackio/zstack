ALTER TABLE CephPrimaryStoragePoolVO ADD availableCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephPrimaryStoragePoolVO ADD usedCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephPrimaryStoragePoolVO ADD replicatedSize int unsigned;

ALTER TABLE CephBackupStorageVO ADD poolAvailableCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephBackupStorageVO ADD poolUsedCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephBackupStorageVO ADD poolReplicatedSize int unsigned;

CREATE TABLE  `zstack`.`CertificateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048),
    `certificate` text NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerListenerCertificateRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `listenerUuid` varchar(32) NOT NULL,
    `certificateUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkLoadBalancerListenerCertificateRefVOLoadBalancerListenerVO` FOREIGN KEY (`listenerUuid`) REFERENCES `zstack`.`LoadBalancerListenerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkLoadBalancerListenerCertificateRefVOCertificateVO` FOREIGN KEY (`certificateUuid`) REFERENCES `zstack`.`CertificateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`L3NetworkHostRouteVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
    `prefix` varchar(255) NOT NULL,
    `nexthop` varchar(255) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkL3NetworkHostRouteVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

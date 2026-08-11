CALL ADD_COLUMN('SNSSnmpPlatformVO', 'version', 'VARCHAR(32)', 0, 'v1');
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'community', 'TEXT', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'userName', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'authEnabled', 'tinyint(1)', 0, 0);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'authAlgorithm', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'authPassword', 'TEXT', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'privacyEnabled', 'tinyint(1)', 0, 0);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'privacyAlgorithm', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'privacyPassword', 'TEXT', 1, NULL);
CALL ADD_COLUMN('SNSSnmpPlatformVO', 'configRevision', 'BIGINT UNSIGNED', 0, 0);

CREATE TABLE IF NOT EXISTS `zstack`.`SnmpEngineVO` (
    `uuid` varchar(32) NOT NULL,
    `engineId` varchar(64) NOT NULL UNIQUE,
    `engineBoots` int unsigned NOT NULL DEFAULT 1,
    `engineStartTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `ownerManagementNodeUuid` varchar(32) DEFAULT NULL,
    `ownerEpochUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LoadBalancerListenerServerGroupVmNicRefVO` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `listenerUuid` varchar(32) NOT NULL,
    `serverGroupUuid` varchar(32) NOT NULL,
    `vmNicUuid` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukListenerServerGroupVmNic` (`listenerUuid`, `serverGroupUuid`, `vmNicUuid`),
    CONSTRAINT `fkLbListenerVmNicRefListener`
        FOREIGN KEY (`listenerUuid`) REFERENCES `LoadBalancerListenerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkLbListenerVmNicRefGroup`
        FOREIGN KEY (`serverGroupUuid`) REFERENCES `LoadBalancerServerGroupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkLbListenerVmNicRefVmNic`
        FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LoadBalancerListenerServerGroupServerIpRefVO` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `listenerUuid` varchar(32) NOT NULL,
    `serverGroupUuid` varchar(32) NOT NULL,
    `serverIpId` bigint unsigned NOT NULL,
    `state` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukListenerServerGroupServerIp` (`listenerUuid`, `serverGroupUuid`, `serverIpId`),
    CONSTRAINT `fkLbListenerServerIpRefListener`
        FOREIGN KEY (`listenerUuid`) REFERENCES `LoadBalancerListenerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkLbListenerServerIpRefGroup`
        FOREIGN KEY (`serverGroupUuid`) REFERENCES `LoadBalancerServerGroupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkLbListenerServerIpRefServerIp`
        FOREIGN KEY (`serverIpId`) REFERENCES `LoadBalancerServerGroupServerIpVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

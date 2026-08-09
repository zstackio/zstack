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
-- ZNS segment binding state and controller inventory

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsControllerStateVO` (
    `sdnControllerUuid` varchar(32) NOT NULL,
    `activeScanToken` varchar(255) DEFAULT NULL,
    `activeScanStartedAt` timestamp NULL DEFAULT NULL,
    `migrationState` varchar(32) NOT NULL DEFAULT 'NotStarted',
    `compatibilityMode` varchar(32) NOT NULL DEFAULT 'LEGACY',
    `compatibilityEpoch` bigint NOT NULL DEFAULT 0,
    `remoteCompatibilityEpoch` bigint DEFAULT NULL,
    `capabilityDigest` varchar(64) DEFAULT NULL,
    `migrationDigest` varchar(64) DEFAULT NULL,
    `pendingContractEpoch` bigint DEFAULT NULL,
    `pendingContractRequestDigest` varchar(64) DEFAULT NULL,
    `lastSuccessfulScanDate` timestamp NULL DEFAULT NULL,
    `lastErrorCode` varchar(128) DEFAULT NULL,
    `lastErrorDetails` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`sdnControllerUuid`),
    CONSTRAINT `fkZnsControllerStateVOSdnControllerVO`
        FOREIGN KEY (`sdnControllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsSegmentInventoryVO` (
    `uuid` varchar(32) NOT NULL,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `znsSegmentUuid` varchar(36) NOT NULL,
    `name` varchar(255) NOT NULL,
    `transportZoneUuid` varchar(36) NOT NULL,
    `availability` varchar(32) NOT NULL,
    `reasonCode` varchar(128) DEFAULT NULL,
    `lastSeenToken` varchar(36) NOT NULL,
    `lastSeenDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_zns_seg_inv_controller_segment` (`sdnControllerUuid`, `znsSegmentUuid`),
    KEY `idx_zns_seg_inv_candidate` (`sdnControllerUuid`, `transportZoneUuid`, `availability`),
    CONSTRAINT `fkZnsSegmentInventoryVOSdnControllerVO`
        FOREIGN KEY (`sdnControllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsSegmentRefVO` (
    `uuid` varchar(32) NOT NULL,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `znsSegmentUuid` varchar(36) DEFAULT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `l3NetworkUuid` varchar(32) DEFAULT NULL,
    `znsTenantRouterUuid` varchar(36) DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `operationUuid` varchar(36) DEFAULT NULL,
    `operationStep` varchar(32) DEFAULT NULL,
    `operationDigest` varchar(64) DEFAULT NULL,
    `appliedConfigVersion` bigint DEFAULT NULL,
    `lastErrorCode` varchar(128) DEFAULT NULL,
    `lastErrorDetails` text DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_zns_seg_ref_controller_segment_zone`
        (`sdnControllerUuid`, `znsSegmentUuid`, `zoneUuid`),
    UNIQUE KEY `uk_zns_seg_ref_l2` (`l2NetworkUuid`),
    KEY `idx_zns_seg_ref_account` (`accountUuid`),
    KEY `idx_zns_seg_ref_state` (`sdnControllerUuid`, `state`),
    CONSTRAINT `fkZnsSegmentRefVOSdnControllerVO`
        FOREIGN KEY (`sdnControllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `chk_zns_seg_ref_segment_state`
        CHECK (`znsSegmentUuid` IS NOT NULL OR `state` = 'MigrationFailed')
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

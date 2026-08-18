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

CALL ADD_COLUMN('ZnsControllerVO', 'zcfAccessKeyId', 'VARCHAR(64)', 1, NULL);
CALL ADD_COLUMN('ZnsControllerVO', 'zcfAccessKeySecret', 'TEXT', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsControllerStateVO` (
    `sdnControllerUuid` varchar(32) NOT NULL,
    `activeScanToken` varchar(255) DEFAULT NULL,
    `activeScanStartedAt` timestamp NULL DEFAULT NULL,
    `previousCompleteScanToken` varchar(255) DEFAULT NULL,
    `previousCompleteScanDigest` varchar(64) DEFAULT NULL,
    `previousCompleteScanTotal` int DEFAULT NULL,
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

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsControllerCapabilityVO` (
    `uuid` varchar(32) NOT NULL,
    `controllerUuid` varchar(32) NOT NULL,
    `capabilityName` varchar(64) NOT NULL,
    `selectedContract` varchar(32) DEFAULT NULL,
    `observedState` varchar(32) NOT NULL DEFAULT 'NOT_ACTIVATED',
    `effectiveState` varchar(32) NOT NULL DEFAULT 'NOT_ACTIVATED',
    `observedEpoch` bigint NOT NULL DEFAULT 0,
    `supportedContracts` text DEFAULT NULL,
    `servingClusterSupported` tinyint(1) DEFAULT NULL,
    `servingClusterDigest` varchar(128) DEFAULT NULL,
    `cloudClusterSupported` tinyint(1) DEFAULT NULL,
    `cloudClusterDigest` varchar(128) DEFAULT NULL,
    `pendingTransitionUuid` varchar(255) DEFAULT NULL,
    `pendingExpectedEpoch` bigint DEFAULT NULL,
    `pendingSelectedContract` varchar(32) DEFAULT NULL,
    `pendingTargetState` varchar(32) DEFAULT NULL,
    `pendingRequestDigest` varchar(64) DEFAULT NULL,
    `lastErrorCode` varchar(128) DEFAULT NULL,
    `lastErrorDetails` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukZnsControllerCapabilityVOControllerName` (`controllerUuid`, `capabilityName`),
    CONSTRAINT `fkZnsControllerCapabilityVOSdnControllerVO`
        FOREIGN KEY (`controllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `chk_zns_controller_capability_state`
        CHECK (`observedState` IN ('NOT_ACTIVATED', 'MIGRATING_READ_ONLY', 'ACTIVE', 'INCOMPATIBLE_READ_ONLY')
            AND `effectiveState` IN ('NOT_ACTIVATED', 'MIGRATING_READ_ONLY', 'ACTIVE', 'INCOMPATIBLE_READ_ONLY'))
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
    `currentConfigVersion` bigint DEFAULT NULL,
    `appliedConfigVersion` bigint DEFAULT NULL,
    `lastErrorCode` varchar(128) DEFAULT NULL,
    `lastErrorDetails` text DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_zns_seg_ref_controller_segment`
        (`sdnControllerUuid`, `znsSegmentUuid`),
    UNIQUE KEY `uk_zns_seg_ref_l2` (`l2NetworkUuid`),
    KEY `idx_zns_seg_ref_account` (`accountUuid`),
    KEY `idx_zns_seg_ref_state` (`sdnControllerUuid`, `state`),
    CONSTRAINT `fkZnsSegmentRefVOSdnControllerVO`
        FOREIGN KEY (`sdnControllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `chk_zns_seg_ref_segment_state`
        CHECK (`znsSegmentUuid` IS NOT NULL OR `state` = 'MigrationFailed')
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TRIGGER IF EXISTS `zstack`.`trg_zns_controller_capability_validate_insert`;
DELIMITER $$
CREATE TRIGGER `zstack`.`trg_zns_controller_capability_validate_insert`
BEFORE INSERT ON `zstack`.`ZnsControllerCapabilityVO`
FOR EACH ROW
BEGIN
    IF NEW.`observedState` NOT IN ('NOT_ACTIVATED', 'MIGRATING_READ_ONLY', 'ACTIVE', 'INCOMPATIBLE_READ_ONLY')
            OR NEW.`effectiveState` NOT IN ('NOT_ACTIVATED', 'MIGRATING_READ_ONLY', 'ACTIVE', 'INCOMPATIBLE_READ_ONLY') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ZnsControllerCapabilityVO has an invalid capability state';
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS `zstack`.`trg_zns_controller_capability_validate_update`;
DELIMITER $$
CREATE TRIGGER `zstack`.`trg_zns_controller_capability_validate_update`
BEFORE UPDATE ON `zstack`.`ZnsControllerCapabilityVO`
FOR EACH ROW
BEGIN
    IF NEW.`observedState` NOT IN ('NOT_ACTIVATED', 'MIGRATING_READ_ONLY', 'ACTIVE', 'INCOMPATIBLE_READ_ONLY')
            OR NEW.`effectiveState` NOT IN ('NOT_ACTIVATED', 'MIGRATING_READ_ONLY', 'ACTIVE', 'INCOMPATIBLE_READ_ONLY') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ZnsControllerCapabilityVO has an invalid capability state';
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS `zstack`.`trg_zns_seg_ref_validate_insert`;
DELIMITER $$
CREATE TRIGGER `zstack`.`trg_zns_seg_ref_validate_insert`
BEFORE INSERT ON `zstack`.`ZnsSegmentRefVO`
FOR EACH ROW
BEGIN
    IF NEW.`znsSegmentUuid` IS NULL AND NEW.`state` <> 'MigrationFailed' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ZnsSegmentRefVO requires znsSegmentUuid unless migration failed';
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS `zstack`.`trg_zns_seg_ref_validate_update`;
DELIMITER $$
CREATE TRIGGER `zstack`.`trg_zns_seg_ref_validate_update`
BEFORE UPDATE ON `zstack`.`ZnsSegmentRefVO`
FOR EACH ROW
BEGIN
    IF NEW.`znsSegmentUuid` IS NULL AND NEW.`state` <> 'MigrationFailed' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ZnsSegmentRefVO requires znsSegmentUuid unless migration failed';
    END IF;
END$$
DELIMITER ;

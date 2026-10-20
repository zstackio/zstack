CALL ADD_COLUMN('ModelVO', 'introductionFilePath', 'VARCHAR(2048)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsControllerApiCompatibilityVO` (
    `uuid` varchar(32) NOT NULL,
    `controllerUuid` varchar(32) NOT NULL,
    `token` varchar(64) NOT NULL,
    `status` varchar(32) NOT NULL,
    `expectedVersion` varchar(32) DEFAULT NULL,
    `actualVersion` varchar(32) DEFAULT NULL,
    `source` varchar(32) NOT NULL,
    `differenceDetails` text DEFAULT NULL,
    `impact` text DEFAULT NULL,
    `fallbackSolution` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukControllerToken` (`controllerUuid`, `token`),
    INDEX `idxControllerUuid` (`controllerUuid`),
    CONSTRAINT `fkZnsControllerApiCompatibilityVOSdnControllerVO`
        FOREIGN KEY (`controllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CALL ADD_COLUMN('ZnsSegmentRefVO', 'managedBy', 'VARCHAR(32)', 1, NULL);
UPDATE `zstack`.`ZnsSegmentRefVO` SET `managedBy` = 'ZNS' WHERE `managedBy` IS NULL;

-- ZCF-6749: shared durable inbox for sync-resource-v2.
DROP PROCEDURE IF EXISTS `ENSURE_ZNS_SYNC_RESOURCE_V2_COLUMNS`;
DELIMITER $$
CREATE PROCEDURE `ENSURE_ZNS_SYNC_RESOURCE_V2_COLUMNS`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsNetworkNotificationVO'
                     AND column_name = 'protocolVersion') THEN
        ALTER TABLE `zstack`.`ZnsNetworkNotificationVO`
            ADD COLUMN `protocolVersion` int NOT NULL DEFAULT 1;
    END IF;
    ALTER TABLE `zstack`.`ZnsNetworkNotificationVO` ALTER COLUMN `protocolVersion` SET DEFAULT 1;
    ALTER TABLE `zstack`.`ZnsResourceEventReceiptVO` MODIFY COLUMN `eventCursor` bigint NULL;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsResourceEventReceiptVO'
                     AND column_name = 'deliveryKey') THEN
        ALTER TABLE `zstack`.`ZnsResourceEventReceiptVO`
            ADD COLUMN `deliveryKey` varchar(255) CHARACTER SET ascii COLLATE ascii_bin NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsResourceEventReceiptVO'
                     AND index_name = 'ukZnsResourceEventReceiptDelivery') THEN
        ALTER TABLE `zstack`.`ZnsResourceEventReceiptVO`
            ADD UNIQUE KEY `ukZnsResourceEventReceiptDelivery` (`controllerUuid`, `taskUuid`, `deliveryKey`);
    END IF;
END $$
DELIMITER ;
CALL `ENSURE_ZNS_SYNC_RESOURCE_V2_COLUMNS`();
DROP PROCEDURE `ENSURE_ZNS_SYNC_RESOURCE_V2_COLUMNS`;

ALTER TABLE `zstack`.`ZnsSegmentInventoryVO` MODIFY COLUMN `transportZoneUuid` varchar(36) NULL;
CALL ADD_COLUMN('ZnsSegmentRefVO', 'leaseUntil', 'TIMESTAMP NULL', 1, NULL);
CALL ADD_COLUMN('ZnsSegmentRefVO', 'fenceToken', 'varchar(32)', 1, NULL);
CALL ADD_COLUMN('ZnsSegmentRefVO', 'fenceUntil', 'TIMESTAMP NULL', 1, NULL);

DROP TABLE IF EXISTS `zstack`.`ZnsControllerTransitionVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsControllerCapabilityVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsCloudOfferSnapshotVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsPeerOfferVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsCloudProtocolIdentityVO`;
DROP TABLE IF EXISTS `zstack`.`ZnsSegmentProjectionDeleteVO`;

CALL DELETE_INDEX('ZnsSegmentRefVO', 'idxZnsSegmentRefVOOperationUuid');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationCapabilityName');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationCapabilityContract');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationCapabilityEpoch');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationProviderIdentity');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationProfileIdentity');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationAdapterIdentity');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationUuid');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationStep');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'operationDigest');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'leaseUntil');
CALL DROP_COLUMN('ZnsSegmentRefVO', 'currentConfigVersion');

CALL ADD_COLUMN('ZnsSegmentRefVO', 'consistencyStatus', 'VARCHAR(32)', 0, 'Unknown');
CALL ADD_COLUMN('ZnsSegmentRefVO', 'consistencyDetails', 'TEXT', 1, NULL);

ALTER TABLE `ZnsSegmentRefVO`
    MODIFY COLUMN `zoneUuid` VARCHAR(32) DEFAULT NULL,
    MODIFY COLUMN `l2NetworkUuid` VARCHAR(32) DEFAULT NULL,
    MODIFY COLUMN `accountUuid` VARCHAR(32) DEFAULT NULL;

UPDATE `zstack`.`SystemTagVO`
SET `tag` = CONCAT('flatNetwork::Dhcp6Server::',
                   SUBSTRING(`tag`, CHAR_LENGTH('flatNetwork::DhcpServer::') + 1))
WHERE `resourceType` = 'L3NetworkVO'
  AND `tag` LIKE 'flatNetwork::DhcpServer::%::ipUuid::%'
  AND LOCATE(':', REPLACE(SUBSTRING_INDEX(
      SUBSTRING(`tag`, CHAR_LENGTH('flatNetwork::DhcpServer::') + 1),
      '::ipUuid::', 1), '--', '::')) > 0;

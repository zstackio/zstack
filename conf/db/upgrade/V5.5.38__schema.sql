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

CREATE TABLE IF NOT EXISTS `zstack`.`SNSPluginPlatformVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `pluginDriverUuid` varchar(32) DEFAULT NULL,
    `properties` text DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkSNSPluginPlatformVOSNSApplicationPlatformVO FOREIGN KEY (`uuid`)
        REFERENCES `SNSApplicationPlatformVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT fkSNSPluginPlatformVOPluginDriverVO FOREIGN KEY (`pluginDriverUuid`)
        REFERENCES `PluginDriverVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSPluginTextTemplateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `pluginDriverUuid` varchar(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkSNSPluginTextTemplateVOSNSTextTemplateVO FOREIGN KEY (`uuid`)
        REFERENCES `SNSTextTemplateVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT fkSNSPluginTextTemplateVOPluginDriverVO FOREIGN KEY (`pluginDriverUuid`)
        REFERENCES `PluginDriverVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`SNSPluginEndpointVO`
    MODIFY COLUMN `pluginDriverUuid` varchar(32) DEFAULT NULL,
    MODIFY COLUMN `properties` text DEFAULT NULL;

DROP PROCEDURE IF EXISTS UpgradeSNSPluginEndpointForeignKeys;
DELIMITER $$
CREATE PROCEDURE UpgradeSNSPluginEndpointForeignKeys()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = 'zstack'
          AND TABLE_NAME = 'SNSPluginEndpointVO'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
          AND CONSTRAINT_NAME = 'fkPluginEndpointVOPluginDriverVO'
    ) THEN
        ALTER TABLE `zstack`.`SNSPluginEndpointVO`
            DROP FOREIGN KEY fkPluginEndpointVOPluginDriverVO;
    END IF;

    CALL ADD_CONSTRAINT('SNSPluginEndpointVO', 'fkSNSPluginEndpointVOPluginDriverVO',
                        'pluginDriverUuid', 'PluginDriverVO', 'uuid', 'SET NULL');

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = 'zstack'
          AND TABLE_NAME = 'SNSSmsReceiverVO'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
          AND CONSTRAINT_NAME = 'fkSNSSmsReceiverVOSNSUniversalSmsEndpointVO'
    ) THEN
        ALTER TABLE `zstack`.`SNSSmsReceiverVO`
            DROP FOREIGN KEY fkSNSSmsReceiverVOSNSUniversalSmsEndpointVO;
    END IF;

    CALL ADD_CONSTRAINT('SNSSmsReceiverVO', 'fkSNSSmsReceiverVOSNSApplicationEndpointVO',
                        'endpointUuid', 'SNSApplicationEndpointVO', 'uuid', 'CASCADE');
END $$
DELIMITER ;
CALL UpgradeSNSPluginEndpointForeignKeys();
DROP PROCEDURE IF EXISTS UpgradeSNSPluginEndpointForeignKeys;

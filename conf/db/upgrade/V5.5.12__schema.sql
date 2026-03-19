CALL ADD_COLUMN('AccessKeyVO', 'type', 'varchar(32)', 0, 'User');

-- ZSTAC-75319: Add normalizedModelName column for GPU spec dedup
CALL ADD_COLUMN('GpuDeviceSpecVO', 'normalizedModelName', 'VARCHAR(255)', 1, NULL);
CALL CREATE_INDEX('GpuDeviceSpecVO', 'idx_gpu_spec_normalized_model', 'normalizedModelName');

-- Add totalScore and endTime columns to ModelEvaluationTaskVO for ZQL sorting support
-- Previously these values were only stored inside the opaque JSON TEXT field,
-- making them invisible to ZQL ORDER BY queries.
CALL ADD_COLUMN('ModelEvaluationTaskVO', 'totalScore', 'DOUBLE', 1, NULL);
CALL ADD_COLUMN('ModelEvaluationTaskVO', 'endTime', 'DATETIME', 1, NULL);

-- Add indexes to support efficient sorting
CALL CREATE_INDEX('ModelEvaluationTaskVO', 'idx_ModelEvaluationTaskVO_totalScore', 'totalScore');
CALL CREATE_INDEX('ModelEvaluationTaskVO', 'idx_ModelEvaluationTaskVO_endTime', 'endTime');

-- Backfill totalScore from opaque JSON for existing completed tasks
-- Uses Json_getKeyValue defined in beforeMigrate.sql for MySQL 5.5+ compatibility
UPDATE `zstack`.`ModelEvaluationTaskVO`
SET `totalScore` = CAST(Json_getKeyValue(`opaque`, 'total_score') AS DECIMAL(20,6))
WHERE `opaque` IS NOT NULL
  AND `totalScore` IS NULL
  AND Json_getKeyValue(`opaque`, 'total_score') IS NOT NULL;

-- Backfill endTime from opaque JSON for existing completed/failed tasks
-- end_time format from Python agent: "MMM dd, yyyy hh:mm:ss a" (e.g. "Jan 01, 2025 10:30:00 AM")
UPDATE `zstack`.`ModelEvaluationTaskVO`
SET `endTime` = STR_TO_DATE(
    Json_getKeyValue(`opaque`, 'end_time'),
    '%b %d, %Y %h:%i:%s %p'
)
WHERE `opaque` IS NOT NULL
  AND `endTime` IS NULL
  AND Json_getKeyValue(`opaque`, 'end_time') IS NOT NULL
  AND Json_getKeyValue(`opaque`, 'end_time') != '';

-- Add ExternalServiceConfiguration table
CREATE TABLE IF NOT EXISTS `zstack`.`ExternalServiceConfigurationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serviceType` varchar(32) NOT NULL,
    `configuration` text DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE VolumeSnapshotVO AS sp, PrimaryStorageVO AS ps
SET sp.primaryStorageInstallPath = REPLACE(sp.primaryStorageInstallPath, '/dev/', 'sharedblock://')
WHERE sp.primaryStorageUuid = ps.uuid AND ps.type = 'SharedBlock' AND sp.volumeType = 'Memory' AND sp.primaryStorageInstallPath LIKE '/dev/%';

UPDATE `zstack`.`ActiveAlarmTemplateVO`
SET `metricName` = 'CPUUsedUtilization'
WHERE `uuid` = 'c9e6cdca107140bea62b4ca919ff9e88'
  AND `metricName` = 'VRouterCPUAverageUsedUtilization';

UPDATE `zstack`.`AlarmVO`
SET `metricName` = 'CPUUsedUtilization'
WHERE `uuid` IN (
    SELECT `alarmUuid` FROM `zstack`.`ActiveAlarmVO`
    WHERE `templateUuid` = 'c9e6cdca107140bea62b4ca919ff9e88'
)
  AND `metricName` = 'VRouterCPUAverageUsedUtilization';

-- ZSTAC-80472: Resource notification webhook tables
CREATE TABLE IF NOT EXISTS `zstack`.`ResNotifySubscriptionVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `resourceTypes` TEXT DEFAULT NULL,
    `eventTypes` VARCHAR(256) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL DEFAULT 'WEBHOOK',
    `state` VARCHAR(32) NOT NULL DEFAULT 'Enabled',
    `accountUuid` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    INDEX `idx_ResNotifySubscriptionVO_accountUuid` (`accountUuid`),
    INDEX `idx_ResNotifySubscriptionVO_type_state` (`type`, `state`),
    CONSTRAINT `fkResNotifySubscriptionVOResourceVO` FOREIGN KEY (`uuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResNotifyWebhookRefVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `webhookUrl` TEXT NOT NULL,
    `secret` VARCHAR(256) DEFAULT NULL,
    `customHeaders` TEXT,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fk_ResNotifyWebhookRefVO_ResNotifySubscriptionVO`
        FOREIGN KEY (`uuid`) REFERENCES `ResNotifySubscriptionVO`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`BareMetal2DpuChassisVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `config` TEXT  DEFAULT NULL,
    `hostUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2DpuChassisVOChassisVO` FOREIGN KEY (`uuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2DpuChassisVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`BareMetal2DpuHostVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `chassisUuid` VARCHAR(32) NOT NULL,
    `vendorType` VARCHAR(255) NOT NULL,
    `url` VARCHAR(255) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBareMetal2DpuHostVOHostVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkBareMetal2DpuHostVOChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2DpuChassisVO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BareMetal2InstanceVO`
DROP FOREIGN KEY `fkBareMetal2InstanceVOGatewayVO`,
DROP FOREIGN KEY `fkBareMetal2InstanceVOGatewayVO1`;

ALTER TABLE `zstack`.`BareMetal2InstanceVO`
    ADD CONSTRAINT `fkBareMetal2InstanceVOGatewayVO`
        FOREIGN KEY (`gatewayUuid`)
        REFERENCES `HostEO` (`uuid`)
        ON DELETE SET NULL,
    ADD CONSTRAINT `fkBareMetal2InstanceVOGatewayVO1`
        FOREIGN KEY (`lastGatewayUuid`)
        REFERENCES `HostEO` (`uuid`)
        ON DELETE SET NULL;

-- ZSTAC-68709: Add targetQueueKey for evaluation task queuing per service endpoint
CALL ADD_COLUMN('ModelEvaluationTaskVO', 'targetQueueKey', 'TEXT', 1, NULL);

-- Add prefixLen column to UsedIpVO for IPv6 addresses outside IP range
CALL ADD_COLUMN('UsedIpVO', 'prefixLen', 'INT', 1, NULL);

-- Backfill prefixLen from IpRangeVO for existing IPv6 UsedIpVO records
UPDATE `zstack`.`UsedIpVO` `u`
INNER JOIN `zstack`.`IpRangeVO` `r` ON `u`.`ipRangeUuid` = `r`.`uuid`
SET `u`.`prefixLen` = `r`.`prefixLen`
WHERE `u`.`ipVersion` = 6
  AND `u`.`ipRangeUuid` IS NOT NULL
  AND `u`.`prefixLen` IS NULL;

-- Modify ipRangeUuid foreign key constraint to SET NULL on delete (instead of CASCADE)
DROP PROCEDURE IF EXISTS ModifyUsedIpVOForeignKey;
DELIMITER $$

CREATE PROCEDURE ModifyUsedIpVOForeignKey()
BEGIN
    DECLARE constraint_exists INT;

    -- Check if the constraint exists before dropping it
    SELECT COUNT(*)
    INTO constraint_exists
    FROM `INFORMATION_SCHEMA`.`TABLE_CONSTRAINTS`
    WHERE `TABLE_SCHEMA` = 'zstack'
      AND `TABLE_NAME` = 'UsedIpVO'
      AND `CONSTRAINT_NAME` = 'fkUsedIpVOIpRangeEO'
      AND `CONSTRAINT_TYPE` = 'FOREIGN KEY';

    IF constraint_exists > 0 THEN
        ALTER TABLE `zstack`.`UsedIpVO` DROP FOREIGN KEY `fkUsedIpVOIpRangeEO`;
    END IF;

    -- Re-check before adding so the migration stays idempotent
    SELECT COUNT(*)
    INTO constraint_exists
    FROM `INFORMATION_SCHEMA`.`TABLE_CONSTRAINTS`
    WHERE `TABLE_SCHEMA` = 'zstack'
      AND `TABLE_NAME` = 'UsedIpVO'
      AND `CONSTRAINT_NAME` = 'fkUsedIpVOIpRangeEO'
      AND `CONSTRAINT_TYPE` = 'FOREIGN KEY';

    IF constraint_exists = 0 THEN
        ALTER TABLE `zstack`.`UsedIpVO`
        ADD CONSTRAINT `fkUsedIpVOIpRangeEO`
        FOREIGN KEY (`ipRangeUuid`) REFERENCES `zstack`.`IpRangeEO`(`uuid`) ON DELETE SET NULL;
    END IF;
END $$

DELIMITER ;

CALL ModifyUsedIpVOForeignKey();
DROP PROCEDURE IF EXISTS ModifyUsedIpVOForeignKey;

-- Add ActiveAlarmTemplate rows for OVN (SDN) VM instance metric alarms
INSERT IGNORE INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('f1a6c2d85e7b8c9d3e4f5a6b7c8d9e0f','OvnVmInstance-DiskAllUsedCapacityInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/OvnVmInstance','DiskAllUsedCapacityInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT IGNORE INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('b3c8e4f07a9d0e1f5a6b7c8d9e0f1a2b','OvnVmInstance-MemoryUsedInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/OvnVmInstance','MemoryUsedInPercent',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);
INSERT IGNORE INTO `ActiveAlarmTemplateVO` (`uuid`,`alarmName`,`comparisonOperator`,`period`,`repeatInterval`,`namespace`,`metricName`,`threshold`,`lastOpDate`,`createDate`,`repeatCount`,`enableRecovery`,`emergencyLevel`,`labels`) VALUES ('d5e0a6b29c1f2a3b7c8d9e0f1a2b3c4d','OvnVmInstance-CPUAverageUsedUtilization','GreaterThanOrEqualTo',300,1800,'ZStack/OvnVmInstance','CPUAverageUsedUtilization',80,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP(),-1,0,'Important',NULL);

-- ZSTAC-74908: Add resourceType to TagPatternVO to scope AI model tags away from VM pages
CALL ADD_COLUMN('TagPatternVO', 'resourceType', 'VARCHAR(128)', 1, NULL);

-- ZSTAC-70478: Add deleted field to ModelServiceVO for soft delete support
CALL ADD_COLUMN('ModelServiceVO', 'deleted', 'TINYINT(1)', 0, 0);

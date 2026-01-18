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

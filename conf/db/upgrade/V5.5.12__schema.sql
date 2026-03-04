-- ZSTAC-75319: Add normalizedModelName column for GPU spec dedup
CALL ADD_COLUMN('GpuDeviceSpecVO', 'normalizedModelName', 'VARCHAR(255)', 1, NULL);
CALL CREATE_INDEX('GpuDeviceSpecVO', 'idx_gpu_spec_normalized_model', 'normalizedModelName');

-- Add totalScore and endTime columns to ModelEvaluationTaskVO for ZQL sorting support
-- Previously these values were only stored inside the opaque JSON TEXT field,
-- making them invisible to ZQL ORDER BY queries.
CALL ADD_COLUMN('ModelEvaluationTaskVO', 'totalScore', 'DOUBLE', 1, NULL);
CALL ADD_COLUMN('ModelEvaluationTaskVO', 'endTime', 'TIMESTAMP', 1, NULL);

-- Add indexes to support efficient sorting
CALL CREATE_INDEX('ModelEvaluationTaskVO', 'idx_ModelEvaluationTaskVO_totalScore', 'totalScore');
CALL CREATE_INDEX('ModelEvaluationTaskVO', 'idx_ModelEvaluationTaskVO_endTime', 'endTime');

-- Backfill totalScore from opaque JSON for existing completed tasks
UPDATE `zstack`.`ModelEvaluationTaskVO`
SET `totalScore` = JSON_EXTRACT(`opaque`, '$.details.total_score')
WHERE `opaque` IS NOT NULL
  AND `totalScore` IS NULL
  AND JSON_EXTRACT(`opaque`, '$.details.total_score') IS NOT NULL;

-- Backfill endTime from opaque JSON for existing completed/failed tasks
-- end_time format from Python agent: "MMM dd, yyyy hh:mm:ss a" (e.g. "Jan 01, 2025 10:30:00 AM")
UPDATE `zstack`.`ModelEvaluationTaskVO`
SET `endTime` = STR_TO_DATE(
    JSON_UNQUOTE(JSON_EXTRACT(`opaque`, '$.details.end_time')),
    '%b %d, %Y %h:%i:%s %p'
)
WHERE `opaque` IS NOT NULL
  AND `endTime` IS NULL
  AND JSON_EXTRACT(`opaque`, '$.details.end_time') IS NOT NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(`opaque`, '$.details.end_time')) != '';

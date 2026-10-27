CALL ADD_COLUMN('MemoryStateVO', 'control', 'VARCHAR(16)', 0, 'active');
CALL ADD_COLUMN('MemoryStateVO', 'desired', 'TEXT', 1, NULL);
CALL ADD_COLUMN('MemoryStateVO', 'failures', 'INT', 0, '0');
CALL ADD_COLUMN('MemoryStateVO', 'nextAttemptAt', 'BIGINT', 1, NULL);

-- Keep a confirmed pause or drain as the Host's control. The desired
-- configuration is computed again when the Host connects.
UPDATE `zstack`.`MemoryStateVO` s JOIN `zstack`.`MemoryTaskVO` t ON t.uuid = s.controlOperationUuid
    SET s.control = IF(t.action = 'drain', 'drained', 'paused')
    WHERE t.status = 'Succeeded' AND t.action IN ('pause', 'drain');
UPDATE `zstack`.`MemoryStateVO` SET `status` = 'Converging', `reason` = NULL, `appliedRevision` = NULL;
UPDATE `zstack`.`HostMemoryStateVO` SET `status` = 'Converging' WHERE `status` NOT IN ('Applied', 'Failed');

CALL DROP_COLUMN('MemoryStateVO', 'controlOperationUuid');
CALL DROP_COLUMN('MemoryStateVO', 'activeTaskUuid');
CALL DROP_COLUMN('MemoryStateVO', 'appliedPolicyHash');
CALL DROP_COLUMN('MemoryStateVO', 'appliedPolicy');
CALL DROP_COLUMN('MemoryStateVO', 'policyTargetHash');
CALL DROP_COLUMN('MemoryStateVO', 'policyPlanHash');
CALL DROP_COLUMN('MemoryStateVO', 'policyPlanStatus');
CALL DROP_COLUMN('MemoryStateVO', 'policyBlockedFields');
CALL DROP_COLUMN('HostMemoryStateVO', 'uncertainRecovery');

-- Operation records now belong to one Host generation; the old ones cannot be finished.
DELETE FROM `zstack`.`MemoryTaskVO`;
CALL DELETE_INDEX('MemoryTaskVO', 'memory_task_parent');
CALL DELETE_INDEX('MemoryTaskVO', 'memory_task_status_created');
CALL DELETE_INDEX('MemoryTaskVO', 'memory_task_created_uuid');
CALL DROP_COLUMN('MemoryTaskVO', 'parentUuid');
CALL DROP_COLUMN('MemoryTaskVO', 'scope');
CALL DROP_COLUMN('MemoryTaskVO', 'resourceUuid');
CALL DROP_COLUMN('MemoryTaskVO', 'reconcileOperationUuid');
CALL DROP_COLUMN('MemoryTaskVO', 'policy');
CALL DROP_COLUMN('MemoryTaskVO', 'expectedControlOperationUuid');
CALL DROP_COLUMN('MemoryTaskVO', 'policyPlanHash');
CALL DROP_COLUMN('MemoryTaskVO', 'requiresKsmNativePartialFence');
CALL DROP_COLUMN('MemoryTaskVO', 'ksmNativeObservationBootId');
CALL DROP_COLUMN('MemoryTaskVO', 'ksmNativeObservationSampleTime');

DROP TABLE IF EXISTS `zstack`.`MemoryPolicyVO`;

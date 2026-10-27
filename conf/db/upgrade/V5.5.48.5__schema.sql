DROP TABLE IF EXISTS `zstack`.`MemoryTaskFailureOutboxVO`;
DROP TABLE IF EXISTS `zstack`.`MemoryTaskFailureReceiptVO`;

CALL DELETE_INDEX('MemoryTaskVO', 'memory_task_request');
CALL DROP_COLUMN('MemoryTaskVO', 'requestKey');
CALL DROP_COLUMN('MemoryTaskVO', 'requestHash');
CALL DROP_COLUMN('MemoryTaskVO', 'expectedInstanceGeneration');
CALL DROP_COLUMN('MemoryTaskVO', 'issuedPermitOperationUuid');
CALL DROP_COLUMN('MemoryTaskVO', 'issuedPermitDeadline');
CALL DROP_COLUMN('MemoryTaskVO', 'ownerManagementNodeUuid');
CALL DROP_COLUMN('MemoryStateVO', 'permitAuthorized');
CALL DROP_COLUMN('MemoryStateVO', 'permitDeadline');
CALL DROP_COLUMN('MemoryPolicyVO', 'revision');
CALL DROP_COLUMN('MemoryVmExclusionVO', 'sourceRevisions');
CALL DROP_COLUMN('MemoryVmExclusionVO', 'vmPolicyRevision');

DELETE FROM `zstack`.`GlobalConfigVO` WHERE `name`='dispatch.concurrency' and `category`='memoryOptimization';

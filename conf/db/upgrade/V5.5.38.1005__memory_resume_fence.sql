-- Persist the control-operation fencing token required for safe resume.
ALTER TABLE zstack.MemoryTaskVO ADD COLUMN expectedControlOperationUuid VARCHAR(32) DEFAULT NULL;

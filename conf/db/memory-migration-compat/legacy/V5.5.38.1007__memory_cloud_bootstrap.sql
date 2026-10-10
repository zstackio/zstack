-- Additive marker for an explicitly identified first Cloud installation.
-- Deliberately do not INSERT a row here: ordinary upgrades must remain inert.
CREATE TABLE IF NOT EXISTS zstack.MemoryCloudBootstrapVO (
    uuid VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requestUuid VARCHAR(32) NOT NULL,
    taskUuid VARCHAR(32) DEFAULT NULL,
    targetPolicyHash VARCHAR(64) DEFAULT NULL,
    reason VARCHAR(2048) DEFAULT NULL,
    createDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastOpDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid),
    UNIQUE KEY memory_cloud_bootstrap_request (requestUuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE zstack.MemoryStateVO ADD COLUMN appliedPolicyHash VARCHAR(64) DEFAULT NULL;
ALTER TABLE zstack.MemoryStateVO ADD COLUMN appliedPolicy MEDIUMTEXT DEFAULT NULL;
ALTER TABLE zstack.MemoryStateVO ADD COLUMN policyTargetHash VARCHAR(64) DEFAULT NULL;
ALTER TABLE zstack.MemoryStateVO ADD COLUMN policyPlanHash VARCHAR(64) DEFAULT NULL;
ALTER TABLE zstack.MemoryStateVO ADD COLUMN policyPlanStatus VARCHAR(16) DEFAULT NULL;
ALTER TABLE zstack.MemoryStateVO ADD COLUMN policyBlockedFields TEXT DEFAULT NULL;
ALTER TABLE zstack.MemoryTaskVO ADD COLUMN policyPlanHash VARCHAR(64) DEFAULT NULL;

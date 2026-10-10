-- Additive test-branch schema: does not alter previously shipped migration checksums.
-- Tasks and desired state survive MN restart. No Host resource is modified by this migration.
CREATE TABLE IF NOT EXISTS zstack.MemoryPolicyVO (
    uuid VARCHAR(80) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    resourceUuid VARCHAR(64) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    policy TEXT NOT NULL,
    createDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastOpDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid),
    UNIQUE KEY memory_policy_scope_resource (scope, resourceUuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS zstack.MemoryTaskVO (
    uuid VARCHAR(32) NOT NULL,
    parentUuid VARCHAR(32) DEFAULT NULL,
    hostUuid VARCHAR(32) DEFAULT NULL,
    scope VARCHAR(16) NOT NULL,
    resourceUuid VARCHAR(64) NOT NULL,
    actorUuid VARCHAR(80) NOT NULL,
    requestKey VARCHAR(160) DEFAULT NULL,
    requestHash VARCHAR(64) DEFAULT NULL,
    reconcileOperationUuid VARCHAR(32) DEFAULT NULL,
    expectedInstanceGeneration VARCHAR(128) DEFAULT NULL,
    issuedPermitOperationUuid VARCHAR(32) DEFAULT NULL,
    issuedPermitDeadline BIGINT DEFAULT NULL,
    action VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(2048) DEFAULT NULL,
    desiredRevision BIGINT NOT NULL,
    policy TEXT NOT NULL,
    ownerManagementNodeUuid VARCHAR(32) DEFAULT NULL,
    createDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastOpDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid),
    UNIQUE KEY memory_task_request (requestKey),
    KEY memory_task_parent (parentUuid),
    KEY memory_task_host_status (hostUuid, status),
    KEY memory_task_status_created (status, createDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS zstack.MemoryStateVO (
    hostUuid VARCHAR(32) NOT NULL,
    permitAuthorized TINYINT(1) NOT NULL DEFAULT 0,
    permitDeadline BIGINT DEFAULT NULL,
    controlOperationUuid VARCHAR(32) DEFAULT NULL,
    desiredRevision BIGINT NOT NULL DEFAULT 0,
    appliedRevision BIGINT DEFAULT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(2048) DEFAULT NULL,
    capabilities TEXT DEFAULT NULL,
    state MEDIUMTEXT DEFAULT NULL,
    lastSampleTime BIGINT DEFAULT NULL,
    activeTaskUuid VARCHAR(32) DEFAULT NULL,
    createDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastOpDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (hostUuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO zstack.MemoryPolicyVO
    (uuid, scope, resourceUuid, revision, policy)
    VALUES ('Global:global', 'Global', 'global', 0, '{"schemaVersion":1}');

CREATE TABLE IF NOT EXISTS zstack.MemoryMigrationVO (
    vmUuid VARCHAR(32) NOT NULL,
    operationUuid VARCHAR(32) NOT NULL,
    sourceHostUuid VARCHAR(32) NOT NULL,
    targetHostUuid VARCHAR(32) NOT NULL,
    poolGeneration VARCHAR(128) DEFAULT NULL,
    sourceInstanceGeneration VARCHAR(128) DEFAULT NULL,
    targetInstanceGeneration VARCHAR(128) DEFAULT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(2048) DEFAULT NULL,
    PRIMARY KEY (vmUuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

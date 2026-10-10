-- Durable low-frequency VM target-list staging. It is never used for
-- per-reclaim records and expires before it can become an active policy.
CREATE TABLE IF NOT EXISTS zstack.MemoryTargetShardVO (
    uuid VARCHAR(32) NOT NULL PRIMARY KEY,
    requestKey VARCHAR(160) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    resourceUuid VARCHAR(64) NOT NULL,
    shardIndex INT NOT NULL,
    shardCount INT NOT NULL,
    totalCount BIGINT NOT NULL,
    digest VARCHAR(128) NOT NULL,
    snapshotGeneration VARCHAR(128) NOT NULL,
    expectedRevision BIGINT NOT NULL,
    targets MEDIUMTEXT NOT NULL,
    requestHash VARCHAR(128) NOT NULL,
    expiresAt BIGINT NOT NULL,
    UNIQUE KEY memory_target_shard_request_index (requestKey, shardIndex),
    KEY memory_target_shard_expiry (expiresAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

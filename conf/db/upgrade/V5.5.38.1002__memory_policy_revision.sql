-- Incremental candidate upgrade; keep V5.5.38.1 checksums unchanged.
-- Raw candidate policy is retained once when the application migrates the
-- removed KSM fields. This migration never enables a Host or changes revision.
ALTER TABLE zstack.MemoryPolicyVO ADD COLUMN legacyPolicy MEDIUMTEXT DEFAULT NULL;
ALTER TABLE zstack.MemoryPolicyVO MODIFY COLUMN policy MEDIUMTEXT NOT NULL;
ALTER TABLE zstack.MemoryTaskVO MODIFY COLUMN policy MEDIUMTEXT NOT NULL;
CREATE TABLE IF NOT EXISTS zstack.MemoryVmExclusionVO (
    vmUuid VARCHAR(32) NOT NULL PRIMARY KEY,
    sourceHostUuid VARCHAR(32) NOT NULL,
    targetHostUuid VARCHAR(32) NOT NULL,
    sourceRevisions TEXT NOT NULL,
    sourceInstanceGeneration VARCHAR(128) DEFAULT NULL,
    vmPolicyRevision BIGINT NOT NULL,
    retained BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

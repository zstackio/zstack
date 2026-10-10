-- Keep low-frequency configuration commit receipts after target staging expires.
ALTER TABLE zstack.MemoryTaskVO ADD COLUMN targetSnapshotHash VARCHAR(64) DEFAULT NULL;
-- A VM list can legitimately exceed TEXT's 64 KiB after atomic shard assembly.
-- Admission uses explicit serialization budgets, not a hidden database VM cap.
ALTER TABLE zstack.MemoryTaskVO MODIFY COLUMN policy LONGTEXT NOT NULL;
ALTER TABLE zstack.MemoryPolicyVO MODIFY COLUMN policy LONGTEXT NOT NULL;
ALTER TABLE zstack.MemoryPolicyVO MODIFY COLUMN legacyPolicy LONGTEXT DEFAULT NULL;

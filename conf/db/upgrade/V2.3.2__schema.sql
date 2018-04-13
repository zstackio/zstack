ALTER TABLE CephPrimaryStoragePoolVO ADD availableCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephPrimaryStoragePoolVO ADD usedCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephPrimaryStoragePoolVO ADD replicatedSize int unsigned;

ALTER TABLE CephBackupStorageVO ADD poolAvailableCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephBackupStorageVO ADD poolUsedCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephBackupStorageVO ADD poolReplicatedSize int unsigned;
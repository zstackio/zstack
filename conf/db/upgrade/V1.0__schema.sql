ALTER TABLE HostCapacityVO ADD totalPhysicalMemory bigint unsigned NOT NULL DEFAULT 0;
ALTER TABLE HostCapacityVO ADD availablePhysicalMemory bigint unsigned NOT NULL DEFAULT 0;
ALTER TABLE HostCapacityVO MODIFY availableMemory bigint signed NOT NULL DEFAULT 0;

ALTER TABLE PrimaryStorageCapacityVO MODIFY availableCapacity bigint signed NOT NULL DEFAULT 0;
ALTER TABLE PrimaryStorageCapacityVO ADD systemUsedCapacity bigint signed DEFAULT NULL;

ALTER TABLE LocalStorageHostRefVO MODIFY availableCapacity bigint signed NOT NULL DEFAULT 0;
ALTER TABLE LocalStorageHostRefVO ADD systemUsedCapacity bigint signed NOT NULL DEFAULT 0;

ALTER TABLE ImageBackupStorageRefVO ADD status varchar(32) NOT NULL;
UPDATE ImageBackupStorageRefVO SET status = 'Ready';

ALTER TABLE VmNicVO MODIFY ip varchar(128) DEFAULT NULL;

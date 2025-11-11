-- ZSTAC-86476: Add normalizedModelName column for GPU spec dedup
CALL ADD_COLUMN('GpuDeviceSpecVO', 'normalizedModelName', 'VARCHAR(255)', 1, NULL);
CALL CREATE_INDEX('GpuDeviceSpecVO', 'idx_gpu_spec_normalized_model', 'normalizedModelName');

UPDATE VolumeSnapshotVO AS sp, PrimaryStorageVO AS ps
SET sp.primaryStorageInstallPath = REPLACE(sp.primaryStorageInstallPath, '/dev/', 'sharedblock://')
WHERE sp.primaryStorageUuid = ps.uuid AND ps.type = 'SharedBlock' AND sp.volumeType = 'Memory' AND sp.primaryStorageInstallPath LIKE '/dev/%';

ALTER TABLE VCenterPrimaryStorageVO ADD COLUMN datastore varchar(64) DEFAULT NULL;
ALTER TABLE VCenterBackupStorageVO  ADD COLUMN datastore varchar(64) DEFAULT NULL;

ALTER TABLE VCenterPrimaryStorageVO ADD COLUMN datastore varchar(64) DEFAULT NULL;
ALTER TABLE VCenterBackupStorageVO  ADD COLUMN datastore varchar(64) DEFAULT NULL;

INSERT INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "IPsecConnectionVO" FROM IPsecConnectionVO t;
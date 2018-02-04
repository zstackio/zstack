UPDATE GarbageCollectorVO SET managementNodeUuid = NULL WHERE managementNodeUuid NOT IN (SELECT uuid FROM ManagementNodeVO);
ALTER TABLE GarbageCollectorVO ADD CONSTRAINT fkGarbageCollectorVOManagementNodeVO FOREIGN KEY (managementNodeUuid) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

UPDATE SystemTagVO SET tag = CONCAT(tag, '::0') WHERE resourceType = "VmInstanceVO" AND type = "System" AND tag LIKE "iso::%" AND tag NOT LIKE "iso::%::%";

ALTER TABLE IdentityZoneVO CHANGE deleted closed varchar(1) DEFAULT NULL;

DELETE FROM `zstack`.`AccountResourceRefVO` where resourceType='VolumeVO' and resourceUuid not in
(SELECT uuid FROM `zstack`.`VolumeVO`)

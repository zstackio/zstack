ALTER TABLE IdentityZoneVO CHANGE deleted closed varchar(1) DEFAULT NULL;

DELETE FROM `zstack`.`AccountResourceRefVO` where resourceType='VolumeVO' and resourceUuid not in (SELECT uuid FROM `zstack`.`VolumeVO`);
ALTER TABLE HybridAccountVO ADD COLUMN aliyunAccountId varchar(32) DEFAULT NULL;
ALTER TABLE HybridAccountVO ADD COLUMN aliyunUserId varchar(32) DEFAULT NULL;

ALTER TABLE EcsInstanceVO ADD COLUMN expireDate datetime DEFAULT '2999-01-01 00:00:00';
ALTER TABLE EcsInstanceVO ADD COLUMN chargeType varchar(16) DEFAULT 'postpaid';
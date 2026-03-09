ALTER TABLE IdentityZoneVO CHANGE deleted closed varchar(1) DEFAULT NULL;

DELETE FROM `zstack`.`AccountResourceRefVO` where resourceType='VolumeVO' and resourceUuid not in (SELECT uuid FROM `zstack`.`VolumeVO`);
ALTER TABLE HybridAccountVO ADD COLUMN hybridAccountId varchar(32) DEFAULT NULL;
ALTER TABLE HybridAccountVO ADD COLUMN hybridUserId varchar(32) DEFAULT NULL;
ALTER TABLE HybridAccountVO ADD COLUMN hybridUserName varchar(64) DEFAULT NULL;

ALTER TABLE EcsInstanceVO ADD COLUMN expireDate datetime DEFAULT '2999-01-01 00:00:00';
ALTER TABLE EcsInstanceVO ADD COLUMN chargeType varchar(16) DEFAULT 'postpaid';

ALTER TABLE VirtualBorderRouterVO MODIFY COLUMN description varchar(1024) DEFAULT NULL;

ALTER TABLE EmailTriggerActionVO MODIFY COLUMN mediaUuid varchar(32);
UPDATE EmailTriggerActionVO SET mediaUuid = NULL WHERE mediaUuid NOT IN (SELECT uuid FROM EmailMediaVO);
ALTER TABLE `zstack`.`EmailTriggerActionVO` ADD CONSTRAINT `fkEmailTriggerActionVOEmailMediaVO` FOREIGN KEY (`mediaUuid`) REFERENCES `EmailMediaVO` (`uuid`) ON DELETE SET NULL;



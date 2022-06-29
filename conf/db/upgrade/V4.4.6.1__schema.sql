
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `ikeVersion` varchar(16) NOT NULL DEFAULT 'ikev1';
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `idType` varchar(16) DEFAULT NULL;
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `remoteId` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `localId` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `ikeLifeTime` int(10) DEFAULT 0;
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `lifeTime` int(10) DEFAULT 0;

ALTER TABLE `zstack`.`VirtualRouterMetadataVO` ADD COLUMN `ipsecCurrentVersion` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`VirtualRouterMetadataVO` ADD COLUMN `ipsecLatestVersion` varchar(32) DEFAULT NULL;
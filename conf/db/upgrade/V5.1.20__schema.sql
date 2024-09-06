ALTER TABLE `zstack`.`ModelCenterVO` ADD COLUMN containerStorageNetwork varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`ModelServiceInstanceVO` ADD COLUMN internalUrl varchar(2048) DEFAULT NULL;

ALTER TABLE `InstanceOfferingEO` ADD COLUMN `imageUuid` CHAR(32) DEFAULT NULL;
ALTER TABLE `InstanceOfferingEO` ADD COLUMN `diskSize` bigint unsigned DEFAULT NULL COMMENT 'root disk size in bytes';
ALTER TABLE `InstanceOfferingEO` ADD CONSTRAINT fkInstanceOfferingImageEO FOREIGN KEY (`imageUuid`) REFERENCES ImageEO (`uuid`) ON DELETE CASCADE;

DROP VIEW IF EXISTS `zstack`.`InstanceOfferingVO`;
CREATE VIEW `zstack`.`InstanceOfferingVO` AS SELECT uuid, name, description, cpuNum, cpuSpeed, memorySize, allocatorStrategy, sortKey, state, createDate, lastOpDate, type, duration, diskSize, imageUuid FROM `zstack`.`InstanceOfferingEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`VirtualRouterOfferingVO`DROP FOREIGN KEY `fkVirtualRouterOfferingVOImageEO`;
ALTER TABLE `zstack`.`VirtualRouterOfferingVO`DROP COLUMN `imageUuid`, DROP INDEX `fkVirtualRouterOfferingVOImageEO` ;

ALTER TABLE VpcVpnConnectionVO ADD COLUMN status varchar(32) NOT NULL DEFAULT 'IPSEC_SUCCESS';

CREATE TABLE `AliyunDiskVO` (
	  `uuid` VARCHAR(32) UNIQUE NOT NULL,
	  `diskId` VARCHAR(32) NOT NULL,
	  `name` VARCHAR(128)  NOT NULL,
	  `description` VARCHAR(1024) DEFAULT NULL,
	  `identityZoneUuid` VARCHAR(32) NOT NULL,
	  `ecsInstanceUuid` VARCHAR(32) DEFAULT NULL,
	  `diskType` VARCHAR(16) NOT NULL,
	  `diskCategory` VARCHAR(16)  DEFAULT NULL,
	  `diskChargeType` VARCHAR(16)  DEFAULT NULL,
 	  `status` VARCHAR(16)  DEFAULT NULL,
 	  `sizeWithGB` INTEGER UNSIGNED  DEFAULT NULL,
 	  `deviceInfo` VARCHAR(32) DEFAULT NULL,
	  `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ,
	  `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
      KEY `fkAliyunDiskVOIdentityZoneVO` (`identityZoneUuid`),
      CONSTRAINT fkAliyunDiskVOIdentityZoneVO FOREIGN KEY (identityZoneUuid) REFERENCES IdentityZoneVO (uuid)  ON DELETE RESTRICT,
      KEY `fkAliyunDiskVOEcsInstanceVO` (`ecsInstanceUuid`),
      CONSTRAINT fkAliyunDiskVOEcsInstanceVO FOREIGN KEY (ecsInstanceUuid) REFERENCES EcsInstanceVO (uuid)  ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunSnapshotVO` (
	  `uuid` VARCHAR(32) UNIQUE NOT NULL,
	  `snapshotId` VARCHAR(32) UNIQUE NOT NULL,
	  `name` VARCHAR(128)  NOT NULL,
	  `description` VARCHAR(1024) DEFAULT NULL,
	  `dataCenterUuid` VARCHAR(32) NOT NULL,
	  `diskUuid` VARCHAR(32) DEFAULT NULL,
	  `status` VARCHAR(16) DEFAULT NULL,
	  `aliyunSnapshotUsage` VARCHAR(16) NOT NULL,
	  `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ,
	  `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  KEY `fkAliyunSnapshotVOAliyunDiskVO` (`diskUuid`),
      CONSTRAINT `fkAliyunSnapshotVOAliyunDiskVO` FOREIGN KEY (diskUuid) REFERENCES AliyunDiskVO (uuid)  ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `EcsImageUsageVO` (
    `id` INT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `ecsImageUuid` VARCHAR(32) NOT NULL,
    `snapshotUuidOfCreatedImage` VARCHAR(32) NOT NULL,
	`createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ,
	`lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `CephPrimaryStoragePoolVO` ADD COLUMN `type` varchar(32) NOT NULL DEFAULT 'Data';
ALTER TABLE `CephPrimaryStoragePoolVO` ADD COLUMN `aliasName` varchar(255);

DELIMITER $$

DROP FUNCTION IF EXISTS `Separate_ceph_pool` $$

CREATE FUNCTION `Separate_ceph_pool` (
    uuid varchar(32),
    rootVolumePoolName varchar(255),
    dataVolumePoolName varchar(255),
    imageCachePoolName varchar(255)
) RETURNS VARCHAR(7) CHARSET utf8

BEGIN
    DECLARE root_volume_pool_uuid, data_volume_pool_uuid, image_cache_pool_uuid, root_volume_pool_tag_uuid, data_volume_pool_tag_uuid, image_cache_pool_tag_uuid varchar(32);
    DECLARE result_string varchar(7);
    SET root_volume_pool_uuid = REPLACE(UUID(),'-','');
    SET data_volume_pool_uuid = REPLACE(UUID(),'-','');
    SET image_cache_pool_uuid = REPLACE(UUID(),'-','');
    SET root_volume_pool_tag_uuid = REPLACE(UUID(),'-','');
    SET data_volume_pool_tag_uuid = REPLACE(UUID(),'-','');
    SET image_cache_pool_tag_uuid = REPLACE(UUID(),'-','');
    SET result_string = 'success';

    INSERT INTO CephPrimaryStoragePoolVO (`uuid`, `primaryStorageUuid`, `poolName`, `aliasName`, `description`, `type`, `createDate`, `lastOpDate`) VALUES (root_volume_pool_uuid, uuid, rootVolumePoolName, NULL, NULL, 'Root', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`) VALUES (root_volume_pool_uuid, rootVolumePoolName, 'CephPrimaryStoragePoolVO');
    INSERT INTO SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`) VALUES (root_volume_pool_tag_uuid, uuid, 'PrimaryStorageVO', 1, 'System', CONCAT('ceph::default::rootVolumePoolName::', rootVolumePoolName), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

    INSERT INTO CephPrimaryStoragePoolVO (`uuid`, `primaryStorageUuid`, `poolName`, `aliasName`, `description`, `type`, `createDate`, `lastOpDate`) VALUES (data_volume_pool_uuid, uuid, dataVolumePoolName, NULL, NULL, 'Data', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`) VALUES (data_volume_pool_uuid, dataVolumePoolName, 'CephPrimaryStoragePoolVO');
    INSERT INTO SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`) VALUES (data_volume_pool_tag_uuid, uuid, 'PrimaryStorageVO', 1, 'System', CONCAT('ceph::default::dataVolumePoolName::', dataVolumePoolName), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

    INSERT INTO CephPrimaryStoragePoolVO (`uuid`, `primaryStorageUuid`, `poolName`, `aliasName`, `description`, `type`, `createDate`, `lastOpDate`) VALUES (image_cache_pool_uuid, uuid, imageCachePoolName, NULL, NULL, 'ImageCache', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`) VALUES (image_cache_pool_uuid, imageCachePoolName, 'CephPrimaryStoragePoolVO');
    INSERT INTO SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`) VALUES (image_cache_pool_tag_uuid, uuid, 'PrimaryStorageVO', 1, 'System', CONCAT('ceph::default::imageCachePoolName::', imageCachePoolName), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

    RETURN(result_string);
END$$

DELIMITER  ;

select Separate_ceph_pool(uuid, rootVolumePoolName, dataVolumePoolName, imageCachePoolName) from CephPrimaryStorageVO;
ALTER TABLE `CephPrimaryStorageVO` DROP COLUMN `rootVolumePoolName`;
ALTER TABLE `CephPrimaryStorageVO` DROP COLUMN `dataVolumePoolName`;
ALTER TABLE `CephPrimaryStorageVO` DROP COLUMN `imageCachePoolName`;

DROP FUNCTION IF EXISTS `Separate_ceph_pool`;

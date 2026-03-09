ALTER TABLE ImageCacheVO DROP FOREIGN KEY fkImageCacheVOImageEO;
ALTER TABLE VolumeEO DROP FOREIGN KEY fkVolumeEOImageEO;

CREATE TABLE  `zstack`.`ImageCacheShadowVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `imageUuid` varchar(32) DEFAULT NULL,
    `installUrl` varchar(1024) NOT NULL,
    `mediaType` varchar(64) NOT NULL,
    `size` bigint unsigned NOT NULL,
    `md5sum` varchar(255) NOT NULL,
    `state` varchar(255) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ImageCacheVO ADD CONSTRAINT fkImageCacheShadowVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

-- ----------------------------
--  for external baremetal pxe server
-- ----------------------------
DELETE FROM `zstack`.`BaremetalPxeServerVO`;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `zoneUuid` varchar(32) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `hostname` varchar(255) NOT NULL UNIQUE;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `sshUsername` varchar(64) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `sshPassword` varchar(255) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `sshPort` int unsigned NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `storagePath` varchar(2048) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `totalCapacity` bigint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `availableCapacity` bigint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `dhcpInterfaceAddress` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` ADD COLUMN `state` varchar(32) NOT NULL;
ALTER TABLE `zstack`.`BaremetalPxeServerVO` DROP INDEX `dhcpInterface`;

DELETE FROM `zstack`.`BaremetalImageCacheVO`;
ALTER TABLE `zstack`.`BaremetalImageCacheVO` ADD COLUMN `pxeServerUuid` varchar(32) NOT NULL;
ALTER TABLE `zstack`.`BaremetalImageCacheVO` ADD CONSTRAINT fkBaremetalImageCacheVOBaremetalPxeServerVO FOREIGN KEY (pxeServerUuid) REFERENCES BaremetalPxeServerVO (uuid) ON DELETE CASCADE;

ALTER TABLE `zstack`.`BaremetalChassisVO` ADD COLUMN `pxeServerUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`BaremetalChassisVO` ADD CONSTRAINT fkBaremetalChassisVOBaremetalPxeServerVO FOREIGN KEY (pxeServerUuid) REFERENCES BaremetalPxeServerVO (uuid) ON DELETE SET NULL;
ALTER TABLE `zstack`.`BaremetalInstanceVO` ADD COLUMN `pxeServerUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`BaremetalInstanceVO` ADD CONSTRAINT fkBaremetalInstanceVOBaremetalPxeServerVO FOREIGN KEY (pxeServerUuid) REFERENCES BaremetalPxeServerVO (uuid) ON DELETE SET NULL;

CREATE TABLE  `zstack`.`BaremetalPxeServerClusterRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `clusterUuid` varchar(32) NOT NULL,
    `pxeServerUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT fkBaremetalPxeServerClusterRefVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkBaremetalPxeServerClusterRefVOBaremetalPxeServerVO FOREIGN KEY (pxeServerUuid) REFERENCES BaremetalPxeServerVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

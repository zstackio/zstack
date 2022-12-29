ALTER TABLE `zstack`.`VmNicVO` ADD COLUMN `state` varchar(255) NOT NULL DEFAULT "enable";
-- -----------------------------------
--  BEGIN OF MTTY DEVICE VIRTUALIZATION
-- -----------------------------------
CREATE TABLE IF NOT EXISTS  `zstack`.`MttyDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` VARCHAR(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL,
    `virtStatus` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkDeviceVOHostEO FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`MdevDeviceVO` MODIFY `parentUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`MdevDeviceVO` ADD COLUMN `mttyUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`MdevDeviceVO` ADD CONSTRAINT `fkMdevDeviceVOMttyDeviceVO` FOREIGN KEY (`mttyUuid`) REFERENCES `MttyDeviceVO` (`uuid`) ON DELETE CASCADE;
-- ---------------------------------
--  END OF MTTY DEVICE VIRTUALIZATION
-- ---------------------------------

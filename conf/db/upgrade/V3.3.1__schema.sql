CREATE TABLE `zstack`.`PciDeviceSpecVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `vendorId` varchar(64) NOT NULL,
    `deviceId` varchar(64) NOT NULL,
    `subvendorId` varchar(64) DEFAULT NULL,
    `subdeviceId` varchar(64) DEFAULT NULL,
    `romContent` MEDIUMTEXT DEFAULT NULL,
    `romVersion` varchar(255) DEFAULT NULL,
    `romMd5sum` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
     PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE AsyncRestVO MODIFY COLUMN `requestData` LONGTEXT DEFAULT NULL;

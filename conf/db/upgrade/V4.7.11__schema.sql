ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `vendorId` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `deviceId` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `subvendorId` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `subdeviceId` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `maxPartNum` INT DEFAULT NULL;

CREATE TABLE `HostVirtualNetworkInterfaceVO`
(
    `uuid`                     varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `description`              varchar(2048)        DEFAULT NULL,
    `hostUuid`                 varchar(32) NOT NULL,
    `vmInstanceUuid`           varchar(32)          DEFAULT NULL,
    `hostNetworkInterfaceUuid` varchar(32)          DEFAULT NULL,
    `status`                   varchar(32) NOT NULL,
    `pciDeviceAddress`         varchar(32) NOT NULL,
    `virtStatus`               VARCHAR(32)          DEFAULT NULL,
    `vendorId`                 varchar(64) NOT NULL,
    `deviceId`                 varchar(64) NOT NULL,
    `subvendorId`              varchar(64)          DEFAULT NULL,
    `subdeviceId`              varchar(64)          DEFAULT NULL,
    `metadata`                 varchar(4096)        DEFAULT NULL,
    `lastOpDate`               timestamp   NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate`               timestamp   NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkHostVirtualNetworkInterfaceVOHostEO FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
    CONSTRAINT fkHostVnicHostNetworkInterfaceVO FOREIGN KEY (`hostNetworkInterfaceUuid`) REFERENCES `zstack`.`HostNetworkInterfaceVO` (`uuid`) ON DELETECASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE VmVdpaNicVO DROP FOREIGN KEY fkVmVdpaNicVOPciDeviceVO;
ALTER TABLE VmVdpaNicVO DROP COLUMN `pciDeviceUuid`;
ALTER TABLE VmVdpaNicVO DROP COLUMN `lastPciDeviceUuid`;
ALTER TABLE VmVdpaNicVO
    ADD COLUMN `virtualPhysicalNicUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE VmVdpaNicVO
    ADD COLUMN `lastVirtualPhysicalNicUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`VmVdpaNicVO`
    ADD CONSTRAINT fkVmVdpaNicVOHostVirtualNetworkInterfaceVO FOREIGN KEY (`virtualPhysicalNicUuid`) REFERENCES `zstack`.`HostVirtualNetworkInterfaceVO` (`uuid`) ON DELETE SET NULL;

ALTER TABLE VmVfNicVO DROP FOREIGN KEY fkVmVfNicVOPciDeviceVO;
ALTER TABLE VmVfNicVO DROP COLUMN `pciDeviceUuid`;
ALTER TABLE VmVfNicVO
    ADD COLUMN `virtualPhysicalNicUuid` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`VmVfNicVO`
    ADD CONSTRAINT fkVmVfNicVOHostVirtualNetworkInterfaceVO FOREIGN KEY (`virtualPhysicalNicUuid`) REFERENCES `zstack`.`HostVirtualNetworkInterfaceVO` (`uuid`) ON DELETE SET NULL;

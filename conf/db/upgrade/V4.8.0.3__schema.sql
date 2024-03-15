-- in version zsv_4.2.0
-- Feature: USB device support sharing | ZSV-4726
delete from ResourceVO where resourceType = 'UsbDeviceVO' and uuid not in (select uuid from UsbDeviceVO);

insert into AccountResourceRefVO (`accountUuid`,`ownerAccountUuid`,`resourceUuid`,`resourceType`,`permission`,`isShared`,`lastOpDate`,`createDate`,`concreteResourceType`)
select '36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', uuid, resourceType, 2, false, NOW(), NOW(), 'org.zstack.usbDevice.UsbDeviceVO'
    from ResourceVO where resourceType = 'UsbDeviceVO';

-- Feature: support OVF uploading breakpoint continuation | ZSV-4467
alter table `zstack`.`LongJobVO` modify `uuid` char(32) not null;
alter table `zstack`.`LongJobVO` add column `parentUuid` char(32) default null;

-- Feature: support SR-IOV | ZSV-5082
CREATE TABLE IF NOT EXISTS `zstack`.`EthernetVfPciDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostDevUuid` varchar(32) DEFAULT NULL,
    `interfaceName` varchar(32) DEFAULT NULL,
    `vmUuid` varchar(32) DEFAULT NULL,
    `l3NetworkUuid` varchar(32) DEFAULT NULL,
    `vfStatus` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkEthernetVfPciDeviceVOVmInstanceEO` FOREIGN KEY (`vmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkEthernetVfPciDeviceVOHostEO` FOREIGN KEY (`hostDevUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkEthernetVfPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkEthernetVfPciDeviceVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

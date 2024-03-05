-- in version zsv_4.2.0
-- Feature: USB device support sharing | ZSV-4726
delete from ResourceVO where resourceType = 'UsbDeviceVO' and uuid not in (select uuid from UsbDeviceVO);

insert into AccountResourceRefVO (`accountUuid`,`ownerAccountUuid`,`resourceUuid`,`resourceType`,`permission`,`isShared`,`lastOpDate`,`createDate`,`concreteResourceType`)
select '36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', uuid, resourceType, 2, false, NOW(), NOW(), 'org.zstack.usbDevice.UsbDeviceVO'
    from ResourceVO where resourceType = 'UsbDeviceVO';

-- Feature: support OVF uploading breakpoint continuation | ZSV-4467
alter table `zstack`.`LongJobVO` modify `uuid` char(32) not null;
alter table `zstack`.`LongJobVO` add column `parentUuid` char(32) default null;

-- Feature: port group improvement | ZSV-4933
CREATE TABLE IF NOT EXISTS `zstack`.`PortGroupVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vSwitchUuid` varchar(32) NOT NULL,
    `vlanMode` varchar(32) NOT NULL default 'ACCESS',
    `vlanId` int unsigned NOT NULL,
    `vlanRanges` varchar(256) default NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkPortGroupVOL2VirtualSwitchNetworkVO` FOREIGN KEY (`vSwitchUuid`) REFERENCES L2VirtualSwitchNetworkVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

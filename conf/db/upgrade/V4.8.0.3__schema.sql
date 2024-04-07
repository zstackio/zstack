-- in version zsv_4.2.0
-- Feature: USB device support sharing | ZSV-4726
delete from ResourceVO where resourceType = 'UsbDeviceVO' and uuid not in (select uuid from UsbDeviceVO);

insert into AccountResourceRefVO (`accountUuid`,`ownerAccountUuid`,`resourceUuid`,`resourceType`,`permission`,`isShared`,`lastOpDate`,`createDate`,`concreteResourceType`)
select '36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', uuid, resourceType, 2, false, NOW(), NOW(), 'org.zstack.usbDevice.UsbDeviceVO'
    from ResourceVO where resourceType = 'UsbDeviceVO';

-- Feature: support OVF uploading breakpoint continuation | ZSV-4467
alter table `zstack`.`LongJobVO` modify `uuid` char(32) not null;
alter table `zstack`.`LongJobVO` add column `parentUuid` char(32) default null;

-- Feature: support vm and vm template conversion
CREATE TABLE `zstack`.`VmTemplateVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `vmInstanceUuid` char(32) NOT NULL,
    `zoneUuid` char(32) NOT NULL,
    `originalType` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVmTemplateVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES VmInstanceEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmTemplateVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES ZoneEO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VolumeTemplateVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `volumeUuid` char(32) NOT NULL,
    `originalType` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVolumeTemplateVOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES VolumeEO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

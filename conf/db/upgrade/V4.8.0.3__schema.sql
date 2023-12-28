-- in version zsv_4.2.0
-- Feature: USB device support sharing | ZSV-4726
delete from ResourceVO where resourceType = 'UsbDeviceVO' and uuid not in (select uuid from UsbDeviceVO);

insert into AccountResourceRefVO (`accountUuid`,`ownerAccountUuid`,`resourceUuid`,`resourceType`,`permission`,`isShared`,`lastOpDate`,`createDate`,`concreteResourceType`)
select '36c27e8ff05c4780bf6d2fa65700f22e', '36c27e8ff05c4780bf6d2fa65700f22e', uuid, resourceType, 2, false, NOW(), NOW(), 'org.zstack.usbDevice.UsbDeviceVO'
    from ResourceVO where resourceType = 'UsbDeviceVO';

-- Feature: support OVF uploading breakpoint continuation | ZSV-4467
alter table `zstack`.`LongJobVO` modify `uuid` char(32) not null;
alter table `zstack`.`LongJobVO` add column `parentUuid` char(32) default null;

-- Feature: SNS support WeCom and FeiShu | ZSV-4868 | ZSTAC-60096
update EventSubscriptionVO set name = 'Host Hardware Changed' where uuid = '829d96de006043c3b34202861ca82078';
CREATE TABLE `zstack`.`SNSWeComEndpointVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `url` varchar(1024) NOT NULL,
    `atAll` int(1) unsigned NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `zstack`.`SNSWeComAtPersonVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `userId` varchar(64) NOT NULL,
    `endpointUuid` char(32) NOT NULL,
    `remark` varchar(128) default '' null,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `zstack`.`SNSFeiShuEndpointVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `url` varchar(1024) NOT NULL,
    `atAll` int(1) unsigned NOT NULL,
    `secret` varchar(128) default '' null,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `zstack`.`SNSFeiShuAtPersonVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `userId` varchar(64) NOT NULL,
    `endpointUuid` char(32) NOT NULL,
    `remark` varchar(128) default '' null,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
alter table SNSDingTalkEndpointVO
    add secret varchar(128) default '' null;
alter table SNSDingTalkAtPersonVO
    add lastOpDate timestamp ON UPDATE CURRENT_TIMESTAMP;
alter table SNSDingTalkAtPersonVO
    add createDate timestamp NOT NULL DEFAULT '0000-00-00 00:00:00';
alter table SNSDingTalkAtPersonVO
    add remark varchar(128) default '' null;


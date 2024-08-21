CALL ADD_COLUMN('SNSApplicationEndpointVO', 'connectionStatus', 'varchar(10)', 1, 'UP');

-- Improvement: VM Cdrom Occupant | ZSV-6691

CALL INSERT_COLUMN('VmCdRomVO', 'occupant', 'varchar(64)', 1, null, 'deviceId');
update `VmCdRomVO` set `occupant` = 'ISO' where `isoUuid` is not null;
update `VmCdRomVO` set `occupant` = 'GuestTools'
    where `vmInstanceUuid` in ( select `resourceUuid` from `SystemTagVO` where `tag` = 'guestToolsHasAttached' )
    and `deviceId` = 0;

-- Feature: IAM1 Role And Policy | ZSV-6559

drop table if exists `IAM2TicketFlowCollectionVO`;
drop table if exists `IAM2TicketFlowVO`;

drop table if exists `IAM2ProjectVirtualIDGroupRefVO`;
drop table if exists `IAM2ProjectVirtualIDRefVO`;
drop table if exists `IAM2OrganizationProjectRefVO`;
drop table if exists `IAM2GroupVirtualIDRefVO`;
drop table if exists `IAM2VirtualIDRoleRefVO`;
drop table if exists `IAM2VirtualIDOrganizationRefVO`;
drop table if exists `IAM2VirtualIDGroupRoleRefVO`;
drop table if exists `IAM2VirtualIDGroupRefVO`;
drop table if exists `IAM2ProjectAccountRefVO`;
drop table if exists `IAM2ProjectResourceRefVO`;

drop table if exists `IAM2OrganizationAttributeVO`;
drop table if exists `IAM2VirtualIDGroupAttributeVO`;
drop table if exists `IAM2ProjectAttributeVO`;
drop table if exists `IAM2VirtualIDAttributeVO`;
drop table if exists `IAM2ProjectRoleVO`;
drop table if exists `IAM2OrganizationVO`;
drop table if exists `IAM2VirtualIDGroupVO`;
drop table if exists `IAM2ProjectTemplateVO`;
drop table if exists `IAM2ProjectVO`;
drop table if exists `IAM2VirtualIDVO`;

delete from `AccountResourceRefVO` where `accountUuid` = '2dce5dc485554d21a3796500c1db007a';
delete from `QuotaVO` where `identityUuid` = '2dce5dc485554d21a3796500c1db007a';
delete from `AccountVO` where `uuid` = '2dce5dc485554d21a3796500c1db007a';
delete from `ResourceVO` where `uuid` = '2dce5dc485554d21a3796500c1db007a';

ALTER TABLE `TwoFactorAuthenticationSecretVO` CHANGE COLUMN `userUuid` `accountUuid` char(32) not null;
call DROP_COLUMN('TwoFactorAuthenticationSecretVO', 'userType');

RENAME TABLE `CCSCertificateUserRefVO` TO `CCSCertificateAccountRefVO`;
ALTER TABLE `CCSCertificateAccountRefVO` CHANGE COLUMN `userUuid` `accountUuid` char(32) not null;

call DROP_COLUMN('AccessKeyVO', 'userUuid');

drop table if exists `UserPolicyRefVO`;
drop table if exists `UserGroupPolicyRefVO`;
drop table if exists `UserGroupUserRefVO`;
drop table if exists `RoleUserGroupRefVO`;
drop table if exists `RoleUserRefVO`;

drop table if exists `UserGroupVO`;
delete from `ResourceVO` where `resourceType` = 'UserGroupVO';

alter table `HybridAccountVO` drop foreign key `fkHybridAccountVOUserVO`;
drop table if exists `UserVO`;
delete from `ResourceVO` where `resourceType` = 'UserVO';

rename table `AccountResourceRefVO` to `AccountResourceRefVODeprecated`;

create table if not exists `zstack`.`AccountResourceRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `accountUuid` char(32) default null,
    `resourceUuid` varchar(32) not null,
    `resourceType` varchar(255) not null,
    `accountPermissionFrom` char(32) default null,
    `resourcePermissionFrom` char(32) default null,
    `type` varchar(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkAccountResourceRefAccountUuid` foreign key (`accountUuid`) references `AccountVO` (`uuid`) on delete cascade,
    constraint `fkAccountResourceRefResourceUuid` foreign key (`resourceUuid`) references `ResourceVO` (`uuid`) on delete cascade,
    index `idxAccountResourceRefResourceTypeAccount` (`resourceUuid`, `type`, `accountUuid`)
) ENGINE=InnoDB default CHARSET=utf8;

insert into `AccountResourceRefVO`
    (`accountUuid`,`resourceUuid`,`resourceType`,`type`,`lastOpDate`,`createDate`)
    select t.accountUuid, t.resourceUuid, t.resourceType, 'Own', t.lastOpDate, t.createDate
        from AccountResourceRefVODeprecated t;
insert into `AccountResourceRefVO`
    (`accountUuid`,`resourceUuid`,`resourceType`,`type`,`lastOpDate`,`createDate`)
    select t.receiverAccountUuid, t.resourceUuid, t.resourceType, 'Share', t.lastOpDate, t.createDate
        from SharedResourceVO t
        where t.toPublic = 0;
insert into `AccountResourceRefVO`
    (`resourceUuid`,`resourceType`,`type`,`lastOpDate`,`createDate`)
    select t.resourceUuid, t.resourceType, 'SharePublic', t.lastOpDate, t.createDate
        from SharedResourceVO t
        where t.toPublic = 1;

drop table if exists `AccountResourceRefVODeprecated`;
drop table if exists `SystemRoleVO`;
drop table if exists `RolePolicyRefVO`;
drop table if exists `PolicyVO`;
delete from `RoleVO`;
delete from `ResourceVO` where resourceType in ('SystemRoleVO', 'RoleVO', 'PolicyVO');

-- Others

CALL INSERT_COLUMN('VmVfNicVO', 'haState', 'varchar(32)', 0, 'Disabled', 'pciDeviceUuid');

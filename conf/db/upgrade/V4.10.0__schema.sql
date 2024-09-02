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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `AccountResourceRefVO`
    (`accountUuid`,`resourceUuid`,`resourceType`,`type`,`lastOpDate`,`createDate`)
    SELECT t.accountUuid, t.resourceUuid, t.resourceType, 'Own', t.lastOpDate, t.createDate
        FROM AccountResourceRefVODeprecated t;
INSERT INTO `AccountResourceRefVO`
    (`accountUuid`,`resourceUuid`,`resourceType`,`type`,`lastOpDate`,`createDate`)
    SELECT t.receiverAccountUuid, t.resourceUuid, t.resourceType, 'Share', t.lastOpDate, t.createDate
        FROM SharedResourceVO t
        WHERE t.toPublic = 0;
INSERT INTO `AccountResourceRefVO`
    (`resourceUuid`,`resourceType`,`type`,`lastOpDate`,`createDate`)
    SELECT t.resourceUuid, t.resourceType, 'SharePublic', t.lastOpDate, t.createDate
        FROM SharedResourceVO t
        WHERE t.toPublic = 1;

drop table if exists `AccountResourceRefVODeprecated`;
drop table if exists `SystemRoleVO`;
drop table if exists `RolePolicyRefVO`;
drop table if exists `PolicyVO`;
drop table if exists `RolePolicyStatementVO`;
drop table if exists `RoleAccountRefVO`;
delete from `RoleVO`;
delete from `ResourceVO` where resourceType in ('SystemRoleVO', 'RoleVO', 'PolicyVO');

CALL DROP_COLUMN('RoleVO', 'identity');
CALL DROP_COLUMN('RoleVO', 'state');

create table if not exists `zstack`.`RolePolicyVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `roleUuid` char(32) not null,
    `actions` varchar(255) not null,
    `effect` varchar(32) not null,
    `resourceType` varchar(255) default null,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkRolePolicyRoleUuid` foreign key (`roleUuid`) references `RoleVO` (`uuid`) on delete cascade,
    index `idxRolePolicyActions` (`actions`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`RolePolicyResourceRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `rolePolicyId` bigint unsigned not null,
    `effect` varchar(32) default 'Allow' not null,
    `resourceUuid` char(32) not null,
    primary key (`id`),
    constraint `fkRolePolicyResourceRefRolePolicyId` foreign key (`rolePolicyId`) references `RolePolicyVO` (`id`) on delete cascade
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`RoleAccountRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `roleUuid` char(32) not null,
    `accountUuid` char(32) not null,
    `accountPermissionFrom` char(32) default null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkRoleAccountRefRoleUuid` foreign key (`roleUuid`) references `RoleVO` (`uuid`) on delete cascade,
    constraint `fkRoleAccountRefAccountUuid` foreign key (`accountUuid`) references `AccountVO` (`uuid`) on delete cascade
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`AccountGroupVO` (
    `uuid` char(32) not null unique,
    `name` varchar(255) not null,
    `description` varchar(2048) default '',
    `parentUuid` char(32) default null,
    `rootGroupUuid` char(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`AccountGroupAccountRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `accountUuid` char(32) not null,
    `groupUuid` char(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkAccountGroupAccountRefAccountUuid` foreign key (`accountUuid`) references `AccountVO` (`uuid`) on delete cascade,
    constraint `fkAccountGroupAccountRefGroupUuid` foreign key (`groupUuid`) references `AccountGroupVO` (`uuid`) on delete cascade
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`AccountGroupRoleRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `roleUuid` char(32) not null,
    `groupUuid` char(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkAccountGroupRoleRefRoleUuid` foreign key (`roleUuid`) references `RoleVO` (`uuid`) on delete cascade,
    constraint `fkAccountGroupRoleRefGroupUuid` foreign key (`groupUuid`) references `AccountGroupVO` (`uuid`) on delete cascade
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`AccountGroupResourceRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `resourceUuid` char(32) not null,
    `groupUuid` char(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkAccountGroupResourceRefResourceUuid` foreign key (`resourceUuid`) references `ResourceVO` (`uuid`) on delete cascade,
    constraint `fkAccountGroupResourceRefGroupUuid` foreign key (`groupUuid`) references `AccountGroupVO` (`uuid`) on delete cascade
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table `zstack`.`AccountResourceRefVO` add constraint fkAccountResourceRefAccountPermissionFrom foreign key (accountPermissionFrom) references AccountGroupVO (uuid) on delete cascade;
alter table `zstack`.`RoleAccountRefVO` add constraint fkRoleAccountRefAccountPermissionFrom foreign key (accountPermissionFrom) references AccountGroupVO (uuid) on delete cascade;


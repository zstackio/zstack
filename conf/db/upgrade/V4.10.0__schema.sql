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

create table `zstack`.`AccountResourceRefVO` (
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

drop table `AccountResourceRefVODeprecated`;
drop table `SystemRoleVO`;
drop table `RolePolicyRefVO`;
drop table `PolicyVO`;
delete from `RoleVO`;
delete from `ResourceVO` where resourceType in ('SystemRoleVO', 'RoleVO', 'PolicyVO');

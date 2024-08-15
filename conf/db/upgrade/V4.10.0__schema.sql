-- Feature: IAM1 Role And Policy | ZSV-6559

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

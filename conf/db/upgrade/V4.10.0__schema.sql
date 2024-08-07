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

CALL ADD_COLUMN('SNSApplicationEndpointVO', 'connectionStatus', 'varchar(10)', 1, 'UP');

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

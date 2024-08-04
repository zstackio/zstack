CALL ADD_COLUMN('SNSApplicationEndpointVO', 'connectionStatus', 'varchar(10)', 1, 'UP');

-- Feature: IAM1 Role And Policy | ZSV-6559

ALTER TABLE `TwoFactorAuthenticationSecretVO` CHANGE COLUMN `userUuid` `accountUuid` char(32) not null;
call DROP_COLUMN('TwoFactorAuthenticationSecretVO', 'userType');

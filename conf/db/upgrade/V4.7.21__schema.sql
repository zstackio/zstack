ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `userinfoUrl` varchar(256) DEFAULT NULL;
ALTER TABLE `zstack`.`SSOClientVO` ADD COLUMN `redirectUrl` varchar(256) DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2ClientVO` ADD COLUMN `logoutUrl` varchar(256) DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2TokenVO` MODIFY COLUMN `accessToken` text DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2TokenVO` MODIFY COLUMN `idToken` text DEFAULT NULL;
ALTER TABLE `zstack`.`OAuth2TokenVO` MODIFY COLUMN `refreshToken` text DEFAULT NULL;
CALL CREATE_INDEX('SSOTokenVO', 'idxSSOTokenVOUserUuid', 'userUuid');


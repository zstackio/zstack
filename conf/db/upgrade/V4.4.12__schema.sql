ALTER TABLE `zstack`.`CdpPolicyEO` ADD COLUMN `dailyRPSinceDay` int unsigned DEFAULT 0;
ALTER TABLE `zstack`.`CdpPolicyEO` ADD COLUMN `expireTime` int unsigned DEFAULT 0;
ALTER TABLE `zstack`.`CdpPolicyEO` ADD COLUMN `fullBackupInterval` int unsigned DEFAULT 0;

DROP VIEW IF EXISTS `zstack`.`CdpPolicyVO`;
CREATE VIEW `zstack`.`CdpPolicyVO` AS SELECT uuid, name, description, retentionTimePerDay, dailyRPSinceDay, expireTime, recoveryPointPerSecond, fullBackupInterval, state, lastOpDate, createDate FROM `zstack`.`CdpPolicyEO` WHERE deleted IS NULL;

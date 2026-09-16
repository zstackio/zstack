-- zsdataset 的 appInstanceUuid 放宽到 36 字符。
--
-- 这个 uuid 来自应用市场的 application instance，用的是带连字符的标准 UUID 形式（36 字符），
-- 不是 ZStack 内部去掉连字符的 32 字符形式。按平台惯例定成 varchar(32) 是错误假设，
-- 实测写入时 API 参数校验先报 "exceeds max length of string, expected <= 32, actual was 36"。

ALTER TABLE `zstack`.`ZsDatasetSpaceRefVO` MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL COMMENT 'marketplace application instance hosting the VM; canonical hyphenated uuid';
ALTER TABLE `zstack`.`ZsDatasetServiceKeyVO` MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL COMMENT 'zsdataset application instance this key authenticates to; canonical hyphenated uuid';
ALTER TABLE `zstack`.`ZsDatasetPublicationVO` MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL COMMENT 'canonical hyphenated uuid from the marketplace application instance';

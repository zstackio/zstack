-- Begin ZSTAC-88163: create the three zsdataset tables.
-- They were registered in premium's conf/persistence.xml without any DDL here, so the
-- management node threw "Table 'zstack.ZsDatasetSpaceRefVO' doesn't exist" on the first
-- query and almost every integration case collapsed with it.
-- Shaped after DatasetVO in the same module: it also extends ResourceVO and likewise
-- declares no foreign key to ResourceVO. Timestamp defaults follow the AI tables added
-- earlier in this file rather than a zero date, which strict SQL mode rejects.
CREATE TABLE IF NOT EXISTS `zstack`.`ZsDatasetSpaceRefVO` (
    `uuid`            varchar(32)  NOT NULL UNIQUE,
    `spaceId`         varchar(255) NOT NULL,
    `appInstanceUuid` varchar(36)  NOT NULL,
    `lastOpDate`      TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`      TIMESTAMP    NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZsDatasetServiceKeyVO` (
    `uuid`            varchar(32)  NOT NULL UNIQUE,
    `appInstanceUuid` varchar(36)  NOT NULL,
    `keyId`           varchar(255) NOT NULL,
    `secret`          varchar(255) NOT NULL,
    `lastOpDate`      TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`      TIMESTAMP    NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- downloadToken holds the VM's publication download token, an HMAC-SHA256 digest encoded as
-- base64url, so it is always 43 characters; varchar(2048) leaves ample room for a later
-- scheme without reserving an off-page text column. errorSummary is a failure summary widened
-- past the JPA default of 255 so a long message is stored rather than truncated.
CREATE TABLE IF NOT EXISTS `zstack`.`ZsDatasetPublicationVO` (
    `uuid`             varchar(32)   NOT NULL UNIQUE,
    `appInstanceUuid`  varchar(36)   NOT NULL,
    `spaceId`          varchar(255)  NOT NULL,
    `publishRequestId` varchar(255)  NOT NULL,
    `modelCenterUuid`  varchar(32)   DEFAULT NULL,
    `downloadToken`    varchar(2048) DEFAULT NULL,
    `datasetUuid`      varchar(32)   DEFAULT NULL,
    `errorSummary`     varchar(2048) DEFAULT NULL,
    `state`            varchar(32)   NOT NULL,
    `lastOpDate`       TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`       TIMESTAMP     NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- End ZSTAC-88163.

-- The original V5.5.38 schema created these columns as varchar(32) on some
-- installations. Editing that already-applied migration does not widen them.
-- Marketplace application instance UUIDs contain hyphens and require 36 characters.

ALTER TABLE `zstack`.`ZsDatasetSpaceRefVO`
    MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL;

ALTER TABLE `zstack`.`ZsDatasetServiceKeyVO`
    MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL;

ALTER TABLE `zstack`.`ZsDatasetPublicationVO`
    MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL;

-- Store 4-byte UTF-8 characters (emoji) in API request and result records.
--
-- These columns hold the serialized API message and event. With the 3-byte utf8 charset an API
-- whose request or result contains an emoji fails to persist its async record: the webhook is
-- never sent and the caller waits until its own timeout, although the API itself succeeded.
-- The JDBC connection already negotiates utf8mb4, so converting the columns is sufficient.
-- Same approach as GuestVmScriptExecutedRecordDetailVO in V5.5.6.

ALTER TABLE `zstack`.`AsyncRestVO`
    MODIFY `requestData` LONGTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci` DEFAULT NULL,
    MODIFY `result` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci` DEFAULT NULL;

ALTER TABLE `zstack`.`AuditsVO`
    MODIFY `requestDump` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`,
    MODIFY `responseDump` MEDIUMTEXT CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`;

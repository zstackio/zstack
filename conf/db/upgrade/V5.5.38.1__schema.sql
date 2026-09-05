-- zsdataset 的三张表。
--
-- 单独成一个版本而不是并入 V5.5.38：那个版本在开发环境已经被 flyway 应用过，
-- 事后往里加语句会改变校验和，已应用过它的环境会在启动时校验失败而不是补建表。
-- 实测于 172.20.9.13：部署的 V5.5.38 只有 1333 字节，与源码分支上的同名文件早已不同。

CREATE TABLE IF NOT EXISTS `zstack`.`ZsDatasetSpaceRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `spaceId` varchar(128) NOT NULL COMMENT 'production space identifier owned by the zsdataset VM',
    `appInstanceUuid` varchar(32) NOT NULL COMMENT 'marketplace application instance hosting the VM',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukZsDatasetSpaceRefVOInstanceSpace` (`appInstanceUuid`, `spaceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZsDatasetServiceKeyVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `appInstanceUuid` varchar(32) NOT NULL COMMENT 'zsdataset application instance this key authenticates to',
    `keyId` varchar(64) NOT NULL,
    `secret` varchar(512) NOT NULL COMMENT 'never returned by any API; see ZsDatasetServiceKeyInventory',
    `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukZsDatasetServiceKeyVOAppInstance` (`appInstanceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZsDatasetPublicationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'derived from appInstanceUuid, spaceId and publishRequestId so a retried publish collides instead of producing a second dataset',
    `appInstanceUuid` varchar(32) NOT NULL,
    `spaceId` varchar(128) NOT NULL,
    `publishRequestId` varchar(128) NOT NULL COMMENT 'chosen by the caller; unique only within a space, which is why uuid is derived from all three',
    `downloadToken` varchar(512) DEFAULT NULL COMMENT 'issued exactly once at prepare and kept only as a hash by the VM, so this row is the only copy; cleared once the publication is completed or failed',
    `datasetUuid` varchar(32) DEFAULT NULL,
    `errorSummary` varchar(512) DEFAULT NULL,
    `state` varchar(32) NOT NULL COMMENT 'PREPARED, DATASET_CREATED, COMPLETED or FAILED',
    `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukZsDatasetPublicationVORequest` (`appInstanceUuid`, `spaceId`, `publishRequestId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

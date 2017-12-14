ALTER TABLE VCenterPrimaryStorageVO ADD COLUMN datastore varchar(64) DEFAULT NULL;
ALTER TABLE VCenterBackupStorageVO  ADD COLUMN datastore varchar(64) DEFAULT NULL;

INSERT INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "IPsecConnectionVO" FROM IPsecConnectionVO t;

CREATE TABLE  `zstack`.`LongJobVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `apiId` varchar(32) NOT NULL,  -- to query from TaskProgressVO
    `jobName` varchar(255) NOT NULL,
    `jobData` text NOT NULL,
    `jobResult` text,
    `state` varchar(255) NOT NULL,
    `targetResourceUuid` varchar(32) DEFAULT NULL,
    `managementNodeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkLongJobVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE EcsInstanceVO ADD COLUMN publicIpAddress varchar(32) DEFAULT NULL;
DELETE FROM SystemTagVO WHERE tag LIKE 'publicIp::%' AND resourceType='EcsInstanceVO';

ALTER TABLE AsyncRestVO MODIFY COLUMN `result` mediumtext DEFAULT NULL;

UPDATE AffinityGroupVO SET policy = "ANTISOFT" where policy = "ANTIAFFINITYSOFT";

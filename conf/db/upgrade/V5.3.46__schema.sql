-- Migration script to update AuditVO table from AccountId to ProjectId.
-- This script efficiently migrates data using a single JOIN operation.

DELIMITER $$
DROP PROCEDURE IF EXISTS changeAccountIdToProjectIdForAuditVO$$
CREATE PROCEDURE changeAccountIdToProjectIdForAuditVO()
    pro_label: BEGIN
    DECLARE v_total_updated INT DEFAULT 0;

    IF (SELECT COUNT(*) FROM IAM2ProjectAccountRefVO) = 0 THEN
SELECT 'No IAM2ProjectAccountRefVO records found, skipping migration.' AS message;
LEAVE pro_label;
END IF;

SELECT 'Starting migration of AuditsVO records from AccountUuid to ProjectUuid...' AS message;

UPDATE AuditsVO a
    JOIN IAM2ProjectAccountRefVO i
ON a.resourceUuid = i.accountUuid
    SET a.resourceUuid = i.projectUuid,
        a.resourceType = CASE
        WHEN a.resourceType = 'AccountVO' THEN 'IAM2ProjectVO'
        ELSE a.resourceType
END
WHERE a.apiName = 'org.zstack.header.identity.APIUpdateQuotaMsg';
    SET v_total_updated = ROW_COUNT();
SELECT CONCAT('Migration completed successfully. Total records updated: ', v_total_updated) AS message;

END$$

DELIMITER ;
CALL changeAccountIdToProjectIdForAuditVO();
DROP PROCEDURE IF EXISTS changeAccountIdToProjectIdForAuditVO;

CALL ADD_COLUMN('SdnControllerVO', 'vendorVersion', 'VARCHAR(32)', 0, 'V1');

CREATE TABLE IF NOT EXISTS `zstack`.`H3cSdnControllerTenantVO` (
                                                                   `uuid` varchar(32) NOT NULL UNIQUE,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `tenantUuid` varchar(255) DEFAULT NULL,
    `vdsUuid` varchar(255) DEFAULT NULL,
    `tenantName` varchar(255) DEFAULT NULL,
    `vdsName` varchar(255) DEFAULT NULL,
    `cloudDomainName` varchar(255) DEFAULT NULL,
    `state` varchar(32) NOT NULL DEFAULT "Enabled",
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkH3cSdnControllerTenantVOSdnControllerVO` FOREIGN KEY (`sdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`H3cSdnSubnetIpRangeRefVO` (
                                                                   `id` BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
                                                                   `sdnControllerUuid` varchar(32) NOT NULL,
    `ipRangeUuid` varchar(32) NOT NULL,
    `subnetUuid` varchar(255) NOT NULL,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkH3cSdnSubnetIpRangeRefVOSdnControllerVO` FOREIGN KEY (`sdnControllerUuid`) REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkH3cSdnSubnetIpRangeRefVOIpRangeVO` FOREIGN KEY (`ipRangeUuid`) REFERENCES `IpRangeEO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE FROM UserTagVO WHERE uuid = 'a4de80903e57422699fb05bd367a3cb4';

CALL ADD_COLUMN('PciDeviceSpecVO', 'allowResourceConfigWithMultipleDevices', 'tinyint(1)', 0, '1');

CALL ADD_COLUMN('GpuDeviceVO', 'opaque', 'MEDIUMTEXT', 1, NULL);

CALL ADD_COLUMN('ModelServiceInstanceVO', 'nodeRank', 'int', 1, 0);

CREATE TABLE IF NOT EXISTS `zstack`.`GpuDeviceSpecVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `memory` bigint unsigned NULL DEFAULT 0,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkGpuDeviceSpecVOPciDeviceSpecVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceSpecVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('ModelServiceVO', 'supportDistributed', 'tinyint(1)', 0, 0);

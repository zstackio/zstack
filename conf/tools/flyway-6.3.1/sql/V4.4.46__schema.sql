DELIMITER $$
CREATE PROCEDURE splitHybridLicenseRecords()
BEGIN
    IF EXISTS ( SELECT 1
                   FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE table_name = 'LicenseHistoryVO'
                         AND table_schema = 'zstack'
                         AND column_name = 'capacity') THEN
        insert into `zstack`.`LicenseHistoryVO` (`uuid`, `cpuNum`, `hostNum`, `vmNum`, `expiredDate`, `issuedDate`, `uploadDate`, `licenseType`, `userName`, `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`, `capacity`)
            select `uuid`, `cpuNum`, `hostNum`, `vmNum`, `expiredDate`, `issuedDate`, `uploadDate`, 'AddOn' as `licenseType`, `userName`, 'hybrid' as `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`, `capacity`
            from `zstack`.`LicenseHistoryVO`
            where `licenseType`='Hybrid';
    ELSE
        insert into `zstack`.`LicenseHistoryVO` (`uuid`, `cpuNum`, `hostNum`, `vmNum`, `expiredDate`, `issuedDate`, `uploadDate`, `licenseType`, `userName`, `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`)
            select `uuid`, `cpuNum`, `hostNum`, `vmNum`, `expiredDate`, `issuedDate`, `uploadDate`, 'AddOn' as `licenseType`, `userName`, 'hybrid' as `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`
            from `zstack`.`LicenseHistoryVO`
            where `licenseType`='Hybrid';
    END IF;
    update `zstack`.`LicenseHistoryVO` set `licenseType`='Paid' where `licenseType`='Hybrid';
    SELECT CURTIME();
END $$
DELIMITER ;

CALL splitHybridLicenseRecords();
DROP PROCEDURE IF EXISTS splitHybridLicenseRecords;

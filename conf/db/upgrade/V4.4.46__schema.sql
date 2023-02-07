insert into `zstack`.`LicenseHistoryVO` (`uuid`, `cpuNum`, `hostNum`, `vmNum`, `expiredDate`, `issuedDate`, `uploadDate`, `licenseType`, `userName`, `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`)
    select `uuid`, `cpuNum`, `hostNum`, `vmNum`, `expiredDate`, `issuedDate`, `uploadDate`, 'AddOn' as `licenseType`, `userName`, 'hybrid' as `prodInfo`, `createDate`, `lastOpDate`, `hash`, `source`, `managementNodeUuid`, `mergedTo`
    from `zstack`.`LicenseHistoryVO`
    where `licenseType`='Hybrid';
update `zstack`.`LicenseHistoryVO` set `licenseType`='Paid' where `licenseType`='Hybrid';

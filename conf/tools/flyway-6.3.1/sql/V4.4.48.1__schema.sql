UPDATE `zstack`.`LicenseHistoryVO`
    SET prodInfo = 'Enterprise.Cloud'
    WHERE LOWER(prodInfo) = 'zstack' and licenseType = 'Paid';

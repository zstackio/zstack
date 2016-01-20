package org.zstack.test.mevoco;

import org.zstack.license.LicenseInfo;
import org.zstack.license.LicenseManagerImpl;
import org.zstack.license.LicenseType;

/**
 * Created by frank on 12/31/2015.
 */
public class MockLicenseManagerImpl extends LicenseManagerImpl {
    public static LicenseInfo mockLicenseInfo ;

    static {
        LicenseInfo l = new LicenseInfo();
        l.setLicenseType(LicenseType.Paid);
        l.setHostNum(10);
        mockLicenseInfo = l;
    }

    @Override
    public LicenseType getLicenseType() {
        return mockLicenseInfo.getLicenseType();
    }

    @Override
    public LicenseInfo getLicenseInfo() {
        return mockLicenseInfo;
    }
}

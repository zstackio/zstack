package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.SessionInventory;
import org.zstack.network.securitygroup.SecurityGroupConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * test quota
 */
public class TestPolicyForSecurityGroup1 {
    static CLogger logger = Utils.getLogger(TestPolicyForSecurityGroup1.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestPolicyForSecurityGroup.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");
        SessionInventory session = identityCreator.getAccountSession();

        api.createSecurityGroup("sg", session);

        api.updateQuota(test.getUuid(), SecurityGroupConstant.QUOTA_SG_NUM, 1);

        boolean s = false;
        try {
            api.createSecurityGroup("sg", session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);
    }
}


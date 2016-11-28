package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupState;
import org.zstack.network.securitygroup.SecurityGroupStateEvent;
import org.zstack.network.securitygroup.SecurityGroupVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestChangeSecurityGroupState {
    static CLogger logger = Utils.getLogger(TestRemoveSecurityGroupRuleOfVmOnKvm.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestCreateSecurityGroup.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        long count = dbf.count(SecurityGroupVO.class);
        Assert.assertEquals(1, count);
        SecurityGroupInventory inv = api.listSecurityGroup(null).get(0);
        Assert.assertEquals(SecurityGroupState.Enabled.toString(), inv.getState());
        inv = api.changeSecurityGroupState(inv.getUuid(), SecurityGroupStateEvent.disable);
        Assert.assertEquals(SecurityGroupState.Disabled.toString(), inv.getState());
        inv = api.changeSecurityGroupState(inv.getUuid(), SecurityGroupStateEvent.enable);
        Assert.assertEquals(SecurityGroupState.Enabled.toString(), inv.getState());
    }
}

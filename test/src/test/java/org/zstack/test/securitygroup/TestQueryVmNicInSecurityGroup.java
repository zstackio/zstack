package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmNicMsg;
import org.zstack.header.vm.APIQueryVmNicReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.securitygroup.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * @author frank
 */
public class TestQueryVmNicInSecurityGroup {
    static CLogger logger = Utils.getLogger(TestQueryVmNicInSecurityGroup.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestQueryVmNicInSecurityGroup.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);
        SecurityGroupInventory sc = deployer.securityGroups.get("test");
        api.addVmNicToSecurityGroup(sc.getUuid(), nic.getUuid());
        SessionInventory session = api.loginByAccount("TestAccount", "password");
        List<VmNicSecurityGroupRefInventory> invs = api.listVmNicSecurityGroupRef(null);
        VmNicSecurityGroupRefInventory inv = invs.get(0);
        QueryTestValidator.validateEQ(new APIQueryVmNicInSecurityGroupMsg(), api, APIQueryVmNicInSecurityGroupReply.class, inv, session);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryVmNicInSecurityGroupMsg(), api, APIQueryVmNicInSecurityGroupReply.class, inv, session, 3);

        APIQuerySecurityGroupMsg msg = new APIQuerySecurityGroupMsg();
        msg.addQueryCondition("vmNic.uuid", QueryOp.EQ, nic.getUuid());
        APIQuerySecurityGroupReply reply = api.query(msg, APIQuerySecurityGroupReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        SecurityGroupInventory sg = reply.getInventories().get(0);
        Assert.assertEquals(sc.getUuid(), sg.getUuid());

        APIQueryVmNicMsg nicMsg = new APIQueryVmNicMsg();
        nicMsg.addQueryCondition("securityGroup.name", QueryOp.EQ, sg.getName());
        APIQueryVmNicReply nreply = api.query(nicMsg, APIQueryVmNicReply.class);
        Assert.assertEquals(1, nreply.getInventories().size());
        VmNicInventory rnic = nreply.getInventories().get(0);
        Assert.assertEquals(nic.getUuid(), rnic.getUuid());
    }
}

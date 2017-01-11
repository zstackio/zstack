package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.*;
import org.zstack.sdk.QueryVmNicAction;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Frank
 */
public class TestQueryVmNic {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestQueryVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        SessionInventory session = api.loginByAccount("test", "password");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);
        QueryTestValidator.validateEQ(new APIQueryVmNicMsg(), api, APIQueryVmNicReply.class, nic, session);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryVmNicMsg(), api, APIQueryVmNicReply.class, nic, session, 3);

        QueryVmNicAction a = new QueryVmNicAction();
        a.sessionId = api.getAdminSession().getUuid();
        a.conditions = asList(
                String.format("l3NetworkUuid=%s", nic.getL3NetworkUuid()),
                String.format("vmInstance.type=%s", VmInstanceConstant.USER_VM_TYPE)
        );
        QueryVmNicAction.Result res = a.call();
        api.throwExceptionIfNeed(res.error);
        Assert.assertEquals(1, res.value.inventories.size());
    }
}

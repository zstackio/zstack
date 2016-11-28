package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.*;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.test.search.QueryTestValidator;

import java.util.ArrayList;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @author Frank
 * @condition 1. deploy a vm using non-admin account
 * 2. query vm on each queryable field of VmInstanceInventory using non-admin account
 * @test the vm can be correctly found
 * <p>
 * 3. test APIQueryAccountResourceRefMsg
 * <p>
 * confirm work
 */
public class TestQueryVm {
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
        QueryTestValidator.validateEQ(new APIQueryVmInstanceMsg(), api, APIQueryVmInstanceReply.class, vm, session);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryVmInstanceMsg(), api, APIQueryVmInstanceReply.class, vm, session, 3);

        IdentityCreator c = new IdentityCreator(api);
        AccountInventory test = c.useAccount("test");

        APIQueryAccountResourceRefMsg qmsg = new APIQueryAccountResourceRefMsg();
        qmsg.addQueryCondition("accountUuid", QueryOp.EQ, test.getUuid());
        qmsg.addQueryCondition("resourceType", QueryOp.EQ, VmInstanceVO.class.getSimpleName());
        APIQueryAccountResourceRefReply r = api.query(qmsg, APIQueryAccountResourceRefReply.class);
        Assert.assertEquals(1, r.getInventories().size());
        AccountResourceRefInventory inv = r.getInventories().get(0);
        Assert.assertEquals(vm.getUuid(), inv.getResourceUuid());

        HostInventory host = deployer.hosts.get("TestHost1");
        Map<String, AccountInventory> ret = api.getResourceAccount(list(vm.getUuid(), host.getUuid()));
        Assert.assertEquals(2, ret.size());
        AccountInventory acnt = ret.get(vm.getUuid());
        Assert.assertEquals(test.getUuid(), acnt.getUuid());
        acnt = ret.get(host.getUuid());
        Assert.assertEquals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, acnt.getUuid());

        api.changeResourceOwner(vm.getUuid(), AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        APIQueryVmInstanceMsg msg = new APIQueryVmInstanceMsg();
        msg.setConditions(new ArrayList<QueryCondition>());
        APIQueryVmInstanceReply reply = api.query(msg, APIQueryVmInstanceReply.class, session);
        Assert.assertEquals(0, reply.getInventories().size());

        SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
        q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, vm.getRootVolumeUuid());
        AccountResourceRefVO ref = q.find();
        Assert.assertEquals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, ref.getAccountUuid());

        boolean s = false;
        try {
            api.changeResourceOwner(vm.getRootVolumeUuid(), AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }
}

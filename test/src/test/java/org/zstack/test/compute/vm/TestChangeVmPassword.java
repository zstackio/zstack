package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.tag.TestQemuAgentSystemTag;

/**
 * Created by mingjian.deng on 16/10/19.
 */
public class TestChangeVmPassword {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/vm/TestChangeVmPassword.xml", con);
//        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test_running() throws ApiSenderException {
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        api.createSystemTag(inv.getUuid(), TestQemuAgentSystemTag.TestSystemTags.qemu.getTagFormat(), VmInstanceVO.class);

        Assert.assertEquals(VmInstanceState.Running.toString(), inv.getState());

        APIChangeVmPasswordEvent account = api.changeVmPassword(new VmAccountPerference(
                inv.getUuid(), "change", "test1234"));
        Assert.assertNotNull(account);

        account = api.changeVmPassword(new VmAccountPerference(
                inv.getUuid(), "change", "||||||"));
        Assert.assertNotNull(account);

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        vm = api.stopVmInstance(inv.getUuid());
        Assert.assertEquals(VmInstanceState.Stopped.toString(), vm.getState());

        try {
            account = api.changeVmPassword(new VmAccountPerference(
                    inv.getUuid(), "change", "test1234"));
            Assert.assertFalse(true);
        } catch (ApiSenderException e) {
            Assert.assertEquals(VmErrors.NOT_IN_CORRECT_STATE.toString(), e.getError().getCode());
        }

        vm = api.startVmInstance(inv.getUuid());
        Assert.assertEquals(VmInstanceState.Running.toString(), vm.getState());
        VmInstanceVO vmvo = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);

        Assert.assertNotNull(vmvo);
        Assert.assertEquals(VmInstanceState.Running, vmvo.getState());
        Assert.assertNotNull(vmvo.getHostUuid());
    }
}

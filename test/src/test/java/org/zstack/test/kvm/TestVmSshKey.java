package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DatabaseFacadeImpl;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmBootDevice;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands.BootDev;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;
/**
 * Created by luchukun on 8/4/16.
 */
public class TestVmSshKey {
    CLogger logger = Utils.getLogger(TestVmSshKey.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmIso.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }
    @Test
    public void test() throws ApiSenderException{
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String sshKey = api.getVmSshKey(vm.getUuid(),null);
        Assert.assertEquals(sshKey,null);
        sshKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCk+SzT7IUeQihH7QqwKjR77+aL//4uZbM1CnRNW0nHj8Pm95Ndr/NWYUz9Z6tchQWRbtsWRrvGzxRAYgLBkJv1aBpXy6dtnbN6RazHjRPg3NWXh097Wl/S2Mc8W4EgH3rrMiU8H+7dQ7ShrRP+HNLZ4jdUQV8Xc8iXDdUG4urp/MMOp6oT+VLnP1OZvYeEZNsUi4WIWk7J8J3d8kjvbTs7VS/KZGSqtgeANlE5j385NfZMZHj5zFpTVWiPt63WUu+BeDNT4c1e9cH81SLeF7fXiEWY8OlELKbrCPVBs1Qu4HE7WqJ/h7bM1jQ2rRpb3ByPGi2VbXwHq+OtgD68rH/1 luchukun@sjtu.edu.cn";
        vm = api.setVmSshKey(vm.getUuid(),sshKey,null);
        Assert.assertTrue(VmSystemTags.SSHKEY.hasTag(vm.getUuid()));
        String key = api.getVmSshKey(vm.getUuid(),null);
        Assert.assertEquals(sshKey,key);
        vm = api.deleteVmSshKey(vm.getUuid(),null);
        Assert.assertFalse(VmSystemTags.SSHKEY.hasTag(vm.getUuid()));

    }
}

package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;

public class TestMigrateVm2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestMigrateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        String srcHost = inv.getHostUuid();

        List<HostInventory> hosts = api.listHosts(null);
        String destHostUuid = null;
        for (HostInventory h : hosts) {
            if (!h.getUuid().equals(srcHost)) {
                destHostUuid = h.getUuid();
                break;
            }
        }

        inv = api.migrateVmInstance(inv.getUuid(), destHostUuid);
        Assert.assertEquals(VmInstanceState.Running.toString(), inv.getState());
        VmInstanceVO vm = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm);
        Assert.assertEquals(VmInstanceState.Running, vm.getState());
        HostCapacityVO hvo = dbf.findByUuid(inv.getHostUuid(), HostCapacityVO.class);
        Assert.assertTrue(hvo.getUsedCpu() != 0);
        Assert.assertTrue(hvo.getUsedMemory() != 0);
        Assert.assertEquals(destHostUuid, inv.getHostUuid());
        Assert.assertTrue(!inv.getHostUuid().equals(srcHost));
        hvo = dbf.findByUuid(srcHost, HostCapacityVO.class);
        Assert.assertEquals(0, hvo.getUsedCpu());
        Assert.assertEquals(0, hvo.getUsedMemory());
    }
}

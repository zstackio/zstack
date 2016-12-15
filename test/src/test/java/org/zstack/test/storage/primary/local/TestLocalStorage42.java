package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVOFinder;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. use local storage
 * 2. create 6 vm
 * <p>
 * confirm they are created on 2 hosts balanced
 */
public class TestLocalStorage42 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage42.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm1 = api.createVmFromClone(vm);
        VmInstanceInventory vm2 = api.createVmFromClone(vm);
        VmInstanceInventory vm3 = api.createVmFromClone(vm);
        VmInstanceInventory vm4 = api.createVmFromClone(vm);
        VmInstanceInventory vm5 = api.createVmFromClone(vm);

        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");

        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.add(VmInstanceVO_.hostUuid, Op.EQ, host1.getUuid());
        long count = q.count();
        Assert.assertEquals(3, count);

        q = dbf.createQuery(VmInstanceVO.class);
        q.add(VmInstanceVO_.hostUuid, Op.EQ, host2.getUuid());
        count = q.count();
        Assert.assertEquals(3, count);

        LocalStorageHostRefVO cap1_1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        LocalStorageHostRefVO cap1_2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());

        LocalStorageHostRefVO cap2_1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local2.getUuid());
        LocalStorageHostRefVO cap2_2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local2.getUuid());
        Assert.assertEquals(cap1_1.getAvailableCapacity() + cap1_2.getAvailableCapacity(),
                cap2_1.getAvailableCapacity() + cap2_2.getAvailableCapacity());
    }
}

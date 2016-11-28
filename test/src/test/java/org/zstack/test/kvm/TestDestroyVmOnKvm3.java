package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * use local storage
 * <p>
 * 1. stop the vm
 * 2. make the host disconnected
 * 3. destroy the vm
 * <p>
 * confirm the vm destroyed successfully
 * <p>
 * 4. deleted the host
 * <p>
 * confirm the GC job done
 */
public class TestDestroyVmOnKvm3 {
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
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage1.xml", con);
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

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        HostInventory host = deployer.hosts.get("host1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory root = vm.getRootVolume();
        api.createSnapshot(root.getUuid());

        api.stopVmInstance(vm.getUuid());

        HostVO hvo = dbf.findByUuid(host.getUuid(), HostVO.class);
        hvo.setStatus(HostStatus.Disconnected);
        dbf.update(hvo);

        api.destroyVmInstance(vm.getUuid());
        Assert.assertEquals(0, config.deleteBitsCmds.size());

        api.deleteHost(host.getUuid());
        TimeUnit.SECONDS.sleep(2);

        List<GarbageCollectorVO> vos = dbf.listAll(GarbageCollectorVO.class);
        Assert.assertEquals(2, vos.size());
        for (GarbageCollectorVO vo : vos) {
            org.junit.Assert.assertEquals(GCStatus.Done, vo.getStatus());
        }
    }
}

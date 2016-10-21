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
import org.zstack.header.vm.VmInstanceConstant.Capability;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.Map;

/**
 * 1. use local storage
 * 2. create a vm
 * <p>
 * confirm all local storage related commands, VOs are set
 * <p>
 * 3. attach another local storage to the cluster
 * <p>
 * confirm able to attach
 */
public class TestLocalStorage1 {
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
        config.capacityMap.put("host4", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        HostInventory host = deployer.hosts.get("host1");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        LocalStorageHostRefVO href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(href.getTotalCapacity(), totalSize);
        Assert.assertEquals(href.getTotalPhysicalCapacity(), totalSize);

        Assert.assertFalse(config.initCmdList.isEmpty());
        Assert.assertFalse(config.downloadBitsCmds.isEmpty());
        Assert.assertFalse(config.createVolumeFromCacheCmds.isEmpty());

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, vm.getRootVolumeUuid());
        LocalStorageResourceRefVO rref = q.find();

        Assert.assertNotNull(rref);
        Assert.assertEquals(vm.getRootVolume().getSize(), rref.getSize());

        Map<String, Object> cap = api.getVmCapabilities(vm.getUuid(), null);
        Assert.assertFalse((Boolean) cap.get(Capability.LiveMigration.toString()));
        Assert.assertTrue((Boolean) cap.get(Capability.VolumeMigration.toString()));

        cap = api.getVolumeCapabilities(vm.getRootVolumeUuid(), null);
        Assert.assertFalse((Boolean) cap.get(VolumeConstant.Capability.MigrationToOtherPrimaryStorage.toString()));
        Assert.assertTrue((Boolean) cap.get(VolumeConstant.Capability.MigrationInCurrentPrimaryStorage.toString()));
    }
}

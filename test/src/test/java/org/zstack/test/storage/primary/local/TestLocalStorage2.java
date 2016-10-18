package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.List;

/**
 * 1. use local storage
 * 2. create a vm
 * 3. destroy the vm
 * <p>
 * confirm all local storage related commands, VOs are set; disk capacity is returned
 */
public class TestLocalStorage2 {
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
    public void test() throws ApiSenderException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.destroyVmInstance(vm.getUuid());

        long isize = 0;
        List<ImageCacheVO> is = dbf.listAll(ImageCacheVO.class);
        for (ImageCacheVO ic : is) {
            isize += ic.getSize();
        }

        HostInventory host = deployer.hosts.get("host1");

        PrimaryStorageInventory local = deployer.primaryStorages.get("local");

        LocalStorageHostRefVO href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(totalSize, href.getTotalCapacity());
        Assert.assertEquals(totalSize - isize, href.getAvailableCapacity());

        PrimaryStorageInventory pri = deployer.primaryStorages.get("local");
        PrimaryStorageVO privo = dbf.findByUuid(pri.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(totalSize, privo.getCapacity().getTotalCapacity());
        Assert.assertEquals(totalSize - isize, privo.getCapacity().getAvailableCapacity());

        Assert.assertFalse(config.deleteBitsCmds.isEmpty());

        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, vm.getRootVolumeUuid());
        LocalStorageResourceRefVO rref = q.find();

        Assert.assertNull(rref);
    }
}

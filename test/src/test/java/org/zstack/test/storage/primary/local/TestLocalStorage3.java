package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 1. use local storage and nfs storage
 * 2. create a vm with a data disk
 * <p>
 * confirm root volume is created on the local storage while the data volume is created on the nfs
 */
public class TestLocalStorage3 {
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
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage3.xml", con);
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
    public void test() {
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");
        PrimaryStorageInventory nfs = deployer.primaryStorages.get("nfs");
        HostInventory host = deployer.hosts.get("host1");
        final VmInstanceInventory vm = deployer.vms.get("TestVm");

        // vm root volume exists in one of these local storage ps randomly
        Assert.assertTrue(
                Arrays.asList(local.getUuid(), local2.getUuid())
                        .contains(vm.getRootVolume().getPrimaryStorageUuid()));

        // new data volume exists in ps on which there is not the vm root volume
        VolumeInventory data = CollectionUtils.find(vm.getAllVolumes(),
                new Function<VolumeInventory, VolumeInventory>() {
                    @Override
                    public VolumeInventory call(VolumeInventory arg) {
                        return arg.getUuid().equals(vm.getRootVolumeUuid()) ? null : arg;
                    }
                });


        ArrayList<String> otherPS = new ArrayList<>();
        otherPS.addAll(Arrays.asList(nfs.getUuid(),
                local.getUuid(),
                local2.getUuid()));
        otherPS.remove(vm.getRootVolume().getPrimaryStorageUuid());
        Assert.assertTrue(otherPS.contains(data.getPrimaryStorageUuid()));
    }
}

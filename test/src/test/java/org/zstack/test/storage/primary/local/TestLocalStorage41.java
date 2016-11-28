package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import java.util.concurrent.TimeUnit;

/**
 * 1. use local storage
 * 2. create a vm with a data volume
 * 3. delete the vm and the data volume
 * 4. delete the host on which the vm is
 * <p>
 * confirm unable to recover the vm and volume because the host is deleted
 */
public class TestLocalStorage41 {
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
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage41.xml", con);
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
        HostInventory host = deployer.hosts.get("host1");
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory data = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return arg.getUuid().equals(vm.getRootVolumeUuid()) ? null : arg;
            }
        });

        api.deleteDataVolume(data.getUuid());
        api.destroyVmInstance(vm.getUuid());
        api.deleteHost(host.getUuid());

        boolean s = false;
        try {
            api.recoverVm(vm.getUuid(), null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        s = false;
        try {
            api.recoverVolume(data.getUuid(), null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        VolumeGlobalConfig.VOLUME_EXPUNGE_INTERVAL.updateValue(1);
        VolumeGlobalConfig.VOLUME_EXPUNGE_PERIOD.updateValue(1);
        TimeUnit.SECONDS.sleep(3);
        Assert.assertFalse(dbf.isExist(data.getUuid(), VolumeVO.class));
    }
}

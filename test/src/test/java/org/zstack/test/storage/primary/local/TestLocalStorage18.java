package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. use local storage and nfs storage
 * 2. create a vm with a data disk
 * 3. create an image from the data volume
 * <p>
 * confirm the image is created successfully
 */
public class TestLocalStorage18 {
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
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage18.xml", con);
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
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");

        VolumeInventory data = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return !arg.getUuid().equals(vm.getRootVolumeUuid()) ? arg : null;
            }
        });

        api.detachVolumeFromVm(data.getUuid());

        ImageInventory img = api.addDataVolumeTemplateFromDataVolume(data.getUuid(), list(sftp.getUuid()));
        Assert.assertFalse(config.createTemplateFromVolumeCmds.isEmpty());
        Assert.assertFalse(config.uploadBitsCmds.isEmpty());
    }
}

package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. use ceph for primary storage and backup storage
 * 2. create a vm
 * 3. create an data volume template from the vm's root volume
 * 4. create a data volume from the template
 * 5. attach the data volume
 * <p>
 * confirm the volume attached successfully
 */
public class TestCeph8 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCeph1.xml", con);
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        config.cloneCmds.clear();
        config.flattenCmds.clear();
        BackupStorageInventory bs = deployer.backupStorages.get("ceph-bk");
        ImageInventory img = api.addDataVolumeTemplateFromDataVolume(vm.getRootVolumeUuid(), list(bs.getUuid()));
        Assert.assertFalse(config.cpCmds.isEmpty());
        Assert.assertEquals(VolumeConstant.VOLUME_FORMAT_RAW, img.getFormat());

        config.cpCmds.clear();
        PrimaryStorageInventory ps = deployer.primaryStorages.get("ceph-pri");
        VolumeInventory dvol = api.createDataVolumeFromTemplate(img.getUuid(), ps.getUuid());
        Assert.assertFalse(config.cpCmds.isEmpty());
        api.attachVolumeToVm(vm.getUuid(), dvol.getUuid());
    }
}

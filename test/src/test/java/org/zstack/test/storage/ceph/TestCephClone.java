package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.APICloneVmInstanceMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. create a ceph primary storage which is not connected
 * 2. create a vm attach to the ps

 * 3. clone vm
 *
 * confirm fail message is ps is not connected.
 */
public class TestCephClone {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    CloudBus bus;

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
        config = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        session = api.loginAsAdmin();


    }

    @Test
    public void test() throws ApiSenderException {

        SimpleQuery<DiskOfferingVO> dq = dbf.createQuery(DiskOfferingVO.class);
        dq.add(DiskOfferingVO_.name, SimpleQuery.Op.EQ, "TestRootDiskOffering");
        DiskOfferingVO dvo = dq.find();
        VolumeInventory vinv = api.createDataVolume("TestRootData", dvo.getUuid());
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        PrimaryStorageInventory ps = deployer.primaryStorages.get("ceph-pri");

        vinv.setPrimaryStorageUuid(ps.getUuid());
        vm.setRootVolumeUuid(vinv.getUuid());

        //set cmsg
        CommitVolumeAsImageMsg msg = new CommitVolumeAsImageMsg();
        msg.setVolumeUuid(vm.getRootVolumeUuid());
        msg.setPrimaryStorageUuid(ps.getUuid());
        if (msg.getPlatform() == null) {
            msg.setPlatform(ImagePlatform.Linux.toString());
        }
        msg.setName(String.format("for-clone-vm-%s", vm.getUuid()));
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vinv.getPrimaryStorageUuid());

        // set primary storage status ,watch fail message
        PrimaryStorageInventory psInv = PrimaryStorageInventory.valueOf(dbf.findByUuid(msg.getPrimaryStorageUuid(), PrimaryStorageVO.class));
        ps.setStatus(PrimaryStorageStatus.Connected.toString());
        Assert.assertTrue("ps should be connected",psInv.getStatus().equals(PrimaryStorageStatus.Connected.toString()));

        psInv = PrimaryStorageInventory.valueOf(dbf.findByUuid(msg.getPrimaryStorageUuid(), PrimaryStorageVO.class));
        ps.setStatus(PrimaryStorageStatus.Disconnected.toString());
        Assert.assertFalse("ps should not be connected-1",psInv.getStatus().equals(PrimaryStorageStatus.Connected.toString()));

        psInv = PrimaryStorageInventory.valueOf(dbf.findByUuid(msg.getPrimaryStorageUuid(), PrimaryStorageVO.class));
        ps.setStatus(PrimaryStorageStatus.Connecting.toString());
        Assert.assertFalse("ps should not be connected-2",psInv.getStatus().equals(PrimaryStorageStatus.Connected.toString()));
    }

}

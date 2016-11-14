package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.storage.ceph.primary.KVMCephVolumeTO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * 1. use ceph for backup storage and primary storage
 * 2. create a vm
 * <p>
 * confirm the vm created successfully
 * <p>
 * 3. delete the ps
 * <p>
 * confirm pools are deleted.
 */
public class TestCeph1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;
    CephBackupStorageSimulatorConfig bconfig;
    RESTFacade restf;

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
        bconfig = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        restf = loader.getComponent(RESTFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        BackupStorageInventory bs = deployer.backupStorages.get("ceph-bk");
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.add(CephBackupStorageMonVO_.hostname, SimpleQuery.Op.EQ, "127.0.0.1");
        CephBackupStorageMonVO bsmon = q.find();

        Assert.assertEquals("root", bsmon.getSshUsername());
        Assert.assertEquals("pass@#$word", bsmon.getSshPassword());
        Assert.assertEquals(23, bsmon.getSshPort());

        Assert.assertFalse(config.createSnapshotCmds.isEmpty());
        Assert.assertFalse(config.protectSnapshotCmds.isEmpty());
        Assert.assertFalse(config.cloneCmds.isEmpty());

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        PrimaryStorageInventory ps = deployer.primaryStorages.get("ceph-pri");

        // test ceph backup storage data network IP
        String dataNetworkIp1 = "172.20.12.51";
        String dataNetworkIp2 = "172.20.12.52";
        CephBackupStorageVO cephb = dbf.findByUuid(bs.getUuid(), CephBackupStorageVO.class);
        Iterator<CephBackupStorageMonVO> bsit = cephb.getMons().iterator();
        CephBackupStorageMonVO bsm1 = bsit.next();
        bconfig.monAddr.put(bsm1.getUuid(), dataNetworkIp1);
        CephBackupStorageMonVO bsm2 = bsit.next();
        bconfig.monAddr.put(bsm2.getUuid(), dataNetworkIp2);
        api.reconnectBackupStorage(bs.getUuid());
        bsm1 = dbf.reload(bsm1);
        Assert.assertEquals(dataNetworkIp1, bsm1.getMonAddr());
        bsm2 = dbf.reload(bsm2);
        Assert.assertEquals(dataNetworkIp2, bsm2.getMonAddr());

        // test ceph primary storage data network IP
        CephPrimaryStorageVO cephp = dbf.findByUuid(ps.getUuid(), CephPrimaryStorageVO.class);
        Iterator<CephPrimaryStorageMonVO> it = cephp.getMons().iterator();
        CephPrimaryStorageMonVO m1 = it.next();
        config.monAddr.put(m1.getUuid(), dataNetworkIp1);
        CephPrimaryStorageMonVO m2 = it.next();
        config.monAddr.put(m2.getUuid(), dataNetworkIp2);
        api.reconnectPrimaryStorage(ps.getUuid());
        m1 = dbf.reload(m1);
        Assert.assertEquals(dataNetworkIp1, m1.getMonAddr());
        m2 = dbf.reload(m2);
        Assert.assertEquals(dataNetworkIp2, m2.getMonAddr());

        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            public void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                if (body instanceof StartVmCmd) {
                    StartVmCmd cmd = (StartVmCmd) body;
                    KVMCephVolumeTO to = (KVMCephVolumeTO) cmd.getRootVolume();
                    Assert.assertTrue(to.getMonInfo().stream().filter(
                            info -> info.getHostname().equals(dataNetworkIp1)).findAny().isPresent());
                    Assert.assertTrue(to.getMonInfo().stream().filter(
                            info -> info.getHostname().equals(dataNetworkIp2)).findAny().isPresent());
                }
            }

            @Override
            public void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
            }
        });
        api.createVmFromClone(vm);

        // test destroying vm
        api.destroyVmInstance(vm.getUuid());

        Assert.assertFalse(config.deleteCmds.isEmpty());

        // test delete primary storage
        ClusterInventory ci = deployer.clusters.get("Cluster1");
        api.detachPrimaryStorage(ps.getUuid(), ci.getUuid());
        api.deletePrimaryStorage(ps.getUuid());

        Assert.assertTrue(config.deletePoolCmds.isEmpty());

        // test sync image size on ceph backup storage
        ImageInventory img = deployer.images.get("TestImage");
        long size = SizeUnit.GIGABYTE.toByte(2);
        bconfig.getImageSizeCmdSize.put(img.getUuid(), size);
        long asize = SizeUnit.GIGABYTE.toByte(1);
        bconfig.getImageSizeCmdActualSize.put(img.getUuid(), asize);
        img = api.syncImageSize(img.getUuid(), null);
        Assert.assertEquals(size, img.getSize());
        Assert.assertEquals(asize, img.getActualSize().longValue());

        api.deleteBackupStorage(bs.getUuid());
        Assert.assertEquals(0, dbf.count(CephBackupStorageMonVO.class));
        api.deletePrimaryStorage(ps.getUuid());
        Assert.assertEquals(0, dbf.count(CephPrimaryStorageMonVO.class));
    }
}

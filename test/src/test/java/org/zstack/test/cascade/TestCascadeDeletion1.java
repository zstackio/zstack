package org.zstack.test.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.Cluster;
import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.*;
import org.zstack.header.host.Host;
import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.network.l2.L2Network;
import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zone.ZoneEO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.AccountReferenceValidator;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 *
 * delete zone
 */
public class TestCascadeDeletion1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ZoneInventory zone = deployer.zones.get("TestZone");
        DiskOfferingInventory do1 = deployer.diskOfferings.get("TestRootDiskOffering");
        DiskOfferingInventory do2 = deployer.diskOfferings.get("TestDataDiskOffering");
        InstanceOfferingInventory io = deployer.instanceOfferings.get("TestInstanceOffering");
        BackupStorageInventory bs = deployer.backupStorages.get("TestBackupStorage");
        AccountReferenceValidator referenceValidator = new AccountReferenceValidator();
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());

        api.deleteZone(zone.getUuid());
        long count = dbf.count(ZoneVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(ClusterVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(HostVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(VmInstanceVO.class);
        Assert.assertEquals(0, count);
        referenceValidator.noReference(VmInstanceVO.class);
        count = dbf.count(PrimaryStorageVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(L2NetworkVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(L3NetworkVO.class);
        Assert.assertEquals(0, count);
        referenceValidator.noReference(L3NetworkVO.class);
        count = dbf.count(IpRangeVO.class);
        Assert.assertEquals(0, count);
        referenceValidator.noReference(IpRangeVO.class);
        DiskOfferingVO dvo = dbf.findByUuid(do1.getUuid(), DiskOfferingVO.class);
        Assert.assertNotNull(dvo);
        dvo = dbf.findByUuid(do2.getUuid(), DiskOfferingVO.class);
        Assert.assertNotNull(dvo);
        InstanceOfferingVO ivo = dbf.findByUuid(io.getUuid(), InstanceOfferingVO.class);
        Assert.assertNotNull(ivo);
        BackupStorageVO bvo = dbf.findByUuid(bs.getUuid(), BackupStorageVO.class);
        Assert.assertNotNull(bvo);

        CascadeTestHelper helper = new CascadeTestHelper();
        helper.zeroInDatabase(
                ZoneEO.class, ClusterEO.class, HostEO.class, VmInstanceEO.class,
                PrimaryStorageEO.class, L2NetworkEO.class, L3NetworkEO.class,
                IpRangeEO.class
        );
    }
}

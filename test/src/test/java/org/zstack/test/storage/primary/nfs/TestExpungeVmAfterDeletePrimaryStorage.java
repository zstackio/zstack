package org.zstack.test.storage.primary.nfs;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 9/23/16.
 */
public class TestExpungeVmAfterDeletePrimaryStorage {
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
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        api.stopVmInstance(inv.getUuid());
        inv = api.startVmInstance(inv.getUuid());
        Assert.assertEquals(VmInstanceState.Running.toString(), inv.getState());
        VmInstanceVO vm = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm);
        Assert.assertEquals(VmInstanceState.Running, vm.getState());
        Assert.assertNotNull(vm.getHostUuid());

        PrimaryStorageInventory pri = api.listPrimaryStorage(null).get(0);
        ClusterInventory cluster = api.listClusters(null).get(0);
        api.detachPrimaryStorage(pri.getUuid(), cluster.getUuid());
        api.deletePrimaryStorage(pri.getUuid());
        TimeUnit.SECONDS.sleep(3);
        VmInstanceVO vm2 = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vm2);
    }
}

package org.zstack.test.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 *
 * 1. create vm
 * 2. delete host the vm is on
 * 3. start vm
 *
 * confirm vm can start success
 */
public class TestCascadeDeletion18 {
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
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String hostUuid = vm.getHostUuid();
        api.deleteHost(hostUuid);
        vm = api.startVmInstance(vm.getUuid());
        Assert.assertEquals(VmInstanceState.Running.toString(), vm.getState());
    }
}

package org.zstack.test.storage.snapshot;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.APIReInitVmInstanceEvent;
import org.zstack.header.storage.snapshot.APIReInitVmInstanceMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/*
* 1. create a vm from a template
* 2. record current root volume install path, as A
* 3. re-init vm, make sure success
* 4. record current root volume install path, as B
* 5. confirm A is different with B
*/
public class TestReInitVMOnKvmWithCephAsPS {
    CLogger logger = Utils.getLogger(TestReInitVMOnKvmWithCephAsPS.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestReInitVmOnKvmWithCephAsPS.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }


    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String volUuid = vm.getRootVolumeUuid();
        String rootVolumeInstallPath_before = vm.getRootVolume().getInstallPath();
        //
        APIReInitVmInstanceMsg msg = new APIReInitVmInstanceMsg();
        msg.setVmInstanceUuid(vm.getUuid());
        msg.setSession(session);

        ApiSender sender = api.getApiSender();
        thrown.expect(ApiSenderException.class);
        thrown.expectMessage("is not in Stopped state");
        sender.send(msg, APIReInitVmInstanceEvent.class);

        api.stopVmInstance(vm.getUuid());
        sender.send(msg, APIReInitVmInstanceEvent.class);
        //
        String rootVolumeInstallPath_after = vm.getRootVolume().getInstallPath();
        Assert.assertTrue(!rootVolumeInstallPath_before.equals(rootVolumeInstallPath_after));
    }

}

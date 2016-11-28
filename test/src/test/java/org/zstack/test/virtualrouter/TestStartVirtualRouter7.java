package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApplianceVmVO_;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.APICreateVmInstanceEvent;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * 1. create a vr
 * 2. stop vr
 * 3. set condition which will make vr start fail
 * 4. start vr
 * <p>
 * confirm vr is stopped and KVM received stopped command
 */
public class TestStartVirtualRouter7 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;
    ApplianceVmSimulatorConfig aconfig;


    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/startVirtualRouter.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        aconfig = loader.getComponent(ApplianceVmSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException {
        ImageInventory iminv = deployer.images.get("TestImage");
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        L3NetworkInventory l3inv = deployer.l3Networks.get("TestL3Network2");
        APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
        msg.setImageUuid(iminv.getUuid());
        msg.setInstanceOfferingUuid(ioinv.getUuid());
        List<String> l3uuids = new ArrayList<String>();
        l3uuids.add(l3inv.getUuid());
        msg.setL3NetworkUuids(l3uuids);
        msg.setName("TestVm");
        msg.setSession(session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setType(VmInstanceConstant.USER_VM_TYPE);
        ApiSender sender = api.getApiSender();
        sender.send(msg, APICreateVmInstanceEvent.class);

        SimpleQuery<ApplianceVmVO> q = dbf.createQuery(ApplianceVmVO.class);
        q.add(ApplianceVmVO_.type, Op.EQ, ApplianceVmConstant.APPLIANCE_VM_TYPE);
        q.add(ApplianceVmVO_.applianceVmType, Op.EQ, VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE);
        q.add(ApplianceVmVO_.state, Op.EQ, VmInstanceState.Running);
        ApplianceVmVO vr = q.find();
        api.stopVmInstance(vr.getUuid());
        kconfig.stopVmCmds.clear();
        aconfig.refreshFirewallSuccess = false;
        api.startVmInstance(vr.getUuid());
        vr = q.find();
        Assert.assertEquals(VmInstanceState.Stopped, vr.getState());
        Assert.assertEquals(1, kconfig.stopVmCmds.size());
    }
}

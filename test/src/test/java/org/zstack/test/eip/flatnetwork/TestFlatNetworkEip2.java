package org.zstack.test.eip.flatnetwork;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.flat.FlatEipBackend.ApplyEipCmd;
import org.zstack.network.service.flat.FlatEipBackend.DeleteEipCmd;
import org.zstack.network.service.flat.FlatEipBackend.EipTO;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.network.service.vip.VipVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * @author frank
 * @condition 1. create a vm
 * 2. set eip
 * <p>
 * confirm eip works
 * <p>
 * 3. detach the eip
 * <p>
 * confirm eip detached
 * <p>
 * 4. attach the eip
 * <p>
 * confirm the eip attached
 * <p>
 * 5. delete the eip
 * <p>
 * confirm the eip deleted
 */
public class TestFlatNetworkEip2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/eip/TestFlatNetworkEip.xml", con);
        deployer.addSpringConfig("flatNetworkServiceSimulator.xml");
        deployer.addSpringConfig("flatNetworkProvider.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    private String getBridgeName(String l3uuid) {
        L3NetworkVO l3 = dbf.findByUuid(l3uuid, L3NetworkVO.class);
        return KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l3.getL2NetworkUuid(), KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        EipInventory eip = deployer.eips.get("eip");
        VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        VmNicVO nicvo = dbf.findByUuid(eip.getVmNicUuid(), VmNicVO.class);

        Assert.assertEquals(1, fconfig.deleteEipCmds.size());
        DeleteEipCmd dcmd = fconfig.deleteEipCmds.get(0);
        EipTO to = dcmd.eip;
        Assert.assertEquals(vipvo.getIp(), to.vip);
        Assert.assertEquals(nicvo.getIp(), to.nicIp);
        Assert.assertEquals(nicvo.getVmInstanceUuid(), to.vmUuid);
        Assert.assertEquals(nicvo.getMac(), to.nicMac);
        Assert.assertEquals(nicvo.getInternalName(), to.nicName);
        Assert.assertEquals(getBridgeName(nicvo.getL3NetworkUuid()), to.vmBridgeName);

        // start vm
        fconfig.applyEipCmds.clear();
        api.startVmInstance(vm.getUuid());
        Assert.assertEquals(1, fconfig.applyEipCmds.size());
        ApplyEipCmd cmd = fconfig.applyEipCmds.get(0);
        to = cmd.eip;
        Assert.assertEquals(vipvo.getIp(), to.vip);
        Assert.assertEquals(nicvo.getIp(), to.nicIp);
        Assert.assertEquals(nicvo.getVmInstanceUuid(), to.vmUuid);
        Assert.assertEquals(nicvo.getMac(), to.nicMac);
        Assert.assertEquals(nicvo.getInternalName(), to.nicName);
        Assert.assertEquals(getBridgeName(nicvo.getL3NetworkUuid()), to.vmBridgeName);

        // delete vm
        fconfig.deleteEipCmds.clear();
        api.destroyVmInstance(vm.getUuid());
        Assert.assertEquals(1, fconfig.deleteEipCmds.size());
        dcmd = fconfig.deleteEipCmds.get(0);
        to = dcmd.eip;
        Assert.assertEquals(vipvo.getIp(), to.vip);
        Assert.assertEquals(nicvo.getIp(), to.nicIp);
        Assert.assertEquals(nicvo.getVmInstanceUuid(), to.vmUuid);
        Assert.assertEquals(nicvo.getMac(), to.nicMac);
        Assert.assertEquals(nicvo.getInternalName(), to.nicName);
        Assert.assertEquals(getBridgeName(nicvo.getL3NetworkUuid()), to.vmBridgeName);
    }
}

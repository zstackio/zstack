package org.zstack.test.eip.flatnetwork;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.eip.EipVO;
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
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

/**
 * 1. create a vm with 2 EIPs
 * 2. detach one eip
 * <p>
 * confirm the eip detached
 * confirm the another eip is still there
 */
@Deprecated
public class TestFlatNetworkEip15 {
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
        deployer = new Deployer("deployerXml/eip/TestFlatNetworkEip14.xml", con);
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

    private void checkApplyEipCommand(EipInventory eip) {
        final VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        VmNicVO nicvo = dbf.findByUuid(eip.getVmNicUuid(), VmNicVO.class);

        EipTO to = CollectionUtils.find(fconfig.applyEipCmds, new Function<EipTO, ApplyEipCmd>() {
            @Override
            public EipTO call(ApplyEipCmd arg) {
                return arg.eip.vip.equals(vipvo.getIp()) ? arg.eip : null;
            }
        });

        Assert.assertNotNull(String.format("cannot find EIP for VIP[%s]", vipvo.getIp()), to);

        Assert.assertEquals(vipvo.getIp(), to.vip);
        Assert.assertEquals(nicvo.getIp(), to.nicIp);
        Assert.assertEquals(nicvo.getVmInstanceUuid(), to.vmUuid);
        Assert.assertEquals(nicvo.getMac(), to.nicMac);
        Assert.assertEquals(nicvo.getInternalName(), to.nicName);
        Assert.assertEquals(getBridgeName(nicvo.getL3NetworkUuid()), to.vmBridgeName);
        Assert.assertEquals(getBridgeName(vipvo.getL3NetworkUuid()), to.publicBridgeName);
    }

    private void checkDeleteEipCommand(EipInventory eip, String nicUuid) {
        final VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        VmNicVO nicvo = dbf.findByUuid(nicUuid, VmNicVO.class);

        EipTO to = CollectionUtils.find(fconfig.deleteEipCmds, new Function<EipTO, DeleteEipCmd>() {
            @Override
            public EipTO call(DeleteEipCmd arg) {
                return arg.eip.vip.equals(vipvo.getIp()) ? arg.eip : null;
            }
        });

        Assert.assertNotNull(String.format("cannot find EIP for VIP[%s]", vipvo.getIp()), to);

        Assert.assertEquals(vipvo.getIp(), to.vip);
        Assert.assertEquals(nicvo.getIp(), to.nicIp);
        Assert.assertEquals(nicvo.getVmInstanceUuid(), to.vmUuid);
        Assert.assertEquals(nicvo.getMac(), to.nicMac);
        Assert.assertEquals(nicvo.getInternalName(), to.nicName);
        Assert.assertEquals(getBridgeName(nicvo.getL3NetworkUuid()), to.vmBridgeName);
        Assert.assertEquals(getBridgeName(vipvo.getL3NetworkUuid()), to.publicBridgeName);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);

        EipInventory eip = deployer.eips.get("eip");
        EipInventory eip2 = deployer.eips.get("eip2");
        Assert.assertEquals(2, fconfig.applyEipCmds.size());
        checkApplyEipCommand(eip);
        checkApplyEipCommand(eip2);

        eip2 = api.detachEip(eip2.getUuid());
        Assert.assertEquals(1, fconfig.deleteEipCmds.size());
        checkDeleteEipCommand(eip2, nic.getUuid());

        EipVO eipvo1 = dbf.findByUuid(eip.getUuid(), EipVO.class);
        Assert.assertEquals(nic.getUuid(), eipvo1.getVmNicUuid());
    }
}

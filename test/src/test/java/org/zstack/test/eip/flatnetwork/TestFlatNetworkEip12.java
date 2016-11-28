package org.zstack.test.eip.flatnetwork;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.flat.FlatEipBackend.BatchDeleteEipCmd;
import org.zstack.network.service.flat.FlatEipBackend.EipTO;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.network.service.vip.VipVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 * @author frank
 * @condition 1. create a vm
 * 2. set the host disconnected
 * 3. delete the eip
 * <p>
 * confirm the eip deleted
 * <p>
 * 4. reconnect the host
 * <p>
 * confirm the eip is deleted by GC
 */
public class TestFlatNetworkEip12 {
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
    public void test() throws ApiSenderException, InterruptedException {
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false);
        kconfig.pingSuccess = false;

        TimeUnit.SECONDS.sleep(2);

        EipInventory eip = deployer.eips.get("eip");
        VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        VmNicVO nicvo = dbf.findByUuid(eip.getVmNicUuid(), VmNicVO.class);

        api.removeEip(eip.getUuid());

        Assert.assertEquals(0, fconfig.deleteEipCmds.size());

        kconfig.pingSuccess = true;
        HostInventory host = deployer.hosts.get("host1");
        api.reconnectHost(host.getUuid());

        TimeUnit.SECONDS.sleep(3);

        Assert.assertEquals(1, fconfig.batchDeleteEipCmds.size());
        BatchDeleteEipCmd cmd = fconfig.batchDeleteEipCmds.get(0);
        EipTO to = cmd.eips.get(0);
        Assert.assertEquals(vipvo.getIp(), to.vip);
        Assert.assertEquals(nicvo.getIp(), to.nicIp);
        Assert.assertEquals(nicvo.getVmInstanceUuid(), to.vmUuid);
        Assert.assertEquals(nicvo.getMac(), to.nicMac);
        Assert.assertEquals(nicvo.getInternalName(), to.nicName);
        Assert.assertEquals(getBridgeName(nicvo.getL3NetworkUuid()), to.vmBridgeName);
        Assert.assertEquals(getBridgeName(vipvo.getL3NetworkUuid()), to.publicBridgeName);
    }
}

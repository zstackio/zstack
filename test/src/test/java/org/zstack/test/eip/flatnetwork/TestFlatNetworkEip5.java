package org.zstack.test.eip.flatnetwork;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.flat.FlatEipBackend.BatchApplyEipCmd;
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

/**
 * @author frank
 * @condition 1. create a vm
 * 2. set eip
 * 3. set the vm as stopped
 * <p>
 * confirm the eip is removed
 * <p>
 * 4. set the vm as running
 * <p>
 * confirm the eip is set
 * <p>
 * 5. set vm unknown on the host2
 * 6. set the vm running on host1
 * <p>
 * confirm the eip is set on the host1 and removed from the host2
 * <p>
 * 9. set the vm running on the host1
 * 10. set the vm running on the host2
 * <p>
 * confirm the eip is set on the host2 and removed from the host1
 * <p>
 * 11. set the vm starting on the host1
 * 8. set the vm running on host1
 * <p>
 * confirm the eip is set on the host1
 */
public class TestFlatNetworkEip5 {
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
        deployer = new Deployer("deployerXml/eip/TestFlatNetworkEip4.xml", con);
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

    private void changeVmState(VmInstanceInventory vm, VmInstanceState state) {
        VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
        msg.setVmInstanceUuid(vm.getUuid());
        msg.setHostUuid(vm.getHostUuid());
        msg.setStateOnHost(state);
        msg.setVmStateAtTracingMoment(VmInstanceState.valueOf(vm.getState()));
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.getUuid());
        bus.call(msg);
    }

    private void checkApplyCmd() {
        EipInventory eip = deployer.eips.get("eip");
        VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        VmNicVO nicvo = dbf.findByUuid(eip.getVmNicUuid(), VmNicVO.class);

        Assert.assertEquals(1, fconfig.batchApplyEipCmds.size());
        BatchApplyEipCmd cmd = fconfig.batchApplyEipCmds.get(0);
        Assert.assertEquals(1, cmd.eips.size());
        EipTO to = cmd.eips.get(0);
        Assert.assertEquals(vipvo.getIp(), to.vip);
        Assert.assertEquals(nicvo.getIp(), to.nicIp);
        Assert.assertEquals(nicvo.getVmInstanceUuid(), to.vmUuid);
        Assert.assertEquals(nicvo.getMac(), to.nicMac);
        Assert.assertEquals(nicvo.getInternalName(), to.nicName);
        Assert.assertEquals(getBridgeName(nicvo.getL3NetworkUuid()), to.vmBridgeName);
        Assert.assertEquals(getBridgeName(vipvo.getL3NetworkUuid()), to.publicBridgeName);
        Assert.assertEquals(nicvo.getGateway(), to.nicGateway);
        Assert.assertEquals(nicvo.getNetmask(), to.nicNetmask);
        Assert.assertEquals(vipvo.getNetmask(), to.vipNetmask);
        Assert.assertEquals(vipvo.getGateway(), to.vipGateway);
    }

    private void checkDeleteCmd() {
        EipInventory eip = deployer.eips.get("eip");
        VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        VmNicVO nicvo = dbf.findByUuid(eip.getVmNicUuid(), VmNicVO.class);

        Assert.assertEquals(1, fconfig.batchDeleteEipCmds.size());
        BatchDeleteEipCmd dcmd = fconfig.batchDeleteEipCmds.get(0);
        Assert.assertEquals(1, dcmd.eips.size());
        EipTO to = dcmd.eips.get(0);
        Assert.assertEquals(vipvo.getIp(), to.vip);
        Assert.assertEquals(nicvo.getIp(), to.nicIp);
        Assert.assertEquals(nicvo.getVmInstanceUuid(), to.vmUuid);
        Assert.assertEquals(nicvo.getMac(), to.nicMac);
        Assert.assertEquals(nicvo.getInternalName(), to.nicName);
        Assert.assertEquals(getBridgeName(nicvo.getL3NetworkUuid()), to.vmBridgeName);
        Assert.assertEquals(getBridgeName(vipvo.getL3NetworkUuid()), to.publicBridgeName);
        Assert.assertEquals(nicvo.getGateway(), to.nicGateway);
        Assert.assertEquals(nicvo.getNetmask(), to.nicNetmask);
        Assert.assertEquals(vipvo.getNetmask(), to.vipNetmask);
        Assert.assertEquals(vipvo.getGateway(), to.vipGateway);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        fconfig.batchApplyEipCmds.clear();
        fconfig.batchDeleteEipCmds.clear();
        changeVmState(vm, VmInstanceState.Stopped);
        checkDeleteCmd();

        vm.setState(VmInstanceState.Stopped.toString());
        changeVmState(vm, VmInstanceState.Running);
        checkApplyCmd();

        // set unknown on the host2, then set running on the host1
        fconfig.batchApplyEipCmds.clear();
        fconfig.batchDeleteEipCmds.clear();
        HostInventory host2 = deployer.hosts.get("host2");
        HostInventory host1 = deployer.hosts.get("host1");
        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        vmvo.setHostUuid(host2.getUuid());
        vmvo.setState(VmInstanceState.Unknown);
        dbf.update(vmvo);

        vm.setHostUuid(host1.getUuid());
        vm.setState(VmInstanceState.Unknown.toString());
        changeVmState(vm, VmInstanceState.Running);
        checkApplyCmd();
        checkDeleteCmd();

        // abnormally migrate to the host2
        fconfig.batchApplyEipCmds.clear();
        fconfig.batchDeleteEipCmds.clear();
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        vmvo.setState(VmInstanceState.Running);
        vmvo.setHostUuid(host1.getUuid());
        dbf.update(vmvo);
        vm.setHostUuid(host2.getUuid());
        vm.setState(VmInstanceState.Running.toString());
        changeVmState(vm, VmInstanceState.Running);
        checkApplyCmd();
        checkDeleteCmd();

        // recover from an intermediate state
        fconfig.batchApplyEipCmds.clear();
        fconfig.batchDeleteEipCmds.clear();
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        vmvo.setState(VmInstanceState.Starting);
        vmvo.setHostUuid(host1.getUuid());
        dbf.update(vmvo);
        vm.setHostUuid(host1.getUuid());
        vm.setState(VmInstanceState.Starting.toString());
        changeVmState(vm, VmInstanceState.Running);
        checkApplyCmd();
    }
}

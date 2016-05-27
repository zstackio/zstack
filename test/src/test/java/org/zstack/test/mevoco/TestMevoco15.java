package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkDnsVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.kvm.KVMAgentCommands.ReportVmStateCmd;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMConstant.KvmVmState;
import org.zstack.network.service.flat.FlatDhcpBackend.ApplyDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo;
import org.zstack.network.service.flat.FlatDhcpBackend.ReleaseDhcpCmd;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. create a vm with mevoco setting
 * 2. set the vm abnormally stopped
 *
 * confirm the dhcp is released
 *
 * 3. set the vm abnormally running
 *
 * confirm the dhcp is configured
 *
 * 4. set the vm to state Starting
 * 5. set the vm abnormally running
 *
 * confirm the dhcp is configured
 *
 * 6. set the vm abnormally migrated to the host2
 *
 * confirm the dhcp released on the host1
 * confirm the dhcp configured on the host2
 *
 */
public class TestMevoco15 {
    CLogger logger = Utils.getLogger(TestMevoco15.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    HostCapacityOverProvisioningManager hostRatioMgr;
    RESTFacade restf;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco15.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        hostRatioMgr = loader.getComponent(HostCapacityOverProvisioningManager.class);
        restf = loader.getComponent(RESTFacade.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    private void checkNic(VmInstanceVO vm, List<DhcpInfo> info) {
        VmNicVO nic = vm.getVmNics().iterator().next();
        DhcpInfo target = null;
        for (DhcpInfo i : info) {
            if (i.mac.equals(nic.getMac())) {
                target = i;
                break;
            }
        }

        Assert.assertNotNull(target);
        Assert.assertEquals(nic.getIp(), target.ip);
        Assert.assertEquals(nic.getNetmask(), target.netmask);
        Assert.assertEquals(nic.getGateway(), target.gateway);
        Assert.assertEquals(true, target.isDefaultL3Network);
        String hostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);
        if (hostname == null) {
            hostname = nic.getIp().replaceAll("\\.", "-");
        }
        Assert.assertEquals(hostname, target.hostname);
        L3NetworkVO l3 = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
        Assert.assertEquals(l3.getDnsDomain(), target.dnsDomain);
        Assert.assertNotNull(target.dns);
        List<String> dns = CollectionUtils.transformToList(l3.getDns(), new Function<String, L3NetworkDnsVO>() {
            @Override
            public String call(L3NetworkDnsVO arg) {
                return arg.getDns();
            }
        });
        Assert.assertTrue(dns.containsAll(target.dns));
        Assert.assertTrue(target.dns.containsAll(dns));
    }
    
	@Test
	public void test() throws InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        fconfig.applyDhcpCmdList.clear();
        fconfig.releaseDhcpCmds.clear();
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        String hostUuid = vm.getHostUuid();

        ReportVmStateCmd cmd = new ReportVmStateCmd();
        cmd.hostUuid = hostUuid;
        cmd.vmState = KvmVmState.Shutdown.toString();
        cmd.vmUuid = vm.getUuid();
        Map<String, String> header = map(e(RESTConstant.COMMAND_PATH, KVMConstant.KVM_REPORT_VM_STATE));

        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl());
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH);
        String url = ub.build().toUriString();

        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class);
        TimeUnit.SECONDS.sleep(3);
        Assert.assertEquals(1, fconfig.releaseDhcpCmds.size());
        ReleaseDhcpCmd rcmd = fconfig.releaseDhcpCmds.get(0);
        checkNic(vmvo, rcmd.dhcp);

        cmd.vmState = KvmVmState.Running.toString();
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class);
        TimeUnit.SECONDS.sleep(3);
        Assert.assertEquals(1, fconfig.applyDhcpCmdList.size());
        ApplyDhcpCmd acmd = fconfig.applyDhcpCmdList.get(0);
        checkNic(vmvo, acmd.dhcp);

        fconfig.applyDhcpCmdList.clear();
        vmvo.setState(VmInstanceState.Starting);
        dbf.update(vmvo);
        cmd.vmState = KvmVmState.Running.toString();
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class);
        TimeUnit.SECONDS.sleep(3);
        Assert.assertEquals(1, fconfig.applyDhcpCmdList.size());
        acmd = fconfig.applyDhcpCmdList.get(0);
        checkNic(vmvo, acmd.dhcp);

        fconfig.applyDhcpCmdList.clear();
        fconfig.releaseDhcpCmds.clear();
        HostInventory host2 = deployer.hosts.get("host2");
        cmd = new ReportVmStateCmd();
        cmd.hostUuid = hostUuid;
        cmd.vmState = KvmVmState.Shutdown.toString();
        cmd.vmUuid = vm.getUuid();
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class);

        cmd.hostUuid = host2.getUuid();
        cmd.vmState = KvmVmState.Running.toString();
        cmd.vmUuid = vm.getUuid();
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class);
        TimeUnit.SECONDS.sleep(3);

        Assert.assertEquals(1, fconfig.releaseDhcpCmds.size());
        rcmd = fconfig.releaseDhcpCmds.get(0);
        checkNic(vmvo, rcmd.dhcp);

        Assert.assertEquals(1, fconfig.applyDhcpCmdList.size());
        acmd = fconfig.applyDhcpCmdList.get(0);
        checkNic(vmvo, acmd.dhcp);
    }
}

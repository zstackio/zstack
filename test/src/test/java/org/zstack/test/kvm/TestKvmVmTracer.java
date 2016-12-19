package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.KVMAgentCommands.ReportVmStateCmd;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMConstant.KvmVmState;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. create a vm
 * 2. report the vm stopped on the host
 * <p>
 * confirm the vm's state changed to stopped
 * confirm the capacity of the host returned
 * <p>
 * 3. report the vm running on the host
 * <p>
 * confirm the vm's state changed to running
 * confirm the capacity of the host allocated
 * <p>
 * 4. report the vm running on the other host
 * <p>
 * confirm the capacity of the original host returned
 * confirm the capacity of the new host allocated
 */
public class TestKvmVmTracer {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    RESTFacade restf;
    SftpBackupStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKvmVmTracer.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        restf = loader.getComponent(RESTFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String hostUuid = vm.getHostUuid();

        HostCapacityVO cap1 = dbf.findByUuid(hostUuid, HostCapacityVO.class);
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

        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Stopped, vmvo.getState());
        Assert.assertNull(vmvo.getHostUuid());

        long cpu = vmvo.getCpuNum();
        HostCapacityVO cap2 = dbf.findByUuid(hostUuid, HostCapacityVO.class);
        Assert.assertEquals(cap2.getAvailableCpu(), cap1.getAvailableCpu() + cpu);
        Assert.assertEquals(cap2.getAvailableMemory(), cap1.getAvailableMemory() + vmvo.getMemorySize());

        cmd.vmState = KvmVmState.Running.toString();
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class);
        TimeUnit.SECONDS.sleep(3);
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vmvo.getState());
        Assert.assertEquals(hostUuid, vmvo.getHostUuid());
        cap2 = dbf.findByUuid(hostUuid, HostCapacityVO.class);
        Assert.assertEquals(cap2.getAvailableCpu(), cap1.getAvailableCpu());
        Assert.assertEquals(cap2.getAvailableMemory(), cap1.getAvailableMemory());

        HostInventory host2 = deployer.hosts.get("host2");
        cmd = new ReportVmStateCmd();
        cmd.hostUuid = host2.getUuid();
        cmd.vmState = KvmVmState.Running.toString();
        cmd.vmUuid = vm.getUuid();
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class);
        TimeUnit.SECONDS.sleep(3);
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vmvo.getState());
        Assert.assertEquals(host2.getUuid(), vmvo.getHostUuid());
        HostCapacityVO host1cap = dbf.findByUuid(hostUuid, HostCapacityVO.class);
        Assert.assertEquals(host1cap.getAvailableCpu(), cap1.getAvailableCpu() + cpu);
        Assert.assertEquals(host1cap.getAvailableMemory(), cap1.getAvailableMemory() + vmvo.getMemorySize());

        HostCapacityVO host2cap = dbf.findByUuid(host2.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(host2cap.getAvailableCpu(), host2cap.getTotalCpu() - cpu);
        Assert.assertEquals(host2cap.getAvailableMemory(), host2cap.getTotalMemory() - vmvo.getMemorySize());

        cmd.vmState = KvmVmState.Paused.toString();
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class);
        TimeUnit.SECONDS.sleep(3);
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Paused, vmvo.getState());
        Assert.assertEquals(host2.getUuid(), vmvo.getHostUuid());
    }
}

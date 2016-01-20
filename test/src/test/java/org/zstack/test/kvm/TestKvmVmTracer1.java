package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.compute.vm.VmInstanceManagerImpl;
import org.zstack.core.Platform;
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
import org.zstack.kvm.KVMAgentCommands.CheckVmStateCmd;
import org.zstack.kvm.KVMAgentCommands.ReportVmStateCmd;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMConstant.KvmVmState;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. create a vm
 * 2. disconnect the host
 *
 * confirm the vm's state becomes unknown
 *
 * 3. reconnect the host
 *
 * confirm the vm's state becomes running
 *
 */

@Deprecated
public class TestKvmVmTracer1 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    RESTFacade restf;
    KVMSimulatorConfig config;
    VmInstanceManagerImpl vmMgr;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKvmVmTracer1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        restf = loader.getComponent(RESTFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        vmMgr = loader.getComponent(VmInstanceManagerImpl.class);
        session = api.loginAsAdmin();
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
    }
    
	@Test
	public void test() throws InterruptedException, ApiSenderException {
        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false);
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        config.pingSuccess = false;
        TimeUnit.SECONDS.sleep(3);

        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Unknown, vmvo.getState());
        Map<String, String> m = new HashMap<String, String>();
        m.put(vm.getUuid(), KvmVmState.Running.toString());
        config.checkVmStatesConfig.put(vm.getHostUuid(), m);
        vmMgr.managementNodeReady();
        TimeUnit.SECONDS.sleep(5);
        Assert.assertEquals(1, config.checkVmStateCmds.size());
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vmvo.getState());
    }
}

package org.zstack.test.console;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.console.ConsoleProxyStatus;
import org.zstack.header.console.ConsoleProxyVO;
import org.zstack.header.console.ConsoleProxyVO_;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.consoleproxy.ConsoleProxySimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

public class TestConsoleProxy5 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    ConsoleProxySimulatorConfig consoleConfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestMigrateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ConsoleManager.xml");
        deployer.addSpringConfig("ConsoleSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        consoleConfig = loader.getComponent(ConsoleProxySimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    private ConsoleProxyVO getConsoleVO(String vmUuid) {
        SimpleQuery<ConsoleProxyVO> q = dbf.createQuery(ConsoleProxyVO.class);
        q.add(ConsoleProxyVO_.vmInstanceUuid, Op.EQ, vmUuid);
        q.add(ConsoleProxyVO_.status, Op.EQ, ConsoleProxyStatus.Active);
        return q.find();
    }

    @Test
    public void test() throws ApiSenderException {
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.getConsole(vm.getUuid());
        HostVO currentHost = dbf.findByUuid(vm.getHostUuid(), HostVO.class);
        ConsoleProxyVO cvo = getConsoleVO(vm.getUuid());
        Assert.assertEquals(currentHost.getManagementIp(), cvo.getTargetHostname());

        HostInventory target = CollectionUtils.find(deployer.hosts.values(), new Function<HostInventory, HostInventory>() {
            @Override
            public HostInventory call(HostInventory arg) {
                if (!arg.getUuid().equals(vm.getHostUuid())) {
                    return arg;
                }
                return null;
            }
        });

        VmInstanceInventory vminv = api.migrateVmInstance(vm.getUuid(), target.getUuid());
        cvo = getConsoleVO(vm.getUuid());
        Assert.assertNull(cvo);
        Assert.assertEquals(1, consoleConfig.deleteProxyCmdList.size());
        api.getConsole(vm.getUuid());
        cvo = getConsoleVO(vm.getUuid());
        Assert.assertEquals(target.getManagementIp(), cvo.getTargetHostname());
    }
}

package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
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

public class TestAttachNicOnKvm {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestAttachNicOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network4");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        vm = api.attachNic(vm.getUuid(), l3.getUuid());
        Assert.assertEquals(4, vm.getVmNics().size());

        VmNicVO nic = Q.New(VmNicVO.class)
                .eq(VmNicVO_.l3NetworkUuid, l3.getUuid())
                .eq(VmNicVO_.vmInstanceUuid, vm.getUuid())
                .find();

        Assert.assertEquals(3, nic.getDeviceId());

        KVMAgentCommands.NicTO to = CollectionUtils.find(config.attachedNics.values(), new Function<KVMAgentCommands.NicTO, KVMAgentCommands.NicTO>() {
            @Override
            public KVMAgentCommands.NicTO call(KVMAgentCommands.NicTO arg) {
                if (arg.getNicInternalName().equals(nic.getInternalName())) {
                    return arg;
                }
                return null;
            }
        });

        Assert.assertNotNull(to);

        long count = dbf.count(VirtualRouterVmVO.class);
        Assert.assertEquals(1, count);
    }
}

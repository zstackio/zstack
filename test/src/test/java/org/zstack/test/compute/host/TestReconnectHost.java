package org.zstack.test.compute.host;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.APIReconnectHostEvent;
import org.zstack.header.host.APIReconnectHostMsg;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm
 * 2. mark vm as destroyed
 * 3. reconnect the host
 * <p>
 * confirm the host capacity not used
 */
public class TestReconnectHost {
    CLogger logger = Utils.getLogger(TestReconnectHost.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    //SftpBackupStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HostInventory inv = deployer.hosts.get("host1");
        //VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        HostVO hovo = dbf.findByUuid(inv.getUuid(),HostVO.class);

        String hostUuid = inv.getUuid();
        APIReconnectHostMsg msg=new APIReconnectHostMsg();
        msg.setSession(session);
        msg.setTimeout(1500);
        msg.setUuid(hostUuid);
        ApiSender sender = new ApiSender();
        APIReconnectHostEvent evt = sender.send(msg,APIReconnectHostEvent.class);
        Assert.assertNotNull(evt.getInventory());
    }

}

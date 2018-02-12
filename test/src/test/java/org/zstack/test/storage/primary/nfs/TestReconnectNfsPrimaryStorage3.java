package org.zstack.test.storage.primary.nfs;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.ReconnectHostMsg;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * 1. resize the nfs primary storage
 * 2. reconnect the nfs primary storage
 * <p>
 * confirm the remount command is sent
 * confirm the nfs capacity is extended
 */
public class TestReconnectNfsPrimaryStorage3 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig config;
    boolean success1 = false;
    boolean success2 = false;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/nfsPrimaryStorage/TestReconnectNfsPrimaryStorage.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        PrimaryStorageInventory ps = deployer.primaryStorages.get("nfs");
        config.totalCapacity = SizeUnit.TERABYTE.toByte(2);
        config.availableCapacity = SizeUnit.GIGABYTE.toByte(10);
        config.remountSuccess = false;

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                success1 = true;
            }
        }, ReconnectHostMsg.class);

        PrimaryStorageCapacityVO cap1 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);

        try {
            api.reconnectPrimaryStorage(ps.getUuid());
        } catch (ApiSenderException e) {
            success2 = true;
        }

        TimeUnit.SECONDS.sleep(2);

        Assert.assertTrue(success1);
        Assert.assertTrue(success2);

        PrimaryStorageCapacityVO cap2 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(cap1.getTotalCapacity(), cap2.getTotalCapacity());
        Assert.assertEquals(cap1.getTotalPhysicalCapacity(), cap2.getTotalPhysicalCapacity());
        Assert.assertEquals(cap1.getAvailableCapacity(), cap2.getAvailableCapacity());
        Assert.assertEquals(cap1.getAvailablePhysicalCapacity(), cap2.getAvailablePhysicalCapacity());

        HostInventory host = deployer.hosts.get("host1");
        HostVO hostvo = dbf.findByUuid(host.getUuid(), HostVO.class);
        Assert.assertEquals(HostStatus.Disconnected, hostvo.getStatus());
    }
}

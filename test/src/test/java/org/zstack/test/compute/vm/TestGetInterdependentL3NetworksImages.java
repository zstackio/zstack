package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.APIGetInterdependentL3NetworkImageReply;
import org.zstack.header.vm.APIGetInterdependentL3NetworksImagesMsg;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import static java.util.Arrays.asList;

public class TestGetInterdependentL3NetworksImages {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig lconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/vm/TestGetInterdependentL3NetworksImages.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("smpPrimaryStorageSimulator.xml");
        deployer.addSpringConfig("sharedMountPointPrimaryStorage.xml");
        deployer.load();
        loader = deployer.getComponentLoader();
        lconfig = loader.getComponent(LocalStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = c.avail = SizeUnit.GIGABYTE.toByte(1000);
        lconfig.capacityMap.put("host1", c);
        lconfig.capacityMap.put("host2", c);
        lconfig.capacityMap.put("host3", c);
        lconfig.capacityMap.put("host4", c);

        deployer.build();
        api = deployer.getApi();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = deployer.zones.get("Zone1");
        ImageInventory imgOnSftp = deployer.images.get("TestImage");
        ImageInventory imgOnCeph = deployer.images.get("TestImage1");
        L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");
        L3NetworkInventory l33 = deployer.l3Networks.get("TestL3Network3");
        L3NetworkInventory l34 = deployer.l3Networks.get("TestL3Network4");

        APIGetInterdependentL3NetworksImagesMsg msg = new APIGetInterdependentL3NetworksImagesMsg();
        msg.setZoneUuid(zone.getUuid());
        msg.setImageUuid(imgOnSftp.getUuid());
        msg.setSession(api.getAdminSession());
        ApiSender sender = api.getApiSender();
        APIGetInterdependentL3NetworkImageReply reply = sender.call(msg, APIGetInterdependentL3NetworkImageReply.class);
        Assert.assertEquals(3, reply.getInventories().size());

        // network 1, 2, 4
        Assert.assertTrue(reply.getInventories().stream().filter(o -> {
            L3NetworkInventory l3 = (L3NetworkInventory) o;
            return l3.getUuid().equals(l31.getUuid());
        }).findAny().isPresent());
        Assert.assertTrue(reply.getInventories().stream().filter(o -> {
            L3NetworkInventory l3 = (L3NetworkInventory) o;
            return l3.getUuid().equals(l32.getUuid());
        }).findAny().isPresent());
        Assert.assertTrue(reply.getInventories().stream().filter(o -> {
            L3NetworkInventory l3 = (L3NetworkInventory) o;
            return l3.getUuid().equals(l34.getUuid());
        }).findAny().isPresent());

        msg = new APIGetInterdependentL3NetworksImagesMsg();
        msg.setZoneUuid(zone.getUuid());
        msg.setImageUuid(imgOnCeph.getUuid());
        msg.setSession(api.getAdminSession());
        sender = api.getApiSender();
        reply = sender.call(msg, APIGetInterdependentL3NetworkImageReply.class);
        Assert.assertEquals(2, reply.getInventories().size());
        // only network 1, 3
        Assert.assertTrue(reply.getInventories().stream().filter(o -> {
            L3NetworkInventory l3 = (L3NetworkInventory) o;
            return l3.getUuid().equals(l33.getUuid());
        }).findAny().isPresent());
        Assert.assertTrue(reply.getInventories().stream().filter(o -> {
            L3NetworkInventory l3 = (L3NetworkInventory) o;
            return l3.getUuid().equals(l31.getUuid());
        }).findAny().isPresent());

        msg = new APIGetInterdependentL3NetworksImagesMsg();
        msg.setZoneUuid(zone.getUuid());
        msg.setL3NetworkUuids(asList(l31.getUuid()));
        msg.setSession(api.getAdminSession());
        sender = api.getApiSender();
        reply = sender.call(msg, APIGetInterdependentL3NetworkImageReply.class);
        Assert.assertEquals(2, reply.getInventories().size());
        Assert.assertTrue(reply.getInventories().stream().filter(o -> {
            ImageInventory i = (ImageInventory) o;
            return i.getUuid().equals(imgOnCeph.getUuid());
        }).findAny().isPresent());
        Assert.assertTrue(reply.getInventories().stream().filter(o -> {
            ImageInventory i = (ImageInventory) o;
            return i.getUuid().equals(imgOnSftp.getUuid());
        }).findAny().isPresent());

        msg = new APIGetInterdependentL3NetworksImagesMsg();
        msg.setZoneUuid(zone.getUuid());
        msg.setL3NetworkUuids(asList(l31.getUuid(), l32.getUuid()));
        msg.setSession(api.getAdminSession());
        sender = api.getApiSender();
        reply = sender.call(msg, APIGetInterdependentL3NetworkImageReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        Assert.assertTrue(reply.getInventories().stream().filter(o -> {
            ImageInventory i = (ImageInventory) o;
            return i.getUuid().equals(imgOnSftp.getUuid());
        }).findAny().isPresent());

        msg = new APIGetInterdependentL3NetworksImagesMsg();
        msg.setZoneUuid(zone.getUuid());
        msg.setL3NetworkUuids(asList(l31.getUuid(), l32.getUuid(), l33.getUuid(), l34.getUuid()));
        msg.setSession(api.getAdminSession());
        sender = api.getApiSender();
        reply = sender.call(msg, APIGetInterdependentL3NetworkImageReply.class);
        Assert.assertEquals(0, reply.getInventories().size());

        ImageInventory image2 = deployer.images.get("TestImage2");
        L3NetworkInventory l3Network5 = deployer.l3Networks.get("TestL3Network5");
        msg = new APIGetInterdependentL3NetworksImagesMsg();
        msg.setZoneUuid(zone.getUuid());
        msg.setImageUuid(image2.getUuid());
        msg.setSession(api.getAdminSession());
        sender = api.getApiSender();
        reply = sender.call(msg, APIGetInterdependentL3NetworkImageReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        L3NetworkInventory l35 = (L3NetworkInventory) reply.getInventories().get(0);
        Assert.assertEquals(l3Network5.getUuid(), l35.getUuid());
    }

}

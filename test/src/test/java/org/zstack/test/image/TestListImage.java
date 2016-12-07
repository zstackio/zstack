package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageDetails;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.test.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import java.util.List;

public class TestListImage {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    int testNum = 10;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml").addXml("BackupStorageManager.xml")
                .addXml("ImageManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException {
        SimulatorBackupStorageDetails ss = new SimulatorBackupStorageDetails();
        ss.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100));
        ss.setUsedCapacity(0);
        ss.setUrl("nfs://simulator/backupstorage/");
        BackupStorageInventory inv = api.createSimulatorBackupStorage(1, ss).get(0);
        BackupStorageVO vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertNotNull(vo);

        for (int i = 0; i < testNum; i++) {
            ImageInventory iinv = new ImageInventory();
            iinv.setName("Test Image");
            iinv.setDescription("Test Image");
            iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
            iinv.setGuestOsType("Window7");
            iinv.setFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING);
            iinv.setUrl(String.format("http://zstack.org/download/%s/win7.qcow2", i));
            api.addImage(iinv, inv.getUuid());
        }

        List<ImageInventory> images = api.listImage(null);
        Assert.assertEquals(testNum, images.size());

        List<String> uuids = CollectionUtils.transformToList(images, new Function<String, ImageInventory>() {
            @Override
            public String call(ImageInventory arg) {
                return arg.getUuid();
            }
        });
        images = api.listImage(uuids);
        for (int i = 0; i < testNum; i++) {
            Assert.assertEquals(uuids.get(i), images.get(i).getUuid());
        }
    }
}

package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageDetails;
import org.zstack.header.storage.backup.BackupStorage;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestDeleteImage2 {
    CLogger logger = Utils.getLogger(TestDeleteImage2.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
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
        BackupStorageInventory bs = api.createSimulatorBackupStorage(5, ss).get(0);

        ImageInventory iinv = new ImageInventory();
        iinv.setName("Test Image");
        iinv.setDescription("Test Image");
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setGuestOsType("Window7");
        iinv.setFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING);
        iinv.setUrl("http://zstack.org/download/win7.qcow2");
        List<BackupStorageVO> bsvos = dbf.listAll(BackupStorageVO.class);
        List<String> bsUuids = CollectionUtils.transformToList(bsvos, new Function<String, BackupStorageVO>() {
            @Override
            public String call(BackupStorageVO arg) {
                return arg.getUuid();
            }
        });
        iinv = api.addImage(iinv, bsUuids.toArray(new String[bsUuids.size()]));

        ImageVO ivo = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNotNull(ivo);
        api.deleteImage(ivo.getUuid(), Arrays.asList(bs.getUuid()));
        ivo = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNotNull(ivo);
        Assert.assertEquals(4, ivo.getBackupStorageRefs().size());
        for (ImageBackupStorageRefVO ref : ivo.getBackupStorageRefs()) {
            if (ref.getBackupStorageUuid().equals(bs.getUuid())) {
                Assert.fail(String.format("image is still on backup storage %s", bs.getUuid()));
            }
        }
    }
}

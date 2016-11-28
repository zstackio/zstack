package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.APIAddSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SftpBackupStorageTestHelper {
    @Autowired
    private SftpBackupStorageSimulatorConfig config;
    @Autowired
    private DatabaseFacade dbf;

    public SftpBackupStorageInventory addSimpleHttpBackupStorage(Api api) throws ApiSenderException {
        APIAddSftpBackupStorageMsg msg = new APIAddSftpBackupStorageMsg();
        msg.setSession(api.getAdminSession());
        msg.setName("TestBackupStorage");
        msg.setUrl("/backupstorage");
        msg.setType(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
        msg.setHostname("localhost");
        msg.setUsername("root");
        msg.setPassword("password");
        ApiSender sender = api.getApiSender();
        APIAddBackupStorageEvent evt = sender.send(msg, APIAddBackupStorageEvent.class);
        BackupStorageInventory inv = evt.getInventory();
        Assert.assertEquals(inv.getTotalCapacity(), config.totalCapacity);
        SftpBackupStorageVO vo = dbf.findByUuid(inv.getUuid(), SftpBackupStorageVO.class);
        Assert.assertEquals(vo.getTotalCapacity(), config.totalCapacity);
        Assert.assertEquals(vo.getAvailableCapacity(), config.availableCapacity);
        Assert.assertEquals(vo.getHostname(), "localhost");
        return JSONObjectUtil.rehashObject(inv, SftpBackupStorageInventory.class);
    }

    public ImageInventory addImage(Api api, SftpBackupStorageInventory sinv) throws ApiSenderException {
        config.downloadSuccess1 = true;
        config.downloadSuccess2 = true;
        config.imageMd5sum = Platform.getUuid();
        ImageInventory iinv = new ImageInventory();
        iinv.setUuid(Platform.getUuid());
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
        iinv.setGuestOsType("CentOS6.3");
        iinv.setName("TestImage");
        iinv.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
        iinv.setUrl("http://zstack.org/download/testimage.qcow2");

        long size = SizeUnit.GIGABYTE.toByte(8);
        config.imageSizes.put(iinv.getUuid(), size);
        long asize = SizeUnit.GIGABYTE.toByte(4);
        config.imageActualSizes.put(iinv.getUuid(), asize);

        iinv = api.addImage(iinv, sinv.getUuid());
        Assert.assertEquals(size, iinv.getSize());
        Assert.assertEquals(asize, iinv.getActualSize().longValue());
        Assert.assertEquals(config.imageMd5sum, iinv.getMd5Sum());
        for (ImageBackupStorageRefInventory ref : iinv.getBackupStorageRefs()) {
            Assert.assertNotNull(ref.getInstallPath());
        }
        ImageVO vo = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertEquals(size, vo.getSize());
        Assert.assertEquals(config.imageMd5sum, vo.getMd5Sum());
        Assert.assertNotNull(vo.getBackupStorageRefs().iterator().next().getInstallPath());
        return iinv;
    }
}

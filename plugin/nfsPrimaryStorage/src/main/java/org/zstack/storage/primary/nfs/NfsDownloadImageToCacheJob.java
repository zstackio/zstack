package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsDownloadImageToCacheJob implements Job {
    private static final CLogger logger = Utils.getLogger(NfsDownloadImageToCacheJob.class);

    @JobContext
    private ImageSpec image;
    @JobContext
    private PrimaryStorageInventory primaryStorage;

    @Autowired
    private NfsPrimaryStorageFactory nfsFactory;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private NfsPrimaryStorageManager nfsMgr;
    @Autowired
    private CloudBus bus;

    @Override
    public void run(final ReturnValueCompletion<Object> completion) {
        SimpleQuery<ImageCacheVO> query = dbf.createQuery(ImageCacheVO.class);
        query.add(ImageCacheVO_.primaryStorageUuid, SimpleQuery.Op.EQ, primaryStorage.getUuid());
        query.add(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, image.getInventory().getUuid());
        ImageCacheVO cvo = query.find();
        if (cvo != null) {
            useExistingCache(cvo, completion);
            return;
        }

        download(completion);
    }

    private void download(final ReturnValueCompletion<Object> completion) {
        BackupStorageVO bsvo = dbf.findByUuid(image.getSelectedBackupStorage().getBackupStorageUuid(), BackupStorageVO.class);
        BackupStorageInventory backupStorage = BackupStorageInventory.valueOf(bsvo);
        NfsPrimaryToBackupStorageMediator mediator = nfsFactory.getPrimaryToBackupStorageMediator(
                BackupStorageType.valueOf(backupStorage.getType()),
                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(image.getInventory().getFormat(), primaryStorage.getUuid())
        );

        final String cacheInstallPath = NfsPrimaryStorageKvmHelper.makeCachedImageInstallUrl(primaryStorage, image.getInventory());
        mediator.downloadBits(primaryStorage, backupStorage, image.getSelectedBackupStorage().getInstallPath(), cacheInstallPath, new Completion(completion) {
            @Override
            public void success() {
                ImageCacheVO cvo = new ImageCacheVO();
                cvo.setImageUuid(image.getInventory().getUuid());
                cvo.setInstallUrl(cacheInstallPath);
                cvo.setMd5sum("no md5");
                cvo.setPrimaryStorageUuid(primaryStorage.getUuid());
                cvo.setSize(image.getInventory().getSize());
                cvo.setMediaType(ImageMediaType.valueOf(image.getInventory().getMediaType()));
                cvo = dbf.persistAndRefresh(cvo);
                logger.debug(String.format("successfully downloaded image[uuid:%s] in image cache[id:%s, path:%s]",
                        image.getInventory().getUuid(), cvo.getId(), cvo.getInstallUrl()));

                TakePrimaryStorageCapacityMsg msg = new TakePrimaryStorageCapacityMsg();
                msg.setPrimaryStorageUuid(primaryStorage.getUuid());
                msg.setSize(image.getInventory().getSize());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorage.getUuid());
                bus.send(msg);

                completion.success(ImageCacheInventory.valueOf(cvo));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void useExistingCache(final ImageCacheVO cvo, final ReturnValueCompletion<Object> completion) {
        NfsPrimaryStorageBackend bkd = nfsFactory.getHypervisorBackend(nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(image.getInventory().getFormat(), primaryStorage.getUuid()));
        bkd.checkIsBitsExisting(primaryStorage, cvo.getInstallUrl(), new ReturnValueCompletion<Boolean>(completion) {
            @Override
            public void success(Boolean returnValue) {
                if (returnValue) {
                    logger.debug(String.format("found image[uuid:%s] in image cache[id:%s, path:%s]",
                            image.getInventory().getUuid(), cvo.getId(), cvo.getInstallUrl()));
                    completion.success(ImageCacheInventory.valueOf(cvo));
                    return;
                }

                dbf.remove(cvo);
                download(completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public ImageSpec getImage() {
        return image;
    }

    public void setImage(ImageSpec image) {
        this.image = image;
    }

    public PrimaryStorageInventory getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(PrimaryStorageInventory primaryStorage) {
        this.primaryStorage = primaryStorage;
    }
}

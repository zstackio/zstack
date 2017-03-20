package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

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
        final BackupStorageInventory backupStorage = BackupStorageInventory.valueOf(bsvo);
        final NfsPrimaryToBackupStorageMediator mediator = nfsFactory.getPrimaryToBackupStorageMediator(
                BackupStorageType.valueOf(backupStorage.getType()),
                nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(image.getInventory().getFormat(), primaryStorage.getUuid())
        );

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-image-%s-to-nfs-primary-storage-%s-cache", image.getInventory().getUuid(), primaryStorage.getUuid()));
        chain.then(new ShareFlow() {
            String cacheInstallPath = NfsPrimaryStorageKvmHelper.makeCachedImageInstallUrl(primaryStorage, image.getInventory());

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-primary-storage";

                    boolean s = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                        amsg.setRequiredPrimaryStorageUuid(primaryStorage.getUuid());
                        amsg.setSize(image.getInventory().getActualSize());
                        amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadImage.toString());
                        amsg.setNoOverProvisioning(true);
                        bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    s = true;
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (s) {
                            IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                            imsg.setDiskSize(image.getInventory().getActualSize());
                            imsg.setNoOverProvisioning(true);
                            imsg.setPrimaryStorageUuid(primaryStorage.getUuid());
                            bus.makeLocalServiceId(imsg, PrimaryStorageConstant.SERVICE_ID);
                            bus.send(imsg);
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "download";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        mediator.downloadBits(primaryStorage, backupStorage, image.getSelectedBackupStorage().getInstallPath(), cacheInstallPath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        ImageCacheVO cvo = new ImageCacheVO();
                        cvo.setImageUuid(image.getInventory().getUuid());
                        cvo.setInstallUrl(cacheInstallPath);
                        cvo.setMd5sum("no md5");
                        cvo.setPrimaryStorageUuid(primaryStorage.getUuid());
                        cvo.setSize(image.getInventory().getActualSize());
                        cvo.setMediaType(ImageMediaType.valueOf(image.getInventory().getMediaType()));
                        cvo = dbf.persistAndRefresh(cvo);
                        logger.debug(String.format("successfully downloaded image[uuid:%s] in image cache[id:%s, path:%s]",
                                image.getInventory().getUuid(), cvo.getId(), cvo.getInstallUrl()));

                        completion.success(ImageCacheInventory.valueOf(cvo));
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
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

                // return capacity and re-download
                IncreasePrimaryStorageCapacityMsg rmsg = new IncreasePrimaryStorageCapacityMsg();
                rmsg.setPrimaryStorageUuid(cvo.getPrimaryStorageUuid());
                rmsg.setDiskSize(cvo.getSize());
                bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                bus.send(rmsg);
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

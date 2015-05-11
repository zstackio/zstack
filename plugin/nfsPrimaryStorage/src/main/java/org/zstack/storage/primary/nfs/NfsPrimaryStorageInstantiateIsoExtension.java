package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMIsoTO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsPrimaryStorageInstantiateIsoExtension implements PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(NfsPrimaryStorageInstantiateIsoExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private JobQueueFacade jobf;

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
    }

    @Override
    public void preInstantiateVmResource(final VmInstanceSpec spec, final Completion completion) {
        final ImageInventory image = spec.getImageSpec().getInventory();
        if (image == null || image.getMediaType().equals(ImageMediaType.RootVolumeTemplate.toString()) || spec.getCurrentVmOperation() != VmOperation.NewCreate) {
            completion.success();
            return;
        }

        VolumeSpec vspec = spec.getVolumeSpecs().get(0);
        PrimaryStorageInventory pinv = vspec.getPrimaryStorageInventory();
        if (!pinv.getType().equals(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)) {
            completion.success();
            return;
        }

        SimpleQuery<ImageCacheVO> query = dbf.createQuery(ImageCacheVO.class);
        query.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
        query.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, pinv.getUuid());
        ImageCacheVO cvo = query.find();
        if (cvo != null) {
            KVMIsoTO isoto = new KVMIsoTO(image);
            isoto.setPathInCache(cvo.getInstallUrl());
            isoto.setInstallUrl(spec.getImageSpec().getSelectedBackupStorage().getInstallPath());
            spec.putExtensionData(KVMConstant.ISO_TO, isoto);
            completion.success();
            return;
        }

        NfsDownloadImageToCacheJob job = new NfsDownloadImageToCacheJob();
        job.setPrimaryStorage(pinv);
        job.setImage(spec.getImageSpec());

        final PrimaryStorageInventory fpinv = pinv;
        jobf.execute(NfsPrimaryStorageKvmHelper.makeDownloadImageJobName(image, pinv),
                NfsPrimaryStorageKvmHelper.makeJobOwnerName(pinv), job,
                new ReturnValueCompletion<ImageCacheInventory>(completion) {

                    @Override
                    public void success(ImageCacheInventory returnValue) {
                        logger.debug(String.format("successfully downloaded iso[uuid:%s, name:%s] from backup storage[uuid:%s] to primary storage[uuid:%s, name:%s], path in cache: %s",
                                image.getUuid(), image.getName(), spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid(), fpinv.getUuid(), fpinv.getName(), returnValue.getInstallUrl()));

                        KVMIsoTO isoto = new KVMIsoTO(image);
                        isoto.setPathInCache(returnValue.getInstallUrl());
                        spec.putExtensionData(KVMConstant.ISO_TO, isoto);
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        String err = String.format("failed to downloaded iso[uuid:%s, name:%s] from backup storage[uuid:%s] to primary storage[uuid:%s, name:%s]",
                                image.getUuid(), image.getName(), spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid(), fpinv.getUuid(), fpinv.getName());
                        logger.warn(err);
                        completion.fail(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR, err, errorCode));
                    }
                }, ImageCacheInventory.class);

    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }
}

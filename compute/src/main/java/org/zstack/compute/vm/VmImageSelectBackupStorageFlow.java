package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;

import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.taskProgress;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmImageSelectBackupStorageFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private String findBackupStorage(VmInstanceSpec spec, String imageUuid) {
        taskProgress("Choose backup storage for downloading the image");

        spec.getImageSpec().setNeedDownload(imageNeedDownload(spec, imageUuid));
        if (!spec.getImageSpec().isNeedDownload() && spec.getImageSpec().getInventory().getBackupStorageRefs().isEmpty()) {
            return null;
        }

        if (spec.getImageSpec().getInventory().getBackupStorageRefs().size() == 1) {
            return spec.getImageSpec().getInventory().getBackupStorageRefs().iterator().next().getBackupStorageUuid();
        }

        DebugUtils.Assert(spec.getVmInventory().getZoneUuid() != null, "zone uuid must be set if the image is on multiple backup storage");

        ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
        selector.setZoneUuid(spec.getVmInventory().getZoneUuid());
        selector.setImageUuid(imageUuid);
        String bsUuid = selector.select();

        if (bsUuid != null) {
            return bsUuid;
        }

        if (!spec.getImageSpec().isNeedDownload()) {
            // the image is already on the primary storage,
            // in this case, the backup storage needs not to be Connected
            selector.setCheckStatus(false);
            bsUuid = selector.select();
            if (bsUuid != null) {
                return bsUuid;
            }
        }

        if (spec.getVmInventory().getZoneUuid() != null) {
            throw new OperationFailureException(operr("cannot find the image[uuid:%s] in any connected backup storage attached to the zone[uuid:%s]. check below:\n" +
                                    "1. if the backup storage is attached to the zone where the VM[name: %s, uuid:%s] is in\n" +
                                    "2. if the backup storage is in connected status, if not, try reconnecting it",
                            imageUuid, spec.getVmInventory().getZoneUuid(), spec.getVmInventory().getName(), spec.getVmInventory().getUuid())
            );
        } else {
            throw new OperationFailureException(operr("cannot find the image[uuid:%s] in any connected backup storage. check below:\n" +
                                    "1. if the backup storage is attached to the zone where the VM[name: %s, uuid:%s] is in\n" +
                                    "2. if the backup storage is in connected status, if not, try reconnecting it",
                            imageUuid, spec.getVmInventory().getName(), spec.getVmInventory().getUuid())
            );
        }
    }

    private boolean imageNeedDownload(VmInstanceSpec spec, String imageUuid) {
        List<String> psUuid;
        if (VmOperation.NewCreate == spec.getCurrentVmOperation()) {
            psUuid = spec.getVolumeSpecs().isEmpty() ? spec.getCandidatePrimaryStorageUuidsForRootVolume() :
                    Collections.singletonList(spec.getVolumeSpecs().get(0).getPrimaryStorageInventory().getUuid());
        } else {
            psUuid = Collections.singletonList(spec.getVmInventory().getRootVolume().getPrimaryStorageUuid());
        }

        if (psUuid.isEmpty()) {
            return true;
        }

        List<String> hasImageCachePsUuids = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, imageUuid)
                .in(ImageCacheVO_.primaryStorageUuid, psUuid)
                .select(ImageCacheVO_.primaryStorageUuid)
                .listValues();

        return new HashSet<>(hasImageCachePsUuids).size() < psUuid.size();
    }

    @Transactional(readOnly = true)
    private String findIsoBsUuidInTheZone(final String isoImageUuid, final String zoneUuid) {
        String sql = "select ref.backupStorageUuid" +
                " from ImageBackupStorageRefVO ref, BackupStorageZoneRefVO zoneref" +
                " where ref.backupStorageUuid = zoneref.backupStorageUuid" +
                " and zoneref.zoneUuid = :zoneUuid" +
                " and ref.imageUuid = :imgUuid";

        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("zoneUuid", zoneUuid);
        q.setParameter("imgUuid", isoImageUuid);
        q.setMaxResults(1);
        List<String> ret = q.getResultList();
        if (ret.isEmpty()) {
            throw new OperationFailureException(operr("no backup storage attached to the zone[uuid:%s] contains the ISO[uuid:%s]",
                            zoneUuid, isoImageUuid));
        }

        return ret.get(0);
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        if (VmOperation.NewCreate == spec.getCurrentVmOperation()
                || VmOperation.ChangeImage == spec.getCurrentVmOperation()) {
            if (spec.getImageSpec().getInventory() == null) {
                trigger.next();
                return;
            }

            final String bsUuid = findBackupStorage(spec, spec.getImageSpec().getInventory().getUuid());
            spec.getImageSpec().setSelectedBackupStorage(CollectionUtils.find(
                    spec.getImageSpec().getInventory().getBackupStorageRefs(),
                    new Function<ImageBackupStorageRefInventory, ImageBackupStorageRefInventory>() {
                        @Override
                        public ImageBackupStorageRefInventory call(ImageBackupStorageRefInventory arg) {
                            return arg.getBackupStorageUuid().equals(bsUuid)
                                    && ImageStatus.Ready.toString().equals(arg.getStatus())
                                    ? arg : null;
                        }
                    }));

            if (ImageMediaType.ISO.toString().equals(spec.getImageSpec().getInventory().getMediaType())) {
                spec.getCdRomSpecs().get(0).setBackupStorageUuid(bsUuid);
            }

            spec.getCdRomSpecs().forEach(cdRomSpec -> {
                if (cdRomSpec.getBackupStorageUuid() != null) {
                    return;
                }
                if (cdRomSpec.getImageUuid() == null) {
                    return;
                }
                cdRomSpec.setBackupStorageUuid(
                        findIsoBsUuidInTheZone(cdRomSpec.getImageUuid(), spec.getVmInventory().getZoneUuid())
                );
            });
        } else if ((VmOperation.Start == spec.getCurrentVmOperation()
                || VmOperation.Reboot == spec.getCurrentVmOperation())
                && !spec.getCdRomSpecs().isEmpty()) {
            spec.getCdRomSpecs().forEach(cdRomSpec -> {
                if (cdRomSpec.getImageUuid() == null) {
                    return;
                }
                cdRomSpec.setBackupStorageUuid(
                        findIsoBsUuidInTheZone(cdRomSpec.getImageUuid(), spec.getVmInventory().getZoneUuid())
                );
            });
        }

        trigger.next();
    }
}

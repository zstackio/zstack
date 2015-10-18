package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmImageSelectBackupStorageFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private String findBackupStorage(VmInstanceSpec spec, String imageUuid) {
        String zoneUuid = spec.getDestHost().getZoneUuid();
        ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
        selector.setZoneUuid(zoneUuid);
        selector.setImageUuid(imageUuid);
        String bsUuid = selector.select();

        if (bsUuid != null) {
            return bsUuid;
        }

        String psUuid;
        if (VmOperation.NewCreate == spec.getCurrentVmOperation()) {
            VolumeSpec rootVolumeSpec = spec.getVolumeSpecs().get(0);
            psUuid = rootVolumeSpec.getPrimaryStorageInventory().getUuid();
        } else {
            psUuid = spec.getVmInventory().getRootVolume().getPrimaryStorageUuid();
        }

        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, imageUuid);
        q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, psUuid);
        if (q.isExists()) {
            // the image is already on the primary storage,
            // in this case, the backup storage needs not to be Connected
            selector.setCheckStatus(false);
            bsUuid = selector.select();
            if (bsUuid != null) {
                return bsUuid;
            }
        }

        throw new OperationFailureException(errf.stringToOperationError(
                String.format("cannot find the image[uuid:%s] in any connected backup storage attached to the zone[uuid:%s]. check below:\n" +
                                "1. if the backup storage is attached to the zone where the VM[name: %s, uuid:%s] is in\n" +
                                "2. if the backup storage is in connected status, if not, try reconnecting it",
                        imageUuid, zoneUuid, spec.getVmInventory().getName(), spec.getVmInventory().getUuid())
        ));
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final String bsUuid = findBackupStorage(spec, spec.getImageSpec().getInventory().getUuid());
        spec.getImageSpec().setSelectedBackupStorage(CollectionUtils.find(spec.getImageSpec().getInventory().getBackupStorageRefs(), new Function<ImageBackupStorageRefInventory, ImageBackupStorageRefInventory>() {
            @Override
            public ImageBackupStorageRefInventory call(ImageBackupStorageRefInventory arg) {
                return arg.getBackupStorageUuid().equals(bsUuid) ? arg : null;
            }
        }));

        if (VmOperation.NewCreate == spec.getCurrentVmOperation() && ImageMediaType.ISO.toString().equals(spec.getImageSpec().getInventory().getMediaType())) {
            spec.getDestIso().setBackupStorageUuid(bsUuid);
        } else if (VmOperation.NewCreate != spec.getCurrentVmOperation() && spec.getDestIso() != null) {
            String isoBsUuid = findBackupStorage(spec, spec.getDestIso().getImageUuid());
            spec.getDestIso().setBackupStorageUuid(isoBsUuid);
        }

        trigger.next();
    }
}

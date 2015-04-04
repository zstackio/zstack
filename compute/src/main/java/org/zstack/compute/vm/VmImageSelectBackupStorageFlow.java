package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowTrigger;
import org.zstack.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
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

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        HostInventory host = spec.getDestHost();
        List<String> bsUuids = CollectionUtils.transformToList(spec.getImageSpec().getInventory().getBackupStorageRefs(), new Function<String, ImageBackupStorageRefInventory>() {
            @Override
            public String call(ImageBackupStorageRefInventory arg) {
                return arg.getBackupStorageUuid();
            }
        });

        SimpleQuery<BackupStorageZoneRefVO> q = dbf.createQuery(BackupStorageZoneRefVO.class);
        q.add(BackupStorageZoneRefVO_.backupStorageUuid, Op.IN, bsUuids);
        List<BackupStorageZoneRefVO> refs = q.list();

        Collections.shuffle(refs);

        String bsUuid = null;
        for (BackupStorageZoneRefVO ref : refs) {
            if (ref.getZoneUuid().equals(host.getZoneUuid())) {
                bsUuid = ref.getBackupStorageUuid();
                break;
            }
        }

        DebugUtils.Assert(bsUuid!=null, "how can bsUuid be null???");

        final String finalBsUuid = bsUuid;
        final ImageBackupStorageRefInventory iref = CollectionUtils.find(spec.getImageSpec().getInventory().getBackupStorageRefs(), new Function<ImageBackupStorageRefInventory, ImageBackupStorageRefInventory>() {
            @Override
            public ImageBackupStorageRefInventory call(ImageBackupStorageRefInventory arg) {
                if (arg.getBackupStorageUuid().equals(finalBsUuid)) {
                    return arg;
                }
                return null;
            }
        });

        spec.getImageSpec().setSelectedBackupStorage(iref);
        trigger.next();
    }
}

package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.backup.BackupStorageAllocationSpec;
import org.zstack.header.storage.backup.BackupStorageConstant.AllocatorParams;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.SizeUtils;

import static org.zstack.core.Platform.operr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BackupStorageReservedCapacityAllocatorFlow extends NoRollbackFlow {
    @Autowired
    private ErrorFacade errf;

    @Override
    @Transactional(readOnly = true)
    public void run(FlowTrigger trigger, Map data) {
        BackupStorageAllocationSpec spec = (BackupStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        List<BackupStorageVO> candidates = (List<BackupStorageVO>) data.get(AllocatorParams.CANDIDATES);
        DebugUtils.Assert(candidates != null && !candidates.isEmpty(), "BackupStorageReservedCapacityAllocatorFlow cannot be the first element in the allocator chain");

        List<BackupStorageVO> ret = new ArrayList<BackupStorageVO>();
        long reservedCapacity = SizeUtils.sizeStringToBytes(BackupStorageGlobalConfig.RESERVED_CAPACITY.value());
        for (BackupStorageVO vo : candidates) {
            if (vo.getAvailableCapacity() - reservedCapacity > spec.getSize()) {
                ret.add(vo);
            }
        }

        if (ret.isEmpty()) {
            throw new OperationFailureException(operr("after subtracting reserved capacity[%s], no backup storage has required capacity[%s bytes]",
                            BackupStorageGlobalConfig.RESERVED_CAPACITY.value(), spec.getSize()));
        }

        data.put(AllocatorParams.CANDIDATES, ret);
        trigger.next();
    }
}

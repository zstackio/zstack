package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;

import static org.zstack.core.Platform.operr;

import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/4/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageAvoidAllocatorFlow extends NoRollbackFlow {
    @Autowired
    protected ErrorFacade errf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(AllocatorParams.CANDIDATES);
        final PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        DebugUtils.Assert(candidates != null && !candidates.isEmpty(),
                "PrimaryStorageAvoidAllocatorFlow cannot be the first element in allocator chain");

        if (spec.getAvoidPrimaryStorageUuids() == null || spec.getAvoidPrimaryStorageUuids().isEmpty()) {
            trigger.next();
            return;
        }

        candidates = CollectionUtils.transformToList(candidates, new Function<PrimaryStorageVO, PrimaryStorageVO>() {
            @Override
            public PrimaryStorageVO call(PrimaryStorageVO arg) {
                return spec.getAvoidPrimaryStorageUuids().contains(arg.getUuid()) ? null : arg;
            }
        });

        if (candidates.isEmpty()) {
            throw new OperationFailureException(operr("after removing primary storage%s to avoid," +
                            " there is no candidate primary storage anymore", spec.getAvoidPrimaryStorageUuids()));
        }

        data.put(AllocatorParams.CANDIDATES, candidates);
        trigger.next();
    }
}

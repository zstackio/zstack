package org.zstack.storage.primary;

import org.zstack.core.workflow.FlowChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.FlowDoneHandler;
import org.zstack.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageAllocatorStrategy;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;


class DefaultPrimaryStorageAllocatorStrategy implements PrimaryStorageAllocatorStrategy {
    private FlowChainBuilder builder;

    DefaultPrimaryStorageAllocatorStrategy(FlowChainBuilder builder) {
        this.builder = builder;
    }

    @Override
    public PrimaryStorageInventory allocate(PrimaryStorageAllocationSpec spec) {
        return allocateAllCandidates(spec).get(0);
    }

    @Override
    public List<PrimaryStorageInventory> allocateAllCandidates(PrimaryStorageAllocationSpec spec) {
        List<PrimaryStorageVO> candidates = allocateAll(spec);
        Collections.shuffle(candidates);
        return PrimaryStorageInventory.valueOf(candidates);
    }

    private List<PrimaryStorageVO> allocateAll(PrimaryStorageAllocationSpec spec) {
        class Result {
            List<PrimaryStorageVO> result;
            ErrorCode errorCode;
        }

        final Result ret = new Result();
        FlowChain allocatorChain = builder.build();
        allocatorChain.setName(String.format("allocate-primary-storage-msg-%s", spec.getAllocationMessage().getId()));
        allocatorChain.setData(map(e(AllocatorParams.SPEC, spec)));
        allocatorChain.done(new FlowDoneHandler() {
            @Override
            public void handle(Map data) {
                ret.result = (List<PrimaryStorageVO>) data.get(AllocatorParams.CANDIDATES);
            }
        }).error(new FlowErrorHandler() {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                ret.errorCode = errCode;
            }
        }).start();

        if (ret.errorCode != null) {
            throw new OperationFailureException(ret.errorCode);
        } else {
            return ret.result;
        }
    }
}

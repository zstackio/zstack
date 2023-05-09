package org.zstack.header.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;

import java.util.ArrayList;
import java.util.List;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractHostAllocatorFlow {
    protected List<HostVO> candidates;
    protected HostAllocatorSpec spec;
    private HostAllocatorTrigger trigger;
    protected HostAllocationPaginationInfo paginationInfo;

    public abstract void allocate();

    public List<HostVO> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<HostVO> candidates) {
        this.candidates = candidates;
    }

    public HostAllocatorSpec getSpec() {
        return spec;
    }

    public void setSpec(HostAllocatorSpec spec) {
        this.spec = spec;
    }

    public void setTrigger(HostAllocatorTrigger trigger) {
        this.trigger = trigger;
    }

    public HostAllocationPaginationInfo getPaginationInfo() {
        return paginationInfo;
    }

    public void setPaginationInfo(HostAllocationPaginationInfo paginationInfo) {
        this.paginationInfo = paginationInfo;
    }

    protected void next(List<HostVO> candidates) {
        if (usePagination()) {
            paginationInfo.setOffset(paginationInfo.getOffset() + paginationInfo.getLimit());
        }
        trigger.next(candidates);
    }

    protected void allocatorTriggerFail(ErrorCode errorCode) {
        trigger.fail(errorCode);
    }

    protected void skip() {
        trigger.skip();
    }

    protected void fail(String reason) {
        if (paginationInfo != null && !trigger.isFirstFlow(this)) {
            // in pagination, and a middle flow fails, we can continue
            ErrorCode errorCode = new ErrorCode();
            errorCode.setCode(HostAllocatorConstant.PAGINATION_INTERMEDIATE_ERROR.getCode());
            errorCode.setDetails(reason);
            errorCode.setDescription(HostAllocatorConstant.PAGINATION_INTERMEDIATE_ERROR.getDescription());
            throw new OperationFailureException(errorCode);
        } else {
            // no host found, stop allocating
            ErrorCode errorCode = new ErrorCode();
            errorCode.setCode(HostAllocatorError.NO_AVAILABLE_HOST.toString());
            errorCode.setDetails(reason);
            throw new OperationFailureException(errorCode);
        }
    }

    protected void fail(ErrorCode reason) {
        if (paginationInfo != null && !trigger.isFirstFlow(this)) {
            reason.setCode(HostAllocatorConstant.PAGINATION_INTERMEDIATE_ERROR.getCode());
            reason.setDescription(HostAllocatorConstant.PAGINATION_INTERMEDIATE_ERROR.getDescription());
        } else {
            reason.setCode(HostAllocatorError.NO_AVAILABLE_HOST.toString());
        }
        throw new OperationFailureException(reason);
    }

    protected boolean usePagination() {
        return paginationInfo != null && trigger.isFirstFlow(this);
    }

    protected void throwExceptionIfIAmTheFirstFlow() {
        if (candidates == null || candidates.isEmpty()) {
            throw new CloudRuntimeException(String.format("%s cannot be the first flow in the allocation chain",
                    this.getClass().getName()));
        }
    }

    protected List<String> getHostUuidsFromCandidates() {
        List<String> huuids = new ArrayList<>(candidates.size());
        for (HostVO vo : candidates) {
            huuids.add(vo.getUuid());
        }
        return huuids;
    }

    protected boolean amITheFirstFlow() {
        return candidates == null;
    }
}

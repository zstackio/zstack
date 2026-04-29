package org.zstack.header.vm;

import org.zstack.header.allocator.HostAllocationPurpose;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by weiwang on 04/09/2017
 */
public class GetVmStartingCandidateClustersHostsMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String uuid;
    /**
     * Allocation purpose. Defaults to ALLOCATE so existing callers keep the same
     * filter behavior. Setting to LIST_CANDIDATES tells filters that this is a
     * candidate-listing call and certain restrictions (e.g. PCI device
     * owner-RBAC) may be relaxed.
     *
     * Callers MUST gate this on admin permission before setting LIST_CANDIDATES;
     * filters trust the value as-is.
     */
    private HostAllocationPurpose purpose = HostAllocationPurpose.ALLOCATE;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public HostAllocationPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(HostAllocationPurpose purpose) {
        this.purpose = purpose == null ? HostAllocationPurpose.ALLOCATE : purpose;
    }
}
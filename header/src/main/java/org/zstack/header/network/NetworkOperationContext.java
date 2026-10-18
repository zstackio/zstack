package org.zstack.header.network;

import org.zstack.header.network.l3.SdnDhcpServiceSnapshot;
import org.zstack.header.network.l3.IpRangeInventory;

import java.util.List;
import java.util.ArrayList;

public class NetworkOperationContext {
    private String fenceToken;
    private String operationOrigin;
    private List<IpRangeInventory> addingIpRanges;
    private SdnDhcpServiceSnapshot pendingDhcp;
    private boolean cascadeIpRangeDelete;
    private boolean standaloneIpRangeDelete;
    private String addingDns;
    private String removingDns;
    private String segmentRefUuid;
    private transient boolean remoteWriteSucceeded;
    private transient boolean fenceInherited;
    private transient boolean localCommitSucceeded;
    private transient boolean deletingIpRange;
    private transient boolean deferredTagNeeded;

    public String getFenceToken() {
        return fenceToken;
    }

    public void setFenceToken(String fenceToken) {
        this.fenceToken = fenceToken;
    }

    public String getOperationOrigin() {
        return operationOrigin;
    }

    public void setOperationOrigin(String operationOrigin) {
        this.operationOrigin = operationOrigin;
    }

    public List<IpRangeInventory> getAddingIpRanges() {
        return addingIpRanges;
    }

    public void setAddingIpRanges(List<IpRangeInventory> addingIpRanges) {
        this.addingIpRanges = addingIpRanges;
    }

    public SdnDhcpServiceSnapshot getPendingDhcp() {
        return pendingDhcp;
    }

    public void setPendingDhcp(SdnDhcpServiceSnapshot pendingDhcp) {
        this.pendingDhcp = pendingDhcp;
    }

    public boolean getCascadeIpRangeDelete() {
        return cascadeIpRangeDelete;
    }

    public void setCascadeIpRangeDelete(boolean cascadeIpRangeDelete) {
        this.cascadeIpRangeDelete = cascadeIpRangeDelete;
    }

    public boolean getStandaloneIpRangeDelete() {
        return standaloneIpRangeDelete;
    }

    public void setStandaloneIpRangeDelete(boolean standaloneIpRangeDelete) {
        this.standaloneIpRangeDelete = standaloneIpRangeDelete;
    }

    public String getAddingDns() {
        return addingDns;
    }

    public void setAddingDns(String addingDns) {
        this.addingDns = addingDns;
    }

    public String getRemovingDns() {
        return removingDns;
    }

    public void setRemovingDns(String removingDns) {
        this.removingDns = removingDns;
    }

    public String getSegmentRefUuid() {
        return segmentRefUuid;
    }

    public void setSegmentRefUuid(String segmentRefUuid) {
        this.segmentRefUuid = segmentRefUuid;
    }

    public boolean getRemoteWriteSucceeded() {
        return remoteWriteSucceeded;
    }

    public void setRemoteWriteSucceeded(boolean remoteWriteSucceeded) {
        this.remoteWriteSucceeded = remoteWriteSucceeded;
    }

    public boolean getFenceInherited() {
        return fenceInherited;
    }

    public void setFenceInherited(boolean fenceInherited) {
        this.fenceInherited = fenceInherited;
    }

    public boolean getLocalCommitSucceeded() {
        return localCommitSucceeded;
    }

    public void setLocalCommitSucceeded(boolean localCommitSucceeded) {
        this.localCommitSucceeded = localCommitSucceeded;
    }

    public boolean getDeletingIpRange() {
        return deletingIpRange;
    }

    public void setDeletingIpRange(boolean deletingIpRange) {
        this.deletingIpRange = deletingIpRange;
    }

    public boolean getDeferredTagNeeded() {
        return deferredTagNeeded;
    }

    public void setDeferredTagNeeded(boolean deferredTagNeeded) {
        this.deferredTagNeeded = deferredTagNeeded;
    }
    public NetworkOperationContext copyForRequest() {
        NetworkOperationContext request = new NetworkOperationContext();
        request.setFenceToken(getFenceToken());
        request.setOperationOrigin(getOperationOrigin());
        request.setAddingIpRanges(getAddingIpRanges() == null ? null : new ArrayList<>(getAddingIpRanges()));
        request.setPendingDhcp(getPendingDhcp());
        request.setCascadeIpRangeDelete(getCascadeIpRangeDelete());
        request.setStandaloneIpRangeDelete(getStandaloneIpRangeDelete());
        request.setAddingDns(getAddingDns());
        request.setRemovingDns(getRemovingDns());
        request.setSegmentRefUuid(getSegmentRefUuid());
        return request;
    }
}

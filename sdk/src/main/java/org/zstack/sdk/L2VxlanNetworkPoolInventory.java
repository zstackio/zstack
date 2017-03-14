package org.zstack.sdk;

public class L2VxlanNetworkPoolInventory extends L2NetworkInventory {

    public java.util.Set<VtepVO> attachedVtepRefs;
    public void setAttachedVtepRefs(java.util.Set<VtepVO> attachedVtepRefs) {
        this.attachedVtepRefs = attachedVtepRefs;
    }
    public java.util.Set<VtepVO> getAttachedVtepRefs() {
        return this.attachedVtepRefs;
    }

    public java.util.Set<VxlanNetworkVO> attachedVxlanNetworkRefs;
    public void setAttachedVxlanNetworkRefs(java.util.Set<VxlanNetworkVO> attachedVxlanNetworkRefs) {
        this.attachedVxlanNetworkRefs = attachedVxlanNetworkRefs;
    }
    public java.util.Set<VxlanNetworkVO> getAttachedVxlanNetworkRefs() {
        return this.attachedVxlanNetworkRefs;
    }

    public java.util.Set<VniRangeVO> attachedVniRanges;
    public void setAttachedVniRanges(java.util.Set<VniRangeVO> attachedVniRanges) {
        this.attachedVniRanges = attachedVniRanges;
    }
    public java.util.Set<VniRangeVO> getAttachedVniRanges() {
        return this.attachedVniRanges;
    }

    public java.util.Map<String, String> attachedCidrs;
    public void setAttachedCidrs(java.util.Map<String, String> attachedCidrs) {
        this.attachedCidrs = attachedCidrs;
    }
    public java.util.Map<String, String> getAttachedCidrs() {
        return this.attachedCidrs;
    }

}

package org.zstack.sdk;

public class L2VxlanNetworkPoolInventory extends L2NetworkInventory {

    public java.util.List<VtepInventory> attachedVtepRefs;
    public void setAttachedVtepRefs(java.util.List<VtepInventory> attachedVtepRefs) {
        this.attachedVtepRefs = attachedVtepRefs;
    }
    public java.util.List<VtepInventory> getAttachedVtepRefs() {
        return this.attachedVtepRefs;
    }

    public java.util.List<L2VxlanNetworkInventory> attachedVxlanNetworkRefs;
    public void setAttachedVxlanNetworkRefs(java.util.List<L2VxlanNetworkInventory> attachedVxlanNetworkRefs) {
        this.attachedVxlanNetworkRefs = attachedVxlanNetworkRefs;
    }
    public java.util.List<L2VxlanNetworkInventory> getAttachedVxlanNetworkRefs() {
        return this.attachedVxlanNetworkRefs;
    }

    public java.util.List<VniRangeInventory> attachedVniRanges;
    public void setAttachedVniRanges(java.util.List<VniRangeInventory> attachedVniRanges) {
        this.attachedVniRanges = attachedVniRanges;
    }
    public java.util.List<VniRangeInventory> getAttachedVniRanges() {
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

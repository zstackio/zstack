package org.zstack.kvm.memory;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventory", "warnings", "warningDetails", "hostUuids", "hostResults"})
public class APIPreviewMemoryPolicyReply extends APIReply {
    private MemoryPolicyInventory inventory;
    public MemoryPolicyInventory getInventory() { return inventory; }
    public void setInventory(MemoryPolicyInventory value) { inventory = value; }

    private java.util.List<String> warnings;
    public java.util.List<String> getWarnings() { return warnings; }
    public void setWarnings(java.util.List<String> value) { warnings = value; }

    private java.util.List<MemoryPreviewWarningInventory> warningDetails;
    public java.util.List<MemoryPreviewWarningInventory> getWarningDetails() { return warningDetails; }
    public void setWarningDetails(java.util.List<MemoryPreviewWarningInventory> value) { warningDetails = value; }

    private java.util.List<String> hostUuids;
    public java.util.List<String> getHostUuids() { return hostUuids; }
    public void setHostUuids(java.util.List<String> value) { hostUuids = value; }

    private java.util.List<MemoryHostPolicyPreviewInventory> hostResults;
    public java.util.List<MemoryHostPolicyPreviewInventory> getHostResults() { return hostResults; }
    public void setHostResults(java.util.List<MemoryHostPolicyPreviewInventory> value) { hostResults = value; }

    public static APIPreviewMemoryPolicyReply __example__() {
        APIPreviewMemoryPolicyReply reply = new APIPreviewMemoryPolicyReply();
        reply.setInventory(MemoryPolicyInventory.__example__());
        reply.setWarnings(java.util.Collections.<String>emptyList());
        reply.setWarningDetails(java.util.Collections.<MemoryPreviewWarningInventory>emptyList());
        reply.setHostUuids(java.util.Collections.singletonList("1234567890abcdef1234567890abcdef"));
        reply.setHostResults(java.util.Collections.singletonList(MemoryHostPolicyPreviewInventory.__example__()));
        return reply;
    }
}

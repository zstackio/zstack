package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APICheckMemorySnapshotGroupConflictReply extends APIReply {
    private List<VmNicConflictEntry> vmNicConflict;

    public List<VmNicConflictEntry> getVmNicConflict() {
        return vmNicConflict;
    }

    public void setVmNicConflict(List<VmNicConflictEntry> vmNicConflict) {
        this.vmNicConflict = vmNicConflict;
    }

    public static APICheckMemorySnapshotGroupConflictReply __example__() {
        APICheckMemorySnapshotGroupConflictReply reply = new APICheckMemorySnapshotGroupConflictReply();
        VmNicConflictEntry inv = new VmNicConflictEntry();
        inv.setIp("127.0.0.1");
        inv.setMac("00:16:3e:00:00:01");
        inv.setVmInstanceName("vmInstanceName");
        inv.setVmInstanceUuid(uuid());
        inv.setVmNicName("vmNicName");
        reply.setVmNicConflict(Collections.singletonList(inv));
        return reply;
    }
}

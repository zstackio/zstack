package org.zstack.compute.vm;

import org.zstack.tag.SystemTagCreator;

import java.util.Collections;

public class VmPortsHelper {
    public void setVmSyncPorts(String vmUuid) {
        SystemTagCreator creator = VmSystemTags.SYNC_PORTS.newSystemTagCreator(vmUuid);
        creator.recreate = true;
        creator.setTagByTokens(Collections.singletonMap(VmSystemTags.SYNC_PORTS_TOKEN, vmUuid));
        creator.create();
    }

    // If the VM has this system tag,
    // when both the QGA status and the tool status are available,
    // or when synchronizing the internal IP of the virtual machine,
    // it will trigger the operation to synchronize the database IP to the VM.
    public boolean vmNeedSyncPorts(String vmUuid) {
        return VmSystemTags.SYNC_PORTS.hasTag(vmUuid);
    }

    public void afterVmSyncPorts(String vmUuid) {
        VmSystemTags.SYNC_PORTS.delete(vmUuid);
    }

}

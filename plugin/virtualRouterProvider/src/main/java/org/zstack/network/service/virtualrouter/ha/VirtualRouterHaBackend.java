package org.zstack.network.service.virtualrouter.ha;

import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.network.service.VirtualRouterHaCallbackInterface;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

import java.util.Map;

public interface VirtualRouterHaBackend {
    NoRollbackFlow getAttachL3NetworkFlow();
    void detachL3NetworkFromVirtualRouterHaGroup(String vrUuid, String l3NetworkUuid, boolean isRollback, Completion completion);
    void submitVirutalRouterHaTask(Map<String, Object> data, Completion completion);
    boolean isSnatDisabledOnRouter(String vrUuid);
    void cleanupHaNetworkService(VmInstanceInventory vrInv, Completion completion);
    String getVirutalRouterHaName(String vrUuid);
    String getVirutalRouterHaUuid(String vrUuid);
    VirtualRouterHaCallbackInterface getCallback(String type);
}

package org.zstack.compute.host;

import org.zstack.header.host.HostBaseExtensionFactory;
import org.zstack.header.host.HypervisorFactory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.Message;

import java.util.List;

public interface HostManager {
    HypervisorFactory getHypervisorFactory(HypervisorType type);

    void handleMessage(Message msg);

    HostBaseExtensionFactory getHostBaseExtensionFactory(Message msg);

    List<String> getHostUuidsByHypervisorType(String hypervisorType);

    String findConnectedEnabledHost(String hypervisorType, List<String> clusterUuids, List<String> hostUuids);
}

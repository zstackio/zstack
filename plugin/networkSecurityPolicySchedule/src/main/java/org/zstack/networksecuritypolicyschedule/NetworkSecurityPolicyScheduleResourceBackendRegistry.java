package org.zstack.networksecuritypolicyschedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NetworkSecurityPolicyScheduleResourceBackendRegistry implements Component {
    @Autowired
    private PluginRegistry pluginRegistry;

    private final Map<String, NetworkSecurityPolicyScheduleResourceBackend> backends = new LinkedHashMap<>();

    public NetworkSecurityPolicyScheduleResourceBackend getBackend(String resourceType) {
        return backends.get(resourceType);
    }

    public Collection<NetworkSecurityPolicyScheduleResourceBackend> getBackends() {
        return Collections.unmodifiableCollection(backends.values());
    }

    @Override
    public boolean start() {
        for (NetworkSecurityPolicyScheduleResourceBackend backend :
                pluginRegistry.getExtensionList(NetworkSecurityPolicyScheduleResourceBackend.class)) {
            NetworkSecurityPolicyScheduleResourceBackend old = backends.putIfAbsent(
                    backend.getResourceType(), backend);
            if (old != null) {
                throw new CloudRuntimeException(String.format(
                        "duplicate NetworkSecurityPolicyScheduleResourceBackend[%s, %s] for resourceType[%s]",
                        old.getClass().getName(), backend.getClass().getName(), backend.getResourceType()));
            }
        }
        return true;
    }

    @Override
    public boolean stop() {
        backends.clear();
        return true;
    }
}

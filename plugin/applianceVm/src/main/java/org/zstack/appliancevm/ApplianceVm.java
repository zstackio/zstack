package org.zstack.appliancevm;

import org.zstack.header.vm.VmInstance;

import java.util.List;

/**
 */
public interface ApplianceVm extends VmInstance {
    Boolean getSnatStateOnRouter(String vrUuid);
    List<String> getSnatL3NetworkOnRouter(String vrUuid);

    void setSnatStateOnRouter(String uuid, String defaultRouteL3NetworkUuid, Boolean enable);
}

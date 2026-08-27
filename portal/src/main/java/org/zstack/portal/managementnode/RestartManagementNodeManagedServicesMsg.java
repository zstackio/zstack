package org.zstack.portal.managementnode;

import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.List;

public class RestartManagementNodeManagedServicesMsg extends NeedReplyMessage {
    private String serverUuid;
    private boolean includeAuxiliaryServices;
    private List<String> serviceNames = new ArrayList<>();

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public boolean isIncludeAuxiliaryServices() {
        return includeAuxiliaryServices;
    }

    public void setIncludeAuxiliaryServices(boolean includeAuxiliaryServices) {
        this.includeAuxiliaryServices = includeAuxiliaryServices;
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }

    public void setServiceNames(List<String> serviceNames) {
        this.serviceNames = serviceNames;
    }
}

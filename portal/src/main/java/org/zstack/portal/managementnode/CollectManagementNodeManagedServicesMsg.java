package org.zstack.portal.managementnode;

import org.zstack.header.message.NeedReplyMessage;

public class CollectManagementNodeManagedServicesMsg extends NeedReplyMessage {
    private String serverUuid;
    private boolean includeAuxiliaryServices;

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
}

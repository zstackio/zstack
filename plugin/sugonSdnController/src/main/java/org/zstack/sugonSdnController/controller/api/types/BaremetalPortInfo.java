//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class BaremetalPortInfo extends ApiPropertyBase {
    Boolean pxe_enabled;
    LocalLinkConnection local_link_connection;
    String node_uuid;
    String address;
    public BaremetalPortInfo() {
    }
    public BaremetalPortInfo(Boolean pxe_enabled, LocalLinkConnection local_link_connection, String node_uuid, String address) {
        this.pxe_enabled = pxe_enabled;
        this.local_link_connection = local_link_connection;
        this.node_uuid = node_uuid;
        this.address = address;
    }
    public BaremetalPortInfo(Boolean pxe_enabled) {
        this(pxe_enabled, null, null, null);    }
    public BaremetalPortInfo(Boolean pxe_enabled, LocalLinkConnection local_link_connection) {
        this(pxe_enabled, local_link_connection, null, null);    }
    public BaremetalPortInfo(Boolean pxe_enabled, LocalLinkConnection local_link_connection, String node_uuid) {
        this(pxe_enabled, local_link_connection, node_uuid, null);    }
    
    public Boolean getPxeEnabled() {
        return pxe_enabled;
    }
    
    public void setPxeEnabled(Boolean pxe_enabled) {
        this.pxe_enabled = pxe_enabled;
    }
    
    
    public LocalLinkConnection getLocalLinkConnection() {
        return local_link_connection;
    }
    
    public void setLocalLinkConnection(LocalLinkConnection local_link_connection) {
        this.local_link_connection = local_link_connection;
    }
    
    
    public String getNodeUuid() {
        return node_uuid;
    }
    
    public void setNodeUuid(String node_uuid) {
        this.node_uuid = node_uuid;
    }
    
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
}

package org.zstack.header.storage.backup;

import org.zstack.utils.network.EndpointAddressFamilyUtils;

import java.io.Serializable;

public class BackupStorageEndpointCandidate implements Serializable {
    public static final String ROLE_STORAGE_IMAGE_TRANSFER = "storage-image-transfer/copyTarget";
    public static final String SOURCE_CONFIGURED_HOSTNAME = "configuredHostname";
    public static final String SOURCE_CONFIGURED_IPV6_ENDPOINT = "configuredIpv6Endpoint";
    public static final String SOURCE_BACKUP_STORAGE_DATA_NETWORK = "backupStorageDataNetwork";
    public static final String SOURCE_IMAGE_STORE_BACKUP_NETWORK = "imageStoreBackupNetwork";
    public static final String SOURCE_SYNC_NETWORK = "syncNetwork";

    private String address;
    private String addressFamily;
    private String source;
    private String role;
    private String protocol;
    private Integer port;
    private boolean primary;

    public static BackupStorageEndpointCandidate copyTarget(String address, String addressFamily, String source,
                                                            String protocol, Integer port, boolean primary) {
        BackupStorageEndpointCandidate candidate = new BackupStorageEndpointCandidate();
        candidate.setAddress(address);
        candidate.setAddressFamily(addressFamily == null ?
                EndpointAddressFamilyUtils.getEndpointAddressFamily(address) : addressFamily);
        candidate.setSource(source);
        candidate.setRole(ROLE_STORAGE_IMAGE_TRANSFER);
        candidate.setProtocol(protocol);
        candidate.setPort(port);
        candidate.setPrimary(primary);
        return candidate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressFamily() {
        return addressFamily;
    }

    public void setAddressFamily(String addressFamily) {
        this.addressFamily = addressFamily;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}

package org.zstack.header.storage.backup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.rest.SDK;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@SDK
@PythonClassInventory
public class StorageTrash {
    private String resourceUuid;
    private String resourceType;
    private String storageUuid;
    private String storageType;
    private String installPath;
    private String hypervisorType;
    private Long size;

    public StorageTrash() {
    }

    public StorageTrash(String resourceUuid, String resourceType, String storageUuid, String storageType, String installPath, Long size) {
        this.resourceUuid = resourceUuid;
        this.resourceType = resourceType;
        this.storageUuid = storageUuid;
        this.storageType = storageType;
        this.installPath = installPath;
        this.size = size;
    }

    public StorageTrash(String resourceUuid, String resourceType, String storageUuid, String storageType, String installPath, String hypervisorType, Long size) {
        this.resourceUuid = resourceUuid;
        this.resourceType = resourceType;
        this.storageUuid = storageUuid;
        this.storageType = storageType;
        this.installPath = installPath;
        this.size = size;
        this.hypervisorType = hypervisorType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getStorageUuid() {
        return storageUuid;
    }

    public void setStorageUuid(String storageUuid) {
        this.storageUuid = storageUuid;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
}

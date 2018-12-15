package org.zstack.header.storage.backup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.rest.SDK;

import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@SDK
@PythonClassInventory
public class StorageTrashSpec {
    private Long id; // for delete one record
    private String resourceUuid;
    private String resourceType;
    private String storageUuid;
    private String storageType;
    private String installPath;
    private boolean isFolder = false;
    private String hypervisorType;
    private Long size;
    private Timestamp createDate;

    public StorageTrashSpec(String resourceUuid, String storageUuid, String installPath, Long size) {
        this.resourceUuid = resourceUuid;
        this.storageUuid = storageUuid;
        this.installPath = installPath;
        this.size = size;
    }

    public StorageTrashSpec(String resourceUuid, String resourceType, String storageUuid, String storageType, String installPath, Long size) {
        this.resourceUuid = resourceUuid;
        this.resourceType = resourceType;
        this.storageUuid = storageUuid;
        this.storageType = storageType;
        this.installPath = installPath;
        this.size = size;
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

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}

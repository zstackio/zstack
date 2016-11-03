package org.zstack.header.volume;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.APICreateVmInstanceMsg;

@ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
public class CreateVolumeMsg extends NeedReplyMessage {
    private long size;
    private String primaryStorageUuid;
    private String vmInstanceUuid;
    private String volumeType;
    private String name;
    private String description;
    private String accountUuid;
    private String rootImageUuid;
    private String diskOfferingUuid;
    private String format;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public void setDiskOfferingUuid(String diskOfferingUuid) {
        this.diskOfferingUuid = diskOfferingUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getRootImageUuid() {
        return rootImageUuid;
    }

    public void setRootImageUuid(String rootImageUuid) {
        this.rootImageUuid = rootImageUuid;
    }
}

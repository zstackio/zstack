package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIScanVmInstanceMetadataFromPrimaryStorageEvent extends APIEvent {
    private List<VmMetadataScanEntry> vmInstanceMetadata = new ArrayList<>();

    public APIScanVmInstanceMetadataFromPrimaryStorageEvent() {
        super(null);
    }

    public APIScanVmInstanceMetadataFromPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public List<VmMetadataScanEntry> getVmInstanceMetadata() {
        return vmInstanceMetadata;
    }

    public void setVmInstanceMetadata(List<VmMetadataScanEntry> vmInstanceMetadata) {
        this.vmInstanceMetadata = vmInstanceMetadata == null ? new ArrayList<>() : vmInstanceMetadata;
    }

    public static APIScanVmInstanceMetadataFromPrimaryStorageEvent __example__() {
        APIScanVmInstanceMetadataFromPrimaryStorageEvent evt = new APIScanVmInstanceMetadataFromPrimaryStorageEvent();
        return evt;
    }
}

package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.concurrent.TimeUnit;

/**
 * Created by MaJin on 2019/7/9.
 */
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volume-snapshots/group",
        method = HttpMethod.POST,
        responseClass = APICreateVolumeSnapshotGroupEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APICreateVolumeSnapshotGroupMsg extends APICreateMessage implements VolumeMessage {
    /**
     * @desc root volume uuid. See :ref:`VolumeInventory`
     */
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String rootVolumeUuid;
    /**
     * @desc snapshot group name. Max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc snapshot group description. Max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false)
    private boolean withMemory = false;

    @APINoSee
    private VmInstanceInventory vmInstance;

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
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

    public SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.FILE_CREATION;
    }

    @Override
    public String getVolumeUuid() {
        return rootVolumeUuid;
    }

    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    public boolean isWithMemory() {
        return withMemory;
    }

    public void setWithMemory(boolean withMemory) {
        this.withMemory = withMemory;
    }

    public static APICreateVolumeSnapshotGroupMsg __example__() {
        APICreateVolumeSnapshotGroupMsg result = new APICreateVolumeSnapshotGroupMsg();
        result.name = "test";
        result.rootVolumeUuid = uuid();
        return result;
    }
}

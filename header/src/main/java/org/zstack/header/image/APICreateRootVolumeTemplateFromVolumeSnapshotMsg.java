package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/images/root-volume-templates/from/volume-snapshots/{snapshotUuid}",
        responseClass = APICreateRootVolumeTemplateFromVolumeSnapshotEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APICreateRootVolumeTemplateFromVolumeSnapshotMsg extends APICreateMessage implements APIAuditor {
    @APIParam(resourceType = VolumeSnapshotVO.class, checkAccount = true, operationTarget = true)
    private String snapshotUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    private String guestOsType;
    @APIParam(nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;
    @APIParam(required = false, validValues = {"Linux", "Windows", "Other", "Paravirtualization", "WindowsVirtio"})
    private String platform;
    private boolean system;

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
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

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }
 
    public static APICreateRootVolumeTemplateFromVolumeSnapshotMsg __example__() {
        APICreateRootVolumeTemplateFromVolumeSnapshotMsg msg = new APICreateRootVolumeTemplateFromVolumeSnapshotMsg();

        msg.setName("My Root Volume Template");
        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setSnapshotUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateRootVolumeTemplateFromVolumeSnapshotEvent)rsp).getInventory().getUuid() : "", ImageVO.class);
    }
}

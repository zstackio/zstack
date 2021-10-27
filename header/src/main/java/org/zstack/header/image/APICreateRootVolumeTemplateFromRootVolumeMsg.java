package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.volume.VolumeVO;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/images/root-volume-templates/from/volumes/{rootVolumeUuid}",
        method = HttpMethod.POST,
        responseClass = APICreateRootVolumeTemplateFromRootVolumeEvent.class,
        parameterName = "params"
)
@TagResourceType(ImageVO.class)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APICreateRootVolumeTemplateFromRootVolumeMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    private String guestOsType;
    @APIParam(required = false, nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String rootVolumeUuid;
    @APIParam(required = false, validValues = {"Linux", "Windows", "Other", "Paravirtualization", "WindowsVirtio"})
    private String platform;
    private boolean system;
    private String architecture;

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

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
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

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
 
    public static APICreateRootVolumeTemplateFromRootVolumeMsg __example__() {
        APICreateRootVolumeTemplateFromRootVolumeMsg msg = new APICreateRootVolumeTemplateFromRootVolumeMsg();

        msg.setName("My Root Volume Template");
        msg.setPlatform(ImagePlatform.Linux.toString());
        msg.setRootVolumeUuid(uuid());
        msg.setBackupStorageUuids(Collections.singletonList(uuid()));

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateRootVolumeTemplateFromRootVolumeEvent)rsp).getInventory().getUuid() : "", ImageVO.class);
    }
}

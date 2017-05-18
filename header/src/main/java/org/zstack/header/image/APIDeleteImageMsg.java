package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.Collections;
import java.util.List;

@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/images/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteImageEvent.class
)
public class APIDeleteImageMsg extends APIDeleteMessage implements ImageMessage {
    @APIParam(resourceType = ImageVO.class, successIfResourceNotExisting = true, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(required = false, nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public APIDeleteImageMsg() {
        super();
    }

    public APIDeleteImageMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    @Override
    public String getImageUuid() {
        return uuid;
    }
 
    public static APIDeleteImageMsg __example__() {
        APIDeleteImageMsg msg = new APIDeleteImageMsg();

        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleted").resource(uuid, ImageVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}

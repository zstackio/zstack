package org.zstack.storage.primary.local;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeVO;

/**
 * Created by frank on 11/16/2015.
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/primary-storage/local-storage/volumes/{volumeUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APILocalStorageMigrateVolumeEvent.class,
        isAction = true
)
public class APILocalStorageMigrateVolumeMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String volumeUuid;
    @APIParam(resourceType = HostVO.class)
    private String destHostUuid;
    @APINoSee
    private String primaryStorageUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
 
    public static APILocalStorageMigrateVolumeMsg __example__() {
        APILocalStorageMigrateVolumeMsg msg = new APILocalStorageMigrateVolumeMsg();

        msg.setVolumeUuid(uuid());
        msg.setDestHostUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Migrated to host[uuid:%s] on local primary storage[uuid:%s] ", destHostUuid, primaryStorageUuid)
                        .resource(volumeUuid, VolumeVO.class.getSimpleName())
                        .context("primaryStorageUuid", primaryStorageUuid)
                        .context("destHostUuid", destHostUuid)
                        .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}

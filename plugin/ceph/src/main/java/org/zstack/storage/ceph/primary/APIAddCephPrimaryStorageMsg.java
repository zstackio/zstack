package org.zstack.storage.ceph.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.*;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.storage.ceph.CephConstants;

import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@OverriddenApiParams({
        @OverriddenApiParam(field = "url", param = @APIParam(maxLength = 2048, required = false))
})
@TagResourceType(PrimaryStorageVO.class)
@RestRequest(
        path = "/primary-storage/ceph",
        method = HttpMethod.POST,
        responseClass = APIAddPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddCephPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @APIParam(nonempty = false, emptyString = false)
    private List<String> monUrls;
    @APIParam(required = false, maxLength = 255)
    private String rootVolumePoolName;
    @APIParam(required = false, maxLength = 255)
    private String dataVolumePoolName;
    @APIParam(required = false, maxLength = 255)
    private String imageCachePoolName;

    public String getUrl() {
        return "not used";
    }

    public String getRootVolumePoolName() {
        return rootVolumePoolName;
    }

    public void setRootVolumePoolName(String rootVolumePoolName) {
        this.rootVolumePoolName = rootVolumePoolName;
    }

    public String getDataVolumePoolName() {
        return dataVolumePoolName;
    }

    public void setDataVolumePoolName(String dataVolumePoolName) {
        this.dataVolumePoolName = dataVolumePoolName;
    }

    public String getImageCachePoolName() {
        return imageCachePoolName;
    }

    public void setImageCachePoolName(String imageCachePoolName) {
        this.imageCachePoolName = imageCachePoolName;
    }

    @Override
    public String getType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    public List<String> getMonUrls() {
        return monUrls;
    }

    public void setMonUrls(List<String> monUrls) {
        this.monUrls = monUrls;
    }
 
    public static APIAddCephPrimaryStorageMsg __example__() {
        APIAddCephPrimaryStorageMsg msg = new APIAddCephPrimaryStorageMsg();

        msg.setName("My Ceph Primary Storage");
        msg.setMonUrls(Collections.singletonList("root:password@localhost/?monPort=7777"));
        msg.setRootVolumePoolName("zs-images");
        msg.setDataVolumePoolName("zs-data-volume");
        msg.setImageCachePoolName("zs-image-cache");
        msg.setZoneUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;
        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Added").resource(((APIAddPrimaryStorageEvent)evt).getInventory().getUuid(), PrimaryStorageVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}

package org.zstack.storage.surfs.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.storage.surfs.SurfsConstants;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by zhouhaiping 2017-09-11
 */
@OverriddenApiParams({
        @OverriddenApiParam(field = "url", param = @APIParam(maxLength = 2048, required = false))
})
@RestRequest(
        path = "/primary-storage/surfs",
        method = HttpMethod.POST,
        responseClass = APIAddPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddSurfsPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @APIParam(nonempty = false, emptyString = false)
    private List<String> nodeUrls;
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
        return SurfsConstants.SURFS_PRIMARY_STORAGE_TYPE;
    }

    public List<String> getNodeUrls() {
        return nodeUrls;
    }

    public void setNodeUrls(List<String> nodeUrls) {
        this.nodeUrls = nodeUrls;
    }
 
    public static APIAddSurfsPrimaryStorageMsg __example__() {
        APIAddSurfsPrimaryStorageMsg msg = new APIAddSurfsPrimaryStorageMsg();
        msg.setNodeUrls(asList("root:password@localhost/?monPort=7777"));
        msg.setName("surfs");
        msg.setZoneUuid(uuid());
        return msg;
    }

}

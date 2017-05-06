package org.zstack.storage.fusionstor.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.storage.fusionstor.FusionstorConstants;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by frank on 7/28/2015.
 */
@OverriddenApiParams({
        @OverriddenApiParam(field = "url", param = @APIParam(maxLength = 2048, required = false))
})
@RestRequest(
        path = "/primary-storage/fusionstor",
        method = HttpMethod.POST,
        responseClass = APIAddPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddFusionstorPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
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
        return FusionstorConstants.FUSIONSTOR_PRIMARY_STORAGE_TYPE;
    }

    public List<String> getMonUrls() {
        return monUrls;
    }

    public void setMonUrls(List<String> monUrls) {
        this.monUrls = monUrls;
    }
 
    public static APIAddFusionstorPrimaryStorageMsg __example__() {
        APIAddFusionstorPrimaryStorageMsg msg = new APIAddFusionstorPrimaryStorageMsg();
        msg.setMonUrls(asList("root:password@localhost/?monPort=7777"));
        msg.setName("fusion");
        msg.setZoneUuid(uuid());
        return msg;
    }

}

package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

/**
 * Created by xing5 on 2016/8/23.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/images-l3networks/dependencies",
        method = HttpMethod.GET,
        responseClass = APIGetInterdependentL3NetworkImageReply.class,
        parameterName = "params"
)
public class APIGetInterdependentL3NetworksImagesMsg extends APISyncCallMessage {
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;
    @APIParam(required = false, nonempty = true, resourceType = L3NetworkVO.class)
    private List<String> l3NetworkUuids;
    @APIParam(required = false, resourceType = ImageVO.class)
    private String imageUuid;

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}

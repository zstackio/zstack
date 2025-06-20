package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(ImageVO.class)
@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/image/clone/{imageUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICloneImageEvent.class
)
public class APICloneImageMsg extends APICreateMessage implements APIAuditor {
    @APIParam(resourceType = ImageVO.class, noOwnerCheck = true)
    private String imageUuid;

    @APIParam(required = false, validValues = {"DatabaseOnly"})
    private String strategy = ImageCloneStrategy.DatabaseOnly.toString();

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}

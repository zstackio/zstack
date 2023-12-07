package org.zstack.header.tag;
import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

import java.util.List;

@Action(category = TagConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/system-tags/{resourceUuid}/tags",
        method = HttpMethod.POST,
        responseClass = APICreateSystemTagsEvent.class,
        parameterName = "params"
)
public class APICreateSystemTagsMsg extends APIMessage {
    @APIParam
    private String resourceType;
    @APIParam(checkAccount = true, resourceType = ResourceVO.class)
    private String resourceUuid;
    @APIParam(nonempty = true)
    @NoLogging(type = NoLogging.Type.Tag)
    private List<String> tags;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

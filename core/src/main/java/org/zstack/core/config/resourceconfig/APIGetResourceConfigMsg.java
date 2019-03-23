package org.zstack.core.config.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

/**
 * Created by MaJin on 2019/2/23.
 */

@RestRequest(path = "/resource-configurations/{resourceUuid}/{category}/{name}",
        method = HttpMethod.GET, responseClass = APIGetResourceConfigReply.class)
public class APIGetResourceConfigMsg extends APISyncCallMessage implements ResourceConfigMessage {
    @APIParam
    private String category;
    @APIParam
    private String name;
    @APIParam(resourceType = ResourceVO.class, checkAccount = true)
    private String resourceUuid;

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}

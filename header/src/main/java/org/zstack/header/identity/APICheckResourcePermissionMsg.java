package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.NoDoc;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by kayo on 2018/8/3.
 */
@Action(category = AccountConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/accounts/resource/api-permissions",
        method = HttpMethod.GET,
        responseClass = APICheckResourcePermissionReply.class)
@NoDoc
public class APICheckResourcePermissionMsg extends APISyncCallMessage {
    @APIParam
    private String resourceType;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}

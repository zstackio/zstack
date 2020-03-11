package org.zstack.network.service.header.acl;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
@Action(category = AccessControlListConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/access-control-lists/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAccessControlListEvent.class
)
public class APIDeleteAccessControlListMsg extends APIDeleteMessage {
    @APIParam(resourceType = AccessControlListVO.class, successIfResourceNotExisting = true, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteAccessControlListMsg __example__() {
        APIDeleteAccessControlListMsg msg = new APIDeleteAccessControlListMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

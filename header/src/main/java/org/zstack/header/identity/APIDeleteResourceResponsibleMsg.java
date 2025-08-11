package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/9/2015.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/resources/responsible/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteResourceResponsibleEvent.class
)
public class APIDeleteResourceResponsibleMsg extends APIDeleteMessage implements AccountMessage {
    @APIParam(resourceType = ResourceResponsibleVO.class, successIfResourceNotExisting = true)
    private String uuid;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }
 
    public static APIDeleteResourceResponsibleMsg __example__() {
        APIDeleteResourceResponsibleMsg msg = new APIDeleteResourceResponsibleMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
